import re
import json
import numpy as np
from typing import List, Dict, Optional, Tuple
from dataclasses import dataclass
from rank_bm25 import BM25Okapi
from sentence_transformers import SentenceTransformer
import jieba


@dataclass
class RetrievedChunk:
    chunk_id: str
    text: str
    level: str
    level_num: int
    parent_id: Optional[str]
    sibling_order: int
    score_dense: float = 0.0
    score_sparse: float = 0.0
    score_combined: float = 0.0
    score_reranked: float = 0.0


class QueryRewriter:
    """Expand legal queries with synonym and abbreviation handling."""

    # Common legal term mappings
    LEGAL_SYNONYMS = {
        "合同法": ["民法典合同编", "合同法律法规"],
        "侵权": ["侵权行为", "侵权责任"],
        "继承": ["遗产继承", "继承权"],
        "婚姻": ["婚姻家庭", "婚姻法"],
        "诉讼": ["打官司", "民事诉讼", "诉讼程序"],
        "证据": ["举证", "证据材料"],
        "合同": ["协议", "合约"],
        "违约": ["违反合同", "合同违约"],
        "赔偿": ["损害赔偿", "赔偿金"],
        "管辖": ["管辖权", "管辖法院"],
        "时效": ["诉讼时效", "期限"],
        "法人": ["法定代表人", "法人组织"],
        "自然人": ["公民", "个人"],
        "物权": ["财产权", "所有权"],
        "担保": ["抵押", "质押", "保证"],
        "债权": ["债权人", "债务"],
        "仲裁": ["仲裁协议", "仲裁条款"],
        "上诉": ["二审", "上诉程序"],
        "再审": ["审判监督", "再审程序"],
        "执行": ["强制执行", "执行程序"],
    }

    def rewrite(self, query: str) -> str:
        """Expand query with synonyms for better recall."""
        expanded = query
        for term, synonyms in self.LEGAL_SYNONYMS.items():
            if term in query:
                expanded += " " + " ".join(synonyms)
        return expanded


