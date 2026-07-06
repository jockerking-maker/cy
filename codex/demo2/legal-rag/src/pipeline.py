"""Legal RAG Pipeline: main orchestrator that ties all modules together."""

import os
import json
from typing import List, Dict, Optional
from dataclasses import dataclass, field

from src.chunking.legal_chunker import LegalDocumentChunker, Chunk, chunks_to_jsonl
from src.retrieval.hybrid_retriever import HybridRetriever
from src.generation.generator import LegalGenerator, GenerationResult
from src.hallucination.detector import HallucinationDetector


@dataclass
class PipelineConfig:
    chunk_max_chars: int = 1500
    chunk_overlap: int = 100
    embedding_model: str = "BAAI/bge-small-zh-v1.5"
    dense_weight: float = 0.5
    sparse_weight: float = 0.5
    retrieval_top_k: int = 20
    rerank_top_k: int = 5
    llm_model: str = "doubao-seed-1-6-flash-250615"
    llm_api_key: str = ""
    llm_base_url: str = "https://ark.cn-beijing.volces.com/api/v3"
    llm_temperature: float = 0.3
    llm_max_tokens: int = 2048
    use_nli: bool = False
    hallucination_threshold: float = 0.4
    device: str = "cpu"


class LegalRAGPipeline:
    """End-to-end legal RAG pipeline."""

    def __init__(self, config: Optional[PipelineConfig] = None):
        self.config = config or PipelineConfig()
        self.chunker = LegalDocumentChunker(
            max_chunk_chars=self.config.chunk_max_chars,
            overlap_chars=self.config.chunk_overlap,
        )
        self.retriever = HybridRetriever(
            embedding_model_name=self.config.embedding_model,
            dense_weight=self.config.dense_weight,
            sparse_weight=self.config.sparse_weight,
            top_k=self.config.retrieval_top_k,
            rerank_top_k=self.config.rerank_top_k,
            device=self.config.device,
        )
        self.generator = LegalGenerator(
            api_key=self.config.llm_api_key,
            model_name=self.config.llm_model,
            base_url=self.config.llm_base_url,
            temperature=self.config.llm_temperature,
            max_tokens=self.config.llm_max_tokens,
        )
        self.detector = HallucinationDetector(
            use_nli=self.config.use_nli,
        )
        self.is_indexed = False

    def index_document(self, filepath: str) -> int:
        """Index a single document file."""
        chunks = self.chunker.chunk_document(
            open(filepath, "r", encoding="utf-8").read(),
            source_file=filepath,
        )
        chunk_dicts = []
        for c in chunks:
            d = {
                "chunk_id": c.chunk_id,
                "text": c.text,
                "level": c.level,
                "level_num": c.level_num,
                "parent_id": c.parent_id,
                "sibling_order": c.sibling_order,
                "source_file": c.source_file,
                "metadata": c.metadata,
            }
            chunk_dicts.append(d)

        if not self.is_indexed:
            self.retriever.index_chunks(chunk_dicts)
            self.is_indexed = True
        else:
            old_chunks = self.retriever.chunks
            self.retriever.index_chunks(old_chunks + chunk_dicts)

        return len(chunk_dicts)

    def index_directory(self, directory: str) -> int:
        """Index all .txt and .md files in a directory."""
        total = 0
        for fname in os.listdir(directory):
            if fname.endswith((".txt", ".md")):
                fpath = os.path.join(directory, fname)
                n = self.index_document(fpath)
                print(f"  Indexed {fname}: {n} chunks")
                total += n
        return total

    def query(self, question: str) -> Dict:
        """Run the full RAG pipeline on a question."""
        if not self.is_indexed:
            return {"error": "No documents indexed. Please index documents first."}

        # Step 1: Retrieve
        retrieved = self.retriever.retrieve(question)
        context_chunks = [r.text for r in retrieved]

        # Step 2: Generate
        gen_result = self.generator.generate(question, context_chunks)

        # Step 3: Hallucination detection
        checks = self.detector.check(gen_result.answer, context_chunks)
        filtered_answer = self.detector.filter_unsafe_claims(
            gen_result.answer, context_chunks, self.config.hallucination_threshold
        )

        # Step 4: Build result
        result = {
            "question": question,
            "answer": filtered_answer,
            "citations": gen_result.citations,
            "retrieved_chunks": [
                {
                    "chunk_id": r.chunk_id,
                    "level": r.level,
                    "score": round(r.score_reranked or r.score_combined, 4),
                    "text_preview": r.text[:200] + ("..." if len(r.text) > 200 else ""),
                    "source_file": next(
                        (c["source_file"] for c in self.retriever.chunks if c["chunk_id"] == r.chunk_id),
                        "",
                    ),
                }
                for r in retrieved
            ],
            "hallucination_checks": [
                {"sentence": s, "confidence": c} for s, c in checks
            ],
        }
        return result
