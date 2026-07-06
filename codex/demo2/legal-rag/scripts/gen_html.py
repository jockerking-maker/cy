import os
import os, re
DIR = "F:/cy/codex/demo2/legal-rag/webui"
path = os.path.join(DIR, "index.html")
with open(path, "r", encoding="utf-8") as f:
    html = f.read()

# 1. Add marked.js CDN (if not present)
if "marked.min.js" not in html:
    html = html.replace("</head>", '<script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>\n</head>')

# 2. Replace answer rendering to use marked.parse
old_render = '''const answerText = data.answer;
  document.getElementById("answerText").textContent = answerText;'''
new_render = '''const answerText = data.answer;
  document.getElementById("answerText").innerHTML = marked.parse(answerText);'''
html = html.replace(old_render, new_render)

# 3. Replace chat assistant rendering to use marked.parse
old_chat_render = '''if (role === "assistant") {
    div.innerHTML = marked.parse(content) + '<span class="msg-time">' + timeStr + '</span>';
  } else {
    div.innerHTML = escapeHtml(content) + '<span class="msg-time">' + timeStr + '</span>';
  }'''
new_chat_render = '''if (role === "assistant") {
    div.innerHTML = marked.parse(content) + '<span class="msg-time">' + timeStr + '</span>';
  } else {
    div.innerHTML = escapeHtml(content) + '<span class="msg-time">' + timeStr + '</span>';
  }'''
html = html.replace(old_chat_render, new_chat_render)

# 4. Replace CSS variables with refined design
css_vars = '''  :root {
    --primary: #1a56db;
    --primary-light: #e8effd;
    --primary-dark: #1e40af;
    --bg: #f0f4f8;
    --card: #ffffff;
    --border: #e2e8f0;
    --text: #1e293b;
    --text-secondary: #64748b;
    --text-muted: #94a3b8;
    --success: #16a34a;
    --warning: #d97706;
    --danger: #dc2626;
    --radius: 12px;
    --radius-sm: 8px;
    --shadow: 0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04);
    --shadow-md: 0 4px 12px rgba(0,0,0,0.08);
    --shadow-lg: 0 8px 30px rgba(0,0,0,0.12);
    --font: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", "Noto Sans SC", sans-serif;
    --font-mono: "Cascadia Code", "Fira Code", "JetBrains Mono", "Consolas", monospace;
    --transition: 0.2s ease;
  }'''
old_vars_start = html.find(":root {")
old_vars_end = html.find("}", old_vars_start) + 1
html = html[:old_vars_start] + css_vars + html[old_vars_end:]

# 5. Add answer-text markdown styles (after existing answer-text styles)
md_styles = '''
.answer-text h1,.answer-text h2,.answer-text h3 { margin: 16px 0 8px 0; font-weight: 600; line-height: 1.4; }
.answer-text h1 { font-size: 20px; } .answer-text h2 { font-size: 18px; } .answer-text h3 { font-size: 16px; }
.answer-text p { margin: 8px 0; }
.answer-text ul,.answer-text ol { margin: 8px 0; padding-left: 24px; }
.answer-text li { margin: 4px 0; }
.answer-text strong { font-weight: 600; }
.answer-text code { background: #f1f5f9; padding: 2px 7px; border-radius: 4px; font-size: 13px; font-family: var(--font-mono); color: #e11d48; }
.answer-text pre { background: #1e293b; color: #e2e8f0; padding: 16px 20px; border-radius: var(--radius-sm); overflow-x: auto; margin: 12px 0; font-size: 13px; line-height: 1.6; }
.answer-text pre code { background: transparent; padding: 0; color: inherit; font-size: inherit; }
.answer-text blockquote { border-left: 3px solid var(--primary); padding: 8px 16px; margin: 12px 0; background: #f8fafc; border-radius: 0 var(--radius-sm) var(--radius-sm) 0; color: var(--text-secondary); }
.answer-text table { border-collapse: collapse; margin: 12px 0; width: 100%; font-size: 14px; }
.answer-text th,.answer-text td { border: 1px solid var(--border); padding: 8px 12px; text-align: left; }
.answer-text th { background: #f8fafc; font-weight: 600; }
.answer-text hr { border: none; border-top: 1px solid var(--border); margin: 16px 0; }
.answer-text a { color: var(--primary); text-decoration: underline; }'''

