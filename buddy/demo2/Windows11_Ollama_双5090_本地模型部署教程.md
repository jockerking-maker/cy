# Windows 11 双卡 RTX 5090 (32GB) + Ollama 本地模型部署教程

> **环境概况**
> - 操作系统：Windows 11
> - GPU：双卡 NVIDIA RTX 5090 × 32GB 显存（共 64GB）
> - 驱动版本：591（已安装）
> - CUDA：待安装
> - 目标：使用 Ollama 部署和管理本地大语言模型

---

## 目录

1. [CUDA 安装](#1-cuda-安装)
2. [Ollama 安装与配置](#2-ollama-安装与配置)
3. [双卡配置与验证](#3-双卡配置与验证)
4. [模型下载与运行](#4-模型下载与运行)
5. [常用模型推荐](#5-常用模型推荐)
6. [进阶配置](#6-进阶配置)
7. [常见问题排查](#7-常见问题排查)
8. [性能优化建议](#8-性能优化建议)

---

## 1. CUDA 安装

### 1.1 确认驱动兼容性

驱动 591 属于 R590 分支，支持到 CUDA 13.x。打开 PowerShell 确认当前驱动：

```powershell
nvidia-smi
```

`nvidia-smi` 顶部右侧的 `CUDA Version` 字段代表**当前驱动能支持的最高 CUDA 版本**（注意：这并不代表你已经装了 CUDA）。

如果你的输出类似：
```
+-----------------------------------------------------------------------------+
| NVIDIA-SMI 591.74       Driver Version: 591.74       CUDA Version: 13.1     |
|-----------------------------------------+---------------------------------+
```

说明你的驱动支持 **CUDA 12.8 / 13.0 / 13.1** 等版本。

### 1.2 选择 CUDA 版本

针对 RTX 5090（Blackwell 架构，sm_120）：

| CUDA 版本 | 是否推荐 | 说明 |
|----------|---------|------|
| **13.1** | ✅ 推荐 | 591 驱动最高支持版本，最新，Blackwell 支持最好 |
| **13.0** | ✅ 可用 | 同样适合 Blackwell |
| 12.8 | ✅ 可用 | 稳定版，Ollama 兼容性更广 |
| 12.6 及以下 | ❌ 不建议 | 对 Blackwell 支持不完整 |

**推荐安装 CUDA 13.1**（最新版，对 5090 优化最完善）。

### 1.3 下载 CUDA Toolkit

- 官方下载地址：https://developer.nvidia.com/cuda-downloads
- 选择：Windows → x86_64 → 11 → exe (local)

或者直接进入下载页：
- CUDA 13.1：https://developer.nvidia.com/cuda-13-1-0-download-archive
- CUDA 12.8（备选）：https://developer.nvidia.com/cuda-12-8-0-download-archive

> 提示：如果你 `nvidia-smi` 显示的 `CUDA Version` 是 12.x，那么只能装 12.x 及以下版本，不能跨大版本安装。

### 1.3 安装 CUDA

1. 以 **管理员身份** 运行下载的安装程序
2. 安装选项选择 **精简（Express）** 即可，包含：
   - CUDA Toolkit
   - CUDA 驱动程序组件（会自动匹配/更新到兼容版本）
   - Nsight 相关工具（可选）
3. 安装完成后重启计算机

### 1.5 验证 CUDA 安装

```powershell
# 查看 CUDA 版本（应该显示 13.1 或你装的版本）
nvcc --version

# 确认 nvidia-smi 正常检测到双卡
nvidia-smi
```

预期 `nvidia-smi` 应显示两张 RTX 5090，每张 32GB 显存（截图 1.1 步骤已经验证了 591 驱动下显示 32607MiB）。

### 1.6 配置环境变量（通常自动完成）

安装程序一般会自动添加以下环境变量，如未添加可手动设置（以 13.1 为例）：

```
CUDA_PATH = C:\Program Files\NVIDIA GPU Computing Toolkit\CUDA\v13.1
PATH 中添加 = %CUDA_PATH%\bin;%CUDA_PATH%\libnvvp
```

---

## 2. Ollama 安装与配置

### 2.1 安装 Ollama

下载 Windows 版 Ollama：

- 官方：https://ollama.com/download/windows
- 或直接下载：https://ollama.com/download/OllamaSetup.exe

双击安装，一路下一步即可。安装完成后 Ollama 会作为 Windows 服务在后台运行，任务栏右下角可见 Ollama 图标。

### 2.2 修改模型存储路径（强烈推荐）

默认模型存储在 `C:\Users\<用户名>\.ollama`，但大模型动辄几十 GB，建议修改到空间充裕的盘符。

**方法一：环境变量（推荐）**

```powershell
# 以管理员身份运行 PowerShell
[Environment]::SetEnvironmentVariable("OLLAMA_MODELS", "D:\ollama\models", "User")
```

然后重启 Ollama：右键任务栏图标 → Quit Ollama → 重新打开。

**方法二：通过系统设置**

1. `Win + R` → 输入 `sysdm.cpl`
2. 高级 → 环境变量
3. 用户变量 → 新建
4. 变量名：`OLLAMA_MODELS`
5. 变量值：`D:\ollama\models`（换成你想要的路径）

### 2.3 配置模型可见策略

```powershell
# 允许局域网其他设备访问 Ollama API（可选）
[Environment]::SetEnvironmentVariable("OLLAMA_HOST", "0.0.0.0:11434", "User")
```

> ⚠️ 注意：生产环境请谨慎开放网络访问，建议仅在内网使用。

---

## 3. 双卡配置与验证

### 3.1 检查双卡识别

安装完成后，在命令行中验证：

```powershell
# 重启 Ollama 后测试
ollama --version

# 查看可用 GPU
nvidia-smi
```

### 3.2 双卡工作原理

Ollama 原生支持多 GPU 推理（通过 CUDA），会自动使用 **所有可见的 GPU**。对于双 5090（共 64GB 显存），你可以：

- **模型完全放入显存**：70B 级别模型（如 Llama 3.3 70B 的 Q4_K_M 量化版约 40GB）可完全加载在显存中，推理速度极快
- **张量并行**：Ollama 自动将模型层分布到两张卡上，无需手动配置
- **并发服务**：两张卡可同时服务不同请求

### 3.3 手动指定 GPU（如需）

如果需要手动控制 GPU 使用，可以通过环境变量：

```powershell
# 仅使用 GPU 0
[Environment]::SetEnvironmentVariable("CUDA_VISIBLE_DEVICES", "0", "User")

# 使用 GPU 0 和 GPU 1（默认行为）
[Environment]::SetEnvironmentVariable("CUDA_VISIBLE_DEVICES", "0,1", "User")
```

> 大多数情况下不需要设置此项，Ollama 会自动使用全部 GPU。

---

## 4. 模型下载与运行

### 4.1 搜索可用模型

```powershell
# 搜索特定模型
ollama search qwen
ollama search deepseek
ollama search llama
```

或访问 https://ollama.com/search 浏览所有模型。

### 4.2 下载并运行模型

```powershell
# 格式：ollama run <模型名>:<标签>
# 标签通常是量化级别，如 :7b, :14b, :70b 等

# 示例 1：阿里 Qwen3（推荐）
ollama run qwen3:14b

# 示例 2：DeepSeek-R1 蒸馏版
ollama run deepseek-r1:32b

# 示例 3：Llama 3.3
ollama run llama3.3:70b
```

首次运行会自动下载模型文件。

### 4.3 常用命令

```powershell
# 列出已下载的模型
ollama list

# 查看模型详细信息（参数量、量化方式、大小等）
ollama show <模型名>

# 删除模型
ollama rm <模型名>

# 复制/重命名模型
ollama cp <源模型名> <新模型名>

# 仅下载模型（不运行）
ollama pull <模型名>
```

### 4.4 模型量化说明

- **fp16**：全精度，质量最高，显存占用最大（70B 模型约 140GB，双 5090 跑不了）
- **Q8_0**：8-bit 量化，质量接近 fp16（70B 约 70GB，双卡勉强）
- **Q4_K_M**：4-bit 量化，性价比最高，**推荐**（70B 约 40GB，双卡轻松跑）
- **Q2_K**：2-bit 量化，显存最小但质量损失明显

Ollama 默认标签（如 `qwen3:14b`）通常使用 Q4_K_M 量化。

---

## 5. 常用模型推荐

### 双 5090 (64GB 显存) 适合的模型

| 模型 | 参数量 | 推荐量化 | 显存需求 | 说明 |
|------|--------|---------|---------|------|
| **DeepSeek-R1** | 671B | Q2_K | ~180GB | 需搭配系统内存，太大会慢 |
| **DeepSeek-V3** | 671B | Q2_K | ~180GB | 同上 |
| **DeepSeek-R1-Distill-Llama** | 70B | Q4_K_M | ~40GB | ✅ 双卡流畅运行 |
| **Llama 3.3** | 70B | Q4_K_M | ~40GB | ✅ 双卡流畅运行 |
| **Qwen3** | 235B | Q2_K | ~65GB | 勉强能跑 |
| **Qwen3** | 72B | Q4_K_M | ~41GB | ✅ 推荐，双卡流畅 |
| **Qwen3** | 32B | Q8_0 | ~32GB | ✅ 单卡即流畅 |
| **Mistral Large** | 123B | Q4_K_M | ~70GB | 接近极限 |
| **Command R+** | 104B | Q4_K_M | ~60GB | ✅ 双卡流畅 |
| **Gemma 3** | 27B | Q8_0 | ~27GB | ✅ 单卡流畅 |
| **Phi-4** | 14B | Q8_0 | ~14GB | ✅ 单卡极快 |

### 中文能力优先推荐

```powershell
# Qwen3 系列 - 中文最强之一
ollama run qwen3:72b          # 双卡推荐，约 41GB
ollama run qwen3:32b          # 单卡推荐，约 18GB

# DeepSeek 系列 - 中文推理强
ollama run deepseek-r1:70b    # 双卡推荐，约 40GB
ollama run deepseek-r1:32b    # 单卡推荐，约 18GB
```

### 代码能力推荐

```powershell
ollama run deepseek-coder-v2:16b
ollama run codestral:22b
ollama run qwen3:32b          # 代码能力也很强
```

---

## 6. 进阶配置

### 6.1 创建自定义 Modelfile

为模型定制系统提示词和参数：

```powershell
# 创建 Modelfile
New-Item -Path .\Modelfile -ItemType File
```

Modelfile 示例：

```dockerfile
FROM qwen3:72b

# 设置系统提示词
SYSTEM "你是一个专业的技术顾问，回答时使用中文，简洁准确。"

# 推理参数
PARAMETER temperature 0.7
PARAMETER top_p 0.9
PARAMETER top_k 40

# 上下文长度（双 5090 显存充足，可以设大一些）
PARAMETER num_ctx 32768
```

创建自定义模型：

```powershell
ollama create my-assistant -f .\Modelfile
ollama run my-assistant
```

### 6.2 并发与并行配置

```powershell
# 设置并行请求数（默认 1）
[Environment]::SetEnvironmentVariable("OLLAMA_NUM_PARALLEL", "4", "User")

# 设置最大加载模型数
[Environment]::SetEnvironmentVariable("OLLAMA_MAX_LOADED_MODELS", "2", "User")
```

### 6.3 开启 Flash Attention（性能提升）

```powershell
[Environment]::SetEnvironmentVariable("OLLAMA_FLASH_ATTENTION", "1", "User")
```

配置后重启 Ollama 生效。

### 6.4 使用 API 调用

Ollama 自带 REST API：

```powershell
# 补全接口
curl http://localhost:11434/api/generate -d '{
  "model": "qwen3:14b",
  "prompt": "解释一下量子计算",
  "stream": false
}'

# 对话接口
curl http://localhost:11434/api/chat -d '{
  "model": "qwen3:14b",
  "messages": [
    {"role": "user", "content": "你好，请用中文回复"}
  ],
  "stream": false
}'

# 查看运行中的模型
curl http://localhost:11434/api/ps
```

### 6.5 Web UI 搭配方案

推荐使用 **Open WebUI**（前身为 Ollama WebUI）：

```powershell
# 使用 Docker 部署（需要先装 Docker Desktop）
docker run -d -p 3000:8080 --add-host=host.docker.internal:host-gateway `
  -v open-webui:/app/backend/data `
  --name open-webui `
  --restart always `
  ghcr.io/open-webui/open-webui:main
```

访问 http://localhost:3000 即可使用。

> 没有 Docker？也可以用 pip 直接在 Windows 上跑，或使用 **Chatbox**、**Lobe Chat** 等桌面客户端连接 Ollama API。

---

## 7. 常见问题排查

### 7.1 Ollama 无法识别 GPU

```powershell
# 确认 CUDA 是否正确安装
nvidia-smi
nvcc --version

# 重新安装 Ollama 或重启服务
# 右键任务栏 Ollama 图标 → Quit → 重新打开
```

如果依然不行，尝试重装 NVIDIA 驱动（清洁安装）。

### 7.2 模型加载失败 / OOM

- 检查显存是否足够：`nvidia-smi` 查看显存占用
- 尝试更低的量化级别：例如从 `:70b` 改为 `:70b-q2_K`
- 减少 `num_ctx` 值以降低 KV Cache 显存占用

### 7.3 两张卡负载不均衡

这是正常现象。Ollama 的张量并行会将模型层分配到两张卡上，通常一张卡负载较高（存储更多层）。只要两张卡都在工作，推理速度就是正常的。

### 7.4 驱动版本不兼容

驱动 591 是较新版本，完全兼容 CUDA 12.x 生态。如遇问题：

- 确保 CUDA 版本 ≤ 驱动支持的版本：`nvidia-smi` 右上角会显示
- 如装错版本，在控制面板中卸载 CUDA Toolkit 后重装

### 7.5 端口占用

```powershell
# 查看 11434 端口占用
netstat -ano | findstr 11434

# 更改 Ollama 端口
[Environment]::SetEnvironmentVariable("OLLAMA_HOST", "127.0.0.1:11435", "User")
```

---

## 8. 性能优化建议

### 8.1 模型选择策略

- **日常对话 / 翻译 / 摘要**：14B-32B 级别模型，单卡即可，速度极快（50-100 token/s）
- **复杂推理 / 编程**：70B 级别 Q4_K_M 量化模型，双卡并行
- **重度推理任务**：可尝试更高量化精度的 70B 模型（Q5_K_M 等）

### 8.2 上下文长度调优

```dockerfile
# 如果常处理长文档
PARAMETER num_ctx 65536    # 双卡 64GB 跑 70B-Q4 一般够用

# 日常短对话可调小以节省显存
PARAMETER num_ctx 8192
```

### 8.3 Flash Attention

Flash Attention 对长上下文场景提升明显：

```powershell
[Environment]::SetEnvironmentVariable("OLLAMA_FLASH_ATTENTION", "1", "User")
```

### 8.4 显存分配

双 5090 共 64GB 显存，推荐分配：

| 场景 | 模型大小 | 上下文 | 剩余显存 |
|------|---------|--------|---------|
| 70B-Q4 | ~40GB | 32K | ~8GB 余量 |
| 70B-Q5 | ~48GB | 16K | ~4GB 余量 |
| 32B-Q8 并发×2 | ~32GB×2 | 8K×2 | 刚好 |
| 14B-fp16 | ~28GB | 128K | 单卡绰绰有余 |

### 8.5 电源管理

```powershell
# 确认为高性能模式
powercfg /setactive 8c5e7fda-e8bf-4a96-9a85-a6e23a8c635c

# 查看当前电源计划
powercfg /getactivescheme
```

---

## 快速开始 Checklist

- [ ] 1. `nvidia-smi` 确认双卡 5090 和驱动 591 正常（截图 1.1 步骤已完成 ✓）
- [ ] 2. 安装 CUDA 13.1 Toolkit（驱动支持到 13.1，Blackwell 优化最好）
- [ ] 3. `nvcc --version` 验证 CUDA 安装（应显示 13.1）
- [ ] 4. 安装 Ollama Windows 版
- [ ] 5. 设置 `OLLAMA_MODELS` 环境变量（改存储路径）
- [ ] 6. 设置 `OLLAMA_FLASH_ATTENTION=1` 开启性能优化
- [ ] 7. `ollama run qwen3:14b` 测试小模型
- [ ] 8. `ollama run qwen3:72b` 加载双卡主力模型
- [ ] 9. 可选：部署 Open WebUI 获得图形界面
- [ ] 10. 开始愉快使用！

---

## 参考链接

- Ollama 官方文档：https://github.com/ollama/ollama
- Ollama 模型库：https://ollama.com/search
- CUDA 下载：https://developer.nvidia.com/cuda-downloads
- NVIDIA 驱动下载：https://www.nvidia.com/download/index.aspx
- Open WebUI：https://github.com/open-webui/open-webui
