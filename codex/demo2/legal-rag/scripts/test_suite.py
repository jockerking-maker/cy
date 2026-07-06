"""Legal RAG 系统测试用例集"""

import urllib.request, json, time

API = "http://127.0.0.1:8000/query"

test_cases = [
    # === 合同法 ===
    {
        "id": "C01",
        "category": "合同法",
        "question": "合同应当包括哪些主要条款？",
        "expect": "第四百七十条",
    },
    {
        "id": "C02",
        "category": "合同法",
        "question": "当事人订立合同有哪些方式？",
        "expect": "要约、承诺",
    },
    {
        "id": "C03",
        "category": "合同法",
        "question": "什么是要约？要约需要满足什么条件？",
        "expect": "内容具体确定",
    },

    # === 民法总则 ===
    {
        "id": "M01",
        "category": "民法总则",
        "question": "完全民事行为能力人的认定标准是什么？",
        "expect": "十八周岁",
    },
    {
        "id": "M02",
        "category": "民法总则",
        "question": "八周岁以上的未成年人属于什么民事行为能力人？",
        "expect": "限制民事行为能力人",
    },
    {
        "id": "M03",
        "category": "民法总则",
        "question": "胎儿在什么情况下视为具有民事权利能力？",
        "expect": "遗产继承、接受赠与",
    },
    {
        "id": "M04",
        "category": "民法总则",
        "question": "处理民事纠纷应当依照什么？没有法律规定时可以适用什么？",
        "expect": "习惯",
    },

    # === 物权法 ===
    {
        "id": "P01",
        "category": "物权法",
        "question": "不动产物权如何设立？",
        "expect": "登记",
    },
    {
        "id": "P02",
        "category": "物权法",
        "question": "不动产登记由哪个机构办理？",
        "expect": "不动产所在地的登记机构",
    },

    # === 刑法 ===
    {
        "id": "CR01",
        "category": "刑法",
        "question": "什么是自首？自首有什么法律效果？",
        "expect": "自动投案、如实供述",
    },
    {
        "id": "CR02",
        "category": "刑法",
        "question": "什么是累犯？累犯如何处罚？",
        "expect": "从重处罚",
    },
    {
        "id": "CR03",
        "category": "刑法",
        "question": "已满多少周岁的人犯罪应当负刑事责任？",
        "expect": "十六周岁",
    },
    {
        "id": "CR04",
        "category": "刑法",
        "question": "什么是故意犯罪？",
        "expect": "希望或者放任",
    },
    {
        "id": "CR05",
        "category": "刑法",
        "question": "立功有什么法律效果？",
        "expect": "从轻或者减轻处罚",
    },

    # === 边界情况 ===
    {
        "id": "E01",
        "category": "边界情况",
        "question": "你好，请问你能帮我做什么？",
        "expect": "法律",
    },
    {
        "id": "E02",
        "category": "边界情况",
        "question": "美国民法典关于合同的规定是什么？",
        "expect": "参考文档",
    },
]


def run_test(tc):
    """Run a single test case."""
    payload = json.dumps({"question": tc["question"], "top_k": 3}).encode()
    req = urllib.request.Request(API, data=payload, headers={"Content-Type": "application/json"})

    try:
        r = urllib.request.urlopen(req, timeout=15)
        data = json.loads(r.read())
    except Exception as e:
        return {"status": "ERROR", "detail": str(e)}

    answer = data.get("answer", "")
    citations = data.get("citations", [])
    chunks = data.get("retrieved_chunks", [])
    checks = data.get("hallucination_checks", [])

    # Check if answer contains expected keywords
    found_expect = tc["expect"] in answer if tc["expect"] else True

    # Calculate average confidence
    avg_conf = sum(c["confidence"] for c in checks) / max(len(checks), 1)

    # Check if any chunks were retrieved
    has_chunks = len(chunks) > 0

    return {
        "status": "PASS" if found_expect and has_chunks else "WARN",
        "answer": answer[:200],
        "citations": citations,
        "num_chunks": len(chunks),
        "avg_confidence": round(avg_conf, 3),
        "found_expect": found_expect,
    }


# ====== Run all tests ======
print("=" * 70)
print("  Legal RAG 系统测试用例集")
print("  API: http://127.0.0.1:8000")
print("  模型: doubao-seed-1-6-flash-250615 (火山引擎)")
print("=" * 70)
print()

results = {"PASS": 0, "WARN": 0, "ERROR": 0}
current_category = ""

for tc in test_cases:
    if tc["category"] != current_category:
        current_category = tc["category"]
        print(f"── {current_category} ──")

    result = run_test(tc)
    status = result["status"]
    results[status] = results.get(status, 0) + 1

    status_symbol = {"PASS": "PASS", "WARN": "WARN", "ERROR": "ERR "}[status]
    print(f"  [{status_symbol}] {tc['id']} {tc['question'][:40]:40s}")

    # Show answer preview on WARN
    if status != "PASS":
        print(f"       -> {result['answer'][:80]}")
    else:
        # Show brief answer preview for passes too
        print(f"       -> {result['answer'][:80]}")

    # Show confidence
    print(f"       可信度: {result['avg_confidence']:.0%} | 引用: {result['citations']} | 检索片段: {result['num_chunks']}")
    print()

# Summary
print("=" * 70)
print(f"  结果汇总: PASS={results['PASS']}  WARN={results['WARN']}  ERROR={results['ERROR']}  共{len(test_cases)}项")
print("=" * 70)
