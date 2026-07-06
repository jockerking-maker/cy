import sys
sys.path.insert(0, "F:/cy/codex/demo2/legal-rag")

with open("F:/cy/codex/demo2/legal-rag/webui/index.html", "r", encoding="utf-8") as f:
    html = f.read()

# ===== 1. Replace chat panel CSS =====
old_chat_css_start = html.find("/* ===== Floating Chat Assistant ===== */")
old_chat_css_end = html.find("</style>", old_chat_css_start)

new_chat_css = """/* ===== Centered Chat Assistant ===== */
.chat-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  z-index: 998;
  display: none;
  opacity: 0;
  transition: opacity 0.3s ease;
}
.chat-overlay.open { display: block; opacity: 1; }

.chat-toggle {
  position: fixed;
  right: 24px;
  bottom: 24px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1a56db, #2563eb);
  color: white;
  border: none;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(26, 86, 219, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  z-index: 1000;
  transition: all 0.3s ease;
}
.chat-toggle:hover { transform: scale(1.08); box-shadow: 0 6px 24px rgba(26, 86, 219, 0.45); }
.chat-toggle svg { width: 26px; height: 26px; fill: currentColor; }

.chat-panel {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%) scale(0.92);
  width: 720px;
  max-width: calc(100vw - 48px);
  height: 80vh;
  max-height: 700px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 16px 60px rgba(0,0,0,0.25);
  display: flex;
  flex-direction: column;
  z-index: 999;
  opacity: 0;
  pointer-events: none;
  transition: all 0.3s ease;
  overflow: hidden;
  border: 1px solid var(--border);
}
.chat-panel.open {
  opacity: 1;
  pointer-events: auto;
  transform: translate(-50%, -50%) scale(1);
}

.chat-header {
  padding: 16px 20px;
  background: linear-gradient(135deg, #1a56db, #2563eb);
  color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}
.chat-header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
}
.chat-header-actions { display: flex; gap: 6px; }
.chat-header-btn {
  background: rgba(255,255,255,0.2);
  border: none;
  color: white;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  transition: background 0.2s;
}
.chat-header-btn:hover { background: rgba(255,255,255,0.35); }

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #f8fafc;
}
.chat-messages::-webkit-scrollbar { width: 5px; }
.chat-messages::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 3px; }

.chat-msg {
  max-width: 80%;
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.7;
  position: relative;
  word-break: break-word;
}
.chat-msg.user {
  align-self: flex-end;
  background: var(--primary);
  color: white;
  border-bottom-right-radius: 4px;
}
.chat-msg.assistant {
  align-self: flex-start;
  background: white;
  color: var(--text);
  border: 1px solid var(--border);
  border-bottom-left-radius: 4px;
  width: 100%;
  max-width: 100%;
}
.chat-msg .msg-time {
  font-size: 11px;
  opacity: 0.6;
  margin-top: 6px;
  display: block;
}
.chat-msg.user .msg-time { text-align: right; }

/* Markdown rendered content */
.chat-msg.assistant h1, .chat-msg.assistant h2, .chat-msg.assistant h3 {
  margin: 12px 0 6px 0;
  font-weight: 600;
  line-height: 1.4;
}
.chat-msg.assistant h1 { font-size: 18px; }
.chat-msg.assistant h2 { font-size: 16px; }
.chat-msg.assistant h3 { font-size: 15px; }
.chat-msg.assistant p { margin: 6px 0; }
.chat-msg.assistant ul, .chat-msg.assistant ol { margin: 6px 0; padding-left: 22px; }
.chat-msg.assistant li { margin: 3px 0; }
.chat-msg.assistant strong { font-weight: 600; }
.chat-msg.assistant em { font-style: italic; }
.chat-msg.assistant code {
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  font-family: "Cascadia Code", "Fira Code", "Consolas", monospace;
}
.chat-msg.assistant pre {
  background: #1e293b;
  color: #e2e8f0;
  padding: 14px 16px;
  border-radius: 10px;
  overflow-x: auto;
  margin: 10px 0;
  font-size: 13px;
  line-height: 1.5;
}
.chat-msg.assistant pre code {
  background: transparent;
  padding: 0;
  color: inherit;
  font-size: inherit;
}
.chat-msg.assistant blockquote {
  border-left: 3px solid var(--primary);
  padding-left: 12px;
  margin: 8px 0;
  color: var(--text-secondary);
}
.chat-msg.assistant table {
  border-collapse: collapse;
  margin: 8px 0;
  width: 100%;
  font-size: 13px;
}
.chat-msg.assistant th, .chat-msg.assistant td {
  border: 1px solid var(--border);
  padding: 6px 10px;
  text-align: left;
}
.chat-msg.assistant th { background: #f1f5f9; font-weight: 600; }
.chat-msg.assistant hr { border: none; border-top: 1px solid var(--border); margin: 12px 0; }
.chat-msg.assistant a { color: var(--primary); text-decoration: underline; }

.chat-typing {
  align-self: flex-start;
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 14px 18px;
  background: white;
  border: 1px solid var(--border);
  border-radius: 14px;
  border-bottom-left-radius: 4px;
}
.chat-typing .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #94a3b8;
  animation: typingBounce 1.4s infinite ease-in-out;
}
.chat-typing .dot:nth-child(2) { animation-delay: 0.2s; }
.chat-typing .dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes typingBounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}

.chat-input-area {
  padding: 14px 20px;
  border-top: 1px solid var(--border);
  display: flex;
  gap: 10px;
  align-items: flex-end;
  flex-shrink: 0;
  background: white;
}
.chat-input-area textarea {
  flex: 1;
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px 14px;
  font-size: 14px;
  font-family: inherit;
  resize: none;
  outline: none;
  min-height: 42px;
  max-height: 120px;
  line-height: 1.5;
  transition: border-color 0.2s;
}
.chat-input-area textarea:focus { border-color: var(--primary); }
.chat-send-btn {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background: var(--primary);
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.2s;
}
.chat-send-btn:hover { background: #1648c0; }
.chat-send-btn:disabled { background: #93b3f0; cursor: not-allowed; }
.chat-send-btn svg { width: 18px; height: 18px; fill: currentColor; }

.chat-welcome {
  text-align: center;
  color: var(--text-secondary);
  padding: 50px 30px;
  font-size: 14px;
  line-height: 1.8;
}
.chat-welcome .icon { font-size: 44px; margin-bottom: 14px; display: block; }
.chat-welcome .suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  margin-top: 14px;
}
.chat-welcome .suggestions button {
  font-size: 13px;
  padding: 6px 16px;
  border-radius: 16px;
  background: var(--primary-light);
  color: var(--primary);
  border: none;
  cursor: pointer;
  transition: background 0.2s;
}
.chat-welcome .suggestions button:hover { background: #d4e2fc; }

.chat-error-msg {
  align-self: center;
  font-size: 13px;
  color: var(--danger);
  background: #fef2f2;
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid #fecaca;
  max-width: 90%;
}

@media (max-width: 640px) {
  .chat-panel {
    width: calc(100vw - 16px);
    height: calc(100vh - 16px);
    max-height: none;
    border-radius: 12px;
  }
  .chat-toggle { right: 16px; bottom: 16px; width: 50px; height: 50px; font-size: 22px; }
  .chat-msg { max-width: 90%; }
  .chat-msg.assistant { max-width: 100%; }
}
"""

