# Ceph 客户端远程挂载操作文档

> **任务目标**：在独立于集群的客户端机器上安装 Ceph 客户端工具，远程连接 Ceph 集群并使用 RBD 块存储。
> **文档版本**：v1.0 | 2026-07-21

---

## 一、操作环境判断

### ❓ 能否在 Windows 本机操作？

**不能**。Ceph 客户端（`ceph-common`）是 Linux 原生的工具集，不支持 Windows 直接安装。需要**新建一台 Linux 虚拟机**作为客户端。

### ✅ 推荐方案

在 VMware / Proxmox 等虚拟化平台上新建一台 **Ubuntu 24.04 LTS** 虚拟机，规格如下：

| 资源 | 最低要求 |
|------|---------|
| CPU | 1 核 |
| 内存 | 2 GB |
| 系统盘 | 20 GB |
| 网络 | 桥接模式，与 Ceph 集群同一网段 |

**虚拟机网络信息（规划）**：

| 项目 | 值 |
|------|-----|
| 主机名 | client |
| IP 地址 | 192.168.12.200（建议，须与集群同网段且不冲突） |
| 子网掩码 | 255.255.255.0 |
| 网关 | 192.168.12.254 |
| DNS | 202.96.209.5 / 202.96.199.133 |

---

## 二、前提条件

开始操作前，请确认以下条件已满足：

- [ ] Ceph 集群 3 节点正常运行，`ceph -s` 返回 `HEALTH_OK`
- [ ] 客户端虚拟机已安装 Ubuntu 24.04 并配置好静态 IP
- [ ] 客户端与集群节点之间网络互通（`ping 192.168.12.176` 通）
- [ ] 集群防火墙已关闭（已在部署时 `ufw disable`）
- [ ] 已在 ceph1 上创建 RBD 块设备用于后续挂载测试（可选，文档会包括）

---

## 三、Ceph 集群参考信息

在后续操作中会用到以下信息（均记录自当前集群）：

| 项目 | 值 |
|------|-----|
| 集群 MON 地址 | 192.168.12.176, 192.168.12.90, 192.168.12.169 |
| MON 端口 | 6789 |
| ceph1 主节点 IP | 192.168.12.176 |
| 集群 FSID | `ceph fsid` 命令获取（登录 ceph1 执行） |
| admin 密钥 | 见步骤 4.2 |

---

## 四、操作步骤

### 步骤 1：客户端虚拟机基础配置

> **执行位置**：在新建的客户端虚拟机上执行

#### 1.1 设置主机名

```bash
hostnamectl set-hostname client
```

#### 1.2 配置 hosts 解析（可选，方便主机名访问）

```bash
cat >> /etc/hosts << 'HOSTS'
192.168.12.176 ceph1
192.168.12.90  ceph2
192.168.12.169 ceph3
192.168.12.200 client
HOSTS
```

#### 1.3 配置静态 IP（根据实际规划修改）

```bash
cat > /etc/netplan/01-netcfg.yaml << 'NETPLAN'
network:
  version: 2
  renderer: NetworkManager
  ethernets:
    ens33:
      dhcp4: no
      addresses:
        - 192.168.12.200/24
      routes:
        - to: default
          via: 192.168.12.254
      nameservers:
        addresses: [202.96.209.5, 202.96.199.133]
NETPLAN

netplan apply
```

#### 1.4 验证网络

```bash
ping -c 2 192.168.12.176
ping -c 2 192.168.12.90
ping -c 2 192.168.12.169
# 三台都应 ping 通，丢包率 0%
```

---

### 步骤 2：安装 ceph-common

> **执行位置**：客户端虚拟机
> ⚠ **注意**：客户端是 Ubuntu 24.04，和集群节点一样会遇到 Ceph Squid 没有 noble apt 包的问题。但客户端只需要 `ceph-common`（不含 cephadm），可以直接从 jammy 源安装。

```bash
# 添加 Ceph Squid 源（使用 jammy 版本）
echo "deb [trusted=yes] https://download.ceph.com/debian-squid/ jammy main" > /etc/apt/sources.list.d/ceph.list
apt update

# 安装 ceph-common（与集群节点不同，客户端安装一般不会报依赖冲突）
apt install -y ceph-common
```

**验证安装**：
```bash
ceph --version
# 应输出：ceph version 19.2.5 (...) squid (stable)

rbd --version
# 应输出版本号
```

> **如果安装报依赖冲突**：以下备选方案二选一
> - 方案 A：使用 `apt install -y ceph-common --no-install-recommends` 跳过推荐依赖
> - 方案 B：参考 ceph3.md 的"部署后配置"章节，使用包装脚本通过容器访问集群

---

### 步骤 3：获取集群认证信息

> **执行位置**：在 **ceph1** 上执行

```bash
# 查看 admin 密钥
cat /etc/ceph/ceph.client.admin.keyring
```

输出示例（**请记录 key 的值**）：
```
[client.admin]
    key = AQCDEf1234567890abcdefghijklmnopqrstuvw==
```

---

### 步骤 4：配置客户端认证

> **执行位置**：客户端虚拟机

#### 4.1 创建 Ceph 配置目录和文件

```bash
mkdir -p /etc/ceph
```

#### 4.2 写入集群连接信息

```bash
cat > /etc/ceph/ceph.conf << 'CEPHCONF'
[global]
fsid = <替换为 ceph fsid 命令输出的值>
mon host = 192.168.12.176, 192.168.12.90, 192.168.12.169
mon initial members = ceph1, ceph2, ceph3
CEPHCONF
```

