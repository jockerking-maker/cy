"""Test the Legal RAG pipeline end-to-end with sample documents."""

import os
import sys
import json

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from src.pipeline import LegalRAGPipeline, PipelineConfig


def test_chunking():
    """Test the hierarchical chunker on sample documents."""
    print("=" * 60)
    print("Test 1: Hierarchical Chunking")
    print("=" * 60)

    from src.chunking.legal_chunker import LegalDocumentChunker

    chunker = LegalDocumentChunker()
    sample_dir = os.path.join(os.path.dirname(__file__), "..", "data", "sample_legal_docs")

    for fname in os.listdir(sample_dir):
        if not fname.endswith((".txt", ".md")):
            continue
        fpath = os.path.join(sample_dir, fname)
        with open(fpath, "r", encoding="utf-8") as f:
            text = f.read()

        chunks = chunker.chunk_document(text, source_file=fpath)
        levels = {}
        for c in chunks:
            levels[c.level] = levels.get(c.level, 0) + 1

        print(f"\n  File: {fname}")
        print(f"  Total chunks: {len(chunks)}")
        print(f"  Levels: {levels}")
        if chunks:
            print(f"  First chunk: [{chunks[0].level}] {chunks[0].text[:80]}...")
        print()

    return True


def test_retrieval():
    """Test the hybrid retriever."""
    print("=" * 60)
    print("Test 2: Hybrid Retrieval")
    print("=" * 60)

    config = PipelineConfig(use_mock_llm=True, device="cpu")
    pipeline = LegalRAGPipeline(config)

    sample_dir = os.path.join(os.path.dirname(__file__), "..", "data", "sample_legal_docs")
    n = pipeline.index_directory(sample_dir)
    print(f"  Indexed {n} chunks\n")

    test_queries = [
        "合同应当包括哪些主要条款？",
        "什么是自首？",
        "不动产物权如何设立？",
    ]

    for q in test_queries:
        print(f"  Query: {q}")
        retrieved = pipeline.retriever.retrieve(q)
        print(f"  Retrieved {len(retrieved)} chunks:")
        for i, r in enumerate(retrieved):
            score = r.score_reranked or r.score_combined
            print(f"    [{i+1}] level={r.level} score={score:.4f} text={r.text[:60]}...")
        print()

    return True


def test_full_pipeline():
    """Test the full RAG pipeline end-to-end."""
    print("=" * 60)
    print("Test 3: Full RAG Pipeline")
    print("=" * 60)

    config = PipelineConfig(use_mock_llm=True, device="cpu")
    pipeline = LegalRAGPipeline(config)

    sample_dir = os.path.join(os.path.dirname(__file__), "..", "data", "sample_legal_docs")
    pipeline.index_directory(sample_dir)

    test_queries = [
        "合同应当包括哪些主要条款？",
        "什么是自首？自首有什么法律效果？",
    ]

    for q in test_queries:
        print(f"\n  Question: {q}")
        result = pipeline.query(q)
        print(f"  Answer: {result['answer'][:200]}...")
        print(f"  Citations: {result['citations']}")
        print(f"  Retrieved chunks: {len(result['retrieved_chunks'])}")
        print(f"  Hallucination checks: {len(result['hallucination_checks'])}")
        print()

    return True


def test_flat_vs_hierarchical():
    """Compare flat chunking vs hierarchical chunking."""
    print("=" * 60)
    print("Test 4: Flat vs Hierarchical Chunking Comparison")
    print("=" * 60)

    from src.chunking.legal_chunker import LegalDocumentChunker

    chunker = LegalDocumentChunker()
    sample_file = os.path.join(
        os.path.dirname(__file__), "..", "data", "sample_legal_docs", "民法典节选.txt"
    )

    with open(sample_file, "r", encoding="utf-8") as f:
        text = f.read()

    hier_chunks = chunker.chunk_document(text, source_file=sample_file)
    flat_chunks = chunker.chunk_document_flat(text, source_file=sample_file)

    print(f"\n  Hierarchical chunks: {len(hier_chunks)}")
    print(f"  Flat chunks: {len(flat_chunks)}")
    print(f"  Avg hierarchical chunk length: {sum(len(c.text) for c in hier_chunks) / max(len(hier_chunks), 1):.0f} chars")
    print(f"  Avg flat chunk length: {sum(len(c.text) for c in flat_chunks) / max(len(flat_chunks), 1):.0f} chars")

    # Check semantic integrity: hierarchical chunks should preserve article boundaries
    articles_hier = [c for c in hier_chunks if c.level == "article"]
    articles_flat = [c for c in flat_chunks if "条" in c.text[:20]]
    print(f"\n  Articles preserved (hierarchical): {len(articles_hier)}")
    print(f"  Articles found (flat): {len(articles_flat)}")

    # Check if any flat chunk cuts an article boundary
    cuts = 0
    for c in flat_chunks:
        if "条" in c.text[:20] and "条" in c.text[-20:]:
            cuts += 1
    print(f"  Flat chunks with multiple articles: {cuts}")

    return True


if __name__ == "__main__":
    tests = [
        test_chunking,
        test_retrieval,
        test_full_pipeline,
        test_flat_vs_hierarchical,
    ]

    passed = 0
    for test in tests:
        try:
            result = test()
            if result:
                passed += 1
                print(f"  [PASS] {test.__name__}")
            else:
                print(f"  [FAIL] {test.__name__}")
        except Exception as e:
            print(f"  [ERROR] {test.__name__}: {e}")
        print()

    print(f"\n{'=' * 60}")
    print(f"Results: {passed}/{len(tests)} tests passed")
    print(f"{'=' * 60}")
