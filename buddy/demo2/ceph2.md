# Ceph 19.x (Squid) 部署文档 —— Ubuntu 24.04 (noble) 实战版

> **适用环境**：Ubuntu 24.04 LTS + Ceph Squid 19.x + cephadm + 3 节点
> **本文记录的是真实部署过程，包含了官方文档未覆盖的 Ubuntu 24.04 适配坑点**

---

## 一、环境规划

### 1. 节点信息

| 主机名 | IP 地址 | 角色 | 数据盘 |
|--------|---------|------|--------|
| ceph1 | 192.168.12.176 | MON / MGR / OSD | /dev/sdb |
| ceph2 | 192.168.12.90 | MON / MGR / OSD | /dev/sdb |
| ceph3 | 192.168.12.169 | MON / MGR / OSD | /dev/sdb |

### 2. 账号信息

| 用户 | 密码 | 说明 |
|------|------|------|
| ps | 1 | 普通用户，有 sudo 权限 |
| root | 1 | 部署过程中会配置 |

> ⚠ Ubuntu 24.04 默认禁用 root SSH 密码登录，部署脚本会自动通过 `ps` 用户配置。

### 3. 网络代理（可选但推荐）

```bash
# 如果集群需要访问外网（如拉取 quay.io 容器镜像），设置代理
export http_proxy="http://192.168.12.187:7897"
export https_proxy="http://192.168.12.187:7897"
```

---

## 二、部署前准备

### 2.1 添加数据盘

三台虚拟机各添加一块空白裸盘（/dev/sdb），**不要格式化，不要分区**。

### 2.2 网络检查

```bash
# 所有节点之间 ping 通
ping -c 2 192.168.12.176
ping -c 2 192.168.12.90
ping -c 2 192.168.12.169
```

---

## 三、部署步骤

### 步骤 1：系统初始化（所有 3 节点执行）

#### 1.1 设置主机名

```bash
# ceph1：
hostnamectl set-hostname ceph1
# ceph2：
hostnamectl set-hostname ceph2
# ceph3：
hostnamectl set-hostname ceph3
```

#### 1.2 配置 hosts 解析

```bash
cat >> /etc/hosts << 'HOSTS'
192.168.12.176 ceph1
192.168.12.90 ceph2
192.168.12.169 ceph3
HOSTS
```

#### 1.3 禁用交换分区

```bash
swapoff -a
sed -i '/swap/s/^/#/' /etc/fstab
```

#### 1.4 安装容器运行时

```bash
apt update && apt install -y podman lvm2
```

#### 1.5 时间同步

```bash
apt install -y chrony
systemctl enable --now chrony
chronyc tracking   # 确认偏移量在毫秒级
```

#### 1.6 关闭防火墙

```bash
ufw disable
```

#### 1.7 安装基础工具

```bash
apt install -y apt-transport-https ca-certificates curl gnupg lsb-release
```

---

### 步骤 2：配置 root SSH 访问（所有 3 节点）

> Ubuntu 24.04 默认禁止 root SSH 密码登录。需要先用 `ps` 用户通过 sudo 修改配置。

#### 2.1 设置 root 密码 + 开启 SSH 登录

```bash
# 以下命令在每台节点上执行，用 ps 用户 + sudo
echo 'root:1' | sudo chpasswd

# 允许 root SSH 登录
sudo sed -i 's/^#PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config
sudo sed -i 's/^PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config

# 开启密码认证
sudo sed -i 's/^#PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config
sudo sed -i 's/^PasswordAuthentication no/PasswordAuthentication yes/' /etc/ssh/sshd_config

# 重启 SSH 服务
sudo systemctl restart ssh
```

> Ubuntu 24.04 上 SSH 服务名为 `ssh`（不是 `sshd`），请注意。

#### 2.2 验证 root 登录

```bash
ssh root@192.168.12.176 hostname
ssh root@192.168.12.90 hostname
ssh root@192.168.12.169 hostname
```

---

### 步骤 3：ceph1 SSH 免密配置（仅 ceph1 执行）

#### 3.1 生成 SSH 密钥

```bash
ssh-keygen -t rsa -N '' -f /root/.ssh/id_rsa
```

#### 3.2 安装 sshpass 并分发密钥

```bash
apt install -y sshpass
sshpass -p '1' ssh-copy-id -o StrictHostKeyChecking=no root@192.168.12.176
sshpass -p '1' ssh-copy-id -o StrictHostKeyChecking=no root@192.168.12.90
sshpass -p '1' ssh-copy-id -o StrictHostKeyChecking=no root@192.168.12.169
```

#### 3.3 验证免密