class HybridRetriever:
    """Hybrid dense + sparse retrieval with hierarchical reranking."""

    def __init__(
        self,
        embedding_model_name: str = "BAAI/bge-small-zh-v1.5",
        dense_weight: float = 0.5,
        sparse_weight: float = 0.5,
        top_k: int = 20,
        rerank_top_k: int = 5,
        device: str = "cpu",
    ):
        self.dense_weight = dense_weight
        self.sparse_weight = sparse_weight
        self.top_k = top_k
        self.rerank_top_k = rerank_top_k
        self.device = device

        print(f"[Retriever] Loading embedding model: {embedding_model_name}")
        self.embedder = SentenceTransformer(embedding_model_name, device=device)
        self.rewriter = QueryRewriter()

        # Data
        self.chunks: List[Dict] = []
        self.chunk_texts: List[str] = []
        self.embeddings: Optional[np.ndarray] = None
        self.bm25: Optional[BM25Okapi] = None

    def index_chunks(self, chunks: List[Dict]):
        """Build dense and sparse indexes from chunk list."""
        self.chunks = chunks
        self.chunk_texts = [c["text"] for c in chunks]

        print(f"[Retriever] Indexing {len(chunks)} chunks...")

        # Dense embeddings
        self.embeddings = self.embedder.encode(
            self.chunk_texts, show_progress_bar=True, normalize_embeddings=True
        )

        # Sparse BM25 index with Chinese tokenization
        tokenized = [list(jieba.cut(t)) for t in self.chunk_texts]
        self.bm25 = BM25Okapi(tokenized)

        print(f"[Retriever] Indexing complete. Shape: {self.embeddings.shape}")

    def _dense_search(self, query: str, top_k: int) -> List[Tuple[int, float]]:
        """Dense retrieval via cosine similarity."""
        q_emb = self.embedder.encode([query], normalize_embeddings=True)[0]
        scores = np.dot(self.embeddings, q_emb)
        top_indices = np.argsort(scores)[::-1][:top_k]
        return [(int(i), float(scores[i])) for i in top_indices]

    def _sparse_search(self, query: str, top_k: int) -> List[Tuple[int, float]]:
        """Sparse retrieval via BM25."""
        tokenized = list(jieba.cut(query))
        scores = self.bm25.get_scores(tokenized)
        top_indices = np.argsort(scores)[::-1][:top_k]
        return [(int(i), float(scores[i])) for i in top_indices]

    def _hierarchical_rerank(
        self, results: List[RetrievedChunk], query: str
    ) -> List[RetrievedChunk]:
        """Rerank considering hierarchical structure.
        Prefer chunks whose parent/sibling context is also relevant.
        """
        if not results:
            return results

        # Build parent lookup
        parent_map: Dict[str, List[RetrievedChunk]] = {}
        for r in results:
            if r.parent_id:
                parent_map.setdefault(r.parent_id, []).append(r)

        # Boost score if sibling chunks from same parent are also retrieved
        for r in results:
            boost = 1.0
            if r.parent_id and r.parent_id in parent_map:
                siblings = parent_map[r.parent_id]
                # More siblings retrieved = stronger contextual signal
                sibling_boost = min(len(siblings) / 3.0, 1.0) * 0.15
                boost += sibling_boost
            # Slight boost for article-level (most important in legal docs)
            if r.level == "article":
                boost += 0.05
            r.score_reranked = r.score_combined * boost

        results.sort(key=lambda x: x.score_reranked, reverse=True)
        return results

    def retrieve(self, query: str) -> List[RetrievedChunk]:
        """Full retrieval pipeline: rewrite -> hybrid search -> rerank."""
        # Step 1: Query rewriting
        expanded_query = self.rewriter.rewrite(query)

        # Step 2: Hybrid search
        dense_results = self._dense_search(expanded_query, self.top_k)
        sparse_results = self._sparse_search(expanded_query, self.top_k)

        # Step 3: Fusion
        score_map: Dict[int, RetrievedChunk] = {}
        for idx, score in dense_results:
            c = self.chunks[idx]
            rc = RetrievedChunk(
                chunk_id=c["chunk_id"],
                text=c["text"],
                level=c["level"],
                level_num=c["level_num"],
                parent_id=c.get("parent_id"),
                sibling_order=c.get("sibling_order", 0),
                score_dense=score,
                score_sparse=0.0,
            )
            score_map[idx] = rc

        for idx, score in sparse_results:
            if idx in score_map:
                score_map[idx].score_sparse = score
            else:
                c = self.chunks[idx]
                rc = RetrievedChunk(
                    chunk_id=c["chunk_id"],
                    text=c["text"],
                    level=c["level"],
                    level_num=c["level_num"],
                    parent_id=c.get("parent_id"),
                    sibling_order=c.get("sibling_order", 0),
                    score_dense=0.0,
                    score_sparse=score,
                )
                score_map[idx] = rc

        # Normalize scores
        all_dense = [r.score_dense for r in score_map.values()]
        all_sparse = [r.score_sparse for r in score_map.values()]
        d_max, d_min = max(all_dense) if all_dense else 1, min(all_dense) if all_dense else 0
        s_max, s_min = max(all_sparse) if all_sparse else 1, min(all_sparse) if all_sparse else 0

        for r in score_map.values():
            d_norm = (r.score_dense - d_min) / (d_max - d_min + 1e-8)
            s_norm = (r.score_sparse - s_min) / (s_max - s_min + 1e-8)
            r.score_combined = self.dense_weight * d_norm + self.sparse_weight * s_norm

        results = sorted(score_map.values(), key=lambda x: x.score_combined, reverse=True)

        # Step 4: Hierarchical reranking
        results = self._hierarchical_rerank(results, query)

        return results[: self.rerank_top_k]
