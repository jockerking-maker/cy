import sys
sys.path.insert(0, "F:/cy/codex/demo2/legal-rag")

with open("F:/cy/codex/demo2/legal-rag/src/api/server.py", "r", encoding="utf-8") as f:
    content = f.read()

lines = []
lines.append("import os")
lines.append("from fastapi.staticfiles import StaticFiles")
lines.append("from fastapi.responses import FileResponse")
lines.append("")
lines.append('_webui_dir = os.path.join(os.path.dirname(__file__), "..", "..", "webui")')
lines.append("if os.path.exists(_webui_dir):")
lines.append('    @app.get("/ui", include_in_schema=False)')
lines.append("    async def serve_ui():")
lines.append('        return FileResponse(os.path.join(_webui_dir, "index.html"))')
lines.append('    app.mount("/static", StaticFiles(directory=_webui_dir), name="static")')
insertion = "\n".join(lines) + "\n"

last_import = content.rfind("from pydantic import")
insert_pos = content.find("\n", last_import) + 1
content = content[:insert_pos] + insertion + content[insert_pos:]

with open("F:/cy/codex/demo2/legal-rag/src/api/server.py", "w", encoding="utf-8") as f:
    f.write(content)
print("done")