```bash
ssh root@ceph1 hostname
ssh root@ceph2 hostname
ssh root@ceph3 hostname
# 应直接返回主机名，无需输入密码
```

---

### 步骤 4：安装 cephadm（仅 ceph1 执行）

> ⚠ **坑点**：Ceph Squid 没有 Ubuntu 24.04 (noble) 的 apt 包。需要使用 jammy (22.04) 的源来安装。cephadm 新版本是 Python zipapp 格式，不能直接执行，需要包装脚本。

#### 4.1 添加 Ceph 源（使用 jammy 版本）

```bash
echo "deb [trusted=yes] https://download.ceph.com/debian-squid/ jammy main" > /etc/apt/sources.list.d/ceph.list
apt update
apt install -y cephadm
```

> 使用 `[trusted=yes]` 跳过 GPG 验证，因为 GPG 密钥下载在没有 TTY 的 SSH 会话中会失败。

#### 4.2 创建 cephadm 包装脚本（重要！）

```bash
# cephadm 是 zipapp，需要通过 python3 执行
printf '#!/bin/bash\nexec python3 /usr/sbin/cephadm "$@"\n' > /usr/local/bin/cephadm
chmod +x /usr/local/bin/cephadm

# 验证
cephadm version
# 应输出类似：cephadm version 19.2.5 (abc7aa7) squid (stable)
```

#### 4.3 配置代理并启动集群（bootstrap）

```bash
# 如果需要代理拉取镜像
export http_proxy="http://192.168.12.187:7897"
export https_proxy="http://192.168.12.187:7897"

# 引导集群（会从 quay.io 拉取 ~1.3GB 容器镜像）
cephadm bootstrap --mon-ip 192.168.12.176
```

> bootstrap 完成后会输出 **Dashboard URL** 和 **初始密码**，请保存好。如果忘记了，后续可以通过 `ceph dashboard ac-user-set-password admin -i <(echo "新密码")` 重置。

#### 4.4 验证初始状态

```bash
ceph -s
# 应显示 1 mon, 1 mgr, HEALTH_WARN（正常，尚未添加 OSD）
```

#### 4.5 分发 ceph 管理密钥到其他节点

```bash
sshpass -p '1' ssh-copy-id -f -i /etc/ceph/ceph.pub -o StrictHostKeyChecking=no root@192.168.12.90
sshpass -p '1' ssh-copy-id -f -i /etc/ceph/ceph.pub -o StrictHostKeyChecking=no root@192.168.12.169
```

---

### 步骤 5：添加节点到集群（仅 ceph1 执行）

#### 5.1 添加主机

```bash
cephadm shell ceph orch host add ceph2 192.168.12.90
cephadm shell ceph orch host add ceph3 192.168.12.169
```

#### 5.2 查看主机列表

```bash
cephadm shell ceph orch host ls
# 应显示 3 hosts in cluster
```

#### 5.3 扩展 MON 为 3 节点高可用

```bash
cephadm shell ceph orch apply mon 3
```

等待 1~2 分钟后验证：

```bash
cephadm shell ceph mon stat
# 应显示 3 mons at {ceph1=..., ceph2=..., ceph3=...}
```

---

### 步骤 6：添加 OSD 数据盘

#### 6.1 清空数据盘（所有 3 节点执行）

```bash
wipefs -a /dev/sdb
```

#### 6.2 查看可用设备（ceph1 执行）

```bash
cephadm shell ceph orch device ls
# 确认三台节点的 /dev/sdb 都显示 Available
```

#### 6.3 逐个添加 OSD（推荐，更可靠）

```bash
cephadm shell ceph orch daemon add osd ceph1:/dev/sdb
cephadm shell ceph orch daemon add osd ceph2:/dev/sdb
cephadm shell ceph orch daemon add osd ceph3:/dev/sdb
```

#### 6.4 验证 OSD

```bash
cephadm shell ceph osd tree
# 应显示 3 osds: 3 up, 3 in
```

---

### 步骤 7：验证集群状态

```bash
cephadm shell ceph -s
```

**预期输出（关键项）：**

| 检查项 | 预期状态 |
|--------|---------|
| health | `HEALTH_OK` |
| mon | 3 daemons, quorum ceph1,ceph2,ceph3 |
| mgr | 1 active + 1 standby |
| osd | 3 osds: 3 up, 3 in |

---

## 四、部署后配置

### 4.1 创建 `ceph` 命令包装脚本

> 由于 ceph-common 在 Ubuntu 24.04 上无法安装（jammy 版本与 noble 底层库冲突），需要通过包装脚本使 `ceph` 命令可用。

