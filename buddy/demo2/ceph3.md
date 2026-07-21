# Ceph 19.x (Squid) 手动部署全流程

> **适用环境**：Ubuntu 24.04 LTS × 3 节点，Ceph Squid 19.x（cephadm 容器化部署）
> **文档性质**：纯手动操作指南，所有命令由你逐条在终端执行
> **总耗时**：约 30~60 分钟（取决于网络下载速度）

---

## 目录

- [一、环境规划](#一环境规划)
- [二、部署前准备](#二部署前准备)
- [三、系统初始化（逐节点执行）](#三系统初始化逐节点执行)
- [四、配置 root SSH 登录（逐节点执行）](#四配置-root-ssh-登录逐节点执行)
- [五、ceph1 SSH 免密配置（ceph1 执行）](#五ceph1-ssh-免密配置ceph1-执行)
- [六、安装 cephadm + 引导集群（ceph1 执行）](#六安装-cephadm--引导集群ceph1-执行)
- [七、添加节点 + 扩展 MON（ceph1 执行）](#七添加节点--扩展-monceph1-执行)
- [八、添加 OSD 数据盘](#八添加-osd-数据盘)
- [九、验证集群](#九验证集群)
- [十、部署后配置](#十部署后配置)
- [十一、RBD 块存储功能测试](#十一rbd-块存储功能测试)
- [十二、Dashboard 登录](#十二dashboard-登录)
- [十三、故障排查](#十三故障排查)
- [十四、完全重置](#十四完全重置)

---

## 一、环境规划

### 1.1 节点信息

| 主机名 | IP 地址 | 角色 | 数据盘 |
|--------|---------|------|--------|
| ceph1  | 192.168.12.176 | MON / MGR / OSD | /dev/sdb |
| ceph2  | 192.168.12.90  | MON / MGR / OSD | /dev/sdb |
| ceph3  | 192.168.12.169 | MON / MGR / OSD | /dev/sdb |

> **⚠ 说明**：三台节点角色完全一致，每台同时承担监控（MON）、管理（MGR）、存储（OSD）三种角色。3 个 MON 满足 Paxos 过半机制（容忍 1 台故障）。

### 1.2 账号信息

| 用户 | 初始密码 | 说明 |
|------|---------|------|
| ps   | 1       | 普通用户，属于 sudo 组，用于初始系统配置 |
| root | 1       | **部署过程中会配置**，初始不可 SSH 登录 |

### 1.3 网络代理（可选）

如果你所在的网络环境无法直接访问 quay.io（拉取 Ceph 容器镜像），需要准备一个 HTTP 代理地址。本文示例使用 `http://192.168.12.187:7897`，请替换为你的实际代理地址。

---

## 二、部署前准备

### 2.1 添加数据盘

- **执行位置**：虚拟机平台（如 VMware / Proxmox / OpenStack）
- **操作**：为三台虚拟机**各添加一块空白虚拟磁盘**
- **规格**：建议 ≥50GB，不需要格式化，不需要分区
- **确认方法**：登录每台节点执行以下命令，能看到 `/dev/sdb` 即为识别成功

```bash
lsblk
# 输出中应有 sdb 设备，且无分区（sdb1/sdb2 等）
```

### 2.2 网络互通检查

- **执行位置**：任一节点

```bash
ping -c 2 192.168.12.176
ping -c 2 192.168.12.90
ping -c 2 192.168.12.169
# 三台都应 ping 通，丢包率 0%
```

---

## 三、系统初始化（逐节点执行）

> **执行位置**：以下 1.1~1.7 所有步骤在 ceph1、ceph2、ceph3 **每台节点逐一执行**

### 3.1 设置主机名

```bash
# ---- 在 ceph1 上执行 ----
hostnamectl set-hostname ceph1

# ---- 在 ceph2 上执行 ----
hostnamectl set-hostname ceph2

# ---- 在 ceph3 上执行 ----
hostnamectl set-hostname ceph3
```

**验证**：
```bash
hostname
# 应输出对应的主机名：ceph1 / ceph2 / ceph3
```

### 3.2 配置 hosts 解析

- **执行位置**：ceph1、ceph2、ceph3 **全部执行**

```bash
cat >> /etc/hosts << 'HOSTS'
192.168.12.176 ceph1
192.168.12.90 ceph2
192.168.12.169 ceph3
HOSTS
```

**验证**：
```bash
ping -c 1 ceph1
ping -c 1 ceph2
ping -c 1 ceph3
# 应能解析到对应 IP
```

### 3.3 禁用交换分区

```bash
swapoff -a
sed -i '/swap/s/^/#/' /etc/fstab
```

**验证**：
```bash
free -m | grep Swap
# Swap total 应为 0
```

### 3.4 安装容器运行时

```bash
apt update && apt install -y podman lvm2
```

**验证**：
```bash
podman --version
# 应输出版本号，如 4.9.3
```

### 3.5 时间同步

```bash
apt install -y chrony
systemctl enable --now chrony
```

**验证**：
```bash
chronyc tracking
# 关注 "System time" 行，偏移量应在毫秒级别
# Stratum 应为 2~4，表示已同步到上级 NTP 服务器
```

### 3.6 关闭防火墙

```bash
ufw disable
```

**验证**：
```bash
ufw status
# Status: inactive
```

### 3.7 安装基础工具

```bash
apt install -y apt-transport-https ca-certificates curl gnupg lsb-release
```

---

## 四、配置 root SSH 登录（逐节点执行）

> ⚠ **为什么需要这一步**：Ubuntu 24.04 默认禁止 root 用户通过 SSH 密码登录，而 cephadm 工具需要 root 权限来部署和管理服务。必须先通过 `ps` 用户（有 sudo 权限）修改 SSH 配置。

### 4.1 设置 root 密码并允许 SSH 登录

- **执行位置**：ceph1、ceph2、ceph3 **全部执行**

```bash
# 设置 root 密码为 1（通过 ps 用户提权）
echo 'root:1' | sudo chpasswd

# 允许 root SSH 登录
sudo sed -i 's/^#PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config
sudo sed -i 's/^PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config

# 开启密码认证
sudo sed -i 's/^#PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config
sudo sed -i 's/^PasswordAuthentication no/PasswordAuthentication yes/' /etc/ssh/sshd_config

# 重启 SSH 服务（注意：Ubuntu 24.04 服务名是 ssh，不是 sshd）
sudo systemctl restart ssh
```

### 4.2 验证 root 登录

- **执行位置**：任一节点（通常从 ceph1 测试）

```bash
# 从 ceph1 分别测试三台节点
ssh root@192.168.12.176 hostname
ssh root@192.168.12.90 hostname
ssh root@192.168.12.169 hostname
# 输入密码 1 后应各自返回 ceph1、ceph2、ceph3
```

> **如果仍然报 `Permission denied`**：检查 `/etc/ssh/sshd_config` 中是否有其他 `PermitRootLogin` 或 `PasswordAuthentication` 行未被覆盖，手动检查并修改，然后 `systemctl restart ssh`。

---

## 五、ceph1 SSH 免密配置（ceph1 执行）

> **说明**：cephadm 通过 SSH 管理集群所有节点，因此需要 ceph1 能无密码访问其他节点。

### 5.1 生成 SSH 密钥

- **执行位置**：ceph1（已切换为 root 用户）

```bash
ssh-keygen -t rsa -N '' -f /root/.ssh/id_rsa
# 如果提示文件已存在，选择覆盖或先 rm -f /root/.ssh/id_rsa /root/.ssh/id_rsa.pub
```

### 5.2 安装 sshpass

```bash
apt install -y sshpass
```

### 5.3 分发公钥到所有节点（包括自身）

```bash
sshpass -p '1' ssh-copy-id -o StrictHostKeyChecking=no root@192.168.12.176
sshpass -p '1' ssh-copy-id -o StrictHostKeyChecking=no root@192.168.12.90
sshpass -p '1' ssh-copy-id -o StrictHostKeyChecking=no root@192.168.12.169
```

### 5.4 验证免密

```bash
ssh root@ceph1 hostname
ssh root@ceph2 hostname
ssh root@ceph3 hostname
# 应直接返回主机名，无需输入密码
```

---

## 六、安装 cephadm + 引导集群（ceph1 执行）

> ⚠ **重要坑点**：
> 1. Ceph Squid 没有 Ubuntu 24.04 (noble) 的官方 apt 包，需要使用 jammy (22.04) 的源
> 2. 新版 cephadm 是 Python zipapp 格式，安装后需要用 python3 调用，不能直接执行

### 6.1 添加 Ceph 源（使用 jammy 版本）

```bash
echo "deb [trusted=yes] https://download.ceph.com/debian-squid/ jammy main" > /etc/apt/sources.list.d/ceph.list
```

> 使用 `[trusted=yes]` 的原因：SSH 终端没有 TTY，`gpg` 命令会报 `cannot open /dev/tty`，导致密钥导入失败。测试环境不做 GPG 校验是安全的。

### 6.2 更新并安装 cephadm

```bash
apt update
apt install -y cephadm
```

**验证二进制已安装**：
```bash
ls -la /usr/sbin/cephadm
# 应显示文件存在，大小约 800KB
```

### 6.3 创建 cephadm 包装脚本（必须！）

```bash
printf '#!/bin/bash\nexec python3 /usr/sbin/cephadm "$@"\n' > /usr/local/bin/cephadm
chmod +x /usr/local/bin/cephadm
```

**验证包装脚本生效**：
```bash
cephadm version
# 应输出类似：cephadm version 19.2.5 (abc7aa7) squid (stable)
# 如果没有这一步直接执行 cephadm，会报 "无法执行：找不到需要的文件"
```

### 6.4 配置代理（如果需要）

如果无法直接访问 quay.io，设置代理：

```bash
export http_proxy="http://192.168.12.187:7897"
export https_proxy="http://192.168.12.187:7897"
```

> 建议将这两行写到 `/root/.bashrc`，后续 `cephadm bootstrap` 拉取容器镜像时也会用到。

### 6.5 引导集群（bootstrap）

```bash
cephadm bootstrap --mon-ip 192.168.12.176
```

> **此步骤需要从 quay.io 拉取约 1.3GB 的容器镜像，耗时取决于网络速度（代理下约 5~15 分钟）。**

**执行完成后**，终端会输出类似以下信息，**务必保存**：
```
Ceph Dashboard is now available at:

     URL: https://ceph1:8443/
    User: admin
Password: <初始随机密码，例如 kxf55g0ge4>
```

如果忘了保存密码，后续可通过以下命令重置（见第十一章）。

### 6.6 验证初始集群状态

```bash
ceph -s
```

此时应显示：
- 1 个 MON（监控节点）
- 1 个 MGR（管理节点），状态 active
- 集群状态 `HEALTH_WARN`（正常，因为还没有 OSD）
- OSD count: 0

### 6.7 分发 ceph 管理密钥到其他节点

```bash
sshpass -p '1' ssh-copy-id -f -i /etc/ceph/ceph.pub -o StrictHostKeyChecking=no root@192.168.12.90
sshpass -p '1' ssh-copy-id -f -i /etc/ceph/ceph.pub -o StrictHostKeyChecking=no root@192.168.12.169
```

---

## 七、添加节点 + 扩展 MON（ceph1 执行）

### 7.1 添加 ceph2、ceph3 到集群

```bash
cephadm shell ceph orch host add ceph2 192.168.12.90
cephadm shell ceph orch host add ceph3 192.168.12.169
```

**验证**：
```bash
cephadm shell ceph orch host ls
# 应显示 3 hosts in cluster（ceph1、ceph2、ceph3）
```

### 7.2 扩展 MON 为 3 节点

```bash
cephadm shell ceph orch apply mon 3
```

此命令会在 ceph2、ceph3 上自动各部署一个 MON 守护进程。

**等待约 1~2 分钟后验证**：
```bash
cephadm shell ceph mon stat
# 应显示 3 mons at {ceph1=[v2:192.168.12.176:3300/0,...], ceph2=..., ceph3=...}
```

如果等了 2 分钟仍然只有 1 个 MON，可以继续（OSD 添加后会自动恢复），或者重启 cephadm：
```bash
cephadm shell ceph mgr module disable orch && sleep 5 && cephadm shell ceph mgr module enable orch
```

---

## 八、添加 OSD 数据盘

### 8.1 清空数据盘

- **执行位置**：ceph1、ceph2、ceph3 **全部执行**

```bash
wipefs -a /dev/sdb
```

> ⚠ **注意**：确认盘符正确！`wipefs -a` 会清除分区表，数据不可恢复。如有疑问，先执行 `lsblk` 确认 `/dev/sdb` 是你要用来做 OSD 的数据盘。

### 8.2 查看可用设备

- **执行位置**：ceph1

```bash
cephadm shell ceph orch device ls
```

关键输出解读：
```
HOST   PATH       AVAIL   REJECT REASONS
ceph1  /dev/sdb   Yes
ceph2  /dev/sdb   Yes
ceph3  /dev/sdb   Yes
```

- `AVAIL = Yes`：设备可用
- `AVAIL = No` 且显示 `Has a FileSystem`：设备有残留文件系统，需要执行 `wipefs -a /dev/sdb`

### 8.3 逐个添加 OSD

- **执行位置**：ceph1

```bash
cephadm shell ceph orch daemon add osd ceph1:/dev/sdb
cephadm shell ceph orch daemon add osd ceph2:/dev/sdb
cephadm shell ceph orch daemon add osd ceph3:/dev/sdb
```

> **推荐用逐个添加而不是 `--all-available-devices`**，因为 bootstrap 节点的盘有时不会被自动识别。

### 8.4 验证 OSD

- **执行位置**：ceph1

```bash
cephadm shell ceph osd tree
```

预期输出：
```
ID  CLASS  WEIGHT   TYPE NAME       STATUS  REWEIGHT  PRI-AFF
-1         0.17578  root default
-3         0.05859      host ceph1
 0    hdd  0.05859          osd.0       up   1.00000  1.00000
-5         0.05859      host ceph2
 1    hdd  0.05859          osd.1       up   1.00000  1.00000
-7         0.05859      host ceph3
 2    hdd  0.05859          osd.2       up   1.00000  1.00000
```

关键点：**3 osds: 3 up, 3 in**。如有 OSD 显示 `down`，等待 30 秒再查（新 OSD 需要一点时间上线）。

---

## 九、验证集群

### 9.1 查看完整集群状态

```bash
cephadm shell ceph -s
```

**预期输出**：
```
  cluster:
    id:     a2173549-...
    health: HEALTH_OK
  services:
    mon: 3 daemons, quorum ceph1,ceph2,ceph3
    mgr: ceph1(active), standbys: ceph2
    osd: 3 osds: 3 up, 3 in
  data:
    pools:   2 pools, 33 pgs
    objects: 0 objects, 0 B
    usage:   81 MiB used, 180 GiB / 180 GiB avail
    pgs:     33 active+clean
```

### 9.2 检查项对照表

| 检查项 | 命令 | 预期状态 |
|--------|------|---------|
| 集群健康 | `cephadm shell ceph health` | `HEALTH_OK` |
| MON 数量 | `cephadm shell ceph mon stat` | 3 mons in quorum |
| MGR 状态 | `cephadm shell ceph mgr stat` | 1 active + 1 standby |
| OSD 数量 | `cephadm shell ceph osd stat` | 3 osds: 3 up, 3 in |
| 存储容量 | `cephadm shell ceph df` | 约 180GiB 原始 / 60GiB 可用 |

### 9.3 检查各节点服务

```bash
cephadm shell ceph orch ps
```

应能看到每台节点上运行的服务进程（mon、mgr、osd、node-exporter 等）。

---

## 十、部署后配置

> ⚠ **为什么需要包装脚本**：`ceph-common`（提供 `ceph`、`rbd` 等命令行工具）是从 jammy 源安装的，它与 Ubuntu 24.04 的系统库（libicu74、libldap 等）版本不兼容，无法安装。替代方案是通过 `cephadm shell` 容器来执行这些命令。

### 10.1 创建 ceph 命令包装脚本

- **执行位置**：ceph1

```bash
cat > /usr/bin/ceph << 'CEPH'
#!/bin/bash
exec cephadm shell -- ceph "$@"
CEPH
chmod +x /usr/bin/ceph
```

**验证**：
```bash
ceph -s
# 现在应该输出集群状态，而不是 "command not found"
```

> 注意：首次执行会慢一点（启动容器），后续执行会复用已有容器，速度快很多。

### 10.2 创建 rbd 命令包装脚本

```bash
cat > /usr/bin/rbd << 'RBD'
#!/bin/bash
# map/showmapped/unmap 需要访问宿主机 /sys 和 /dev 来加载内核模块
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

**验证**：
```bash
rbd ls      # 普通模式，不需要挂载宿主机
rbd showmapped   # map/showmapped 模式，自动 --mount
# 两个命令都应正常运行
```

### 10.3 加载 RBD 内核模块（ceph1 执行）

> 只有需要将 RBD 镜像映射为本地块设备（`rbd map`）时才需要这一步。

```bash
# 手动加载
modprobe rbd

# 设置开机自动加载
echo 'rbd' > /etc/modules-load.d/rbd.conf
```

**验证**：
```bash
lsmod | grep rbd
# 应显示 rbd 和 libceph 两个模块
```

### 10.4 设置代理开机生效（可选）

如果你使用了代理，建议加入 bashrc：

```bash
cat >> /root/.bashrc << 'PROXY'
export http_proxy="http://192.168.12.187:7897"
export https_proxy="http://192.168.12.187:7897"
PROXY
```

---

## 十一、RBD 块存储功能测试

> **说明**：测试验证集群存储功能是否正常工作。

### 11.1 创建存储池

```bash
ceph osd pool create mypool 32 32
ceph osd pool application enable mypool rbd
```

### 11.2 创建块设备镜像

```bash
rbd create --size 5G mypool/my-disk
```

**验证**：
```bash
rbd ls mypool
# 应输出：my-disk

rbd info mypool/my-disk
# 应显示 size 5 GiB, format 2
```

### 11.3 映射到本地

```bash
rbd map mypool/my-disk
```

**验证**：
```bash
rbd showmapped
# 应输出类似：
# id  pool    image     snap  device
# 0   mypool  my-disk   -     /dev/rbd0

ls -la /dev/rbd0
# 块设备文件已创建
```

> **如果报错 `modinfo: ERROR: Module rbd not found`**：回到 10.3 节执行 `modprobe rbd`。

### 11.4 格式化并挂载

```bash
mkfs.ext4 /dev/rbd0
mkdir -p /mnt/ceph-rbd
mount /dev/rbd0 /mnt/ceph-rbd
```

**验证**：
```bash
df -h /mnt/ceph-rbd
# Filesystem      Size  Used Avail Use% Mounted on
# /dev/rbd0       4.9G   24K  4.6G   1% /mnt/ceph-rbd
```

### 11.5 读写测试

```bash
echo "Hello Ceph RBD!" > /mnt/ceph-rbd/test.txt
cat /mnt/ceph-rbd/test.txt
# 应输出：Hello Ceph RBD!
```

### 11.6 卸载

```bash
umount /mnt/ceph-rbd
rbd unmap mypool/my-disk
```

---

## 十二、Dashboard 登录

### 12.1 访问地址

在浏览器中打开：

```
https://192.168.12.176:8443/
```

> 由于使用自签名证书，浏览器会提示"不安全"，点击"高级"→"继续前往"即可。

### 12.2 登录账号

- **用户名**：`admin`
- **密码**：bootstrap 时终端输出的随机密码

### 12.3 如果密码忘了或太弱

```bash
# Ceph 19 有强密码策略：至少 8 位，需含大写字母 + 数字
echo "CephAdmin@2026" | cephadm shell -- ceph dashboard ac-user-set-password admin -i -
```

> 密码 `123456` 会被拒绝，提示 `Error EINVAL: Password is too weak.`

---

## 十三、故障排查

### 13.1 常见错误及处理

| # | 错误信息 | 原因 | 解决方案 |
|---|---------|------|----------|
| 1 | `ssh: connect to host 192.168.12.xx port 22: No route to host` | 网络不通 | 检查虚拟机网络配置，确认同一网段 |
| 2 | `Permission denied (publickey,password)` | root SSH 未开启 | 回到第 4 步，用 ps 用户 + sudo 修改 sshd_config |
| 3 | `apt update` 报 404 `noble Release` | 用错了 apt 源（写了 noble） | 改为 `jammy main` 而不是 `noble main` |
| 4 | `cephadm: command not found` | 包装脚本未创建 | 执行 6.3 节的 printf + chmod |
| 5 | `bash: /usr/local/bin/cephadm: 无法执行` | zipapp 直接执行了 | 需要 python3 解释器调用，确认包装脚本存在 |
| 6 | `gpg: cannot open '/dev/tty'` | SSH 无 TTY | 使用 `[trusted=yes]` 跳过 GPG 验证 |
| 7 | `Error EINVAL: Password is too weak` | 密码不符合 Ceph 19 策略 | 用 8 位以上含大小写+数字的密码 |
| 8 | `rbd: sysfs write failed` | 内核 rbd 模块未加载 | `modprobe rbd` 加载模块 |
| 9 | `bootstrap` 拉取镜像极慢 | 国内访问 quay.io 慢 | 配置 HTTP 代理后重新 bootstrap |

### 13.2 如何查看日志

```bash
# 查看 cephadm 引导日志
cephadm bootstrap --mon-ip 192.168.12.176 2>&1 | tee /root/ceph-bootstrap.log

# 查看 ceph 集群日志
journalctl -u ceph-mon@ceph1 -n 50 --no-pager

# 查看容器运行时日志
podman logs <容器名>
```

### 13.3 OSD 添加失败

如果 `ceph orch daemon add osd` 失败或 OSD 状态为 `down`：

```bash
# 1. 检查磁盘是否可被识别
cephadm shell ceph orch device ls

# 2. 如果显示 "Has a FileSystem"，登录该节点执行
wipefs -a /dev/sdb

# 3. 重新添加
cephadm shell ceph orch daemon add osd <hostname>:/dev/sdb
```

---

## 十四、完全重置

如果需要从头重新部署：

### 14.1 在 ceph1 上获取 FSID 并清空集群

```bash
ceph fsid
# 记下输出的 FSID，例如 a2173549-84bf-11f1-8f8e-000c29c6cc12

cephadm rm-cluster --fsid <FSID> --force
```

### 14.2 在全部 3 节点上清理 LVM 残留

```bash
vgremove -f $(vgs --noheadings -o vg_name 2>/dev/null) 2>/dev/null
pvremove -f $(pvs --noheadings -o pv_name 2>/dev/null) 2>/dev/null
wipefs -a /dev/sdb
```

### 14.3 清理 apt 源

```bash
rm -f /etc/apt/sources.list.d/ceph.list
```

### 14.4 重新部署

从 **第三章（系统初始化）** 重新开始执行。

---

> **文档版本**：v2.0 — 适配 Ubuntu 24.04 (noble) + Ceph Squid 19.x
> **最后更新**：2026-07-21
