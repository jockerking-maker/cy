import re
import json
from typing import List, Dict, Optional, Tuple
from dataclasses import dataclass, field, asdict


@dataclass
class Chunk:
    """A single chunk with hierarchical metadata."""
    text: str
    chunk_id: str
    level: str  # "chapter", "section", "article", "clause", "item"
    level_num: int  # 0=chapter, 1=section, 2=article, 3=clause, 4=item
    parent_id: Optional[str] = None
    sibling_order: int = 0
    source_file: str = ""
    metadata: Dict = field(default_factory=dict)


# Chinese legal document hierarchy patterns
LEVEL_PATTERNS = [
    # Chapter: 第一章, 第二章, ...
    (0, "chapter", re.compile(r"^第[一二三四五六七八九十百零]+章\s*(.*)")),
    # Section: 第一节, 第二节, ...
    (1, "section", re.compile(r"^第[一二三四五六七八九十百零]+节\s*(.*)")),
    # Article: 第一条, 第二条, ... or 第1条, 第2条, ...
    (2, "article", re.compile(r"^第[一二三四五六七八九十百零\d]+条\s*(.*)")),
    # Clause: （一）（二）（三）...
    (3, "clause", re.compile(r"^[（(][一二三四五六七八九十百零\d]+[)）]\s*(.*)")),
    # Item: 1. 2. 3. or (1) (2) (3)
    (4, "item", re.compile(r"^[\(（]?\d+[\)）]\.?\s*(.*)")),
]


def _chinese_num_to_int(chinese: str) -> int:
    """Convert Chinese numeral to integer."""
    mapping = {
        "零": 0, "一": 1, "二": 2, "三": 3, "四": 4,
        "五": 5, "六": 6, "七": 7, "八": 8, "九": 9,
        "十": 10, "百": 100, "千": 1000,
    }
    result = 0
    temp = 0
    for char in chinese:
        if char in mapping:
            val = mapping[char]
            if val >= 10:
                if temp == 0:
                    temp = 1
                result += temp * val
                temp = 0
            else:
                temp = val
    result += temp
    return result


def _extract_level(line: str) -> Optional[Tuple[int, str, str, int]]:
    """Detect if a line starts a new hierarchical level."""
    for level_num, level_name, pattern in LEVEL_PATTERNS:
        m = pattern.match(line.strip())
        if m:
            num_str = re.search(r"\d+|[一二三四五六七八九十百零]+", line)
            order = 0
            if num_str:
                raw = num_str.group()
                if raw.isdigit():
                    order = int(raw)
                else:
                    order = _chinese_num_to_int(raw)
            return (level_num, level_name, m.group(1).strip(), order)
    return None


class LegalDocumentChunker:
    """Hierarchy-aware chunker for Chinese legal documents."""

    # Levels that create new chunks (clause/item merge into parent)
    CHUNK_BOUNDARY_LEVELS = {0, 1, 2}  # chapter, section, article

    def __init__(self, max_chunk_chars: int = 2000, overlap_chars: int = 100):
        self.max_chunk_chars = max_chunk_chars
        self.overlap_chars = overlap_chars

    def chunk_document(self, text: str, source_file: str = "") -> List[Chunk]:
        """Chunk a legal document using hierarchy-aware parsing.
        
        Only chapter/section/article boundaries create new chunks.
        Clause and item content is merged into the parent article chunk.
        """
        lines = text.split("\n")
        chunks: List[Chunk] = []
        stack: List[Tuple[int, str, str, int]] = []

        current_lines: List[str] = []
        current_level_info: Optional[Tuple[int, str, str, int]] = None
        chunk_counter = 0

        def _flush():
            nonlocal chunk_counter, current_lines
            if not current_lines:
                return
            text_content = "\n".join(current_lines).strip()
            if not text_content:
                return

            if current_level_info:
                lvl_num, lvl_name, title, order = current_level_info
                parent_id = None
                for i in range(len(stack) - 1, -1, -1):
                    if stack[i][0] < lvl_num:
                        parent_id = f"chunk_{i}_{stack[i][3]}"
                        break
            else:
                lvl_num, lvl_name, title, order = -1, "preamble", "", 0
                parent_id = None

            chunk_id = f"chunk_{chunk_counter}"
            chunk_counter += 1

            chunk = Chunk(
                text=text_content,
                chunk_id=chunk_id,
                level=lvl_name,
                level_num=lvl_num,
                parent_id=parent_id,
                sibling_order=order,
                source_file=source_file,
                metadata={"title": title, "order": order},
            )
            chunks.append(chunk)
            current_lines = []

        for line in lines:
            stripped = line.strip()
            if not stripped:
                current_lines.append(line)
                continue

            level_info = _extract_level(stripped)

            if level_info is not None:
                lvl_num, lvl_name, title, order = level_info

                # Only chapter/section/article create new chunk boundaries
                if lvl_num in self.CHUNK_BOUNDARY_LEVELS:
                    _flush()
                    current_level_info = (lvl_num, lvl_name, title, order)

                    # Update stack
                    while stack and stack[-1][0] >= lvl_num:
                        stack.pop()
                    stack.append(current_level_info)

                    current_lines.append(stripped)
                else:
                    # Clause/item: merge into current chunk
                    current_lines.append(stripped)
            else:
                current_lines.append(line)

                # Split if too long (only for very long articles)
                acc_text = "\n".join(current_lines)
                if len(acc_text) > self.max_chunk_chars:
                    _flush()
                    overlap_text = "\n".join(current_lines[-3:]) if len(current_lines) >= 3 else ""
                    current_lines = [overlap_text] if overlap_text else []

        _flush()
        return chunks

    def chunk_document_flat(self, text: str, source_file: str = "",
                            chunk_size: int = 512, overlap: int = 64) -> List[Chunk]:
        """Fallback: fixed-size chunking."""
        chunks = []
        start = 0
        counter = 0
        while start < len(text):
            end = min(start + chunk_size, len(text))
            chunk_text = text[start:end]
            chunk = Chunk(
                text=chunk_text,
                chunk_id=f"flat_{counter}",
                level="flat",
                level_num=-1,
                source_file=source_file,
            )
            chunks.append(chunk)
            counter += 1
            start = end - overlap if end < len(text) else end
        return chunks


def chunk_file(filepath: str, use_hierarchy: bool = True) -> List[Chunk]:
    """Convenience: chunk a file from path."""
    with open(filepath, "r", encoding="utf-8") as f:
        text = f.read()
    chunker = LegalDocumentChunker()
    if use_hierarchy:
        return chunker.chunk_document(text, source_file=filepath)
    else:
        return chunker.chunk_document_flat(text, source_file=filepath)


def chunks_to_jsonl(chunks: List[Chunk], output_path: str):
    """Save chunks to JSONL format."""
    with open(output_path, "w", encoding="utf-8") as f:
        for c in chunks:
            f.write(json.dumps(asdict(c), ensure_ascii=False) + "\n")
