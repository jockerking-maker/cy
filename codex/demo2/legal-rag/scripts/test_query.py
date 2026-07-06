import urllib.request, json

req = urllib.request.Request(
    "http://127.0.0.1:8000/query",
    data=json.dumps({"question": "合同应当包括哪些主要条款？", "top_k": 5}).encode(),
    headers={"Content-Type": "application/json"},
)
r = urllib.request.urlopen(req)
data = json.loads(r.read())

print("=== 问题 ===")
print(data["question"])
print()
print("=== 回答 ===")
print(data["answer"])
print()
print("=== 引用来源 ===")
print(data["citations"])
print()
print("=== 检索到的文档片段 ===")
for i, c in enumerate(data["retrieved_chunks"]):
    print(f"  [{i+1}] ({c['level']}) 得分={c['score']} | {c['text_preview'][:80]}")
print()
print("=== 逐句可信度 ===")
for s in data["hallucination_checks"]:
    print(f"  {s['sentence'][:50]:50s} -> {s['confidence']}")