# Insert after the .answer-text line-height rule
target = ".answer-text {"
target_end = html.find("}", html.find(target)) + 1
if target_end > 0:
    html = html[:target_end] + md_styles + html[target_end:]

# 6. Add header logo
logo_html = '''<div class="header-logo">&#9878;</div>'''
header_h1 = html.find("<h1>")
if header_h1 > 0:
    html = html[:header_h1] + logo_html + html[header_h1:]

# 7. Add font-smoothing to body
body_style = "body {"
body_end = html.find("}", html.find(body_style)) + 1
if body_end > 0:
    insert = "  -webkit-font-smoothing: antialiased;\n"
    html = html[:body_end] + insert + html[body_end:]

# 8. Add sticky header
header_style = ".header {"
h_end = html.find("}", html.find(header_style)) + 1
if h_end > 0:
    sticky = "  position: sticky;\n  top: 0;\n  z-index: 100;\n  backdrop-filter: blur(8px);\n  background: rgba(255,255,255,0.92);\n"
    html = html[:h_end] + sticky + html[h_end:]

# 9. Update confidence bar gradient fills
html = html.replace("background: var(--success);", "background: linear-gradient(90deg, var(--success), #22c55e);")
html = html.replace("background: var(--warning);", "background: linear-gradient(90deg, var(--warning), #f59e0b);")
html = html.replace("background: var(--danger);", "background: linear-gradient(90deg, var(--danger), #ef4444);")

# 10. Add hover effect to ref items
ref_style = ".ref-item {"
ref_end = html.find("}", html.find(ref_style)) + 1
if ref_end > 0:
    hover = "  transition: border-color var(--transition);\n"
    html = html[:ref_end] + hover + html[ref_end:]
    html = html.replace(".ref-item:last-child { margin-bottom: 0; }", ".ref-item:hover { border-color: var(--primary); }\n.ref-item:last-child { margin-bottom: 0; }")

# 11. Add chat markdown styles
chat_md = '''
.chat-msg.assistant h1,.chat-msg.assistant h2,.chat-msg.assistant h3 { margin: 12px 0 6px 0; font-weight: 600; line-height: 1.4; }
.chat-msg.assistant h1 { font-size: 17px; } .chat-msg.assistant h2 { font-size: 15px; } .chat-msg.assistant h3 { font-size: 14px; }
.chat-msg.assistant p { margin: 6px 0; }
.chat-msg.assistant ul,.chat-msg.assistant ol { margin: 6px 0; padding-left: 22px; }
.chat-msg.assistant li { margin: 3px 0; }
.chat-msg.assistant strong { font-weight: 600; }
.chat-msg.assistant code { background: #f1f5f9; padding: 2px 6px; border-radius: 4px; font-size: 13px; font-family: var(--font-mono); color: #e11d48; }
.chat-msg.assistant pre { background: #1e293b; color: #e2e8f0; padding: 14px 16px; border-radius: 10px; overflow-x: auto; margin: 10px 0; font-size: 13px; line-height: 1.5; }
.chat-msg.assistant pre code { background: transparent; padding: 0; color: inherit; font-size: inherit; }
.chat-msg.assistant blockquote { border-left: 3px solid var(--primary); padding-left: 12px; margin: 8px 0; color: var(--text-secondary); }
.chat-msg.assistant table { border-collapse: collapse; margin: 8px 0; width: 100%; font-size: 13px; }
.chat-msg.assistant th,.chat-msg.assistant td { border: 1px solid var(--border); padding: 6px 10px; text-align: left; }
.chat-msg.assistant th { background: #f1f5f9; font-weight: 600; }'''
chat_msg_end = html.find(".chat-msg .msg-time {")
if chat_msg_end > 0:
    html = html[:chat_msg_end] + chat_md + html[chat_msg_end:]

with open(path, "w", encoding="utf-8") as f:
    f.write(html)
print("done, final size:", len(html))
