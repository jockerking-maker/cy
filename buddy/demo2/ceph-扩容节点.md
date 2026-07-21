# Ceph 集群扩容：添加第4台节点

> 前提：已有 ceph1 (192.168.12.176)、ceph2 (192.168.12.90)、ceph3 (192.168.12.169) 组成的 3 节点集群，状态 HEALTH_OK。

---

### 一、新节点硬件要求

| 项目 | 要求 |
|------|------|
| 操作系统 | Ubuntu 24.04 LTS |
| CPU | 2 核以上 |
| 内存 | 3GB 以上 |
| 系统盘 | 30GB+ |
| 数据盘 | 一块空白裸盘（如 /dev/sdb，容量参考现有节点 60GB） |
| 网络 | 与现有节点同一网段，静态 IP |

---

### 二、前期规划

| 主机名 | IP 地址 | 角色 |
|--------|---------|------|
| ceph4  | 192.168.12.xxx | MON + OSD |

> IP 自行规划为同网段（192.168.12.0/24）的空闲地址。

---

### 三、操作步骤

#### 步骤1：ceph4 系统初始化

> **执行位置：ceph4**

**1.1 设置主机名**
```bash
hostnamectl set-hostname ceph4
```

**1.2 配置主机名解析**

在 ceph4 上添加所有节点（含自身）：
```bash
cat >> /etc/hosts << 'EOF'
192.168.12.176 ceph1
192.168.12.90  ceph2
192.168.12.169 ceph3
192.168.12.xxx ceph4
EOF
```

**1.3 基础环境配置**
```bash
# 禁用 swap
swapoff -a
sed -i '/swap/s/^/#/' /etc/fstab

# 安装 podman + lvm2
apt update && apt install -y podman lvm2

# 时间同步
apt install -y chrony
systemctl enable --now chrony

# 关闭防火墙
ufw disable

# 依赖工具
apt install -y apt-transport-https ca-certificates curl gnupg lsb-release
```

**1.4 允许 root SSH 登录**
```bash
sed -i 's/^#PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config
sed -i 's/^PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config
sed -i 's/^#PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config
sed -i 's/^PasswordAuthentication no/PasswordAuthentication yes/' /etc/ssh/sshd_config
systemctl restart sshd
```

---

#### 步骤2：配置 SSH 免密

> **执行位置：ceph1**

**2.1 分发 cephadm 管理密钥到 ceph4**
```bash
ssh-copy-id -f -i /etc/ceph/ceph.pub root@ceph4
```
> 根据提示输入 ceph4 的 root 密码。

**2.2 验证免密**
```bash
ssh -i /etc/ceph/ceph.pub root@ceph4 hostname
```
返回 `ceph4` 即为成功。

---

#### 步骤3：将 ceph4 加入集群

> **执行位置：ceph1**

```bash
ceph orch host add ceph4 192.168.12.xxx
```

确认加入成功：
```bash
ceph orch host ls
```
应看到 ceph1 ~ ceph4 四台主机。

---

#### 步骤4：添加 OSD 数据盘

> **执行位置：ceph4 上先清盘，ceph1 上添加 OSD**

**4.1** 在 ceph4 上确认数据盘并清空：
```bash
lsblk
# 确认看到了 sdb（或 vdb）

wipefs -a /dev/sdb
```

**4.2** 在 ceph1 上添加 OSD：
```bash
ceph orch daemon add osd ceph4:/dev/sdb
```

> 也可以用 `ceph orch apply osd --all-available-devices` 自动添加，但精确命令更可控。

**4.3** 等待 1~2 分钟，验证 OSD：
```bash
ceph osd tree
```
应看到 ceph4 下有一个 `up` 状态的 OSD。

---

#### 步骤5：验证集群状态

> **执行位置：ceph1**

```bash
ceph -s
```

预期：`HEALTH_OK`，可用容量增加。如果数据盘大小一致（三台 60GB），新增后总容量约 240GiB 原始，可用约 80GiB（3 副本）。

---

### 四、关于 MON 的说明

添加第 4 个节点后，MON 数量仍然是 3 个（ceph1、ceph2、ceph3）。因为 MON 节点推荐保持**奇数**（1/3/5），3 个 MON 对 4 节点集群完全够用，没必要再加 MON。

如果想在 ceph4 上也跑 MON（改为 5 节点法定人数），执行：

```bash
ceph orch apply mon 5
```

但 3~4 节点的测试集群不建议折腾，3 个 MON 够用了。

---

### 五、后续：数据自动重平衡

新 OSD 加入后，Ceph 会**自动**将部分现有数据迁移到新盘上，无需手动干预。可以用以下命令观察进度：

```bash
ceph -w          # 实时观察集群状态变化
ceph osd df      # 查看各 OSD 用量分布
```
