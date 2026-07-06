import sys
sys.path.insert(0, "F:/cy/codex/demo2/legal-rag")

# Read the existing HTML
with open("F:/cy/codex/demo2/legal-rag/webui/index.html", "r", encoding="utf-8") as f:
    html = f.read()

# Insert chat CSS before the closing </style>
chat_css = """
/* ===== Floating Chat Assistant ===== */
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
  right: 24px;
  bottom: 90px;
  width: 380px;
  height: 580px;
  max-height: calc(100vh - 140px);
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(0,0,0,0.15);
  display: flex;
  flex-direction: column;
  z-index: 999;
  transform: translateY(20px);
  opacity: 0;
  pointer-events: none;
  transition: all 0.3s ease;
  overflow: hidden;
  border: 1px solid var(--border);
}
.chat-panel.open {
  transform: translateY(0);
  opacity: 1;
  pointer-events: auto;
}

.chat-header {
  padding: 14px 16px;
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
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
}
.chat-header-actions { display: flex; gap: 6px; }
.chat-header-btn {
  background: rgba(255,255,255,0.2);
  border: none;
  color: white;
  width: 30px;
  height: 30px;
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
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #f8fafc;
}
.chat-messages::-webkit-scrollbar { width: 4px; }
.chat-messages::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 2px; }

.chat-msg {
  max-width: 85%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  position: relative;
  word-break: break-word;
  white-space: pre-wrap;
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
}
.chat-msg .msg-time {
  font-size: 11px;
  opacity: 0.6;
  margin-top: 4px;
  display: block;
}
.chat-msg.user .msg-time { text-align: right; }

.chat-typing {
  align-self: flex-start;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
  background: white;
  border: 1px solid var(--border);
  border-radius: 12px;
  border-bottom-left-radius: 4px;
}
.chat-typing .dot {
  width: 7px;
  height: 7px;
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
  padding: 12px 16px;
  border-top: 1px solid var(--border);
  display: flex;
  gap: 8px;
  align-items: flex-end;
  flex-shrink: 0;
  background: white;
}
.chat-input-area textarea {
  flex: 1;
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 14px;
  font-family: inherit;
  resize: none;
  outline: none;
  min-height: 40px;
  max-height: 120px;
  line-height: 1.4;
  transition: border-color 0.2s;
}
.chat-input-area textarea:focus { border-color: var(--primary); }
.chat-send-btn {
  width: 40px;
  height: 40px;
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
  padding: 40px 20px;
  font-size: 14px;
  line-height: 1.8;
}
.chat-welcome .icon { font-size: 40px; margin-bottom: 12px; display: block; }
.chat-welcome .suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;
  margin-top: 12px;
}
.chat-welcome .suggestions button {
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 14px;
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
  padding: 8px 14px;
  border-radius: 8px;
  border: 1px solid #fecaca;
}

@media (max-width: 640px) {
  .chat-panel {
    right: 12px;
    bottom: 80px;
    width: calc(100vw - 24px);
    height: calc(100vh - 120px);
    max-height: none;
    border-radius: 12px;
  }
  .chat-toggle { right: 16px; bottom: 16px; width: 50px; height: 50px; font-size: 22px; }
}
"""

html = html.replace("</style>", chat_css + "\n</style>")

