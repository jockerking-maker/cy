x=1
import os
DIR = "F:/cy/codex/demo2/legal-rag/webui"
path = os.path.join(DIR, "index.html")
with open(path, "r", encoding="utf-8") as f:
    html = f.read()
print("read ok, len=", len(html))
