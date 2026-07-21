# H20-3E (8卡) + Ubuntu 22.04 部署 DeepSeek V4 Pro 完整指南

> 更新时间：2026-07-06

---

## 一、硬件概览

| 组件 | 规格 |
|------|------|
| GPU | NVIDIA H20-3E × 8 |
| 单卡显存 | **141 GB** HBM3 |
| 总显存 | **1,128 GB** |
| NVLink | 18条 NVLink 互联，总带宽约 **478 GB/s** |
| 操作系统 | Ubuntu 22.04 LTS |
| 系统内存 | 建议 ≥ 1 TB |
| 存储 | ≥ 2 TB NVMe SSD（存放模型文件） |

**关键判断**：H20-3E 单卡 141G，8卡共 **1,128 GB**，而 DeepSeek V4 Pro 的 FP4+FP8 官方权重约 **862~865 GB**，加上 1M 上下文 KV Cache（~10GB）和运行时开销后约 **900 GB 左右**。8卡 H20-3E **完全可以跑满 V4 Pro**，还有约 200GB 的余量。

---

## 二、环境准备

### 2.1 安装 NVIDIA 驱动

> ⚠️ **驱动版本必须与 fabricmanager 版本匹配**，否则 NVLink 无法正常启用。

```bash
# 添加 PPA 源
sudo add-apt-repository ppa:graphics-drivers/ppa
sudo apt update

# 安装驱动（推荐 550+ 版本）
sudo apt install nvidia-driver-550

# 安装 fabricmanager（NVLink 管理服务，版本号必须与驱动一致）
sudo apt install nvidia-fabricmanager-550

# 重启
sudo reboot

# 验证
nvidia-smi
nvidia-smi nvlink --status
nvidia-smi topo -m
```

**验证要点**：
- `nvidia-smi` 应显示 8 张 H20-3E，每张 141GB
- `nvidia-smi nvlink --status` 显示 18 条 NVLink 已联通
- 如出现驱动与 fabricmanager 版本不匹配，卸载后统一版本重新安装

### 2.2 安装 CUDA

```bash
# 推荐 CUDA 12.4+
wget https://developer.download.nvidia.com/compute/cuda/12.4.0/local_installers/cuda_12.4.0_550.54.14_linux.run
sudo sh cuda_12.4.0_550.54.14_linux.run

# 配置环境变量
echo 'export PATH=/usr/local/cuda-12.4/bin:$PATH' >> ~/.bashrc
echo 'export LD_LIBRARY_PATH=/usr/local/cuda-12.4/lib64:$LD_LIBRARY_PATH' >> ~/.bashrc
source ~/.bashrc

# 验证
nvcc --version
```

### 2.3 安装 Miniconda 和虚拟环境

```bash
# 下载安装 Miniconda
wget https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh
bash Miniconda3-latest-Linux-x86_64.sh
source ~/.bashrc

# 创建 Python 3.12 虚拟环境
conda create -n vllm python=3.12
conda activate vllm
```

---

## 三、安装推理框架

### 3.1 安装 vLLM（推荐，DeepSeek V4 原生支持）

> vLLM 对 DeepSeek V4 的原生支持在 **v0.22.0+** 引入，生产环境推荐 **v0.23.0+**。
> 支持：MoE Expert Parallelism、CSA+HCA 混合注意力、1M 上下文 KV Cache 管理。

```bash
# 安装 vLLM
pip install vllm>=0.23.0

# 验证
python -c "import vllm; print(vllm.__version__)"
```

### 3.2 可选框架：SGLang

```bash
# SGLang 在 v0.5.12 中 Day-0 支持 V4
pip install sglang[all]>=0.5.12
```

---

## 四、下载模型权重

### 方式一：Hugging Face（推荐）

```bash
pip install huggingface_hub

# 下载 V4-Pro Instruct（FP4+FP8 混合精度，约 862 GB）
# 注意：下载前确认有足够磁盘空间（建议 ≥ 2TB）
huggingface-cli download deepseek-ai/DeepSeek-V4-Pro \
  --local-dir /data/models/DeepSeek-V4-Pro \
  --resume-download
```

### 方式二：ModelScope（国内用户推荐，速度更快）

```bash
pip install modelscope

modelscope download --model deepseek-ai/DeepSeek-V4-Pro \
  --local_dir /data/models/DeepSeek-V4-Pro
```

> ⏱ 模型文件约 862 GB，下载时间取决于网络环境，建议使用 **screen / tmux** 后台运行：
> ```bash
> screen -S download
> # 执行下载命令...
> # Ctrl+A+D 脱离
> ```

---

## 五、启动推理服务

### 5.1 最小化启动（测试用）

```bash
conda activate vllm

vllm serve /data/models/DeepSeek-V4-Pro \
  --tensor-parallel-size 8 \
  --served-model-name deepseek-v4-pro \
  --trust-remote-code \
  --port 8000
```

- `--tensor-parallel-size 8`：利用全部 8 张 GPU 做张量并行
- 首次加载约 **3~5 分钟**，之后每张卡显存占用约 **108~110 GB**
- 启动成功后提供 OpenAI 兼容 API：`http://<IP>:8000/v1`

### 5.2 生产环境部署（推荐配置）

```bash
nohup vllm serve /data/models/DeepSeek-V4-Pro \
  --tensor-parallel-size 8 \
  --served-model-name deepseek-v4-pro \
  --max-model-len 131072 \
  --gpu-memory-utilization 0.95 \
  --trust-remote-code \
  --port 8000 \
  --host 0.0.0.0 \
  > /var/log/vllm.log 2>&1 &
```