```bash
cat > /usr/bin/ceph << 'CEPH'
#!/bin/bash
exec cephadm shell -- ceph "$@"
CEPH
chmod +x /usr/bin/ceph
```

验证：

```bash
ceph -s   # 现在应该直接可用
```

### 4.2 创建 `rbd` 命令包装脚本

```bash
cat > /usr/bin/rbd << 'RBD'
#!/bin/bash
# map/showmapped/unmap 需要访问宿主机 /sys 和 /dev
NEED_MOUNT="map showmapped unmap"
FIRST=$1
if echo "$NEED_MOUNT" | grep -qw "$FIRST"; then
    exec cephadm shell --mount /sys:/sys --mount /dev:/dev -- rbd "$@"
else
    exec cephadm shell -- rbd "$@"
fi
RBD
chmod +x /usr/bin/rbd
```

验证：

```bash
rbd ls mypool          # 正常模式
rbd showmapped         # 自动挂载 /sys /dev 模式
```

### 4.3 加载 RBD 内核模块

> 如果在 ceph1 上需要 `rbd map` 将 RBD 镜像映射为本地块设备，需要加载内核模块。

```bash
modprobe rbd
echo 'rbd' > /etc/modules-load.d/rbd.conf   # 开机自动加载

# 验证
lsmod | grep rbd
# 应显示：rbd 和 libceph 模块已加载
```

### 4.4 Dashboard 密码重置

```bash
# 如果忘记了初始密码或密码不符合强度要求
echo "CephAdmin@2026" | cephadm shell -- ceph dashboard ac-user-set-password admin -i -

# 登录信息
# URL:    https://192.168.12.176:8443/
# 用户名: admin
# 密码:   设为自己记得的强密码（Ceph 19 要求至少 8 位，含大写字母 + 数字）
```

---

## 五、RBD 块存储功能测试

```bash
# 1. 创建存储池
ceph osd pool create mypool 32 32
ceph osd pool application enable mypool rbd

# 2. 创建 5G 块设备
rbd create --size 5G mypool/my-disk

# 3. 映射到本地
rbd map mypool/my-disk

# 4. 查看映射
rbd showmapped
# id  pool    image     device
# 0   mypool  my-disk   /dev/rbd0

# 5. 格式化并挂载
mkfs.ext4 /dev/rbd0
mkdir -p /mnt/ceph-rbd
mount /dev/rbd0 /mnt/ceph-rbd

# 6. 测试读写
echo "Hello Ceph" > /mnt/ceph-rbd/test.txt
cat /mnt/ceph-rbd/test.txt
df -h /mnt/ceph-rbd

# 7. 卸载
umount /mnt/ceph-rbd
rbd unmap mypool/my-disk
```

---

## 六、坑点总结

| # | 问题 | 原因 | 解决方案 |
|---|------|------|----------|
| 1 | root SSH 登录失败 | Ubuntu 24.04 默认禁 root SSH 密码登录 | 先用 ps 用户 + sudo 修改 sshd_config 并设置 root 密码 |
| 2 | apt 源 404 | Ceph Squid 没有 noble 的 apt 包 | 使用 jammy 源 + `[trusted=yes]` |
| 3 | GPG 密钥导入失败 | SSH 无 TTY，gpg 无法打开 /dev/tty | 直接用 `[trusted=yes]` 绕过 GPG 验证 |
| 4 | cephadm 命令找不到 | 新版 cephadm 是 zipapp，需 python3 执行 | 创建 `/usr/local/bin/cephadm` 包装脚本 |
| 5 | ceph-common 装不上 | jammy 包依赖的高级库版本与 noble 不兼容 | 跳过，改用包装脚本走 `cephadm shell` |
| 6 | Dashboard 密码太弱 | Ceph 19 强密码策略拒绝简单密码 | 使用含大小写+数字的 8 位以上密码 |
| 7 | rbd map 失败 | 容器无法访问宿主机内核模块接口 | `modprobe rbd` 加载模块，rbd map 时 `--mount /sys:/sys --mount /dev:/dev` |
| 8 | quay.io 镜像拉不动 | 国内网络问题 | 配置 HTTP 代理 |

---

## 七、一键部署脚本

项目目录下的 `deploy_ceph.py` 包含了以上所有步骤的自动化实现，可用于重装或新装。基本用法：

```bash
# 确保本机可 SSH 到三台节点（密码 root/1）
python3 deploy_ceph.py
```

脚本会自动处理以下适配：
- 通过 `ps` 用户配置 root SSH
- 使用 jammy 源安装 cephadm
- 创建 zipapp 包装脚本
- 通过代理拉取 quay.io 容器镜像
- 跳过 ceph-common（用 `cephadm shell` 替代）