#### 4.3 写入 admin 密钥

```bash
cat > /etc/ceph/ceph.client.admin.keyring << 'KEYRING'
[client.admin]
    key = <替换为步骤 3 中记录的实际 key 值>
KEYRING
```

#### 4.4 设置文件权限

```bash
chmod 644 /etc/ceph/ceph.conf
chmod 600 /etc/ceph/ceph.client.admin.keyring
```

---

### 步骤 5：验证客户端连接

> **执行位置**：客户端虚拟机

```bash
ceph -s
```

**预期输出**：
```
  cluster:
    id:     a2173549-...
    health: HEALTH_OK
  services:
    mon: 3 daemons, quorum ceph1,ceph2,ceph3
    osd: 3 osds: 3 up, 3 in
```

> 如果能正常显示集群状态，说明客户端配置成功。如果连接失败，检查：
> - `/etc/ceph/ceph.conf` 中的 MON IP 是否正确
> - 网络是否互通（`ping` 和 `telnet 192.168.12.176 6789`）
> - 客户端防火墙是否关闭（`ufw disable`）

---

### 步骤 6：远程使用 RBD 块存储

#### 6.1 在集群端创建测试 RBD 镜像

> **执行位置**：ceph1

如果还没有测试用的存储池和镜像：

```bash
# 创建存储池
ceph osd pool create client_pool 32 32
ceph osd pool application enable client_pool rbd

# 创建 5G 块设备
rbd create --size 5G client_pool/test_disk

# 验证
rbd ls client_pool
# 应输出：test_disk
```

#### 6.2 在客户端查看集群中的 RBD 镜像

> **执行位置**：客户端虚拟机

```bash
rbd ls client_pool
# 应输出远端集群中的镜像列表：test_disk

rbd info client_pool/test_disk
# 显示镜像大小、格式等信息
```

#### 6.3 映射 RBD 镜像到本地

> **执行位置**：客户端虚拟机

```bash
# 加载内核 rbd 模块
modprobe rbd

# 验证模块已加载
lsmod | grep rbd

# 映射镜像
rbd map client_pool/test_disk
```

**预期输出**：
```
/dev/rbd0
```

**验证**：
```bash
rbd showmapped
# id  pool         image      snap  device
# 0   client_pool  test_disk  -     /dev/rbd0
```

> **如果报错**：参考下方"常见问题"章节。

#### 6.4 格式化并挂载

```bash
# 格式化（仅首次）
mkfs.ext4 /dev/rbd0

# 创建挂载点
mkdir -p /mnt/ceph-rbd

# 挂载
mount /dev/rbd0 /mnt/ceph-rbd
```

#### 6.5 读写测试

```bash
# 写入测试文件
echo "Ceph RBD remote mount test OK" > /mnt/ceph-rbd/test.txt

# 读取验证
cat /mnt/ceph-rbd/test.txt
# 应输出：Ceph RBD remote mount test OK

# 查看容量
df -h /mnt/ceph-rbd
# 应显示 5G 容量
```

#### 6.6 卸载

```bash
umount /mnt/ceph-rbd
rbd unmap client_pool/test_disk
```

---

### 步骤 7：配置开机自动挂载（可选）

#### 7.1 创建挂载脚本

```bash
cat > /usr/local/bin/mount-ceph-rbd.sh << 'SCRIPT'
#!/bin/bash
modprobe rbd 2>/dev/null
rbd map client_pool/test_disk 2>/dev/null
sleep 2
mount /dev/rbd0 /mnt/ceph-rbd 2>/dev/null
SCRIPT
chmod +x /usr/local/bin/mount-ceph-rbd.sh
```

#### 7.2 创建 systemd 服务

```bash
cat > /etc/systemd/system/ceph-rbd-mount.service << 'SERVICE'
[Unit]
Description=Mount Ceph RBD device
After=network-online.target
Wants=network-online.target

[Service]
Type=oneshot
ExecStart=/usr/local/bin/mount-ceph-rbd.sh
RemainAfterExit=yes

[Install]
WantedBy=multi-user.target
SERVICE

systemctl daemon-reload
systemctl enable ceph-rbd-mount.service
```

---

## 五、常见问题

| # | 问题 | 原因 | 解决 |
|---|------|------|------|
| 1 | `ceph -s` 连接失败，输出 `libceph: connect to ...:6789` 超时 | 网络不通或端口被防火墙阻止 | 检查客户端与集群网络，执行 `telnet 192.168.12.176 6789` 测试 |
| 2 | `rbd map` 报 `rbd: sysfs write failed` | 内核 rbd 模块未加载 | 执行 `modprobe rbd` |
| 3 | `rbd map` 报 `modinfo: ERROR: Module rbd not found` | 内核没有 rbd 模块 | 安装 `linux-modules-extra-$(uname -r)` 后重试 |
| 4 | `rbd: failed to create rbd image: (2) No such file or directory` | 存储池不存在或名称错误 | 执行 `rbd ls` 检查可用的 pool 和镜像 |
| 5 | `mount: /dev/rbd0 is not a block device` | 映射未成功 | 先执行 `rbd showmapped` 确认设备名 |
| 6 | `key is not installed` 或 `auth: incorrect key` | keyring 文件配置错误 | 检查 `/etc/ceph/ceph.client.admin.keyring` 内容是否正确 |

---

## 六、参考资料

- Ceph 官方文档：https://docs.ceph.com/en/squid/rbd/
- 集群部署文档：`ceph3.md`
