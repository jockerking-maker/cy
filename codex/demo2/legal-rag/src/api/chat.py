"""Chat API endpoint - multi-turn conversation with Volcengine."""

import os
import sys
import json
from typing import List, Optional
from pydantic import BaseModel, Field

# Import from the existing server module
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", ".."))

from openai import OpenAI

VOLC_API_KEY = os.environ.get("VOLC_API_KEY", "f9281dee-60fa-46c2-979d-445e435e7513")
VOLC_BASE_URL = "https://ark.cn-beijing.volces.com/api/v3"
CHAT_MODEL = "doubao-seed-1-6-flash-250615"


class ChatMessage(BaseModel):
    role: str  # "user" or "assistant"
    content: str


class ChatRequest(BaseModel):
    messages: List[ChatMessage] = Field(..., description="对话历史 + 当前问题")
    temperature: float = Field(default=0.7, ge=0, le=2)
    max_tokens: int = Field(default=2048, ge=64, le=8192)


class ChatResponse(BaseModel):
    reply: str
    model: str


_client: Optional[OpenAI] = None


def get_client() -> OpenAI:
    global _client
    if _client is None:
        _client = OpenAI(api_key=VOLC_API_KEY, base_url=VOLC_BASE_URL)
    return _client


def chat_completion(messages: List[dict], temperature: float = 0.7, max_tokens: int = 2048) -> str:
    """Call Volcengine chat API with the given messages."""
    client = get_client()

    system_msg = {
        "role": "system",
        "content": (
            "你是一位知识渊博、乐于助人的AI助手。你可以回答各种领域的问题，"
            "包括但不限于法律、科技、文化、教育、生活等。"
            "请用中文回答，语言简洁清晰、有条理。"
        )
    }
    full_messages = [system_msg] + messages

    try:
        response = client.chat.completions.create(
            model=CHAT_MODEL,
            messages=full_messages,
            temperature=temperature,
            max_tokens=max_tokens,
        )
        return response.choices[0].message.content.strip()
    except Exception as e:
        raise RuntimeError(f"API call failed: {e}")


def register_chat_routes(app):
    """Register chat routes on a FastAPI app."""

    @app.post("/api/chat", response_model=ChatResponse)
    async def chat(req: ChatRequest):
        try:
            msg_dicts = [{"role": m.role, "content": m.content} for m in req.messages]
            reply = chat_completion(
                msg_dicts,
                temperature=req.temperature,
                max_tokens=req.max_tokens,
            )
            return ChatResponse(reply=reply, model=CHAT_MODEL)
        except RuntimeError as e:
            from fastapi import HTTPException
            raise HTTPException(status_code=502, detail=str(e))
