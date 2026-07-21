# Windows 11 + vLLM + 双 RTX 5090 本地模型部署教程

## 环境说明

| 项目 | 配置 |
|------|------|
| 操作系统 | Windows 11 |
| GPU | 2 × NVIDIA GeForce RTX 5090 32GB |
| 驱动版本 | 591.74（CUDA 13.1） |
| 总显存 | 64GB |
| 推理框架 | vLLM（SystemPanic Windows 适配版） |

> **为什么不用 Ollama？** Ollama 当前 Windows 版本捆绑的 CUDA 后端（cuda_v12/cuda_v13）在 RTX 5090 + CUDA 13.1 环境下会直接崩溃（0xc0000005 内存访问违例），所有模型回退到 CPU 推理。这是 Ollama 二进制与新版驱动的兼容性问题，非配置问题。

---

## 第一步：安装 Python 3.12

vLLM 0.25.0 需要 **Python 3.12**（不要用 3.11 或 3.13+）。

1. 下载 Python 3.12：https://www.python.org/downloads/
2. 安装时 **勾选 "Add Python to PATH"**
3. 验证：

```powershell
python --version
# 应输出: Python 3.12.x
```

---

## 第二步：创建虚拟环境

```powershell
# 进入你的工作目录
cd D:\ai-server    # 换成你自己的路径

# 创建虚拟环境
python -m venv vllm_env

# 激活虚拟环境
vllm_env\Scripts\activate

# 终端提示符前面出现 (vllm_env) 即表示成功
```

---

## 第三步：安装 PyTorch（CUDA 13 版本）

```powershell
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu130
```

此命令会安装 PyTorch 2.11 及对应的 CUDA 13 运行时，下载量约 3GB，等待完成。

**验证 PyTorch 能否识别 GPU：**

```powershell
python -c "import torch; print('CUDA:', torch.cuda.is_available()); print('GPU数:', torch.cuda.device_count()); print('GPU0:', torch.cuda.get_device_name(0)); print('GPU1:', torch.cuda.get_device_name(1))"
```

预期输出：

```
CUDA: True
GPU数: 2
GPU0: NVIDIA GeForce RTX 5090
GPU1: NVIDIA GeForce RTX 5090
```

---

## 第四步：安装 vLLM（Windows 适配版）

使用 **SystemPanic/vllm-windows** 预编译版本 v0.25.0（2026-07-12 发布），支持 Blackwell RTX 50 系列、CUDA 13、多卡 NCCL 张量并行。

```powershell
# 下载 wheel 包（194MB）
curl -L -o vllm-0.25.0+cu132-cp312-cp312-win_amd64.whl ^
  "https://github.com/SystemPanic/vllm-windows/releases/download/v0.25.0/vllm-0.25.0+cu132-cp312-cp312-win_amd64.whl"

# 安装
pip install vllm-0.25.0+cu132-cp312-cp312-win_amd64.whl --extra-index-url https://download.pytorch.org/whl/cu130
```

安装完成后验证：

```powershell
python -c "import vllm; print('vLLM version:', vllm.__version__)"
```

---

## 第五步：小模型测试（单卡）

先用一个小模型验证 GPU 推理通道是否正常：

```powershell
python -c "
from vllm import LLM, SamplingParams

# 加载 Qwen3-1.7B（约 3.4GB，auto 模式自动放 GPU）
llm = LLM(model='Qwen/Qwen3-1.7B-Instruct', trust_remote_code=True)

prompts = ['你好，请用一句话介绍你自己。']
outputs = llm.generate(prompts, SamplingParams(temperature=0.7, max_tokens=128))

for output in outputs:
    print(output.outputs[0].text)
"
```

**预期现象：**
- 模型下载后加载时，任务管理器 GPU 显存占用会上升
- 生成速度应该很快（5090 上约 100-200 tokens/s）
- 如果看到显存占用为 0，说明有问题

---

## 第六步：验证双卡并行（张量并行）

这才是跑大模型的关键——让两张 5090 当一张 64GB 卡用：

