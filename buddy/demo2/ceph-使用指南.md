# Ceph 集群使用指南

> 集群概要：3 节点（ceph1/2/3），3 副本，原始容量 180GiB，可用 ≈60GiB，Ceph Squid 19.x

---

## 目录

- [一、RBD 块存储（最常用）](#一rbd-块存储最常用)
- [二、CephFS 文件存储](#二cephfs-文件存储)
- [三、S3 对象存储（RGW）](#三s3-对象存储rgw)
- [四、Dashboard 管理界面](#四dashboard-管理界面)
- [五、日常管理命令](#五日常管理命令)
- [六、客户端挂载（其他 Linux 机器使用 Ceph 存储）](#六客户端挂载其他-linux-机器使用-ceph-存储)

---

## 一、RBD 块存储（最常用）

块存储就像一个虚拟硬盘，可以映射到 Linux 系统上，格式化后像本地磁盘一样读写。适合给虚拟机提供磁盘、数据库存储等场景。

> **执行位置：ceph1**

### 1.1 创建存储池

```bash
# 创建存储池（PG 数 = OSD数×100÷副本数，3 个 OSD 取 32）
ceph osd pool create mypool 32 32

# 启用 RBD 功能
ceph osd pool application enable mypool rbd
```

### 1.2 创建块设备镜像

```bash
# 创建一个 5GB 的块设备
rbd create --size 5G mypool/my-disk

# 查看已有镜像
rbd ls mypool

# 查看镜像详细信息
rbd info mypool/my-disk
```

### 1.3 映射到本地使用

```bash
# 1. 映射 RBD 镜像为本地块设备
rbd map mypool/my-disk

# 2. 查看映射的设备名（通常是 /dev/rbd0）
rbd showmapped

# 3. 格式化（仅首次）
mkfs.ext4 /dev/rbd0

# 4. 挂载
mkdir -p /mnt/ceph-rbd
mount /dev/rbd0 /mnt/ceph-rbd

# 5. 正常使用
echo "Hello Ceph" > /mnt/ceph-rbd/test.txt
cat /mnt/ceph-rbd/test.txt
df -h /mnt/ceph-rbd
```

### 1.4 使用完毕卸载

```bash
umount /mnt/ceph-rbd
rbd unmap mypool/my-disk
```

### 1.5 扩容（在线扩，不影响使用）

```bash
# 扩容到 10G
rbd resize --size 10G mypool/my-disk

# 扩容文件系统（在挂载端执行）
resize2fs /dev/rbd0
```

### 1.6 快照

```bash
# 创建快照
rbd snap create mypool/my-disk@snap1

# 查看快照
rbd snap ls mypool/my-disk

# 回滚到快照（⚠️ 会丢失快照之后的数据）
rbd snap rollback mypool/my-disk@snap1

# 删除快照
rbd snap rm mypool/my-disk@snap1
```

### 1.7 删除块设备

```bash
# 先确保已卸载再删
rbd rm mypool/my-disk
```

---

## 二、CephFS 文件存储

CephFS 像 NFS 一样，多台客户端可以同时挂载同一个目录，共享读写。适合共享文件、容器持久卷等场景。

### 2.1 部署 MDS 服务

> **执行位置：ceph1**

```bash
# 创建 CephFS 所需的存储池
ceph osd pool create cephfs_data 32 32
ceph osd pool create cephfs_metadata 32 32

# 创建 CephFS 文件系统
ceph fs new myfs cephfs_metadata cephfs_data

# 部署 MDS 服务（每个节点一个）
ceph orch apply mds myfs --placement="3 ceph1 ceph2 ceph3"

# 验证 MDS 状态
ceph mds stat
```

### 2.2 挂载 CephFS

#### 方式一：内核驱动挂载（推荐）

```bash
# 安装 ceph-common（如果还没有）
apt install -y ceph-common

# 创建挂载点
mkdir -p /mnt/cephfs

# 挂载（ceph1 上已有 keyring，其他节点需先获取 key）
mount -t ceph ceph1:6789:/ /mnt/cephfs -o name=admin
```

#### 方式二：用 secret key 挂载

```bash
# 获取 admin 密钥
cat /etc/ceph/ceph.client.admin.keyring

# 挂载（其他节点上用）
mount -t ceph 192.168.12.176:6789:/ /mnt/cephfs \
  -o name=admin,secret=<你的密钥>
```

### 2.3 验证

```bash
echo "CephFS works!" > /mnt/cephfs/hello.txt

# 在 ceph2 上挂载同一目录后也能看到
```

---

## 三、S3 对象存储（RGW）

RGW 提供兼容 AWS S3 的 RESTful API，适合存图片、视频、备份等非结构化数据。

### 3.1 部署 RGW 服务

> **执行位置：ceph1**

```bash
# 创建 RGW 存储池
ceph osd pool create rgw_data 32 32
ceph osd pool create rgw_meta 32 32
ceph osd pool create rgw_log 32 32

# 在各节点部署 RGW 服务
ceph orch apply rgw myrgw --placement="3 ceph1 ceph2 ceph3"
```

### 3.2 创建 S3 用户

```bash
# 创建一个 S3 用户
radosgw-admin user create --uid=testuser --display-name="Test User"

# 输出中会包含 access_key 和 secret_key，请记录下来
```

### 3.3 使用 S3 客户端测试

```bash
# 安装 s3cmd
apt install -y s3cmd

# 配置 s3cmd（根据上一步的 access_key 和 secret_key）
s3cmd --configure

# 创建桶
s3cmd mb s3://my-bucket

# 上传文件
s3cmd put /etc/hosts s3://my-bucket/hosts.txt

# 列举文件
s3cmd ls s3://my-bucket/

# 下载文件
s3cmd get s3://my-bucket/hosts.txt /tmp/hosts-backup.txt
```

> RGW 默认监听端口 7480（HTTP），实际使用时建议通过 Nginx 反代成 80/443。

---

## 四、Dashboard 管理界面

### 4.1 访问

浏览器打开：**https://192.168.12.176:8443**

- 用户名：`admin`
- 密码：bootstrap 时输出的密码（你的是 `kxf55g0ge4`）

### 4.2 功能概览

| 页面 | 功能 |
|------|------|
| 仪表盘 | 集群容量、IOPS、吞吐量、健康状态总览 |
| 集群 → 主机 | 查看各节点 CPU/内存/磁盘使用率 |
| 集群 → OSD | 查看每个 OSD 状态、用量、性能 |
| 集群 → 存储池 | 管理 pool，查看 PG 分布 |
| 集群 → 监控 | MON/MGR 状态 |

---

## 五、日常管理命令

> **以下命令均在 ceph1 上执行**

### 查看状态

```bash
ceph -s                  # 集群总览
ceph -w                  # 实时监控（Ctrl+C 退出）
ceph osd tree            # OSD 拓扑
ceph osd df              # 各 OSD 用量
ceph df                  # 集群容量
ceph health detail       # 健康详情
```

### 存储池管理

```bash
ceph osd lspools                        # 列出所有 pool
ceph osd pool get mypool size           # 查看副本数
ceph osd pool set mypool size 2         # 临时改为 2 副本（省空间）
ceph osd pool stats mypool              # pool 用量统计
ceph osd pool delete mypool mypool --yes-i-really-really-mean-it  # 删除 pool
```

### 性能测试

```bash
# RBD 写入测试
rbd bench-write mypool/test --io-size 4096 --io-threads 4 --io-total 1G

# RADOS 吞吐量测试
rados bench -p mypool 60 write
rados bench -p mypool 60 read

# 更专业的 fio 测试
fio --name=test --ioengine=rbd --rbdname=mypool/test --rw=randwrite --bs=4k --size=1G --numjobs=4 --runtime=30
```

### OSD 维护

```bash
# 临时停止重平衡（维护前先执行，防止拔盘时数据迁移）
ceph osd set noout

# 维护完成后恢复
ceph osd unset noout

# 查看某个 OSD 的详细信息
ceph osd find 0
```

---

## 六、客户端挂载（其他 Linux 机器使用 Ceph 存储）

如果你想在集群之外的机器（比如应用服务器）上使用 Ceph 存储，需要安装客户端。

### 6.1 安装客户端

> **执行位置：客户端机器（Ubuntu/Debian）**

```bash
apt update
apt install -y ceph-common
```

### 6.2 获取集群配置

> **执行位置：ceph1**

```bash
# 查看 admin 密钥
cat /etc/ceph/ceph.client.admin.keyring
```

输出类似：
```
[client.admin]
    key = AQABCDEF1234567890abcdef==
```

### 6.3 客户端配置

> **执行位置：客户端机器**

```bash
# 创建 ceph 配置目录
mkdir -p /etc/ceph

# 写入集群配置（指定 MON 地址）
cat > /etc/ceph/ceph.conf << 'EOF'
[global]
mon host = 192.168.12.176,192.168.12.90,192.168.12.169
EOF

# 写入 admin 密钥（把 <KEY> 换成上一步的实际值）
cat > /etc/ceph/ceph.client.admin.keyring << 'EOF'
[client.admin]
    key = <KEY>
EOF
```

### 6.4 验证客户端

```bash
ceph -s
# 如果能正常显示集群状态，说明客户端配置成功
```

### 6.5 客户端使用 RBD

```bash
# 映射块设备
rbd map mypool/my-disk

# 格式化（首次）
mkfs.ext4 /dev/rbd0

# 挂载使用
mount /dev/rbd0 /mnt/data
```

### 6.6 客户端挂载 CephFS

```bash
mkdir -p /mnt/cephfs
mount -t ceph 192.168.12.176,192.168.12.90,192.168.12.169:6789:/ /mnt/cephfs \
  -o name=admin,secret=<KEY>
```

---

## 常见问题

| 问题 | 解决 |
|------|------|
| `rbd: sysfs write failed` | 检查是否已 `rbd map` 过，用 `rbd showmapped` 确认 |
| 挂载 CephFS 报错 `connection refused` | 确认 MDS 已部署且 `ceph mds stat` 显示 active |
| 客户端 `ceph -s` 连不上 | 检查 `/etc/ceph/ceph.conf` 中 MON IP 是否正确，网络是否互通 |
| 存储池满无法写入 | `ceph df` 查看用量，扩容加 OSD 或清理数据 |
