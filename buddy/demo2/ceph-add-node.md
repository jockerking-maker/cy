# Ceph 新增存储节点操作文档

> **任务目标**：向现有 Ceph 集群添加一台新的存储节点（ceph4），扩增集群存储容量。
> **文档版本**：v1.0 | 2026-07-21

---

## 一、操作环境判断

### ❓ 能否在 Windows 本机完成？

**不能**。新增集群节点需要**新建一台 Ubuntu 24.04 LTS 虚拟机**，配置到与现有集群同一网络中。

### ✅ 推荐方案

在 VMware / Proxmox 等虚拟化平台新建一台虚拟机，规格建议如下：

| 资源 | 推荐配置 | 说明 |
|------|---------|------|
| CPU | 4 核 | OSD 压缩/加密需要额外 CPU 资源 |
| 内存 | 6 GB | OSD 进程 ≥1GB + 容器运行时开销 |
| 系统盘 | 50 GB | 安装 OS 与容器镜像 |
| **数据盘** | **1 块 × 50GB+** | **独立的裸磁盘**，不分区不格式化，将作为新 OSD |
| 网络 | 桥接模式，与集群同网段 | |

### 新增节点规划

| 项目 | 值 |
|------|-----|
| 主机名 | ceph4 |
| IP 地址 | 192.168.12.175（建议，须与集群同网段且不冲突） |
| MAC 地址 | 虚拟化平台自动分配，安装后通过 `ip link` 查看 |
| 子网掩码 | 255.255.255.0 |
| 网关 | 192.168.12.254 |
| DNS | 202.96.209.5 / 202.96.199.133 |
| 数据盘 | /dev/sdb（新增的裸盘） |

---

## 二、前提条件

- [ ] 现有 Ceph 集群运行正常，`ceph -s` 返回 `HEALTH_OK`
- [ ] 新建 ceph4 虚拟机已安装 Ubuntu 24.04，已配置静态 IP
- [ ] 已为 ceph4 添加一块空白数据盘（如 /dev/sdb）
- [ ] ceph4 与现有 3 节点网络互通（`ping` 通所有节点）
- [ ] ceph1 上的 root SSH 免密已配置（部署时已完成）
- [ ] 数据盘 `/dev/sdb` 无分区无文件系统（`lsblk` 确认）

---

## 三、操作步骤

### 步骤 1：新增节点系统初始化

> **执行位置**：在 **ceph4** 上以 root 身份执行

#### 1.1 设置主机名

```bash
hostnamectl set-hostname ceph4
```

#### 1.2 配置 hosts 解析

```bash
cat >> /etc/hosts << 'HOSTS'
192.168.12.176 ceph1
192.168.12.90  ceph2
192.168.12.169 ceph3
192.168.12.175 ceph4
HOSTS
```

**验证**：
```bash
ping -c 1 ceph1
ping -c 1 ceph2
ping -c 1 ceph3
# 都能 ping 通
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

**验证**：
```bash
podman --version
# 应输出 4.x 版本号
```

#### 1.5 时间同步

```bash
apt install -y chrony
systemctl enable --now chrony
```

**验证**：
```bash
chronyc tracking | grep -i "system time"
# 偏移量应在毫秒级别
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

### 步骤 2：配置 root SSH 访问

> **执行位置**：ceph4
> ⚠ Ubuntu 24.04 默认禁止 root SSH 密码登录，需要先用普通用户（ps）提权配置。

#### 2.1 如果已有 ps 用户（有 sudo 权限）

如果 ceph4 上也创建了 ps 用户（密码 1），执行：

```bash
# 设置 root 密码
echo 'root:1' | sudo chpasswd

# 允许 root SSH 登录
sudo sed -i 's/^#PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config
sudo sed -i 's/^PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config

# 开启密码认证
sudo sed -i 's/^#PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config
sudo sed -i 's/^PasswordAuthentication no/PasswordAuthentication yes/' /etc/ssh/sshd_config

# 重启 SSH（注意服务名是 ssh，不是 sshd）
sudo systemctl restart ssh
```

#### 2.2 如果只有 root 终端（通过虚拟化平台控制台）

直接在控制台以 root 登录：