html = html[:old_chat_css_start] + new_chat_css + html[old_chat_css_end:]

# ===== 2. Add marked.js CDN before </head> =====
marked_script = '<script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>\n'
html = html.replace("</head>", marked_script + "</head>")

# ===== 3. Add overlay div after chat-toggle button =====
overlay_div = '\n<div class="chat-overlay" id="chatOverlay" onclick="toggleChat()"></div>\n'
html = html.replace('<button class="chat-toggle"', overlay_div + '<button class="chat-toggle"')

# ===== 4. Update addChatMsg to render markdown for assistant =====
old_addChatMsg = """function addChatMsg(role, content) {
  const container = document.getElementById("chatMessages");
  const welcome = document.getElementById("chatWelcome");
  if (welcome) welcome.style.display = "none";

  const div = document.createElement("div");
  div.className = "chat-msg " + role;
  const now = new Date();
  const timeStr = now.getHours().toString().padStart(2,"0") + ":" + now.getMinutes().toString().padStart(2,"0");
  div.innerHTML = escapeHtml(content) + '<span class="msg-time">' + timeStr + '</span>';
  container.appendChild(div);
  container.scrollTop = container.scrollHeight;
}"""

new_addChatMsg = """function addChatMsg(role, content) {
  const container = document.getElementById("chatMessages");
  const welcome = document.getElementById("chatWelcome");
  if (welcome) welcome.style.display = "none";

  const div = document.createElement("div");
  div.className = "chat-msg " + role;
  const now = new Date();
  const timeStr = now.getHours().toString().padStart(2,"0") + ":" + now.getMinutes().toString().padStart(2,"0");
  if (role === "assistant") {
    div.innerHTML = marked.parse(content) + '<span class="msg-time">' + timeStr + '</span>';
  } else {
    div.innerHTML = escapeHtml(content) + '<span class="msg-time">' + timeStr + '</span>';
  }
  container.appendChild(div);
  container.scrollTop = container.scrollHeight;
}"""

html = html.replace(old_addChatMsg, new_addChatMsg)

# ===== 5. Update toggleChat to also toggle overlay =====
old_toggle = """function toggleChat() {
  const panel = document.getElementById("chatPanel");
  const toggle = document.getElementById("chatToggle");
  panel.classList.toggle("open");
  if (panel.classList.contains("open")) {
    setTimeout(() => document.getElementById("chatInput").focus(), 300);
  }
}"""

new_toggle = """function toggleChat() {
  const panel = document.getElementById("chatPanel");
  const overlay = document.getElementById("chatOverlay");
  const toggle = document.getElementById("chatToggle");
  panel.classList.toggle("open");
  overlay.classList.toggle("open");
  if (panel.classList.contains("open")) {
    setTimeout(() => document.getElementById("chatInput").focus(), 300);
  }
}"""

html = html.replace(old_toggle, new_toggle)

with open("F:/cy/codex/demo2/legal-rag/webui/index.html", "w", encoding="utf-8") as f:
    f.write(html)
print("done")
