import urllib.request
import os

base_url = "https://raw.githubusercontent.com/liuhuanyong/ChineseLawCorpus/master/data/"

laws = {
    "劳动法.txt": "labor_law.txt",
    "公司法.txt": "company_law.txt", 
    "著作权法.txt": "copyright_law.txt",
    "商标法.txt": "trademark_law.txt",
    "专利法.txt": "patent_law.txt",
    "消费者权益保护法.txt": "consumer_law.txt",
    "个人所得税法.txt": "tax_law.txt",
    "道路交通安全法.txt": "traffic_law.txt",
}

out_dir = "F:/cy/codex/demo2/legal-rag/data/sample_legal_docs"
downloaded = 0

for fname, url_path in laws.items():
    url = base_url + url_path
    fpath = os.path.join(out_dir, fname)
    try:
        r = urllib.request.urlopen(url, timeout=10)
        content = r.read().decode("utf-8", errors="replace")
        with open(fpath, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"  OK  {fname} ({len(content)} chars)")
        downloaded += 1
    except Exception as e:
        print(f"  ERR {fname}: {e}")

print(f"\nDownloaded {downloaded}/{len(laws)} files")
