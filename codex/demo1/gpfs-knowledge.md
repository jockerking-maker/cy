# GPFS (General Parallel FileSystem) 知识文档

> **GPFS** 是 IBM 开发的高性能并行文件系统，后更名为 **IBM Storage Scale**（曾用名 **Spectrum Scale**）。它最初为 AIX 设计，现已支持 Linux 和 Windows，广泛应用于 HPC、AI/ML 和大数据分析场景。GPFS 是许多超算 TOP500 系统的底层存储方案，也是 **Lustre** 之外最主流的并行文件系统之一。

---

## 目录

1. [概述与历史](#1-概述与历史)
2. [核心架构](#2-核心架构)
3. [关键概念](#3-关键概念)
4. [特色功能](#4-特色功能)
5. [部署与运维](#5-部署与运维)
6. [性能调优](#6-性能调优)
7. [与竞品对比](#7-与竞品对比)
8. [常用命令速查](#8-常用命令速查)
9. [参考资源](#9-参考资源)

---

## 1. 概述与历史

| 时间 | 里程碑 |
|------|--------|
| 1990s | IBM 为 AIX 超级计算集群开发 GPFS |
| 2009 | 发布 GPFS 3.2，支持 Linux 客户端 |
| 2014 | 更名为 IBM Spectrum Scale，纳入软件定义存储战略 |
| 2019 | 更名为 IBM Storage Scale，持续演进 |
| 至今 | 支撑全球数十套 TOP500 超算，活跃迭代 |

**适用场景：**

- 高性能计算（HPC）—— 数千节点并发读写
- AI/ML 训练数据管道 —— 高吞吐、低延迟
- 大数据分析（Hadoop/Spark 本地化）
- 文件共享与内容存储 —— 统一命名空间
- 灾备与多站点复制 —— 跨数据中心同步

---

## 2. 核心架构

### 2.1 架构总览

```
+-----------------------+
|   NSD Clients         |
| Node1 Node2 Node3 ... |
+-----------+-----------+
            |
  Storage Network (IB/Ethernet)
            |
+-----------v-----------+
|   NSD Servers          |
|  (I/O Nodes)           |
+-----------+-----------+
            |
+-----------v-----------+
|   SAN / JBOD / NVMe   |
|  Disk1 Disk2 Disk3... |
+-----------------------+
```

### 2.2 核心组件

| 组件 | 说明 |
|------|------|
| **NSD (Network Shared Disk)** | 核心抽象层，将物理磁盘抽象为网络共享磁盘，供所有节点访问 |
| **NSD Client** | 挂载 GPFS 文件系统的节点，不直接管理磁盘 I/O |
| **NSD Server** | 直接管理物理磁盘 I/O 的节点，为客户端提供数据访问 |
| **Quorum Node** | 参与集群仲裁的节点，用于故障恢复和一致性保证 |
| **Manager Node** | 负责集群元数据管理和协调的特殊节点 |
| **CES (Cluster Export Services)** | 对外提供 NFS/SMB/Object 协议的网关节点 |

### 2.3 数据分布策略

GPFS 使用 **Striped 布局**（类似 RAID 0）将文件数据分片存储到多个 NSD 上：

- **数据块（Data Blocks）**：文件被分割成固定大小的块，分布在多个磁盘上
- **间接块（Indirect Blocks）**：管理数据块到磁盘位置的映射关系
- **分配映射（Allocation Map）**：跟踪每个磁盘的可用空间

支持三种文件复制策略：

1. **无复制（No Replication）**：类似 RAID 0，无冗余
2. **数据复制（Data Replication）**：副本数 1~3，写入时同步复制
3. **元数据复制（Metadata Replication）**：仅复制文件元数据，数据可不复制

---

## 3. 关键概念

### 3.1 集群（Cluster）

GPFS 集群是一组协同工作的节点集合。所有节点共享同一个配置文件，通过心跳保持连接。

### 3.2 文件系统（File System）

一个 GPFS 集群中可以创建多个文件系统，每个文件系统由一组 NSD 组成。

### 3.3 NSD (Network Shared Disk)

NSD 是 GPFS 的核心抽象，将物理存储设备（SAN LUN、本地盘、NVMe）封装为网络可访问的磁盘。

### 3.4 配额（Quota）

支持按用户、用户组、文件系统维度的空间和文件数限制。

### 3.5 策略引擎（Policy Engine）

GPFS 的策略引擎是一个强大的文件管理工具，支持基于规则的自动化操作，例如数据迁移、归档、删除等。

### 3.6 文件系统布局（Filesystem Layout）

GPFS 文件系统包含超级块、分配映射、日志、inode 表和数据块等核心结构。

---

## 4. 特色功能

### 4.1 分布式元数据管理

GPFS 采用 **分布式元数据架构**，没有专用的元数据服务器（MDS），元数据分散在所有 NSD 上，避免单点瓶颈。

### 4.2 分布式锁管理（DLM）

GPFS 的分布式锁管理器确保多节点并发访问时的数据一致性，支持字节范围锁、锁缓存和死锁检测。

### 4.3 数据生命周期管理（ILM）

基于策略引擎的自动分层存储，支持热数据（NVMe/SSD）→ 冷数据（HDD）→ 归档（Tape）的自动迁移。

### 4.4 快照（Snapshot）

基于写时复制（Copy-on-Write），创建几乎瞬时，空间占用极小，支持快照浏览和回滚。

### 4.5 远程复制（AFM / Active File Management）

AFM 是 GPFS 的多站点数据管理方案，支持 Local（本地缓存异步回写）、Global（全局命名空间按需拉取）、Disconnected（断联模式自动同步）和 Read-only（只读缓存）四种模式。

### 4.6 加密与安全

支持文件级加密（FKEK）、传输加密（TLS）、基于角色的访问控制（RBAC）。

### 4.7 DNFS (Direct NFS)

GPFS 提供了优化的 NFS 实现，通过并行 I/O 和多路径数据路径绕过传统 NFS 协议的单线程瓶颈。

---

## 5. 部署与运维

### 5.1 安装流程

1. 安装 RPM 包：`yum install gpfs.base gpfs.gpl gpfs.docs gpfs.msg.en_US gpfs.compression`
2. 配置节点描述文件（/etc/gpfs/nodefile）
3. 启动集群：`mmstartup -a`
4. 验证集群状态：`mmgetstate -a`

### 5.2 节点状态

```
mmgetstate -a 输出示例:

Node  Number  State      Remarks
----  ------  -----      -------
node1      1  active     quorum node
node2      2  active     quorum node
node3      3  active     quorum node
node4      4  active
node5      5  active
```

### 5.3 常见运维操作

| 操作 | 命令 |
|------|------|
| 关闭节点 | `mmshutdown node1` |
| 添加节点 | `mmaddnode -N node6 -A` |
| 删除节点 | `mmdelnode node6` |
| 添加磁盘 | `mmadddisk gpfs0 -F /path/to/new_disk` |
| 调整 inode 数量 | `mmchfs gpfs0 -i 65536` |
| 调整块大小 | `mmchfs gpfs0 -B 4M` |
| 查看文件系统使用 | `mmdf gpfs0` |
| 查看集群配置 | `mmlscluster` |

### 5.4 监控与告警

| 命令 | 作用 |
|------|------|
| `mmmon` | 实时监控集群状态 |
| `mmperfmon` | 性能监控框架 |
| `mmlslicense` | 查看许可证状态 |
| `mmhealth` | 健康检查 |
| `mmfsadm` | 高级调试与诊断 |

### 5.5 日志与排错

```bash
# 查看 GPFS 日志
tail -f /var/adm/ras/mmfs.log.basic

# 收集诊断信息
mmdiag 2>&1 | tee /tmp/gpfs_diag.log

# 跟踪文件系统 I/O
mmtrace -T all -f /tmp/trace.out
```

---

## 6. 性能调优

### 6.1 关键参数

| 参数 | 说明 | 建议值 |
|------|------|--------|
| `blockSize` | 文件系统块大小 | 256K ~ 8M，根据 IO 特征 |
| `maxMBpS` | 单节点最大吞吐 | 根据网络带宽 |
| `numaMemoryPerf` | NUMA 感知优化 | 开启 |
| `workerThreads` | 工作线程数 | 通常为 CPU 核心数 |
| `pagePool` | 页缓存池大小 | 节点内存的 10%~50% |

### 6.2 调优要点

1. **块大小选择**：大文件为主 → 4M~8M 块；小文件为主 → 256K~1M 块
2. **网络优化**：使用 RDMA（InfiniBand / RoCE）减少延迟，多网卡绑定提升带宽
3. **存储布局**：跨足够多的磁盘条带化数据，使用多路径（MPIO）提高可用性
4. **内存配置**：增大 pagepool 提升读缓存命中率，增大 maxStatCache 加速 stat 操作

### 6.3 常见性能问题

| 现象 | 可能原因 | 解决方向 |
|------|---------|---------|
| 读吞吐低 | pagepool 太小 | 增大 pagepool |
| 写吞吐低 | 磁盘数不足 | 增加 NSD 数量 |
| 元数据操作慢 | maxStatCache 太小 | 增大 stat cache |
| 延迟波动大 | 网络拥塞 | 检查网络队列，启用 QoS |
| 小文件性能差 | 块大小不合理 | 缩小块大小或使用子块 |

---

## 7. 与竞品对比

### 7.1 GPFS vs Lustre

| 对比维度 | GPFS | Lustre |
|----------|------|--------|
| 元数据架构 | 分布式（无专用 MDS） | 主从式（专用 MDS） |
| 一致性模型 | 强一致性（POSIX） | 接近 POSIX |
| 小文件性能 | 较强 | 较弱（MDS 瓶颈） |
| 快照 | 原生支持 | 需结合 LVM 等 |
| 配额管理 | 原生支持 | 较弱 |
| 加密 | 内置文件级加密 | 需外部方案 |
| 部署复杂度 | 中等 | 较高 |
| 社区版 | 无免费社区版 | 开源社区版可用 |
| 许可 | 商业许可 | GPL（社区版）/ 商业（企业版） |

### 7.2 GPFS vs CephFS

| 对比维度 | GPFS | CephFS |
|----------|------|--------|
| 设计目标 | HPC 并行文件系统 | 软件定义存储统一平台 |
| POSIX 兼容性 | 完整 POSIX | 接近 POSIX |
| 延迟 | 低（裸金属优化） | 稍高（CRUSH 算法开销） |
| 规模上限 | 数千节点 | 数万节点 |
| 协议支持 | NFS/SMB/Object | S3/Swift/NFS/CephFS |
| 成熟度 | 30 年历史，非常成熟 | 较新，仍在快速迭代 |
| 运维工具 | 丰富 CLI/GUI | Cephadm 等 |

### 7.3 GPFS vs BeeGFS

| 对比维度 | GPFS | BeeGFS |
|----------|------|--------|
| 元数据服务 | 分布式 | 独立元数据服务，可扩展 |
| 安装难度 | 中等 | 容易 |
| 企业支持 | IBM 官方支持 | ThinkParQ 支持 |
| 社区生态 | 相对封闭 | 开源，社区活跃 |
| 场景侧重 | 超算、企业关键业务 | 中小型 HPC、教育科研 |

---

## 8. 常用命令速查

### 8.1 集群管理

```bash
mmstartup -a          # 启动集群所有节点
mmstartup node1       # 启动指定节点
mmgetstate -a         # 查看所有节点状态
mmgetstate -N node1   # 查看指定节点状态
```

### 8.2 磁盘 & NSD 管理

```bash
mmlsnsd -m            # 查看所有 NSD 及其映射
mmcrnsd -F /path/to/disk_desc_file    # 创建 NSD
mmdelnsd -F /path/to/nsd_file         # 删除 NSD
```

### 8.3 文件系统管理

```bash
mmcrfs gpfs0 -F /path/to/nsd_file -A yes -Q yes   # 创建文件系统
mmmount gpfs0 -a                        # 挂载所有文件系统
mmumount gpfs0 -a                       # 卸载所有文件系统
mmdf gpfs0                              # 查看文件系统使用情况
mmlsfs gpfs0 -a                         # 查看文件系统所有属性
```

### 8.4 配额管理

```bash
mmrepquota gpfs0 -u                     # 查看所有用户配额
mmrepquota gpfs0 -g                     # 查看所有用户组配额
mmsetquota -u username --block 1G       # 设置用户空间软限制
mmedquota gpfs0 -u username             # 编辑用户配额
```

### 8.5 快照管理

```bash
mmcrsnapshot gpfs0 snapshot_name        # 创建快照
mmlssnapshot gpfs0                      # 列出所有快照
mmdelsnapshot gpfs0 snapshot_name       # 删除快照
```

### 8.6 策略管理

```bash
mmchpolicy gpfs0 /path/to/policy_file   # 应用策略文件
mmlspolicy gpfs0                        # 查看文件系统策略
mmapplypolicy gpfs0 -f /path/to/policy_file  # 手动执行策略
```

### 8.7 诊断 & 日志

```bash
mmdiag                                  # 收集诊断信息
mmfsadm test health                      # 健康检查
mmtrace -T all -f /tmp/trace.out        # 跟踪所有事件
```

### 8.8 高级命令

```bash
mmhealth                               # 健康检查
mmperfmon                              # 性能监控
mmlslicense                            # 查看许可证
mmlscluster                            # 查看集群配置
mmchconfig pagePool=4G                # 修改 pagepool 大小
mmchconfig workerThreads=32            # 修改 worker 线程数
mmchconfig numaMemoryPerf=yes          # 开启 NUMA 优化
```

---

## 9. 参考资源

### 官方文档

- [IBM Storage Scale 产品主页](https://www.ibm.com/products/storage-scale)
- [IBM Storage Scale 文档中心](https://www.ibm.com/docs/en/storage-scale)

### 社区与支持

- [IBM Storage Scale 社区](https://community.ibm.com/community/user/storage/blogs/storage-scale)
- [IBM Storage Scale 论坛](https://www.ibm.com/mysupport/s/forums)

### 推荐阅读

- *IBM Spectrum Scale (GPFS) 管理与优化指南*
- *GPFS 部署与运维最佳实践*
- *IBM Storage Scale Performance Tuning Guide*
- *GPFS 在 TOP500 超算中的应用案例*

---

> **版本说明**：本文档基于 IBM Storage Scale 5.1.x 版本编写，部分内容可能因版本不同而有所差异，请以官方文档为准。
> **最后更新**：2026-07-20
