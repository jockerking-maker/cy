# RTX 5090 深度学习 GPU 环境搭建手册（Ubuntu 24.04）

> 客户验收标准：
> 1. 宿主机 `nvidia-smi` 正常显示 RTX 5090
> 2. Docker 容器内可调用 GPU
> 3. 容器内 Python `torch.cuda.is_available()` 返回 `True`
>
> 环境现状（已满足）：
> - NVIDIA Driver：**580.142**（≥ 客户要求 570.124.06）✅
> - CUDA：**12.8** ✅
> - 系统：**Ubuntu 24.04** ✅
>
> 说明：宿主机已有的 CUDA 12.8 toolkit 对“容器内用 GPU”不是必须的（真正起作用的是宿主机驱动 + nvidia-container-toolkit 挂载）。PyTorch 的 `cu128` wheel 自带 CUDA 运行时，因此容器内无需再装 CUDA toolkit。

---

## 第 0 步：确认宿主机驱动正常

```bash
nvidia-smi
```

期望输出里能看到：
- 右上角 `Driver Version: 580.142`
- 右上角 `CUDA Version: 12.8`
- 下方 GPU 列表里有 `NVIDIA GeForce RTX 5090`

如果这一步已经正常，说明驱动没问题，直接往下走。

---

## 第 1 步：安装 Docker（宿主机）

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
| sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

把当前用户加入 docker 组（免 sudo 跑 docker，可选但推荐）：

```bash
sudo usermod -aG docker $USER
# 执行后需要**注销并重新登录**（或重开终端）才能生效
```

验证 Docker 安装：

```bash
sudo docker run --rm hello-world
```

---

## 第 2 步：安装 NVIDIA Container Toolkit

```bash
curl -fsSL https://nvidia.github.io/libnvidia-container/gpgkey \
  | sudo gpg --dearmor -o /usr/share/keyrings/nvidia-container-toolkit-keyring.gpg

curl -s -L https://nvidia.github.io/libnvidia-container/stable/deb/nvidia-container-toolkit.list \
  | sed 's#deb https://#deb [signed-by=/usr/share/keyrings/nvidia-container-toolkit-keyring.gpg] https://#g' \
  | sudo tee /etc/apt/sources.list.d/nvidia-container-toolkit.list

sudo apt-get update
sudo apt-get install -y nvidia-container-toolkit
```

---

## 第 3 步：让 Docker 支持 GPU

```bash
sudo nvidia-ctk runtime configure --runtime=docker
sudo systemctl restart docker
```

这条命令会自动往 `/etc/docker/daemon.json` 写入 nvidia runtime 配置。

**快速验证容器能调 GPU**（直接拉官方 CUDA 镜像试）：

```bash
sudo docker run --rm --gpus all nvidia/cuda:12.8.0-base-ubuntu24.04 nvidia-smi
```

如果这里能打印出和宿主机一样的 RTX 5090 信息，说明 Docker + GPU 通道已经打通（验收标准 1、2 已具备）。

---

## 第 4 步：准备 PyTorch cu128 + Python 3.10 容器环境

客户要求：PyTorch 用 **cu128** 版本，**Python 3.10**，且**不要装在宿主机全局**（放 Docker 容器里即可）。

本仓库已提供 `Dockerfile`（基于 `python:3.10-slim`，pip 安装官方 cu128 wheel）：

```dockerfile
FROM python:3.10-slim

RUN apt-get update \
 && apt-get install -y --no-install-recommends wget git ca-certificates \
 && rm -rf /var/lib/apt/lists/*

# 安装 PyTorch cu128（自带 CUDA 运行时，无需容器内再装 CUDA toolkit）
RUN pip install --no-cache-dir \
      --index-url https://download.pytorch.org/whl/cu128 \
      torch torchvision torchaudio

WORKDIR /workspace
```

> 如果你后续要**编译 CUDA 扩展 / 需要 nvcc**，请把 `FROM python:3.10-slim` 换成：
> `FROM nvidia/cuda:12.8.0-cudnn-devel-ubuntu24.04`（其余不变），容器内就带 nvcc 了。

---

## 第 5 步：构建并运行

```bash
# 在 Dockerfile 所在目录
sudo docker build -t pytorch-cu128:py310 .

# 进入交互式容器（带 GPU）
sudo docker run --rm -it --gpus all pytorch-cu128:py310 bash
```

若想挂载代码目录（把宿主机 ./workspace 挂进容器）：

```bash
sudo docker run --rm -it --gpus all -v "$PWD/workspace":/workspace pytorch-cu128:py310 bash
```

---

## 第 6 步：验收测试（三条标准全跑一遍）

宿主机：

```bash
nvidia-smi                              # 标准 1：显示 RTX 5090
```

容器内（一条命令直接验证标准 2、3）：

```bash
sudo docker run --rm --gpus all pytorch-cu128:py310 python -c \
"import torch; \
 print('torch version :', torch.__version__); \
 print('cuda available :', torch.cuda.is_available()); \
 print('device name    :', torch.cuda.get_device_name(0)); \
 print('device count   :', torch.cuda.device_count())"
```

期望输出：

```
torch version : 2.7.x+cu128
cuda available : True
device name    : NVIDIA GeForce RTX 5090
device count   : 1
```

三条验收标准全部满足 ✅

---

## 常见问题 / 排错

| 现象 | 原因 | 解决 |
|------|------|------|
| `docker: permission denied` | 当前用户不在 docker 组 | `sudo usermod -aG docker $USER` 后**重新登录** |
| `docker: Error response from daemon: could not select device driver` | nvidia runtime 未配置 | 重做第 3 步；确认 `daemon.json` 含 nvidia，并 `restart docker` |
| 容器内 `torch.cuda.is_available()` 为 `False` | 没加 `--gpus all`，或驱动太旧 | 运行容器务必带 `--gpus all`；确认宿主机驱动 ≥ 570.124.06 |
| 拉镜像慢 / 超时 | 网络问题 | 配置国内镜像源，或用 `nvidia/cuda` 替代 `python:3.10-slim` 作基础镜像 |
| RTX 5090 不识别 | 驱动低于 570.124.06 | 升级到 570.124.06+（你已是 580.142，正常） |

---

## 一句话总结

你驱动和 CUDA 已达标；按 **第1步装 Docker → 第2步装 Toolkit → 第3步配置 → 用本仓库 Dockerfile 构建 → 第6步验证** 走完，即可交付满足客户三项验收标准的环境。
