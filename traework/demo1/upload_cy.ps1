<#
.SYNOPSIS
    将 F:\cy 目录下的文件增量上传到 GitHub 仓库 jockerking-maker/cy
.DESCRIPTION
    - 更新 .gitignore 规则
    - 保留文件原有目录结构
    - 仅上传新增/修改的文件
    - 上传前展示文件清单，等待用户确认后执行
.PARAMETER AutoConfirm
    跳过确认步骤，直接执行推送
.PARAMETER CommitMessage
    自定义 commit 消息
#>
param(
    [switch]$AutoConfirm,
    [string]$CommitMessage = ""
)

$ErrorActionPreference = "Stop"
$RepoPath = "F:\cy"

# 颜色输出
function Write-CL { param($T, $C="White") Write-Host $T -ForegroundColor $C }

# ========== 步骤 1: 前置检查 ==========
Write-CL "`n========================================" "Cyan"
Write-CL "  GitHub 增量推送脚本 - jockerking-maker/cy" "Cyan"
Write-CL "========================================`n" "Cyan"

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Write-CL "[错误] 未检测到 git" "Red"; exit 1
}
if (-not (Test-Path $RepoPath)) {
    Write-CL "[错误] 目录不存在: $RepoPath" "Red"; exit 1
}
Set-Location $RepoPath
Write-CL "[信息] 工作目录: $RepoPath" "Gray"
if (-not (Test-Path ".git")) {
    Write-CL "[错误] 不是 Git 仓库" "Red"; exit 1
}

# ========== 步骤 2: 更新 .gitignore ==========
Write-CL "`n>>> [1/5] 更新 .gitignore 规则..." "Yellow"

$GitignoreLines = @(
    "# ============================================",
    "# Git 自身",
    "# ============================================",
    ".git/",
    "",
    "# ============================================",
    "# Agent / AI 工作目录",
    "# ============================================",
    ".workbuddy/",
    ".codebuddy/",
    ".trae-cn/",
    ".cursor/",
    ".codex/",
    ".composer/",
    ".copilot/",
    ".claude/",
    "",
    "# ============================================",
    "# 依赖包 & 虚拟环境",
    "# ============================================",
    "node_modules/",
    ".conda/",
    ".venv/",
    "venv/",
    "env/",
    ".m2/",
    ".gradle/",
    "",
    "# ============================================",
    "# IDE / 编辑器配置",
    "# ============================================",
    ".idea/",
    ".vscode/",
    "*.swp",
    "*.swo",
    "*~",
    "",
    "# ============================================",
    "# Python 编译产物",
    "# ============================================",
    "__pycache__/",
    "*.py[cod]",
    "*.pyo",
    "*.pyd",
    "*.so",
    "*.egg-info/",
    "dist/",
    "build/",
    "*.egg",
    "",
    "# ============================================",
    "# 系统文件",
    "# ============================================",
    ".DS_Store",
    "Thumbs.db",
    "Desktop.ini",
    '$RECYCLE.BIN/',
    "",
    "# ============================================",
    "# 环境变量 & 密钥",
    "# ============================================",
    ".env",
    ".env.local",
    ".env.*.local",
    "*.key",
    "*.pem",
    "secrets/",
    "",
    "# ============================================",
    "# 压缩包 / 大文件",
    "# ============================================",
    "*.rar",
    "*.7z",
    "*.zip",
    "*.tar.gz",
    "*.tgz",
    "",
    "# ============================================",
    "# 二进制 / 运行时文件",
    "# ============================================",
    "*.dll",
    "*.exe",
    "*.msi",
    "*.pdb",
    "",
    "# ============================================",
    "# 日志",
    "# ============================================",
    "*.log",
    "logs/",
    "",
    "# ============================================",
    "# 不需要推送的目录",
    "# ============================================",
    "linux/",
    "Oppo Connect/",
    "vm/",
    "WorkBuddy/",
    "",
    "# ============================================",
    "# 临时文件",
    "# ============================================",
    "*.tmp",
    "*.temp",
    "*.cache"
)

try {
    $GitignoreContent = $GitignoreLines -join "`r`n"
    Set-Content -Path ".gitignore" -Value $GitignoreContent -Encoding UTF8 -Force
    Write-CL "  [OK] .gitignore 已更新" "Green"
    Write-CL "  忽略: Agent工作目录 | 依赖包 | IDE配置 | 编译产物 | 压缩包 | 环境变量 | 系统文件" "Gray"
} catch {
    Write-CL "  [!] .gitignore 更新失败: $_" "Red"
}