```bash
# 修改 SSH 配置允许 root 密码登录
sed -i 's/^#PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config
sed -i 's/^PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config
sed -i 's/^#PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config
sed -i 's/^PasswordAuthentication no/PasswordAuthentication yes/' /etc/ssh/sshd_config

# 设置 root 密码
passwd root
# 输入密码 1

# 重启 SSH
systemctl restart ssh
```

---

### 步骤 3：从 ceph1 分发 SSH 密钥到新节点

> **执行位置**：在 **ceph1** 上执行

```bash
# 将 ceph1 的 root SSH 公钥分发到 ceph4
sshpass -p '1' ssh-copy-id -o StrictHostKeyChecking=no root@192.168.12.175

# 将 ceph 集群管理密钥分发到 ceph4（重要！ceph orch 用这套密钥）
sshpass -p '1' ssh-copy-id -f -i /etc/ceph/ceph.pub -o StrictHostKeyChecking=no root@192.168.12.175
```

**验证**：
```bash
ssh root@ceph4 hostname
# 应直接返回 ceph4，无需输入密码
```

---

### 步骤 4：将新节点加入集群

> **执行位置**：在 **ceph1** 上执行

#### 4.1 添加主机到集群

```bash
cephadm shell ceph orch host add ceph4 192.168.12.175
```

**验证主机已加入**：
```bash
cephadm shell ceph orch host ls
# 应显示 4 hosts in cluster（ceph1, ceph2, ceph3, ceph4）
```

#### 4.2 验证 ceph4 上的 daemon 已启动

等待约 30 秒后，查看各节点上的守护进程：

```bash
cephadm shell ceph orch ps | grep ceph4
```

应看到类似输出（new-exporter 是自动部署的基础监控组件）：
```
ceph-exporter.ceph4  ceph4  *:9926  running (...) ...
node-exporter.ceph4   ceph4  *:9100  running (...) ...
crash.ceph4           ceph4          running (...) ...
```

---

### 步骤 5：在新节点上添加 OSD

#### 5.1 准备数据盘

> **执行位置**：ceph4

```bash
# 确认数据盘
lsblk | grep sdb
# 应显示 sdb 无分区

# 清空数据盘
wipefs -a /dev/sdb
```

#### 5.2 在集群中查看新设备

> **执行位置**：ceph1

```bash
cephadm shell ceph orch device ls | grep ceph4
```

如果显示 `AVAIL = Yes`，则设备可用。如果显示其他拒绝原因，根据提示处理。

#### 5.3 添加 OSD

> **执行位置**：ceph1

```bash
cephadm shell ceph orch daemon add osd ceph4:/dev/sdb
```

#### 5.4 验证新 OSD

> **执行位置**：ceph1

```bash
cephadm shell ceph osd tree
```

应看到新增了一行：

```
ID  CLASS  WEIGHT   TYPE NAME           STATUS  REWEIGHT  PRI-AFF
...
-9         0.05859      host ceph4
 3    hdd  0.05859          osd.3           up   1.00000  1.00000
```

关键检查：**osd: 4 osds: 4 up, 4 in**

```bash
cephadm shell ceph -s | grep osd
# 应输出：4 osds: 4 up, 4 in
```

> 新 OSD 添加后，Ceph 会自动开始数据再平衡（rebalancing），将部分 PG 分布到新 OSD 上。这在后台进行，不影响现有业务。

---

### 步骤 6：监控数据再平衡

> **执行位置**：ceph1

添加新 OSD 后，Ceph 会自动触发数据再平衡，将部分数据从原有 3 个 OSD 迁移到新 OSD。

#### 6.1 查看再平衡状态

```bash
ceph -w
```

按 `Ctrl+C` 退出实时监控。在再平衡期间，会看到 `pg` 状态中出现 `acting` 等中间状态，以及 PG 迁移的进度百分比。

#### 6.2 查看容量变化

```bash
ceph df
```

扩容前/后对比：

| 指标 | 3 OSD 时 | 4 OSD 后 |
|------|---------|---------|
| RAW USED | ~17 GiB | 不变 |
| RAW TOTAL | 180 GiB | **240 GiB**（+60GiB） |
| RAW AVAIL | 163 GiB | **223 GiB** |
| 可用容量（3副本）| ~54 GiB | **~74 GiB** |

