from typing import List, Tuple


class HallucinationDetector:
    """Detect hallucinations by checking consistency between generated answer
    and retrieved context using NLI (Natural Language Inference).
    
    In production, this would use a dedicated NLI model (e.g. BAAI/bge-reranker-v2-m3
    or a cross-encoder). Here we provide a rule-based fallback plus the NLI interface.
    """

    def __init__(self, use_nli: bool = False, nli_model_name: str = "MoritzLaurer/mDeBERTa-v3-base-xnli-multilingual-nli-2shot7"):
        self.use_nli = use_nli
        self.nli_model = None
        self.nli_tokenizer = None

        if use_nli:
            print(f"[Hallucination] Loading NLI model: {nli_model_name}")
            from transformers import AutoModelForSequenceClassification, AutoTokenizer
            self.nli_tokenizer = AutoTokenizer.from_pretrained(nli_model_name)
            self.nli_model = AutoModelForSequenceClassification.from_pretrained(nli_model_name)
        else:
            print("[Hallucination] Using rule-based fallback (no NLI model loaded)")

    def _rule_based_check(self, answer: str, context_chunks: List[str]) -> List[Tuple[str, float]]:
        """Simple rule-based consistency check.
        
        For each claim in the answer (split by sentences), check if key terms
        appear in the context. Returns (claim, confidence) pairs.
        """
        import re
        # Simple sentence splitting
        sentences = re.split(r"[。！？\n]", answer)
        sentences = [s.strip() for s in sentences if len(s.strip()) > 5]

        combined_context = " ".join(context_chunks)
        results = []

        for sent in sentences:
            # Extract key terms (Chinese words of length >= 2)
            key_terms = [w for w in re.findall(r"[\u4e00-\u9fff]{2,}", sent) if len(w) >= 2]

            if not key_terms:
                results.append((sent, 1.0))
                continue

            # Check how many key terms appear in context
            matched = sum(1 for t in key_terms if t in combined_context)
            ratio = matched / len(key_terms)

            # Heuristic confidence
            if ratio >= 0.6:
                confidence = 0.8 + 0.2 * (ratio - 0.6) / 0.4
            elif ratio >= 0.3:
                confidence = 0.5 + 0.3 * (ratio - 0.3) / 0.3
            else:
                confidence = max(0.1, ratio * 1.5)

            results.append((sent, round(min(confidence, 1.0), 4)))

        return results

    def _nli_check(self, answer: str, context_chunks: List[str]) -> List[Tuple[str, float]]:
        """NLI-based consistency check using a cross-encoder."""
        import re
        sentences = re.split(r"[。！？\n]", answer)
        sentences = [s.strip() for s in sentences if len(s.strip()) > 5]
        results = []

        combined_context = " ".join(context_chunks)

        for sent in sentences:
            inputs = self.nli_tokenizer(
                combined_context, sent,
                truncation=True, return_tensors="pt", max_length=512
            )
            outputs = self.nli_model(**inputs)
            probs = outputs.logits.softmax(dim=-1).detach().numpy()[0]
            # NLI labels: 0=contradiction, 1=neutral, 2=entailment
            entailment_score = float(probs[2])
            results.append((sent, round(entailment_score, 4)))

        return results

    def check(self, answer: str, context_chunks: List[str]) -> List[Tuple[str, float]]:
        """Check answer consistency against context.
        Returns list of (sentence, confidence) where confidence in [0,1].
        """
        if self.use_nli:
            return self._nli_check(answer, context_chunks)
        else:
            return self._rule_based_check(answer, context_chunks)

    def filter_unsafe_claims(self, answer: str, context_chunks: List[str],
                             threshold: float = 0.4) -> str:
        """Remove or flag low-confidence claims from the answer."""
        checks = self.check(answer, context_chunks)
        import re

        low_conf_sentences = [s for s, c in checks if c < threshold]
        if not low_conf_sentences:
            return answer

        # Append a disclaimer for low-confidence claims
        disclaimer = (
            "\n\n[注意] 以上回答中部分陈述未能从参考文档中得到充分验证，"
            "建议您查阅原始法条或咨询专业律师确认。"
        )
        return answer + disclaimer
