FROM python:3.10-slim

# 基础工具（按需增删）
RUN apt-get update \
 && apt-get install -y --no-install-recommends wget git ca-certificates \
 && rm -rf /var/lib/apt/lists/*

# 安装 PyTorch cu128（自带 CUDA 运行时，无需容器内再装 CUDA toolkit）
# 满足客户要求：Python 3.10 + PyTorch cu128，且隔离在容器内（不污染宿主机全局）
RUN pip install --no-cache-dir \
      --index-url https://download.pytorch.org/whl/cu128 \
      torch torchvision torchaudio

WORKDIR /workspace

# 一键验证：
# sudo docker build -t pytorch-cu128:py310 .
# sudo docker run --rm --gpus all pytorch-cu128:py310 python -c "import torch; print(torch.__version__, torch.cuda.is_available(), torch.cuda.get_device_name(0))"

# 如需 nvcc（编译 CUDA 扩展），把首行换成：
# FROM nvidia/cuda:12.8.0-cudnn-devel-ubuntu24.04
# 其余保持不变。