> 可用容量公式：原始总量 ÷ 副本数。4 OSD × 60GiB ÷ 3 = **最大约 80GiB 可用**。

#### 6.3 等待再平衡完成

> 再平衡速度受网络带宽和 OSD 性能影响。对于 20GiB 数据量，通常在 **5~15 分钟** 内完成。

```bash
# 查看 PG 状态
cephadm shell ceph pg stat
# 当所有 PG 状态为 active+clean 时，再平衡完成
```

---

### 步骤 7：验证最终集群状态

> **执行位置**：ceph1

```bash
ceph -s
```

**预期结果（关键项）**：

| 检查项 | 扩容前 | 扩容后 |
|--------|--------|--------|
| health | HEALTH_OK | HEALTH_OK |
| mon | 3 daemons | 3 daemons（不变） |
| osd | 3 osds: 3 up, 3 in | **4 osds: 4 up, 4 in** |
| 原始容量 | ~180 GiB | **~240 GiB** |
| 可用容量 | ~54 GiB | **~74 GiB** |

> MON 数量保持不变（3 个已满足高可用要求，不需要也不需要增加）。

---

## 四、验证方法汇总

| 验证点 | 命令 | 预期结果 |
|--------|------|---------|
| 新节点在线 | `ceph orch host ls` | 4 hosts: ceph1~ceph4 |
| 新 OSD 在线 | `ceph osd tree` | osd.3 up, host ceph4 |
| OSD 总数 | `ceph -s \| grep osd` | 4 osds: 4 up, 4 in |
| 集群健康 | `ceph health` | HEALTH_OK |
| PG 分布 | `ceph pg stat` | 所有 active+clean |
| 新节点服务 | `ceph orch ps \| grep ceph4` | exporter 等 daemon 运行中 |

---

## 五、常见问题

| # | 问题 | 原因 | 解决 |
|---|------|------|------|
| 1 | `ceph orch host add` 失败，报 `Failed to connect to ceph4` | ceph1 到 ceph4 的 SSH 免密未配置好 | 在 ceph1 执行 `ssh root@ceph4 hostname` 测试免密，重新执行步骤 3 |
| 2 | `ceph orch device ls` 中 ceph4 的磁盘 `AVAIL = No` | 磁盘有残留分区 | 在 ceph4 执行 `wipefs -a /dev/sdb` 后重新查看 |
| 3 | 新 OSD 状态为 `down` | 刚添加需要时间上线 | 等待 30~60 秒，或 `ceph osd tree` 查看原因 |
| 4 | 集群状态变为 `HEALTH_WARN`，PG 状态 `remapped` 或 `peering` | 数据再平衡中，正常现象 | 等待自动完成，`ceph -w` 追踪进度 |
| 5 | 添加 OSD 后磁盘 I/O 飙升 | 再平衡的读写压力 | 正常，可调低再平衡速度：`ceph osd set-backfillfull-ratio 0.9` |
| 6 | 新增节点上 daemon 起不来 | 时间不同步 | 在 ceph4 检查 `chronyc tracking`，确保时间误差 ≤50ms |

---

## 六、扩展示：添加多节点

如果需要一次性添加多台节点，重复 **步骤 1~5** 即可。

批量添加建议流程：
1. 准备好所有新节点虚拟机（系统初始化完成）
2. 逐一配置 root SSH（步骤 2）
3. 在 ceph1 上一次性分发 SSH 密钥到所有新节点（步骤 3）
4. 逐个执行 `ceph orch host add` 和 `ceph orch daemon add osd`（步骤 4~5）
5. 全部添加完成后，让 Ceph 自动进行数据再平衡，所有 OSD 会同时参与

---

## 七、注意事项

1. **数据盘必须是独立裸盘**，不能是系统盘分区，否则 cephadm 不会识别
2. **磁盘容量建议与现有 OSD 一致**，否则会导致集群容量分布不均
3. **再平衡期间集群性能会受影响**，生产环境建议在维护窗口操作
4. 新节点添加后 **无需重启现有集群**，cephadm 自动管理
5. 如果后续需要移除该节点，使用 `ceph orch host rm ceph4`（需先清空其 OSD）
6. **扩容后不可逆**——虽然可以删除 OSD，但会导致再平衡，生产操作需谨慎
