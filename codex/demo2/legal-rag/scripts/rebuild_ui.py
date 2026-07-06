import os, re
DIR = "F:/cy/codex/demo2/legal-rag/webui"
path = os.path.join(DIR, "index.html")
with open(path, "r", encoding="utf-8") as f:
    old = f.read()
chat_js_start = old.find("// ===== Chat Assistant Logic =====")
chat_js_end = old.find("</script>", chat_js_start)
chat_js = old[chat_js_start:chat_js_end] if chat_js_start > 0 else ""
print("chat_js len:", len(chat_js))
print("has marked:", "marked.parse" in old)
print("has overlay:", "chat-overlay" in old)
import os, re
DIR = "F:/cy/codex/demo2/legal-rag/webui"
path = os.path.join(DIR, "index.html")
with open(path, "r", encoding="utf-8") as f:
    old = f.read()
chat_js_start = old.find("// ===== Chat Assistant Logic =====")
chat_js_end = old.find("</script>", chat_js_start)
chat_js = old[chat_js_start:chat_js_end] if chat_js_start > 0 else ""
render_start = old.find("function renderResult")
render_end = old.find("checkStatus();", render_start)
render_js = old[render_start:render_end] if render_start > 0 else ""
status_start = old.find("async function checkStatus")
status_end = old.find("function setQuestion", status_start)
status_js = old[status_start:status_end] if status_start > 0 else ""
ask_start = old.find("async function ask")
ask_end = old.find("function renderResult", ask_start)
ask_js = old[ask_start:ask_end] if ask_start > 0 else ""
print("extracted all js parts")
print("chat:", len(chat_js), "render:", len(render_js), "status:", len(status_js), "ask:", len(ask_js))