```powershell
python -c "
from vllm import LLM, SamplingParams

# tensor_parallel_size=2 启用跨卡张量并行
# 用 Qwen3-14B 测试（约 28GB fp16，一张卡放不下，必须跨卡）
llm = LLM(
    model='Qwen/Qwen3-14B-Instruct',
    trust_remote_code=True,
    tensor_parallel_size=2,
    gpu_memory_utilization=0.90
)

prompts = ['用中文解释什么是量子纠缠。']
outputs = llm.generate(prompts, SamplingParams(temperature=0.7, max_tokens=256))

for output in outputs:
    print(output.outputs[0].text)
"
```

**预期现象：**
- 两张卡的显存都会被占用（nvidia-smi 可见）
- GPU 利用率两张卡都会上升
- 推理速度应该流畅

---

## 第七步：启动 API 服务（OpenAI 兼容）

```powershell
# 启动 API 服务器，双卡张量并行跑 14B 模型
python -m vllm.entrypoints.openai.api_server ^
  --model Qwen/Qwen3-14B-Instruct ^
  --trust-remote-code ^
  --tensor-parallel-size 2 ^
  --gpu-memory-utilization 0.90 ^
  --port 8000
```

服务启动后，另开终端测试：

```powershell
curl http://localhost:8000/v1/chat/completions ^
  -H "Content-Type: application/json" ^
  -d "{\"model\":\"Qwen/Qwen3-14B-Instruct\",\"messages\":[{\"role\":\"user\",\"content\":\"你好\"}]}"
```

---

## 常用参数说明

| 参数 | 说明 | 建议值 |
|------|------|--------|
| `--tensor-parallel-size` | 张量并行卡数 | 2（你双卡就用 2） |
| `--gpu-memory-utilization` | 显存利用率上限 | 0.85-0.90 |
| `--max-model-len` | 最大上下文长度 | 按需设置，越大越吃显存 |
| `--dtype` | 推理精度 | auto（自动匹配模型） |
| `--port` | API 服务端口 | 8000 |

---

## 不同模型的推荐配置

| 模型 | 参数量 | 精度 | 显存需求 | 配置 |
|------|--------|------|---------|------|
| Qwen3-1.7B | 1.7B | fp16 | ~3.4GB | 单卡即可 |
| Qwen3-8B | 8B | fp16 | ~16GB | 单卡即可 |
| Qwen3-14B | 14B | fp16 | ~28GB | 单卡 32GB 可跑（加长上下文需双卡） |
| Qwen3-32B | 32B | fp16 | ~64GB | `--tensor-parallel-size 2` |
| Qwen3-72B | 72B | int4 | ~40GB | `--tensor-parallel-size 2 --quantization awq` |
| DeepSeek-V3 | 671B (MoE) | int4 | ~180GB+ | 显存不够，需更多卡 |

---

## 故障排查

### 问题：`no kernel image is available for execution`

**原因：** PyTorch 版本不支持 RTX 5090 的 sm_120 计算能力。

**解决：** 确认 PyTorch 是从 `https://download.pytorch.org/whl/cu130` 安装的 CUDA 13 版本：

```powershell
pip show torch | findstr "Version"
# 应显示 2.11.x 或更高
```

### 问题：`CUDA out of memory`

**解决：**
- 降低 `--gpu-memory-utilization`（如设为 0.80）
- 缩短 `--max-model-len`（减少上下文长度）
- 使用量化模型 `--quantization awq` 或 `--quantization gptq`

### 问题：显存中有一张卡空闲

**解决：** 确认带了 `--tensor-parallel-size 2` 参数，且两张卡都在 nvidia-smi 中正常显示。

### 问题：下载模型太慢

**解决：** 设置 HuggingFace 镜像：

```powershell
$env:HF_ENDPOINT="https://hf-mirror.com"
```

---

## 进阶：使用 LM Studio 作为备选

如果觉得 vLLM 配置麻烦，LM Studio 可以开箱即用（但不支持跨卡张量并行）：

1. 下载：https://lmstudio.ai/
2. 安装后搜索下载模型
3. 自动使用 GPU 推理
4. 也有本地 API 服务（OpenAI 兼容）

**缺点：只能用单张 5090（32GB），双卡不能协同工作。**

---

## 参考链接

- vLLM Windows 适配版：https://github.com/SystemPanic/vllm-windows
- vLLM 官方文档：https://docs.vllm.ai/
- PyTorch CUDA 13 版本：https://pytorch.org/get-started/locally/
- HuggingFace 模型镜像：https://hf-mirror.com/