| 参数 | 说明 | 建议值 |
|------|------|--------|
| `--tensor-parallel-size` | 张量并行 GPU 数 | 8 |
| `--max-model-len` | 最大上下文长度 | 131072 (128K) 起步 |
| `--gpu-memory-utilization` | GPU 显存利用率 | 0.90~0.95 |
| `--host 0.0.0.0` | 监听所有网卡 | 远程访问需要 |
| `--port 8000` | 服务端口 | 8000 |

### 5.3 1M 上下文窗口配置

如需使用 V4 Pro 的 **1M token 上下文**，需增加显存预留：

```bash
vllm serve /data/models/DeepSeek-V4-Pro \
  --tensor-parallel-size 8 \
  --max-model-len 1048576 \
  --gpu-memory-utilization 0.92 \
  --trust-remote-code
```

> 📌 1M 上下文时 KV Cache 约占用 **10 GB**，总显存需求约 **900 GB**，8×H20-3E（1,128 GB）仍有余量，但建议调低 `gpu-memory-utilization` 预留更多缓存空间。

### 5.4 Expert Parallelism 优化（进阶）

对于 MoE 模型的 Expert Parallelism，vLLM 可自动处理。如需手动指定：

```bash
vllm serve /data/models/DeepSeek-V4-Pro \
  --tensor-parallel-size 4 \
  --pipeline-parallel-size 2 \
  --trust-remote-code
```

---

## 六、Docker 部署（可选）

### 6.1 安装 Docker + NVIDIA Container Toolkit

```bash
# 安装 Docker
curl -fsSL https://get.docker.com | sh

# 添加 NVIDIA Container Toolkit
distribution=$(. /etc/os-release;echo $ID$VERSION_ID)
curl -s -L https://nvidia.github.io/nvidia-docker/gpgkey | sudo apt-key add -
curl -s -L https://nvidia.github.io/nvidia-docker/$distribution/nvidia-docker.list | sudo tee /etc/apt/sources.list.d/nvidia-docker.list
sudo apt-get update && sudo apt-get install -y nvidia-container-toolkit

# 配置运行时
sudo nvidia-ctk runtime configure --runtime=docker
sudo systemctl restart docker
```

### 6.2 运行容器

```bash
docker run --runtime nvidia --gpus all \
  -v /data/models/DeepSeek-V4-Pro:/models \
  -p 8000:8000 -d \
  --ipc=host \
  --name vllm-v4pro \
  vllm/vllm-openai:latest \
  --model /models \
  --tensor-parallel-size 8 \
  --gpu-memory-utilization 0.95
```

---

## 七、调用测试

### 7.1 curl 测试

```bash
curl http://localhost:8000/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "deepseek-v4-pro",
    "messages": [{"role": "user", "content": "你好，请介绍一下你自己。"}],
    "max_tokens": 512,
    "temperature": 0.7,
    "stream": true
  }'
```

### 7.2 Python SDK 调用

```python
from openai import OpenAI

client = OpenAI(
    base_url="http://localhost:8000/v1",
    api_key="not-needed"
)

response = client.chat.completions.create(
    model="deepseek-v4-pro",
    messages=[{"role": "user", "content": "简述什么是MoE架构"}],
    max_tokens=1024,
    stream=False
)
print(response.choices[0].message.content)
```

---

## 八、性能压测

使用魔搭 EvalScope 进行吞吐量和延迟测试：

```bash
# 安装
pip install 'evalscope[all]' evalscope[perf] -U

# 运行压测（32 并发，20 轮）
evalscope perf \
  --url "http://127.0.0.1:8000/v1/chat/completions" \
  --parallel 32 \
  --model deepseek-v4-pro \
  --number 20 \
  --api openai \
  --dataset openqa \
  --stream
```

**预期性能参考（8×H20-3E + V4 Pro）：**

| 指标 | 参考值 |
|------|--------|
| 输出 Token 吞吐量 | 500~600 tok/s |
| 首 Token 延迟（TTFT）P50 | ~1.0 s |
| Token 间延迟（ITL） | ~22 ms |
| 请求成功率 | 100% |

---

## 九、常见问题

### Q1: NVLink 未连接 / fabricmanager 报错

```bash
# 检查状态
systemctl status nvidia-fabricmanager

# 常见错误："driver version XXX don't match with fabricmanager version YYY"
# 解决方案：卸载并统一版本后重装
sudo apt remove nvidia-fabricmanager-xxx
sudo apt install nvidia-fabricmanager-550  # 与驱动版本一致
```

### Q2: CUDA Out of Memory

- 降低 `--max-model-len` 减少 KV Cache
- 调低 `--gpu-memory-utilization`（如 0.85）
- 检查是否有其他进程占用显存

### Q3: 多 GPU 负载不均衡

```bash
# 指定 GPU
export CUDA_VISIBLE_DEVICES=0,1,2,3,4,5,6,7

# 使用 NCCL 调试
nccl-tests/build/all_reduce_perf -b 8 -e 256M -f 2
```

### Q4: V4 Pro 跑不动怎么办？

如果 8 卡仍然资源紧张，退而求其次：

1. **改用 V4-Flash**（284B 总参/13B 激活，~158 GB 权重，2×H200 即可跑）
2. **使用社区量化版**（如 GGUF Q4，但质量会下降）

---

## 十、参考资源

- [DeepSeek V4 Pro - Hugging Face](https://huggingface.co/deepseek-ai/DeepSeek-V4-Pro)
- [vLLM DeepSeek V4 Blog](https://vllm.ai/blog/deepseek-v4)
- [SGLang V4 Support](https://lmsys.org/blog/2026-04-25-deepseek-v4/)
- [DeepSeek API Pricing](https://api-docs.deepseek.com/quick_start/pricing)
- [NVIDIA H20 规格](https://www.burncloud.com/zh-cn/gpu-catalog/H20.html)
