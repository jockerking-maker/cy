# 个人全局规则

> 位置：`~/.claude/CLAUDE.md` | 每次会话自动加载 | 目标 **< 100 行**
> 项目专属规则 → 项目根目录 `CLAUDE.md` | 模板 → `D:\APP\claudecode\CLAUDE.md.template`

---

## 规则优先级

1. 用户**当前对话**中的最新明确指令
2. 当前项目 `CLAUDE.md` / `CLAUDE.local.md`
3. 本文件
4. Auto memory（自动记忆，仅供参考）

---

## 工作流程

**收到任务后按顺序执行，不要跳步：**

1. **Explore** — 读相关代码、配置、`CLAUDE.md`；不确定时先问，不猜测
2. **Plan** — 多文件改动 / 不熟悉代码 / 方案不唯一时，先列计划再动手
3. **Implement** — 最小 diff，匹配现有风格，不顺手重构无关代码
4. **Verify** — 运行测试/构建/lint，用**命令输出**证明通过，不只说「已完成」

小改动（改 typo、单行修复）可跳过 Plan，直接 Implement + Verify。

---

## 硬性规则（NEVER / ALWAYS）

### NEVER

- NEVER 扩大任务范围：不做用户未要求的重构、依赖升级、文档生成
- NEVER 在用户未要求时 `git commit` / `git push`
- NEVER `git push --force` 到 `main` / `master`
- NEVER 修改 `git config`
- NEVER 硬编码 API Key、密码、Token；不提交 `.env` 等敏感文件
- NEVER 用 `git add -A` 盲加所有文件 — 按文件名逐个 stage
- NEVER 删除/覆盖/批量改文件前不说明影响范围

### ALWAYS

- ALWAYS 先读项目 `CLAUDE.md`（若存在），其规则覆盖本文件
- ALWAYS 复用现有函数/组件，不重复造轮子
- ALWAYS 改 bug 时优先写能复现该 bug 的测试（若项目有测试体系）
- ALWAYS 完成后用**中文**简要说明：改了什么、为什么、如何验证
- ALWAYS 给出可复制的验证命令（Windows PowerShell/CMD 可运行）
- ALWAYS **记录聊天**：由 hooks 自动写入工作区 `claude-chat-log.md`（见「聊天记录」）

---

## 聊天记录

由 **Hooks 自动写入**（`~/.claude/hooks/chat-log.py`），不依赖 Claude 手动执行。

- **路径**：当前打开工作区根目录下的 `claude-chat-log.md`
  - 例：打开 `C:\Users\26273\Documents\Claude\demo1` → `...\demo1\claude-chat-log.md`
  - 换开其他文件夹 → 写入对应文件夹根目录
- **机制**：`SessionStart` 锁定工作区 → `UserPromptSubmit` 记录用户 → `Stop` 记录助手回复
- **禁止写入**：API Key、密码、Token — 脚本会自动 `[已省略]`
- 验证 hooks：在 Claude Code 中输入 `/hooks` 查看是否已注册

---

## 代码与改动

- **最小 diff**：只改解决问题所需的行
- **不破坏 API**：除非用户要求，不改对外接口签名与行为
- **注释**：只解释 WHY，不解释 WHAT；代码应自解释
- **错误处理**：处理真实可能发生的错误，不为极端场景堆砌逻辑
- **测试**：用户要求时才新增；不写无意义断言

---

## Git

- Commit 信息说明**为什么改**，不只列文件名
- 一次 commit 聚焦一个逻辑变更

---

## 沟通

- 默认**简体中文**交流
- 代码/变量/注释语言跟随项目既有习惯
- 引用代码：`文件路径:行号`
- 响应长度匹配任务复杂度

---

## 本机环境

| 项 | 值 |
|----|-----|
| Claude Code | `claude`（`D:\APP\claudecode`） |
| API 切换 | CC Switch `D:\APP\ccswitch\cc-switch.exe` |
| 配置 | `~/.claude/settings.json`（由 CC Switch 写入，勿手改密钥） |

- CC Switch 切换供应商后 → **必须 `/exit` 重启 Claude Code** 才生效
- Shell 命令需兼容 Windows（PowerShell / CMD）

---

## 完成前自检

- [ ] 改动范围与用户要求一致
- [ ] 已运行相关测试/构建/lint 并贴出结果
- [ ] 无调试代码、临时代码、硬编码密钥
- [ ] 未做未要求的 commit

---

## 维护

- 同一错误纠正 **2 次** → 把规则写进本文件或项目 `CLAUDE.md`
- 每行自问：「删掉这行 Claude 会犯错吗？」→ 不会就删
- 详细工作流、项目结构、命令 → 放项目 `CLAUDE.md`，不放本文件
