with open("F:/cy/codex/demo2/legal-rag/src/api/server.py", "r", encoding="utf-8") as f:
    content = f.read()

# Remove the misplaced webui code (lines 13-20)
lines = content.split("\n")
# Find and remove the inserted webui block (after "from pydantic import" line)
new_lines = []
skip_block = False
for line in lines:
    if 'import os' in line and 'from fastapi.staticfiles' not in content[content.find(line):content.find(line)+100]:
        # This is the first "import os" - keep it
        new_lines.append(line)
    elif 'from fastapi.staticfiles import StaticFiles' in line:
        skip_block = True
    elif 'from fastapi.responses import FileResponse' in line:
        skip_block = True
    elif '_webui_dir = os.path.join' in line:
        skip_block = True
    elif '@app.get("/ui"' in line:
        skip_block = True
    elif 'async def serve_ui' in line:
        skip_block = True
    elif 'return FileResponse' in line:
        skip_block = True
    elif 'app.mount("/static"' in line:
        skip_block = True
    elif skip_block and line.strip() == "":
        skip_block = False
        continue
    elif skip_block:
        continue
    else:
        new_lines.append(line)

content = "\n".join(new_lines)

# Now insert webui code after app = FastAPI(...)
insert_pos = content.find("app = FastAPI(")
end_pos = content.find("\n", content.find("\n", insert_pos) + 1)  # end of FastAPI call

webui_code = '''
# Serve web UI
_webui_dir = os.path.join(os.path.dirname(__file__), "..", "..", "webui")
if os.path.exists(_webui_dir):
    @app.get("/ui", include_in_schema=False)
    async def serve_ui():
        return FileResponse(os.path.join(_webui_dir, "index.html"))
    app.mount("/static", StaticFiles(directory=_webui_dir), name="static")
'''

content = content[:end_pos+1] + webui_code + content[end_pos+1:]

with open("F:/cy/codex/demo2/legal-rag/src/api/server.py", "w", encoding="utf-8") as f:
    f.write(content)
print("done")