# Insert chat HTML before the closing </body>
chat_html = """
<!-- ===== Floating Chat Assistant ===== -->
<button class="chat-toggle" id="chatToggle" onclick="toggleChat()" title="打开AI助手">
  <svg viewBox="0 0 24 24"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/><path d="M7 9h2v2H7zm4 0h2v2h-2zm4 0h2v2h-2z"/></svg>
</button>

<div class="chat-panel" id="chatPanel">
  <div class="chat-header">
    <div class="chat-header-title">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/></svg>
      AI 助手
    </div>
    <div class="chat-header-actions">
      <button class="chat-header-btn" onclick="clearChat()" title="清空对话">🗑</button>
      <button class="chat-header-btn" onclick="toggleChat()" title="收起">✕</button>
    </div>
  </div>
  <div class="chat-messages" id="chatMessages">
    <div class="chat-welcome" id="chatWelcome">
      <span class="icon">💬</span>
      你好！我是 AI 助手，可以回答你的各种问题。
      <div class="suggestions">
        <button onclick="chatSendSuggestion('合同应当包括哪些主要条款？')">合同主要条款</button>
        <button onclick="chatSendSuggestion('什么是自首？')">自首的定义</button>
        <button onclick="chatSendSuggestion('人工智能有哪些应用？')">AI 应用</button>
        <button onclick="chatSendSuggestion('用Python写一个快速排序')">写代码</button>
      </div>
    </div>
  </div>
  <div class="chat-input-area">
    <textarea id="chatInput" rows="1" placeholder="输入问题，Enter发送，Shift+Enter换行" onkeydown="chatHandleKey(event)"></textarea>
    <button class="chat-send-btn" id="chatSendBtn" onclick="chatSend()" title="发送">
      <svg viewBox="0 0 24 24"><path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/></svg>
    </button>
  </div>
</div>

<script>
// ===== Chat Assistant Logic =====
let chatHistory = [];
let chatSending = false;

function toggleChat() {
  const panel = document.getElementById("chatPanel");
  const toggle = document.getElementById("chatToggle");
  panel.classList.toggle("open");
  if (panel.classList.contains("open")) {
    setTimeout(() => document.getElementById("chatInput").focus(), 300);
  }
}

function addChatMsg(role, content) {
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
}

function showTyping() {
  const container = document.getElementById("chatMessages");
  const div = document.createElement("div");
  div.className = "chat-typing";
  div.id = "chatTyping";
  div.innerHTML = '<span class="dot"></span><span class="dot"></span><span class="dot"></span>';
  container.appendChild(div);
  container.scrollTop = container.scrollHeight;
}

function hideTyping() {
  const el = document.getElementById("chatTyping");
  if (el) el.remove();
}

function showChatError(msg) {
  const container = document.getElementById("chatMessages");
  const div = document.createElement("div");
  div.className = "chat-error-msg";
  div.textContent = "⚠ " + msg;
  container.appendChild(div);
  container.scrollTop = container.scrollHeight;
}

async function chatSend() {
  const input = document.getElementById("chatInput");
  const text = input.value.trim();
  if (!text || chatSending) return;

  chatSending = true;
  document.getElementById("chatSendBtn").disabled = true;

  // Add user message
  addChatMsg("user", text);
  chatHistory.push({ role: "user", content: text });
  input.value = "";
  input.style.height = "auto";

  showTyping();

  try {
    const r = await fetch(API_BASE + "/api/chat", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ messages: chatHistory, temperature: 0.7, max_tokens: 2048 }),
      signal: AbortSignal.timeout(30000),
    });

    hideTyping();

    if (!r.ok) {
      const err = await r.json();
      throw new Error(err.detail || "请求失败");
    }

    const data = await r.json();
    addChatMsg("assistant", data.reply);
    chatHistory.push({ role: "assistant", content: data.reply });
  } catch (e) {
    hideTyping();
    if (e.name === "TimeoutError" || e.name === "AbortError") {
      showChatError("请求超时，请稍后重试");
    } else {
      showChatError(e.message);
    }
  } finally {
    chatSending = false;
    document.getElementById("chatSendBtn").disabled = false;
    document.getElementById("chatInput").focus();
  }
}

function chatHandleKey(e) {
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    chatSend();
  }
  // Auto-resize textarea
  const el = e.target;
  el.style.height = "auto";
  el.style.height = Math.min(el.scrollHeight, 120) + "px";
}

function chatSendSuggestion(text) {
  document.getElementById("chatInput").value = text;
  chatSend();
}

function clearChat() {
  chatHistory = [];
  const container = document.getElementById("chatMessages");
  container.innerHTML = "";
  const welcome = document.getElementById("chatWelcome");
  welcome.style.display = "block";
  container.appendChild(welcome);
}

// Auto-focus input when panel opens
document.addEventListener("keydown", function(e) {
  const panel = document.getElementById("chatPanel");
  if (panel.classList.contains("open") && e.key === "Escape") {
    toggleChat();
  }
});
</script>
"""

html = html.replace("</body>", chat_html + "\n</body>")

with open("F:/cy/codex/demo2/legal-rag/webui/index.html", "w", encoding="utf-8") as f:
    f.write(html)
print("done")
