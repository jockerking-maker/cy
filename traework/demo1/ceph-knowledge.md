# Ceph 分布式存储知识体系

> Ceph 是一个开源分布式存储系统，设计目标是通过统一架构同时支持**块存储（RBD）**、**对象存储（RGW）**和**文件系统（CephFS）**服务。自 2004 年由 Sage Weil 在其博士研究中提出，2006 年开源以来，凭借无中心节点设计、高可扩展性和自愈能力，成为云计算和大数据场景中的主流存储方案。

---

## 目录

1. [Ceph 整体架构](#1-ceph-整体架构)
2. [核心组件](#2-核心组件)
3. [CRUSH 数据分布算法](#3-crush-数据分布算法)
4. [存储类型](#4-存储类型)
5. [Placement Group（PG）原理](#5-placement-grouppg原理)
6. [数据读写流程](#6-数据读写流程)
7. [网络通信（Messenger 层）](#7-网络通信messenger-层)
8. [运维工具与管理命令](#8-运维工具与管理命令)
9. [版本演进](#9-版本演进)
10. [部署方式](#10-部署方式)
11. [性能优化](#11-性能优化)
12. [故障处理与数据恢复](#12-故障处理与数据恢复)

---

## 1. Ceph 整体架构

### 1.1 架构分层

Ceph 的架构从底层到上层分为四个层次：

| 层级 | 名称 | 说明 |
|------|------|------|
| **应用层** | RBD / CephFS / RGW | 块设备、文件系统、对象存储三种接口 |
| **协议层** | librados | 编程语言库（C++/Python/Java），封装对 RADOS 的直接操作 |
| **核心层** | RADOS | Reliable Autonomic Distributed Object Store，可靠自主分布式对象存储，Ceph 的核心引擎 |
| **物理层** | MON / OSD / MGR | 集群的监控、存储、管理守护进程 |

### 1.2 RADOS — 核心引擎

RADOS 是 Ceph 最核心的组件，由 MON 和 OSD 两大角色构成，提供基于对象（Object）的可靠存储，负责数据的复制、恢复、再均衡等功能。所有上层三种存储类型最终都通过 librados 访问 RADOS。

### 1.3 librados

librados 是 RADOS 的访问协议层实现，提供对 MON 和 OSD 直接操作的编程接口。Ceph 部署 RADOS 时默认安装 C++ 版的 librados，也可独立安装 Python 版或 Java 版。上层的 RBD、CephFS、RGW 都基于 librados 构建。

### 1.4 存储池（Pool）与对象（Object）

- **Object（对象）**：数据存储的最小逻辑单元，每个 Object 默认最大 2MB~4MB，包含元数据头部和数据体。
- **PG（Placement Group）**：Object 的逻辑分组，是 CRUSH 算法映射的核心单元。
- **Pool（存储池）**：定义数据冗余策略（副本数/纠删码）、PG 数量及 QoS 限制，是用户操作的基本单元。

---

## 2. 核心组件

### 2.1 Ceph Monitor（MON）— 集群的"大脑中枢"

**核心职责**：维护集群状态映射表（OSD Map、PG Map、Monitor Map、CRUSH Map），确保所有节点同步集群拓扑和健康状态。

**工作机制**：
- 通过 **Paxos 算法**实现多节点选举，保证主 Monitor 故障时快速切换。
- 生产环境建议部署 **3 或 5 个（奇数个）**MON 节点，分散在不同物理机。
- 管理 CRUSH 算法规则，定义数据在 OSD 上的分布逻辑（机架感知、故障域隔离）。

**Map 类型**：

| Map 类型 | 说明 |
|----------|------|
| **Monitor Map** | monitor 节点信息，包括集群 ID、quorum 状态 |
| **OSD Map** | OSD 状态（up/down/in/out）、权重、容量等信息 |
| **PG Map** | PG 版本、时间戳、各 OSD 的 PG 统计 |
| **MDS Map** | MDS 元数据服务器状态 |
| **CRUSH Map** | 集群拓扑结构和故障域定义 |

### 2.2 Ceph OSD（Object Storage Device）— 数据存储的"物理载体"

**核心职责**：管理物理磁盘，执行数据的存储、复制、恢复及副本一致性维护。

**技术细节**：
- 每个 OSD 守护进程对应一块物理磁盘（或一个分区）。
- 数据以 Object 格式存储，默认 Object 大小 2MB~4MB。
- 基于 PG 实现数据分片，通过**多副本（默认 3 副本）**或**纠删码（EC）**保证可靠性。
- 支持两种存储后端：
  - **FileStore**（传统）：基于 XFS 文件系统，已逐步弃用。
  - **BlueStore**（默认）：直接管理裸盘，绕过中间文件系统层，性能更高，支持 WAL + RocksDB 元数据管理。

**自愈能力**：OSD 故障时，剩余 OSD 自动检测数据缺失，触发数据重建，确保副本数达标。

### 2.3 Ceph MGR（Manager）— 集群的"智能运维中枢"

**核心职责**：辅助 MON 实现集群管理，提供监控、自动化运维及生态集成能力。

**功能扩展**：
- 内置 **Prometheus exporter** 和 **Grafana** 插件，实时采集 OSD 负载、存储利用率。
- 通过 **Ceph Dashboard** 提供可视化管理界面。
- 支持 **Ansible** 集成，实现 OSD 自动部署与扩容。
- 新版本增加了 AI 驱动的故障预测功能，通过历史数据预判 OSD 硬件故障风险。

### 2.4 Ceph MDS（Metadata Server）— 文件系统的"元数据管家"

**核心职责**：专门处理 CephFS 的元数据操作（文件目录创建、权限管理、访问控制等）。

**技术特点**：
- **非 RADOS 核心必需组件**：MDS 仅在启用 CephFS 时才需要；没有 MDS 时集群仍可 HEALTH_OK。
- 缓存常用元数据（目录树、文件属性），减少对 OSD 的访问压力。
- 支持**多 MDS 节点的 Active-Standby 模式**，避免单点故障。
- **瓶颈场景**：在海量小文件的元数据操作场景中，MDS 性能直接影响大数据框架的执行效率。

---

## 3. CRUSH 数据分布算法

### 3.1 什么是 CRUSH

CRUSH（Controlled Replication Under Scalable Hashing）是一种基于哈希的数据分布算法。它以数据唯一标识符、当前集群拓扑结构及数据备份策略作为输入，通过纯计算获取数据所在的底层存储设备位置并直接通信，无需查表，从而实现去中心化和高度并发。

### 3.2 两级映射

数据从文件到最终存储设备经历两级映射：

```
File → (分片) → Object[] → (hash) → PG → (CRUSH) → OSD[]
```

1. **第一级：Object → PG 映射**：基于 Object 的 oid 进行 hash 计算，将 Object 均匀映射到 PG 上。
2. **第二级：PG → OSD 映射（CRUSH）**：根据 PGID、集群拓扑和 placement rule，通过 CRUSH 算法计算出 PG 应该分布在哪一组 OSD 上。

### 3.3 CRUSH 的三输入参数

| 输入 | 说明 |
|------|------|
| **对象/PG 标识符** | 输入 x 和随机因子 r |
| **Cluster Map** | 集群层级拓扑结构（root → region → datacenter → rack → host → osd） |
| **Placement Rule** | 定义副本选择策略，如 take → choose/chooseleaf → emit |

### 3.4 CRUSH 层级拓扑（Cluster Map）

Cluster Map 中，叶子节点（device）是物理磁盘，所有中间节点称为 bucket，根节点称为 root。常见层级类型（按规模由小到大）：

```
osd(0) → host(1) → chassis(2) → rack(3) → row(4) → pdu(5) →
pod(6) → room(7) → datacenter(8) → zone(9) → region(10) → root(11)
```

每个节点有唯一数字 ID，叶子节点 ID 为非负数，非叶子节点 ID 为负数。

### 3.5 Placement Rule 操作

一条 placement rule 包含三种操作：

- **take**：从 Cluster Map 选择指定 bucket（如 root default）作为输入。
- **choose/chooseleaf**：
  - `choose firstn N type <bucket-type>`：从 bucket 选出 N 个指定类型的子 bucket（深度优先）。
  - `chooseleaf firstn N type <bucket-type>`：选出 N 个子 bucket 后再递归到叶子 OSD。
  - 若 `num == 0`，数量等于 pool 设置的副本数。
- **emit**：输出最终结果。

**firstn vs indep**：当无法选出足够数量时，firstn 返回找到的部分结果，indep 使用占位符保留位置。

### 3.6 straw / straw2 算法

- **straw**：将所有元素比作吸管，针对输入为每个元素随机计算签长，选最长的。添加/删除元素时只涉及迁入和迁出元素的数据迁移。
- **straw2**（当前默认）：仅关注自身权重，不再依赖集合中其他元素权重，进一步减少不必要的数据迁移。

### 3.7 权重与过载测试

- **weight（权重）**：基于容量动态调整，影响 CRUSH 选择概率。
- **reweight（重新权重）**：额外控制因子。当选中一个 OSD 后，还需通过 reweight 过载测试（根据 hash 判断），低于 reweight 阈值才真正选择。reweight 设为 0 可标记 OSD 暂时失效，避免触发大规模数据迁移。

### 3.8 PG 数量计算

官方推荐公式：

```
Total PGs = (OSD总数 × 100) / max_replication_count / pool_count
```

建议取**2 的 N 次方**（最接近的整数）。生产经验范围：

- **最小**：`OSD数量 × 100 / 副本数 / 常用池数`
- **最大**：`OSD数量 × 300 / 副本数 / 常用池数`

---

## 4. 存储类型

### 4.1 RBD（RADOS Block Device）— 块存储

**定义**：基于 RADOS 的块存储设备，将分布式对象存储模拟为块设备供客户端使用。

**使用场景**：
- OpenStack 集成（Glance/Cinder 将 Ceph 视为块存储设备）
- KVM/QEMU 虚拟机的磁盘后端
- Kubernetes 持久化存储（通过 RBD CSI 插件）

**关键技术**：
- 支持**动态扩容**和**快照**
- 支持 **thin-provisioning**（精简配置）
- 客户端缓存（rbd_cache）可显著提升读写性能
- 支持分层快照（layered snapshot）和克隆

### 4.2 CephFS — 文件存储

**定义**：基于 librados 的 POSIX 兼容文件系统。

**关键组件**：需要 **MDS**（元数据服务器）提供目录和文件元数据管理。

**使用方式**：
- Linux 内核驱动挂载（内核态）
- FUSE 挂载（用户态，通过 `ceph-fuse`）

**特性**：
- 支持子卷（subvolume）和子卷组管理
- 多 MDS 支持 Active-Standby 高可用模式
- 适合大数据分析场景（Hadoop/Spark）
- 支持配额管理（目录级别）

### 4.3 RGW（RADOS Gateway）— 对象存储

**定义**：为 RADOS 提供的 HTTP RESTful 对象存储网关，实现为 FastCGI 模块。

**协议兼容**：
- 兼容 **Amazon S3 API**
- 兼容 **OpenStack Swift API**

**使用场景**：通过 HTTP/S3 协议进行文件上传、下载和管理，适用于云原生应用、静态网站托管等。

**架构特点**：客户端通过 HTTP 请求到 RGW，RGW 转换为 RADOS 操作，数据存储在 OSD 上。支持多租户、存储桶策略、生命周期管理等功能。

---

## 5. Placement Group（PG）原理

### 5.1 PG 的定义

PG（Placement Group）是 Ceph 数据分布的核心逻辑单元，是 Object 的逻辑容器和 CRUSH 算法的映射目标。PG 位于 Object 和 OSD 之间，起到**中间映射层**的作用。

### 5.2 PG 的状态

| 状态 | 含义 |
|------|------|
| **active+clean** | 正常状态，PG 可用且数据完整 |
| **active+degraded** | 副本数不足（如某个 OSD 挂了），但数据仍可用 |
| **peering** | PG 正在协商主从关系 |
| **recovering** | PG 正在恢复数据 |
| **backfilling** | PG 正在回填数据（新 OSD 加入时） |
| **remapped** | PG 的 OSD 映射发生变化 |
| **inconsistent** | PG 的副本间数据不一致 |
| **stale** | PG 的 Primary OSD 状态未知 |
| **down** | PG 的副本全部不可用 |
| **scrubbing** | PG 正在进行数据校验 |
| **deep** | PG 正在进行深度 scrub（全量数据比对） |

### 5.3 PG 分裂（Split）

当 pool 的 `pg_num` 增加时，现有的 PG 会分裂成更多 PG。分裂是单向操作（只增不减），因为 PG 已经存储数据。分裂前最好先增加 OSD 数量，否则负载均衡效果有限。

### 5.4 PG 的副本与纠删码

- **副本模式（Replicated）**：默认 3 副本，Primary PG 负责读写协调，数据完全复制到多个 OSD。
- **纠删码模式（Erasure Code，EC）**：如 2+1 模式（2 数据块 + 1 校验块），节省存储空间但性能较低，适合冷数据场景。

### 5.5 PG 数量规划

PG 数量过少会导致单个 PG 管理过多 Object，OSD 间负载不均衡；PG 数量过多会占用大量内存。生产中推荐使用 `ceph osd pool autoscale-status` 监控各 pool 的 PG 利用率，并根据实际负载自动调整。

---

## 6. 数据读写流程

### 6.1 数据读取流程

```
Client → Monitor（获取最新的 Cluster Map，缓存到本地）
       → 通过 CRUSH 计算 Object → PG → Primary OSD
       → 向 Primary OSD 发送读请求
       → Primary OSD 从本地存储（BlueStore）读取数据
       → 返回数据给 Client
```

**关键点**：
- 客户端首次从 MON 获取 Cluster Map 后缓存，后续不再需要 MON 参与。
- 读操作默认从 **Primary OSD** 读取（可通过配置允许多副本读取）。
- RBD 开启缓存（`rbd_cache=true`）时，读操作优先从本地缓存获取。

### 6.2 数据写入流程

```
Client → 计算目标 PG 和 Primary OSD
       → 向 Primary OSD 发送写请求
       → Primary OSD 接收数据：
          写入 WAL（Write-Ahead Log）
          写入 RocksDB 元数据
          写入数据区
       → Primary OSD 并行将数据转发给所有 Secondary/Replica OSD
       → 各副本 OSD 完成写入并返回 ACK
       → Primary OSD 确认所有副本写入成功后，向 Client 返回确认
```

**写流程关键特点**：
- **Primary OSD 负责协调**：所有的写副本同步由 Primary OSD 发起。
- **写顺序保证**：Primary OSD 保证所有副本的写入顺序一致。
- **三种副本写入模式**：
  - `write`（全写）：所有副本都写入后才返回 ACK。
  - `write_ahead`（预写）：先写日志再写数据。
  - `writeback`（回写）：先返回 ACK 再异步写（需要缓存支持）。

### 6.3 BlueStore 写入流程

在 BlueStore 存储引擎下：
- **小块 IO（< min_alloc_size）**：走 `_do_write_small` 流程，涉及读-改-写操作，性能较低。
- **大块 IO（>= min_alloc_size）**：走 `_do_write_big` 流程，直接分配新区块并写入，性能较高。
- BlueStore 使用 RocksDB 管理元数据，数据直接写入裸设备。

---

## 7. 网络通信（Messenger 层）

### 7.1 Messenger 架构

Ceph 的网络通信层位于 `src/msg/` 目录，由以下核心类组成：

| 组件 | 说明 |
|------|------|
| **Messenger** | 消息调度框架，负责消息的发送和接收 |
| **Connection** | 表示一个网络连接 |
| **Message** | 通信的基本数据单元 |
| **Dispatcher** | 消息分发器，将收到的消息派发给对应的上层模块 |

### 7.2 三种网络通信模型

| 模型 | 说明 | 状态 |
|------|------|------|
| **simple** | 简单线程模型，每个连接使用独立线程 | 已基本废弃 |
| **async** | 基于事件驱动（EventCenter）的异步通信模型 | **当前默认** |
| **xio** | 基于 Accelio/RDMA 的高性能通信 | 特定场景使用 |

### 7.3 AsyncMessenger（默认模型）

- 采用**异步事件驱动**模型，每个 worker 线程管理一个 EventCenter。
- EventCenter 处理不同类型的事件（读/写/定时器）。
- 高并发、低延迟，是当前 Ceph 的默认网络通信层。
- 支持 MSGR2 协议（从 Nautilus 开始引入，Quincy 起默认启用），提供加密传输和更好的连接管理。

### 7.4 网络拓扑建议

| 网络 | 作用 | 建议 |
|------|------|------|
| **public_network** | 客户端访问网络 | 千兆及以上 |
| **cluster_network** | OSD 间副本复制/数据均衡/心跳 | 万兆及以上（独立于 public 网络） |

分离 public 和 cluster 网络可显著减轻客户端通信与 OSD 内部复制之间的带宽争抢。

---

## 8. 运维工具与管理命令

### 8.1 集群信息查看

| 命令 | 说明 |
|------|------|
| `ceph -s` 或 `ceph status` | 查看集群整体状态 |
| `ceph -w` | 实时监控集群状态变化 |
| `ceph health` | 查看集群健康状态 |
| `ceph version` | 查看 Ceph 版本 |
| `ceph df` | 查看集群存储空间使用情况 |
| `ceph osd perf` | 查看 OSD 性能指标 |
| `ceph device health` | 查看磁盘健康状态 |
| `ceph config dump` | 查看所有配置参数 |

### 8.2 OSD 管理

| 命令 | 说明 |
|------|------|
| `ceph osd tree` | 查看 OSD 层级拓扑 |
| `ceph osd stat` | 查看 OSD 统计信息 |
| `ceph osd df` | 查看 OSD 使用量分布 |
| `ceph osd out <id>` | 将 OSD 标记为 out（停止服务，触发数据迁移） |
| `ceph osd in <id>` | 将 OSD 重新加入集群 |
| `ceph osd down <id>` | 标记 OSD 为 down |
| `ceph osd reweight <id> <weight>` | 调整 OSD 权重（临时均衡） |
| `ceph osd crush reweight <id> <weight>` | 调整 CRUSH 权重（永久） |
| `ceph osd rm <id>` | 从集群中移除 OSD |
| `ceph osd destroy <id>` | 销毁 OSD（保留 ID 但清空数据） |

### 8.3 MON 管理

| 命令 | 说明 |
|------|------|
| `ceph mon stat` | 查看 MON 状态 |
| `ceph mon add <name> <ip>` | 添加 MON |
| `ceph mon remove <name>` | 移除 MON |
| `ceph quorum_status` | 查看 MON 选举状态 |

### 8.4 PG 管理

| 命令 | 说明 |
|------|------|
| `ceph pg stat` | 查看 PG 统计 |
| `ceph pg dump` | 导出所有 PG 信息 |
| `ceph pg <pgid> query` | 查看单个 PG 的详细信息 |
| `ceph pg map <pool> <object>` | 查看 Object 所在的 PG 和 OSD |
| `ceph pg repair <pgid>` | 修复 PG 中的不一致问题 |
| `ceph pg deep-scrub <pgid>` | 对 PG 执行深度数据校验 |

### 8.5 Pool 管理

| 命令 | 说明 |
|------|------|
| `ceph osd pool ls` | 列出所有存储池 |
| `ceph osd pool create <name> <pg_num>` | 创建存储池 |
| `ceph osd pool set <name> <key> <value>` | 修改存储池参数 |
| `ceph osd pool delete <name>` | 删除存储池 |
| `ceph osd pool autoscale-status` | 查看 PG 自动缩放状态 |

### 8.6 CRUSH 管理

| 命令 | 说明 |
|------|------|
| `ceph osd crush dump` | 导出 CRUSH Map 的 JSON 格式 |
| `ceph osd getcrushmap -o <file>` | 导出编译版 CRUSH Map |
| `ceph osd setcrushmap -i <file>` | 注入 CRUSH Map |
| `crushtool -d <in> -o <out>` | 反编译 CRUSH Map |
| `crushtool -c <in> -o <out>` | 编译 CRUSH Map |
| `ceph osd crush add <id> <weight> <location>` | 添加 OSD 到 CRUSH Map |
| `ceph osd crush rule dump` | 查看 CRUSH 规则 |

### 8.7 认证管理

| 命令 | 说明 |
|------|------|
| `ceph auth list` | 列出所有认证用户 |
| `ceph auth get-or-create <entity>` | 获取或创建认证 key |
| `ceph auth caps <entity> <caps>` | 修改用户权限 |
| `ceph auth del <entity>` | 删除用户 |

### 8.8 常用 rados 命令

| 命令 | 说明 |
|------|------|
| `rados -p <pool> ls` | 列出 pool 中的所有对象 |
| `rados put <obj> <file> --pool=<pool>` | 上传对象到 pool |
| `rados get <obj> <file> --pool=<pool>` | 下载对象 |
| `rados rm <obj> --pool=<pool>` | 删除对象 |
| `rados df` | 查看存储池的空间使用 |
| `rados bench -p <pool> <seconds> write --no-cleanup` | 基准测试（写） |
| `rados bench -p <pool> <seconds> seq` | 基准测试（顺序读） |

---

## 9. 版本演进

### 9.1 版本命名规则

Ceph 版本格式为 `x.y.z`：

| 版本类型 | 格式 | 说明 |
|----------|------|------|
| 开发版 | x.0.z | 早期测试者使用 |
| 候选版 | x.1.z | 测试集群使用 |
| 稳定版 | x.2.z | **生产环境推荐** |

版本代号按字母顺序递增（A-Z）。

### 9.2 主要历史版本

| 代号 | 版本 | 时间 | 主要特性 |
|------|------|------|----------|
| **Argonaut** | 0.48 | 2012.06 | 首个 LTS 版本 |
| **Bobtail** | 0.56 | 2013.01 | LTS |
| **Firefly** | 0.80 | 2014.05 | LTS |
| **Hammer** | 0.94 | 2015.04 | LTS |
| **Infernalis** | 9.2 | 2015.11 | 引入新版本号方案 |
| **Jewel** | 10.2 | 2016.04 | LTS，大规模改进 |
| **Kraken** | 11.2 | 2017.01 | 引入 BlueStore |
| **Luminous** | 12.2 | 2017.10 | **BlueStore 稳定并成为默认**，MGR 引入 |
| **Mimic** | 13.2 | 2018.05 | CephFS 改进，多 MDS 稳定 |
| **Nautilus** | 14.2 | 2019.03 | CephFS 里程碑改进，MGR 仪表盘增强 |
| **Octopus** | 15.2 | 2020.03 | 大规模管理功能，**cephadm 引入** |
| **Pacific** | 16.2 | 2021.04 | 性能优化，EC 改进，MGR AI 预测 |
| **Quincy** | 17.2 | 2022.04 | 大规模集群优化，**MSGR2 默认** |
| **Reef** | 18.2 | 2023.08 | cephadm 伸缩性演进，性能大幅提升 |
| **Squid** | 19.2 | 2024~2025 | 最新版本，进一步优化管理和性能 |

### 9.3 Squid（19.2.x）版本特性

- 进一步优化 cephadm 管理能力，减少 SSH 依赖。
- 快照功能升级：新增对厚置备 LVM 共享存储的虚拟机快照支持。
- 采用快照-卷链模型（子卷记录与父快照的差异）。
- 更好的 NFS 集群支持。
- 与 Kubernetes CSI 的更深度集成。
- 已集成在 Proxmox VE 9.0，作为默认分布式存储选项。

### 9.4 Reef（18.2.x）版本特性

- cephadm 引入 agent 模式，减少 SSH 轮询，提升大规模集群管理效率。
- 在高性能硬件（NVMe SSD）上可实现 **800K IOPS（随机写入）**和 **4.4M IOPS（随机读取）**。
- MSGR2（新版网络协议）默认启用。
- 性能增强：约 71GB/s 读取和 25GB/s 写入吞吐。

---

## 10. 部署方式

### 10.1 cephadm（官方推荐）

从 Octopus（15.2.0）开始推荐使用，基于容器化部署。

| 步骤 | 命令 | 说明 |
|------|------|------|
| Bootstrap | `cephadm bootstrap --mon-ip <ip>` | 单节点初始化集群 |
| 添加主机 | `ceph orch host add <hostname>` | 将节点加入集群 |
| 部署 OSD | `ceph orch apply osd --all-available-devices` | 自动发现并部署 OSD |
| 部署 MGR | `ceph orch apply mgr <count>` | 部署 MGR 副本 |
| 启用 Dashboard | `ceph mgr module enable dashboard` | 启用 Web 管理界面 |

**特点**：
- 所有组件作为容器运行（Podman/Docker）
- 使用 `ceph orch` 命令统一管理服务
- 自动收集日志和监控数据
- 当前最新稳定部署方式

### 10.2 Rook（Kubernetes 原生）

基于 Kubernetes 的 Operator 模式，将 Ceph 作为云原生存储运行。

| 组件 | 说明 |
|------|------|
| **Operator** | 自动管理 Ceph 组件的 Pod 生命周期 |
| **CRD** | 通过 `CephCluster`、`CephBlockPool` 等 CRD 定义资源 |
| **CSI 驱动** | 提供 RBD CSI 和 CephFS CSI 驱动 |
| **Dashboard** | 自动暴露 Ceph Dashboard |

**适用场景**：已经使用 Kubernetes 的用户。版本升级需谨慎，注意版本兼容性。

### 10.3 其他部署方式

| 方式 | 说明 | 状态 |
|------|------|------|
| **ceph-deploy** | 旧的部署工具 | 已废弃 |
| **Ansible（ceph-ansible）** | 自动化部署 | 仍可用但非主流 |
| **手动部署** | 逐台安装配置 | 适合学习理解架构 |
| **Proxmox VE 集成** | PVE 内置 Ceph Squid 19.2 | 虚拟化场景推荐 |

### 10.4 生产环境最小部署建议

| 组件 | 测试环境 | 生产环境 |
|------|----------|----------|
| MON | 1 个 | 3 个（奇数，不同物理机） |
| OSD | 3 个 | 根据容量需求，每节点建议 2×CPU(64+ 核)、256GB 内存、20×HDD(10TB+) + 2×SSD(缓存) |
| MGR | 1 个 | 2 个 |
| MDS | 按需 | Active-Standby 模式 |
| 网络 | 千兆 | public 千兆 + cluster 万兆分离 |

---

## 11. 性能优化

### 11.1 网络优化

| 参数 | 说明 | 建议值 |
|------|------|--------|
| `public_network` | 客户端访问网络 | 千兆及以上 |
| `cluster_network` | OSD 间复制网络 | 万兆及以上（独立网段） |
| `ms_bind_before_connect` | 多网口流量均衡 | true |

### 11.2 OSD 优化

| 类别 | 参数 | 默认值 | 建议值 | 说明 |
|------|------|--------|--------|------|
| 存储后端 | `osd_objectstore` | bluestore | bluestore | 默认引擎，管理裸盘 |
| 内存 | `osd_memory_target` | 4GB | 4GB+ | 根据主机内存调整 |
| 分片 | `osd_op_num_shards_hdd` | 5 | 5 | HDD 场景 |
| 分片 | `osd_op_num_shards_ssd` | 8 | 8 | SSD 场景 |
| 写入 | `osd_max_write_size` | 90 MiB | 512 MiB | 提高大块写吞吐量 |
| Map 缓存 | `osd_map_cache_size` | 50 | 1024 | 减少频繁读 Map |

### 11.3 恢复与心跳优化

| 参数 | 默认值 | 建议值 | 说明 |
|------|--------|--------|------|
| `osd_recovery_op_priority` | 3 | 2 | 降低恢复优先级，避免影响业务 IO |
| `osd_recovery_max_active` | 3 | 10 | 加速恢复 |
| `osd_max_backfills` | 1 | 4 | 加速回填 |
| `mon_osd_down_out_interval` | 600 | 864000 | 避免网络抖动触发大规模重建 |
| `osd_heartbeat_grace` | 20 | 60 | 心跳优雅期，降低误判 |

### 11.4 RBD 客户端缓存优化

| 参数 | 默认值 | 建议值 | 说明 |
|------|--------|--------|------|
| `rbd_cache` | true | true | 启用客户端缓存 |
| `rbd_cache_size` | 32 MiB | 128~1024 MiB | 小文件场景偏大，大文件场景偏小 |
| `rbd_cache_max_dirty` | 24 MiB | 128 MiB | 写回模式脏数据上限 |
| `rbd_cache_max_dirty_age` | 1s | 30s | 减少频繁刷盘 |

### 11.5 CRUSH/MAP 优化

| 参数 | 说明 | 建议 |
|------|------|------|
| `osd_crush_chooseleaf_type` | 副本故障域层级 | 0（按 OSD）/ 1（按主机）/ 3（按机架） |
| `osd_pool_default_size` | 默认副本数 | 3 |
| `osd_pool_default_min_size` | 最小副本数 | 2（容忍 1 个副本故障） |

### 11.6 其他优化建议

- PG 数量合理规划（参考第 3.8 节的公式），使用 `ceph osd pool autoscale-status` 监控。
- 调优完成后关闭 `throttler_perf_counter` 以减少性能损耗。
- 使用 NVMe SSD 作为 OSD 数据盘或日志盘，配合 BlueStore 可显著提升 IOPS。
- HDD 和 SSD 使用不同的分片参数。
- 启用 `osd_op_queue` 的 mclock_scheduler 进行 QoS 控制。

---

## 12. 故障处理与数据恢复

### 12.1 常见故障类型

| 故障类型 | 表现 | 可能原因 |
|----------|------|----------|
| OSD down | `ceph osd tree` 显示 down | 磁盘故障、网络中断、进程崩溃 |
| OSD full | 集群停止写入 | 磁盘使用率超过 full 阈值 |
| PG inconsistent | PG 状态报不一致 | 数据损坏、意外宕机 |
| PG peering 卡住 | PG 长时间处于 peering | 网络问题、MON 故障 |
| MON 失联 | quorum 不完整 | 网络隔离、节点宕机 |

### 12.2 OSD 故障处理

**场景一：OSD 进程崩溃但磁盘完好**

```bash
# 1. 检查状态
ceph osd tree
ceph osd find <id>
ceph -s

# 2. 尝试重启
systemctl restart ceph-osd@<id>

# 3. 检查日志
journalctl -u ceph-osd@<id> -n 100
```

**场景二：OSD 磁盘损坏（需更换）**

```bash
# 1. 标记 OSD out（停止服务，触发数据重建）
ceph osd out <id>

# 2. 等待数据重建完成（pg 恢复至 active+clean）
watch ceph -s

# 3. 停止并移除 OSD
systemctl stop ceph-osd@<id>
ceph osd crush remove osd.<id>
ceph osd rm <id>
ceph auth del osd.<id>

# 4. 更换磁盘后重新添加（cephadm 环境自动发现）
# 手动添加方式：
ceph-volume lvm zap /dev/<new_disk>
ceph-volume lvm create --data /dev/<new_disk>
```

**场景三：OSD 临时故障（网络抖动/误判）**

```bash
# 不触发数据迁移 — 不执行 osd out
# 只需重启 OSD 即可恢复
systemctl restart ceph-osd@<id>

# 若因 mon_osd_down_out_interval 过短被标记 out，手动 in
ceph osd in <id>
```

### 12.3 PG 不一致修复

```bash
# 1. 定位不一致的 PG
ceph health detail

# 2. 查询 PG 详细信息
ceph pg <pgid> query

# 3. 尝试修复
ceph pg repair <pgid>

# 4. 查看 OSD 日志确认修复进展
journalctl -u ceph-osd@<primary-osd-id> | grep <pgid>
```

### 12.4 OSD 间数据迁移（objectstore 工具）

当需要从损坏的 OSD 恢复数据到其他 OSD 时：

```bash
# 1. 停止目标 OSD
systemctl stop ceph-osd@<good_osd_id>
systemctl stop ceph-osd@<bad_osd_id>

# 2. 导出数据
ceph-objectstore-tool --data-path /var/lib/ceph/osd/ceph-<bad> \
  --op export --file /tmp/osd_export

# 3. 导入到好的 OSD
ceph-objectstore-tool --data-path /var/lib/ceph/osd/ceph-<good> \
  --op import --file /tmp/osd_export

# 4. 重启 OSD
systemctl start ceph-osd@<good_osd_id>
```

### 12.5 MON 故障恢复

```bash
# 检查 quorum
ceph quorum_status

# 重建 MON（cephadm 环境）
ceph orch ps --daemon-type mon
ceph orch daemon add mon <new-host>:<ip>

# 传统环境
ceph mon remove <bad-mon>
ceph mon add <new-mon> <ip:port>

# 所有 MON 均故障（灾难恢复）
# 需手动选举恢复，使用 monmaptool 重建 monmap
```

### 12.6 OSD Full 紧急处理

```bash
# 1. 查看当前阈值
ceph osd dump | grep full_ratio

# 2. 临时调整 full 比例阈值（紧急释放写入能力）
ceph osd set-full-ratio 0.95

# 3. 临时调整 reweight（紧急释放空间）
ceph osd reweight <full-osd-id> 0.8

# 4. 根本方案：扩容或清理旧数据
```

### 12.7 SCRUB 与数据一致性

| 类型 | 命令 | 说明 |
|------|------|------|
| Light Scrub | 自动执行（每日） | 校验 PG 的元数据一致性 |
| Deep Scrub | `ceph pg deep-scrub <pgid>` | 全量数据比对，校验数据完整性 |
| 全集群 Deep Scrub | `ceph osd pool deep-scrub <pool>` | 对指定 pool 的所有 PG 执行深度校验 |

### 12.8 数据恢复核心原则

1. **不要贸然 osd out**：如果只是临时故障，等待 OSD 自动恢复（使用较大的 `mon_osd_down_out_interval` 值）。
2. **先定位后操作**：用 `ceph health detail` 和日志定位问题再动手。
3. **控制恢复速率**：设置 `osd_recovery_max_active`、`osd_max_backfills` 等参数，避免恢复流量压垮集群。
4. **定期 Deep Scrub**：定期执行深度 scrub 检查数据完整性。
5. **仔细验证 pool 删除**：Ceph 默认禁止删除 pool，需额外配置 `mon_allow_pool_delete=true`，确认无误后再执行。

---

## 附录：常用配置参数速查

### A. 全局参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `osd_pool_default_size` | 3 | 默认副本数 |
| `osd_pool_default_min_size` | 2 | 最小副本数（允许降级读写） |
| `osd_pool_default_pg_num` | 8 | 默认 PG 数量 |
| `osd_pool_default_pgp_num` | 8 | 默认 PG 放置数量 |
| `osd_crush_chooseleaf_type` | 1 | 故障域类型（1=主机级） |

### B. BlueStore 参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `bluestore_block_size` | 自动 | 数据块设备大小 |
| `bluestore_block_db_size` | 自动 | RocksDB/WAL 块设备大小 |
| `bluestore_cache_size_hdd` | 1GB | HDD 场景 BlueStore 缓存大小 |
| `bluestore_cache_size_ssd` | 3GB | SSD 场景 BlueStore 缓存大小 |
| `bluestore_min_alloc_size_hdd` | 64KB | HDD 最小分配单元 |
| `bluestore_min_alloc_size_ssd` | 16KB | SSD 最小分配单元 |

### C. 日志与调试参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `debug_ms` | 0/5 | 消息层调试级别 |
| `debug_osd` | 0/5 | OSD 调试级别 |
| `debug_mon` | 0/5 | MON 调试级别 |
| `debug_bluestore` | 0/5 | BlueStore 调试级别 |
| `debug_rocksdb` | 0/5 | RocksDB 调试级别 |
