import urllib.request, json

for q in ["什么是自首？自首有什么法律效果？", "不动产物权如何设立？", "完全民事行为能力人的认定标准是什么？"]:
    req = urllib.request.Request(
        "http://127.0.0.1:8000/query",
        data=json.dumps({"question": q, "top_k": 3}).encode(),
        headers={"Content-Type": "application/json"},
    )
    r = urllib.request.urlopen(req)
    data = json.loads(r.read())
    print("=" * 60)
    print("问题:", data["question"])
    print("回答:", data["answer"][:300])
    print("引用:", data["citations"])
    print()