# ========== 步骤 3: 检查远程仓库 ==========
Write-CL "`n>>> [2/5] 检查远程仓库配置..." "Yellow"
$RemoteUrl = git remote get-url origin 2>$null
if ($RemoteUrl -match "jockerking-maker/cy") {
    Write-CL "  [OK] 远程仓库: $RemoteUrl" "Green"
} else {
    Write-CL "  [!] 正在配置远程仓库..." "Yellow"
    if ($RemoteUrl) { git remote remove origin 2>$null }
    git remote add origin git@github.com:jockerking-maker/cy.git
    Write-CL "  [OK] 远程仓库已配置" "Green"
}

# ========== 步骤 4: 扫描变更文件 ==========
Write-CL "`n>>> [3/5] 扫描变更文件..." "Yellow"
Write-CL "`n  --- 即将推送的文件清单 ---" "Cyan"

$StatusOutput = git status --porcelain 2>&1

if (-not $StatusOutput -or $StatusOutput.Count -eq 0) {
    Write-CL "  [信息] 没有检测到变更，工作区已是最新状态。" "Green"
    exit 0
}

$NewFiles = @()
$ModifiedFiles = @()
$DeletedFiles = @()

foreach ($line in $StatusOutput) {
    if (-not $line) { continue }
    $status = $line.Substring(0, 2).Trim()
    $file = $line.Substring(2).Trim()
    switch -Wildcard ($status) {
        "??" { $NewFiles += $file }
        "M*" { $ModifiedFiles += $file }
        "A*" { $NewFiles += $file }
        "D*" { $DeletedFiles += $file }
        "R*" { $ModifiedFiles += $file }
        "AM" { $NewFiles += $file }
        default { $ModifiedFiles += $file }
    }
}

if ($NewFiles.Count -gt 0) {
    Write-CL "  [新增] $($NewFiles.Count) 个:" "Green"
    $NewFiles | ForEach-Object { Write-CL "    + $_" "Green" }
}
if ($ModifiedFiles.Count -gt 0) {
    Write-CL "  [修改] $($ModifiedFiles.Count) 个:" "Yellow"
    $ModifiedFiles | ForEach-Object { Write-CL "    ~ $_" "Yellow" }
}
if ($DeletedFiles.Count -gt 0) {
    Write-CL "  [删除] $($DeletedFiles.Count) 个:" "Red"
    $DeletedFiles | ForEach-Object { Write-CL "    - $_" "Red" }
}

$TotalChanges = $NewFiles.Count + $ModifiedFiles.Count + $DeletedFiles.Count
Write-CL "`n  --- 总计: $TotalChanges 个变更 ---" "Cyan"

# ========== 步骤 5: 确认并推送 ==========
if (-not $AutoConfirm) {
    Write-CL "`n>>> [4/5] 请确认操作..." "Yellow"
    $Confirm = Read-Host "  是否确认推送? (Y/n)"
    if ($Confirm -ne "" -and $Confirm -ne "Y" -and $Confirm -ne "y") {
        Write-CL "  [取消] 用户取消推送" "Red"; exit 0
    }
} else {
    Write-CL "`n>>> [4/5] 自动确认模式，跳过确认..." "Yellow"
}

Write-CL "`n>>> [5/5] 执行推送..." "Yellow"

# git add
Write-CL "  [1/3] git add -A ..." "Gray"
git add -A
if ($LASTEXITCODE -ne 0) { Write-CL "  [错误] git add 失败" "Red"; exit 1 }
Write-CL "  [OK] 文件已暂存" "Green"

# git commit
if (-not $CommitMessage) {
    $Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $CommitMessage = "Update: $Timestamp ($TotalChanges files)"
}
Write-CL "  [2/3] git commit -m `"$CommitMessage`" ..." "Gray"
git commit -m $CommitMessage
if ($LASTEXITCODE -ne 0) { Write-CL "  [错误] git commit 失败" "Red"; exit 1 }
Write-CL "  [OK] 提交成功" "Green"

# git push
Write-CL "  [3/3] git push origin main ..." "Gray"
git push origin main
if ($LASTEXITCODE -ne 0) { Write-CL "  [错误] git push 失败" "Red"; exit 1 }
Write-CL "  [OK] 推送成功" "Green"

Write-CL "`n========================================" "Cyan"
Write-CL "  推送完成! https://github.com/jockerking-maker/cy" "Cyan"
Write-CL "========================================`n" "Cyan"