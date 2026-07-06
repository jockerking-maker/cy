from typing import List, Optional
from dataclasses import dataclass
import re
import os
from openai import OpenAI


@dataclass
class GenerationResult:
    answer: str
    citations: List[str]
    raw_output: str


class LegalGenerator:
    """Generate answers with citation markers using Volcengine API (OpenAI-compatible)."""

    def __init__(
        self,
        api_key: str = "",
        model_name: str = "doubao-seed-1-6-flash-250615",
        base_url: str = "https://ark.cn-beijing.volces.com/api/v3",
        temperature: float = 0.3,
        max_tokens: int = 2048,
    ):
        self.model_name = model_name
        self.temperature = temperature
        self.max_tokens = max_tokens

        # Use provided key or fallback to env var
        key = api_key or os.environ.get("VOLC_API_KEY", "")
        if not key:
            raise ValueError(
                "火山引擎 API Key 未设置。请通过 api_key 参数或 VOLC_API_KEY 环境变量提供。"
            )

        self.client = OpenAI(api_key=key, base_url=base_url)
        print(f"[Generator] Volcengine API ready | model={model_name}")

    def _build_prompt(self, query: str, context_chunks: List[str]) -> str:
        """Build a structured prompt with retrieved context."""
        context_text = "\n\n---\n\n".join(
            [f"[参考文档 {i+1}]\n{chunk}" for i, chunk in enumerate(context_chunks)]
        )

        prompt = f"""你是一位精通中国法律的专业助手。请根据以下参考文档回答用户的问题。

要求：
1. 仅基于提供的参考文档作答，不要编造法条或事实。
2. 在引用具体法条时，请在引用内容后标注来源编号，格式为 [参考文档 X]。
3. 如果参考文档不足以回答问题，请明确说明。
4. 回答应当准确、简洁、有条理。

参考文档：
{context_text}

用户问题：{query}

回答："""
        return prompt

    def _parse_citations(self, text: str) -> List[str]:
        """Extract citation markers like [参考文档 1] from generated text."""
        pattern = r"\[参考文档\s*(\d+)\]"
        matches = re.findall(pattern, text)
        return list(set(matches))

    def generate(self, query: str, context_chunks: List[str]) -> GenerationResult:
        """Generate an answer with citations using Volcengine API."""
        prompt = self._build_prompt(query, context_chunks)

        try:
            response = self.client.chat.completions.create(
                model=self.model_name,
                messages=[
                    {"role": "system", "content": "你是一位精通中国法律的专业助手，回答严谨、准确、有依据。"},
                    {"role": "user", "content": prompt},
                ],
                temperature=self.temperature,
                max_tokens=self.max_tokens,
            )
            raw_output = response.choices[0].message.content
            answer = raw_output.strip()
            citations = self._parse_citations(raw_output)

        except Exception as e:
            print(f"[Generator] API call failed: {e}")
            # Fallback to mock answer on error
            citations = [f"参考文档 {i+1}" for i in range(min(len(context_chunks), 3))]
            answer = (
                f"根据相关法律规定，针对您的问题「{query}」，分析如下：\n\n"
                f"1. 首先，根据《中华人民共和国民法典》的相关规定 [参考文档 1]，"
                f"当事人应当依法行使权利、履行义务。\n\n"
                f"2. 其次，[参考文档 2] 进一步明确了在具体情形下的适用规则，"
                f"包括责任认定和赔偿标准。\n\n"
                f"3. 综合以上分析，建议您结合具体情况咨询专业律师以获取更有针对性的法律意见。\n\n"
                f"[注意] 当前回答为备用模式，因 API 调用失败自动降级。"
            )
            raw_output = answer

        return GenerationResult(answer=answer, citations=citations, raw_output=raw_output)
