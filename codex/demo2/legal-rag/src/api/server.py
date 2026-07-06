"""FastAPI server for the Legal RAG system."""

import os
import sys
from typing import Optional

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from pydantic import BaseModel, Field

from src.pipeline import LegalRAGPipeline, PipelineConfig
from src.api.chat import register_chat_routes

VOLC_API_KEY = os.environ.get("VOLC_API_KEY", "f9281dee-60fa-46c2-979d-445e435e7513")

app = FastAPI(
    title="Legal RAG API - 法律文档智能问答系统",
    description="基于层级化检索增强生成的法律文档问答系统（已接入火山引擎豆包模型）",
    version="1.0.0",
)

# Serve web UI
_webui_dir = os.path.join(os.path.dirname(__file__), "..", "..", "webui")
if os.path.exists(_webui_dir):
    @app.get("/ui", include_in_schema=False)
    async def serve_ui():
        return FileResponse(os.path.join(_webui_dir, "index.html"))
    app.mount("/static", StaticFiles(directory=_webui_dir), name="static")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Register chat routes
register_chat_routes(app)

pipeline: Optional[LegalRAGPipeline] = None


class QueryRequest(BaseModel):
    question: str = Field(..., description="用户的法律问题")
    top_k: int = Field(default=5, ge=1, le=20, description="返回的参考文档数量")


class QueryResponse(BaseModel):
    question: str
    answer: str
    citations: list
    retrieved_chunks: list
    hallucination_checks: list


class IndexResponse(BaseModel):
    message: str
    chunks_indexed: int


class StatusResponse(BaseModel):
    indexed: bool
    num_chunks: int
    num_documents: int
    config: dict


@app.on_event("startup")
async def startup():
    global pipeline
    config = PipelineConfig(
        embedding_model="BAAI/bge-small-zh-v1.5",
        llm_api_key=VOLC_API_KEY,
        llm_model="doubao-seed-1-6-flash-250615",
        llm_temperature=0.3,
        llm_max_tokens=2048,
        device="cpu",
    )
    pipeline = LegalRAGPipeline(config)

    sample_dir = os.path.join(os.path.dirname(__file__), "..", "..", "data", "sample_legal_docs")
    if os.path.exists(sample_dir):
        n = pipeline.index_directory(sample_dir)
        print(f"Startup: indexed {n} chunks from sample documents")
    else:
        print(f"Startup: sample directory not found at {sample_dir}")


@app.get("/", response_model=StatusResponse)
async def root():
    global pipeline
    if pipeline is None:
        raise HTTPException(status_code=503, detail="Pipeline not initialized")
    num_chunks = len(pipeline.retriever.chunks) if pipeline.is_indexed else 0
    return StatusResponse(
        indexed=pipeline.is_indexed,
        num_chunks=num_chunks,
        num_documents=len(set(c["source_file"] for c in pipeline.retriever.chunks)) if pipeline.is_indexed else 0,
        config={
            "embedding_model": pipeline.config.embedding_model,
            "llm_model": pipeline.config.llm_model,
            "rerank_top_k": pipeline.config.rerank_top_k,
        },
    )


@app.post("/query", response_model=QueryResponse)
async def query(req: QueryRequest):
    global pipeline
    if pipeline is None:
        raise HTTPException(status_code=503, detail="Pipeline not initialized")
    if not pipeline.is_indexed:
        raise HTTPException(status_code=400, detail="No documents indexed")

    pipeline.config.rerank_top_k = req.top_k
    result = pipeline.query(req.question)
    if "error" in result:
        raise HTTPException(status_code=400, detail=result["error"])

    return QueryResponse(**result)


@app.post("/index", response_model=IndexResponse)
async def index_document(filepath: str):
    global pipeline
    if pipeline is None:
        raise HTTPException(status_code=503, detail="Pipeline not initialized")
    if not os.path.exists(filepath):
        raise HTTPException(status_code=404, detail=f"File not found: {filepath}")

    n = pipeline.index_document(filepath)
    return IndexResponse(message=f"Indexed {n} chunks", chunks_indexed=n)


@app.post("/index-directory", response_model=IndexResponse)
async def index_directory(directory: str):
    global pipeline
    if pipeline is None:
        raise HTTPException(status_code=503, detail="Pipeline not initialized")
    if not os.path.exists(directory):
        raise HTTPException(status_code=404, detail=f"Directory not found: {directory}")

    n = pipeline.index_directory(directory)
    return IndexResponse(message=f"Indexed {n} chunks from directory", chunks_indexed=n)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
