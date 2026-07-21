# Ceph 分布式存储系统知识详解

> 最后更新：2026-07-20

---

## 目录

1. [Ceph 概述](#1-ceph-概述)
2. [核心架构](#2-核心架构)
3. [核心组件详解](#3-核心组件详解)
4. [数据存储逻辑分层](#4-数据存储逻辑分层)
5. [CRUSH 算法](#5-crush-算法)
6. [三种存储接口](#6-三种存储接口)
7. [数据恢复与一致性机制](#7-数据恢复与一致性机制)
8. [部署与运维](#8-部署与运维)
9. [性能调优](#9-性能调优)
10. [故障排查](#10-故障排查)
11. [云原生集成](#11-云原生集成)
12. [最佳实践](#12-最佳实践)
13. [常见面试题](#13-常见面试题)

---

## 1. Ceph 概述

### 1.1 什么是 Ceph

Ceph 是一个开源的分布式存储系统，设计目标是通过统一架构同时支持**块存储**、**对象存储**和**文件系统**三种存储服务。自 2006 年由 Sage Weil 在加州大学圣克鲁兹分校的博士研究中诞生以来，Ceph 凭借无中心节点设计、高可扩展性和自愈能力，成为云计算和大数据场景中主流的软件定义存储（SDS）方案。

### 1.2 核心特性

| 特性 | 说明 |
|------|------|
| **统一存储** | 一套集群同时提供块存储（RBD）、文件存储（CephFS）、对象存储（RGW） |
| **无中心架构** | 无单点故障，所有节点对等，通过 CRUSH 算法实现去中心化数据分布 |
| **高可扩展性** | 支持从几个节点扩展到数千个节点，容量可达 EB 级别 |
| **自愈能力** | 自动检测故障并触发数据恢复，保证数据副本数达标 |
| **数据可靠性** | 支持多副本（Replication）和纠删码（Erasure Coding）两种冗余策略 |
| **硬件无关** | 可运行在通用 x86 服务器上，无需专用存储硬件 |

### 1.3 与传统存储的对比

| 维度 | Ceph | 传统 SAN/NAS |
|------|------|--------------|
| 架构 | 软件定义，分布式 | 硬件绑定，集中式 |
| 扩展性 | 水平扩展，在线扩容 | 垂直扩展，受限于控制器 |
| 成本 | 通用硬件，TCO 低 | 专用硬件，成本高 |
| 灵活性 | 统一存储，多种接口 | 通常单一接口 |
| 运维 | 自动化程度高 | 人工干预多 |

---

## 2. 核心架构

### 2.1 整体架构图

```
┌────────────────────────────────────────────────┐
│                  应用层（APP）                    │
├──────────────┬──────────────┬───────────────────┤
│   RBD        │   CephFS     │      RGW          │
│  (块设备)     │  (文件系统)   │   (对象网关)        │
├──────────────┴──────────────┴───────────────────┤
│              LibRADOS（客户端库）                 │
├─────────────────────────────────────────────────┤
│         RADOS（可靠自治分布式对象存储）             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │  MON     │  │  OSD     │  │  MDS     │      │
│  │ Monitor  │  │ 存储节点  │  │ 元数据   │      │
│  └──────────┘  └──────────┘  └──────────┘      │
│  ┌──────────┐                                    │
│  │  MGR     │                                    │
│  │ Manager  │                                    │
│  └──────────┘                                    │
└─────────────────────────────────────────────────┘
```

### 2.2 架构分层

Ceph 采用分层架构，从下到上依次为：

1. **RADOS（基础存储层）**：Ceph 的核心，一个可靠的、自治的分布式对象存储系统，由大量 OSD 和少量 Monitor 组成。
2. **LibRADOS（访问接口层）**：提供 C/C++/Python/Java 等语言的 API 接口，简化对 RADOS 的访问。
3. **应用接口层**：在 LibRADOS 之上构建的高级存储接口：
   - **RBD（RADOS Block Device）**：块存储接口
   - **CephFS（Ceph File System）**：POSIX 兼容的文件系统
   - **RGW（RADOS Gateway）**：兼容 S3/Swift 的对象存储网关

---

## 3. 核心组件详解

### 3.1 Ceph Monitor（MON）—— 集群大脑

**核心职责**：维护集群状态映射表，确保所有节点同步集群拓扑与健康状态。

**关键映射表**：
- **Monitor Map**：Monitor 节点信息及集群 fsid
- **OSD Map**：所有 OSD 的状态信息、权重、归属
- **PG Map**：PG 与 OSD 的映射关系
- **CRUSH Map**：数据分布规则定义
- **MDS Map**：MDS 节点状态信息

**工作机制**：
- 通过 **Paxos 算法**实现多节点分布式共识，保证元数据强一致性
- 生产环境建议部署 **3 或 5 个** MON 节点（奇数个，支持容错）
- 超过半数 MON 存活即可保证集群正常运行
- 管理 CRUSH 规则，定义数据在 OSD 上的分布逻辑

**关键配置**：
```ini
[mon]
mon initial members = node1, node2, node3
mon host = 192.168.1.1, 192.168.1.2, 192.168.1.3
```

### 3.2 Ceph OSD（Object Storage Device）—— 数据载体

**核心职责**：管理物理磁盘，执行数据的存储、复制、恢复及副本一致性维护。

**技术细节**：
- 每个 OSD 守护进程通常对应一块物理磁盘
- 数据以对象（Object）格式存储，默认大小为 **4MB**
- 每个 OSD 维护一个本地文件系统（通常为 BlueStore 或 FileStore）
- 通过心跳机制向 MON 上报自身状态

**BlueStore vs FileStore**：
| 特性 | FileStore | BlueStore |
|------|-----------|-----------|
| 存储方式 | 文件系统（XFS）+ 日志 | 直接管理裸盘 |
| 写放大 | 有（双写问题） | 无 |
| 性能 | 一般 | 更优 |
| 当前状态 | 已废弃 | 默认推荐 |

**OSD 状态**：
- `up` / `down`：OSD 是否在线
- `in` / `out`：OSD 是否参与数据分布
- `up + in`：正常工作状态
- `up + out`：在线但被移出集群
- `down + in`：被标记为 in 但已离线（触发恢复）

### 3.3 Ceph MDS（Metadata Server）—— 元数据管家

**核心职责**：专门处理 CephFS 的元数据操作，如文件目录创建、权限管理、访问控制。

**关键特性**：
- 缓存常用元数据（目录树、文件属性），减少对 OSD 的访问压力
- 支持多 MDS 节点的 **Active-Standby** 模式
- 支持 **Active-Active** 模式（多活，动态子树分区）
- 元数据存储在 RADOS 池中，MDS 本身不持久化数据

**适用场景**：
- 大数据分析（Hadoop/Spark 对接 CephFS）
- 共享文件存储（NAS 替代方案）
- 容器持久化存储（Kubernetes RWX 卷）

### 3.4 Ceph MGR（Manager）—— 智能运维中枢

**核心职责**：辅助 MON 实现集群管理，提供监控、自动化运维及生态集成能力。

**内置模块**：
- **Prometheus 模块**：导出集群指标供 Prometheus 采集
- **Dashboard 模块**：提供 Web 可视化管理界面
- **Zabbix 模块**：集成 Zabbix 监控
- **Balancer 模块**：自动平衡 PG 分布
- **Crash 模块**：收集和展示守护进程崩溃信息
- **Device Health 模块**：采集磁盘 SMART 健康数据

---

## 4. 数据存储逻辑分层

### 4.1 数据层级关系

```
File/Block Data
      │
      ▼
┌──────────┐
│  Object  │   ← 数据最小逻辑单元（默认 4MB）
└──────────┘
      │ Hash(oid) & mask → PG ID
      ▼
┌──────────┐
│    PG    │   ← Object 的逻辑分组
└──────────┘
      │ CRUSH(PG ID) → [OSD1, OSD2, OSD3]
      ▼
┌──────────┐
│   OSD    │   ← 物理存储设备
└──────────┘
```

### 4.2 Object（对象）

- 数据存储的最小逻辑单元
- 所有数据（块设备、文件、对象）最终都被切分为 Object
- 默认大小：**4MB**（可通过 `osd_max_object_size` 调整）
- 每个 Object 包含：**数据** + **元数据**（名称、大小、属性等）

### 4.3 PG（Placement Group / 归置组）

**PG 是 Ceph 中最重要的逻辑概念之一**，它是一组 Object 的逻辑集合。

**PG 的核心作用**：
- 将海量 Object 聚合为有限数量的 PG，降低元数据管理开销
- PG 数量直接影响集群性能和资源消耗
- 一个 PG 内的所有 Object 共享相同的副本分布策略

**PG 数量计算公式**：
```
PG 总数 = (OSD 数量 × 100) / 副本数
单池 PG 数 = PG 总数 / 池数量
```

**PG 状态**：
| 状态 | 含义 |
|------|------|
| `active` | PG 正常处理读写请求 |
| `clean` | 所有副本一致，无需恢复 |
| `active+clean` | 完全正常 |
| `degraded` | 部分副本不可用，但仍可读写 |
| `peering` | PG 正在协商副本间的一致性 |
| `recovering` | 正在恢复数据 |
| `backfilling` | 正在回填数据 |
| `incomplete` | PG 缺少必要信息，无法处理请求 |
| `stale` | PG 长时间未收到 OSD 心跳 |

### 4.4 Pool（存储池）

- 用户操作的基本单元，类似"命名空间"的概念
- 定义数据冗余策略（副本数或 EC 编码）
- 定义 PG 数量
- 可设置 QoS 限制和配额

**Pool 类型**：
| 类型 | 说明 |
|------|------|
| **Replicated Pool** | 多副本存储（默认 3 副本），读性能好，空间利用率低 |
| **Erasure Coded Pool** | 纠删码存储，空间利用率高（如 4+2 编码利用率为 67%），但计算开销大 |

**常用 Pool 操作命令**：
```bash
# 创建副本池
ceph osd pool create mypool 128 128

# 创建纠删码池
ceph osd erasure-code-profile set myecprofile k=4 m=2
ceph osd pool create myecpool 128 128 erasure myecprofile

# 查看池列表
ceph osd pool ls

# 设置池副本数
ceph osd pool set mypool size 3
```

---

## 5. CRUSH 算法

### 5.1 什么是 CRUSH

CRUSH（Controlled Replication Under Scalable Hashing）是 Ceph 的核心数据分布算法，用于**计算数据对象应该存储在哪些 OSD 上**。它替代了传统分布式存储中的中心化元数据查找表，实现了完全去中心化的数据定位。

### 5.2 CRUSH 的两次映射

```
第一次映射：Object → PG
   Hash(Object名称) % PG数量 → PG ID
   （使用稳定哈希，PG 数量不变则结果始终不变）

第二次映射：PG → OSD
   CRUSH(PG ID, CRUSH Map, 故障域规则) → [OSD1, OSD2, OSD3]
```

### 5.3 CRUSH Map 核心概念

**CRUSH Map 组成**：

1. **Devices（设备）**：所有 OSD 的列表
2. **Bucket Types（桶类型）**：层级结构定义，如：
   ```
   root（根）
   ├── datacenter（数据中心）
   │   ├── rack（机架）
   │   │   ├── host（主机）
   │   │   │   └── osd（磁盘）
   ```
3. **Rules（规则）**：定义数据如何根据故障域分布

**CRUSH Rule 示例**：
```json
rule replicated_rule {
    id 0
    type replicated
    min_size 1
    max_size 10
    step take default                    # 选择根节点
    step chooseleaf firstn 0 type host   # 选择不同主机的 OSD
    step emit                            # 输出结果
}
```

### 5.4 故障域设计

故障域决定了数据副本的分布策略，常见的故障域级别：

| 故障域级别 | 含义 | 适用场景 |
|-----------|------|----------|
| **OSD** | 副本分布在不同磁盘 | 单机测试 |
| **Host** | 副本分布在不同主机 | 默认推荐 |
| **Rack** | 副本分布在不同机架 | 中大型集群 |
| **Row** | 副本分布在不同排 | 大型集群 |
| **Datacenter** | 副本分布在不同数据中心 | 灾备场景 |

### 5.5 CRUSH 权重

- 每个 OSD 有权重属性，默认与磁盘容量成正比
- CRUSH 根据权重决定数据分布的倾向性
- 例如：1TB 磁盘权重 = 1.0，2TB 磁盘权重 = 2.0，2TB 磁盘将接收约两倍的数据量

---

## 6. 三种存储接口

### 6.1 RBD（RADOS Block Device）—— 块存储

**概述**：将 RADOS 存储池中的对象组合成块设备，对外提供类似物理磁盘的接口。

**核心特性**：
- 支持 **精简置备**（Thin Provisioning）
- 支持 **快照**（Snapshot）和 **克隆**（Clone）
- 支持 **分层快照**（Layered Snapshot）
- 支持 **动态扩容**和缩容
- 对接 OpenStack Cinder、Kubernetes CSI

**架构原理**：
```
VM/Docker
  │
  ▼
/dev/rbdX (内核模块或 librbd)
  │
  ▼
RADOS (数据被切分为 4MB Object 存储)
```

**常用命令**：
```bash
# 创建 RBD 镜像
rbd create myimage --size 10240 --pool rbd

# 查看镜像
rbd ls -p rbd

# 创建快照
rbd snap create rbd/myimage@snap1

# 克隆快照
rbd clone rbd/myimage@snap1 rbd/myclone

# 映射到本地
rbd map rbd/myimage
```

### 6.2 CephFS（Ceph File System）—— 文件存储

**概述**：基于 RADOS 的 POSIX 兼容分布式文件系统，支持共享挂载。

**核心特性**：
- 完全 **POSIX 兼容**
- 支持 **多客户端并发读写**
- 元数据与数据分离，MDS 管理元数据
- 支持 **配额**（Quota）管理
- 支持 **快照**（Snapshot）

**架构**：
```
Client
  │
  ├──→ MDS (元数据操作：open/close/stat/mkdir)
  │
  └──→ OSD (数据读写：read/write)
```

**常用命令**：
```bash
# 创建 CephFS
ceph fs volume create myfs

# 挂载（内核驱动）
mount -t ceph 192.168.1.1:6789:/ /mnt/cephfs -o name=admin,secret=xxx

# 挂载（FUSE）
ceph-fuse /mnt/cephfs

# 设置配额
setfattr -n ceph.quota.max_bytes -v 10737418240 /mnt/cephfs/dir
```

### 6.3 RGW（RADOS Gateway）—— 对象存储

**概述**：基于 HTTP 的对象存储网关，兼容 S3 和 Swift API。

**核心特性**：
- 兼容 **Amazon S3 API**
- 兼容 **OpenStack Swift API**
- 支持 **多站点同步**（Multi-Site Replication）
- 支持 **对象版本控制**（Versioning）
- 支持 **生命周期管理**（Lifecycle）
- 支持 **加密**（SSE-S3 / SSE-C）
- 内置 **多租户**支持

**RGW 数据组织**：
```
RGW
  ├── User
  │   ├── Access Key / Secret Key
  │   └── Subuser (Swift 接口)
  ├── Bucket
  │   ├── Object
  │   └── Bucket Policy
  └── Zone / ZoneGroup / Realm (多站点)
```

**常用命令**：
```bash
# 创建用户
radosgw-admin user create --uid=testuser --display-name="Test User"

# 查看用户
radosgw-admin user info --uid=testuser

# 创建 S3 bucket
s3cmd mb s3://mybucket

# 启用版本控制
s3cmd setbucketversioning s3://mybucket enable
```

### 6.4 三种接口对比

| 维度 | RBD | CephFS | RGW |
|------|-----|--------|-----|
| 访问方式 | 块设备映射 | POSIX 挂载 | HTTP REST API |
| 共享能力 | 单客户端（独占） | 多客户端共享 | 多客户端共享 |
| 典型场景 | 虚拟机磁盘、数据库 | 共享文件存储、HPC | 图片/视频存储、备份 |
| 性能 | 高（接近裸盘） | 中等 | 中等 |
| 协议 | 内核模块/librbd | NFS-like / FUSE | S3 / Swift |
| POSIX | 不适用 | 完全兼容 | 不适用 |

---

## 7. 数据恢复与一致性机制

### 7.1 Recovery（恢复）

**触发条件**：
- OSD 离线后重新上线（数据有差异）
- PG 副本不一致
- 新增 OSD 导致数据迁移

**恢复流程**：
1. MON 检测到 OSD 状态变化，更新 OSD Map
2. 受影响的 PG 进入 `peering` 状态
3. PG 主副本（Primary OSD）与其他副本协商差异
4. 进入 `recovering` 状态，开始同步缺失数据
5. 同步完成后回到 `active+clean` 状态

**关键参数**：
```ini
[osd]
osd recovery max active = 3        # 每个 OSD 同时恢复的 PG 数
osd recovery op priority = 3       # 恢复操作优先级（低优先级不影响业务）
osd recovery max chunk = 8388608   # 每次恢复数据块大小（8MB）
osd max backfills = 1              # 同时回填的 PG 数
```

### 7.2 Backfill（回填）

**与 Recovery 的区别**：
- Recovery：增量同步，只同步缺失的数据
- Backfill：全量同步，当 PG 日志不足以进行增量恢复时触发

**触发场景**：
- OSD 离线时间过长，PG 日志已不完整
- 新增 OSD 后的数据迁移
- CRUSH Map 变更导致大量数据重新分布

### 7.3 Scrubbing（数据清洗）

**目的**：定期检查数据一致性，发现并修复静默数据损坏。

**两种模式**：
| 模式 | 频率 | 检查内容 |
|------|------|---------|
| **Light Scrub** | 每天（默认） | 检查对象元数据（大小、校验和） |
| **Deep Scrub** | 每周（默认） | 逐字节比对所有副本的完整数据 |

**关键参数**：
```ini
[osd]
osd scrub begin hour = 0           # scrub 开始时间（凌晨 0 点）
osd scrub end hour = 6             # scrub 结束时间（凌晨 6 点）
osd scrub during recovery = false  # 恢复期间不进行 scrub
osd deep scrub interval = 604800   # 深度 scrub 间隔（7 天）
```

### 7.4 Rebalance（重平衡）

当集群中新增或移除 OSD 时，CRUSH 算法会自动重新计算数据分布，触发数据迁移以达到新的平衡状态。

**Balancer 模块**（MGR 插件）：
- 自动优化 PG 分布，避免某些 OSD 负载过高
- 支持 `upmap` 模式（精细调整）和 `crush-compat` 模式（兼容模式）

```bash
# 启用 balancer
ceph balancer mode upmap
ceph balancer on
```

---

## 8. 部署与运维

### 8.1 部署方式

| 方式 | 适用场景 | 工具 |
|------|---------|------|
| **cephadm** | 生产环境推荐 | Ceph 内置 |
| **ceph-ansible** | 传统部署 | Ansible |
| **Rook** | Kubernetes 环境 | Rook Operator |
| **手动部署** | 学习/测试 | ceph-deploy（已废弃） |

**cephadm 部署示例**：
```bash
# 在 bootstrap 节点上
cephadm bootstrap --mon-ip 192.168.1.1

# 添加主机
ssh-copy-id -f -i /etc/ceph/ceph.pub root@node2
ceph orch host add node2

# 添加 OSD
ceph orch device zap node1 /dev/sdb --force
ceph orch daemon add osd node1:/dev/sdb
```

### 8.2 硬件选型建议

| 组件 | 推荐配置 |
|------|---------|
| **OSD 节点 CPU** | 每 OSD 至少 1 个核心（推荐 64 核以上） |
| **OSD 节点内存** | 每 OSD 至少 1GB（推荐 256GB+） |
| **OSD 节点存储** | 全闪存（NVMe SSD）或混合（HDD + SSD 缓存） |
| **MON 节点** | 低延迟磁盘（SSD），2-4 核 CPU，8-16GB 内存 |
| **网络** | 10GbE 起步，生产环境推荐 25GbE/100GbE |
| **SSD 缓存** | 使用 NVMe SSD 作为 BlueStore 的 WAL/DB 分区 |

### 8.3 网络规划

- **Public Network**（客户端网络）：处理客户端读写请求
- **Cluster Network**（集群网络）：处理 OSD 间数据复制、恢复
- 建议使用独立的 Cluster Network 避免与业务流量竞争

```ini
[global]
public network = 192.168.1.0/24
cluster network = 10.0.0.0/24
```

### 8.4 日常运维命令

```bash
# 集群状态
ceph -s                          # 集群整体状态
ceph health detail               # 详细健康状态
ceph df                          # 存储使用情况

# OSD 管理
ceph osd tree                    # 查看 OSD 树
ceph osd status                  # OSD 状态
ceph osd out osd.0               # 将 OSD 移出集群
ceph osd crush reweight osd.0 0  # 降低 OSD 权重（触发数据迁移）
ceph osd crush remove osd.0      # 从 CRUSH 中移除 OSD

# PG 管理
ceph pg stat                     # PG 统计
ceph pg dump                     # PG 详细列表
ceph pg scrub <pgid>             # 手动触发 scrub
ceph pg deep-scrub <pgid>        # 手动触发 deep scrub

# Pool 管理
ceph osd pool ls detail          # 池详细信息
ceph osd pool set <pool> pg_num 256  # 调整 PG 数量
ceph osd pool delete <pool> <pool> --yes-i-really-really-mean-it
```

---

## 9. 性能调优

### 9.1 硬件层面

- **全闪存优先**：NVMe SSD 比 SATA SSD 延迟低 10 倍以上
- **网络**：至少 10GbE，推荐 25GbE 或 100GbE
- **内存**：每 TB 存储建议 1GB 以上内存
- **CPU**：高频 CPU 有利于降低延迟，多核有利于提高并发

### 9.2 操作系统层面

```bash
# 调整内核参数
echo "vm.swappiness=0" >> /etc/sysctl.conf
echo "vm.vfs_cache_pressure=50" >> /etc/sysctl.conf

# 调整磁盘调度器（SSD 使用 noop/none，HDD 使用 deadline）
echo none > /sys/block/sda/queue/scheduler

# 调整 read-ahead
echo 4096 > /sys/block/sda/queue/read_ahead_kb
```

### 9.3 Ceph 配置调优

```ini
[global]
# 网络优化
ms bind ipv6 = false
ms async op threads = 8

[osd]
# BlueStore 优化
bluestore block size = 8K
bluestore cache size hdd = 2GB
bluestore cache size ssd = 6GB

# OSD 操作线程
osd op threads = 8
osd op num threads per shard hdd = 3
osd op num threads per shard ssd = 6

# PG 调优
osd pg bits = 8
osd pgp bits = 8

# 日志
osd journal size = 10240  # 10GB

# 恢复参数（控制恢复速度，避免影响业务）
osd recovery max active = 3
osd recovery op priority = 3
osd max backfills = 1
osd recovery sleep = 0.1   # 每次恢复操作间休眠 0.1s
```

### 9.4 PG 数量优化

| 每个 OSD 的 PG 数 | 评价 |
|-------------------|------|
| < 30 | 数据分布可能不均匀 |
| 30 ~ 100 | 推荐范围 |
| 100 ~ 200 | 可接受，但资源消耗增加 |
| > 200 | 不推荐，可能导致内存和 CPU 压力过大 |

**PG 数量建议**：
- 采用 2 的幂次方（如 128、256、512），利于 CRUSH 计算
- 总 PG 数 = (OSD 数量 × 100) / 副本数，取最接近的 2 的幂

### 9.5 客户端优化

```ini
[client]
# RBD 缓存
rbd cache = true
rbd cache size = 64MB
rbd cache max dirty = 48MB
rbd cache target dirty = 32MB
rbd cache writethrough until flush = true

# 对象映射（加速 discard/trim）
rbd object map = true

# 排他锁（防止多客户端冲突）
rbd exclusive lock = true
```

---

## 10. 故障排查

### 10.1 常见故障及处理

#### 故障一：OSD Down

**现象**：`ceph -s` 显示 OSD 为 down 状态

**排查步骤**：
```bash
# 1. 检查 OSD 状态
ceph osd tree

# 2. 检查 OSD 日志
journalctl -u ceph-osd@0 --no-pager -n 100

# 3. 检查磁盘健康
smartctl -a /dev/sdb
ceph device scrape-health-metrics

# 4. 尝试手动启动
systemctl start ceph-osd@0

# 5. 如果持续无法启动，重建 OSD
ceph osd out osd.0
ceph osd purge osd.0 --yes-i-really-mean-it
```

#### 故障二：PG Inactive / Stale

**现象**：`ceph health detail` 显示 PG 处于 inactive/stale

**排查步骤**：
```bash
# 查看具体 PG 状态
ceph pg <pgid> query

# 检查是否缺少 OSD（PG 的 acting set 不足）
# 等待 peering 完成或手动修复
ceph pg repair <pgid>
```

#### 故障三：时钟不同步

**现象**：MON 频繁告警 `clock skew detected`

**解决方案**：
```bash
# 确保所有节点配置 NTP 或 Chrony
timedatectl set-ntp true
chronyc sources -v
```

#### 故障四：磁盘满

**现象**：`ceph health detail` 显示 `OSD full`

**解决方案**：
```bash
# 立即扩容或清理数据
ceph osd df                    # 查看各 OSD 使用率
ceph osd reweight-by-utilization  # 自动调整权重
```

### 10.2 告警阈值

```ini
[mon]
mon osd nearfull ratio = 0.75   # 接近满阈值
mon osd full ratio = 0.85       # 满阈值（集群变为只读）
mon osd backfillfull ratio = 0.85
```

---

## 11. 云原生集成

### 11.1 Ceph + Kubernetes

**Rook Operator** 是 Ceph 在 Kubernetes 上的标准部署方式：

```yaml
# 使用 RBD 创建 PVC
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: rbd-pvc
spec:
  accessModes:
    - ReadWriteOnce
  storageClassName: rook-ceph-block
  resources:
    requests:
      storage: 10Gi
---
# 使用 CephFS 创建共享 PVC
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: cephfs-pvc
spec:
  accessModes:
    - ReadWriteMany
  storageClassName: rook-cephfs
  resources:
    requests:
      storage: 10Gi
```

### 11.2 CSI 插件

Ceph 提供 CSI（Container Storage Interface）插件，支持：
- **RBD CSI**：为 Pod 提供块存储卷（RWO），支持快照和克隆
- **CephFS CSI**：为 Pod 提供共享文件系统（RWX）

### 11.3 OpenStack 集成

Ceph 是 OpenStack 最常用的后端存储：
- **Cinder**（块存储）→ RBD
- **Glance**（镜像存储）→ RBD
- **Nova**（虚拟机磁盘）→ RBD
- **Swift**（对象存储）→ RGW

---

## 12. 最佳实践

### 12.1 集群设计原则

1. **奇数个 MON**：3 或 5 个，保证 Paxos 多数派可用
2. **故障域隔离**：至少按 Host 级别隔离，大型集群按 Rack 级别
3. **独立集群网络**：避免恢复流量影响业务
4. **NTP 时间同步**：所有节点严格时钟同步
5. **SSD 加速**：使用 SSD 作为 BlueStore 的 WAL/DB 设备

### 12.2 容量规划

| 冗余策略 | 有效容量 | 示例 |
|---------|---------|------|
| 3 副本 | 总容量 ÷ 3 | 100TB 裸容量 → 约 33TB 可用 |
| EC 4+2 | 总容量 × 4/6 | 100TB 裸容量 → 约 67TB 可用 |
| EC 8+3 | 总容量 × 8/11 | 100TB 裸容量 → 约 73TB 可用 |

**注意**：建议预留 20-30% 的缓冲空间，避免接近 `nearfull` 阈值。

### 12.3 监控体系

| 监控维度 | 工具 | 关键指标 |
|---------|------|---------|
| 集群状态 | Ceph Dashboard | 集群健康、OSD 状态 |
| 性能指标 | Prometheus + Grafana | IOPS、延迟、吞吐量 |
| 硬件健康 | Device Health | SMART 数据、磁盘寿命 |
| 日志聚合 | Loki / ELK | OSD/MON 日志 |

### 12.4 版本升级策略

- 先在测试环境验证
- 一次升级一个组件（MON → MGR → OSD → MDS）
- 设置 `noout` 标志避免升级期间数据迁移
- 保留回滚方案

```bash
# 升级前设置 noout
ceph osd set noout
ceph osd set norebalance

# 升级完成后取消
ceph osd unset noout
ceph osd unset norebalance
```

---

## 13. 常见面试题

### 13.1 基础概念

**Q1：Ceph 的核心组件有哪些？各有什么作用？**

- **MON**：维护集群状态和映射表，基于 Paxos 实现分布式共识
- **OSD**：管理物理磁盘，执行数据存储、复制、恢复
- **MDS**：管理 CephFS 的元数据
- **MGR**：提供监控、管理和运维功能

**Q2：什么是 PG？为什么需要 PG？**

PG（Placement Group）是 Object 的逻辑分组。引入 PG 的主要原因：
- 将海量 Object（可能数十亿）聚合为有限数量的 PG（数千到数万），大幅降低元数据管理开销
- 以 PG 为单位进行数据复制和恢复，提高效率
- PG 是 CRUSH 算法映射的基本单位

**Q3：Ceph 如何处理数据分布？**

通过两次映射：
1. Object → PG：使用 Hash(Object名称) 取模，确定 PG
2. PG → OSD：使用 CRUSH 算法，根据 CRUSH Map 和故障域规则，计算 PG 的 OSD 列表

### 13.2 进阶问题

**Q4：Ceph 的三种存储接口有何区别？适用场景？**

| 接口 | 访问方式 | 共享 | 场景 |
|------|---------|------|------|
| RBD | 块设备（独占） | 否 | 虚拟机磁盘、数据库 |
| CephFS | POSIX 文件系统 | 是 | 共享文件存储、HPC |
| RGW | HTTP REST（S3/Swift） | 是 | 图片、视频、备份归档 |

**Q5：副本（Replication）和纠删码（EC）如何选择？**

- **副本**：读性能好，写延迟低，但空间利用率低（3 副本 = 33%）。适合性能敏感场景（如数据库）。
- **EC**：空间利用率高（4+2 = 67%），但读写有计算开销，延迟较高。适合冷数据、备份归档场景。

**Q6：OSD down 后集群如何处理？**

1. MON 检测到 OSD 心跳超时（默认 300s），标记为 down
2. 等待 `mon osd down out interval`（默认 600s），如果仍未恢复，标记为 out
3. 受影响的 PG 进入 `degraded` 状态
4. 集群自动启动 recovery/backfill 流程，从其他副本重建数据
5. 如果在 out 之前恢复，只需增量同步；如果 out 之后恢复，需要全量 backfill

**Q7：Ceph 的性能瓶颈通常在哪里？**

- **网络**：OSD 间数据复制和客户端访问都依赖网络，是最大瓶颈
- **磁盘 IOPS**：HDD 的随机 IOPS 有限
- **PG 数量**：过多或过少都会影响性能
- **OSD 内存**：不足会导致频繁的缓存击穿
- **客户端缓存**：配置不当会影响读写性能

**Q8：如何保证 Ceph 的数据一致性？**

- **强一致性**：Ceph 采用同步写入，所有副本写入成功后才返回确认
- **Scrubbing**：定期检查副本间数据一致性
- **Paxos**：MON 通过 Paxos 保证元数据一致性
- **Peering**：PG 恢复时通过 peering 协商确保副本一致

---

## 参考资源

- [Ceph 官方文档](https://docs.ceph.com/)
- [Ceph GitHub 仓库](https://github.com/ceph/ceph)
- [Ceph 社区](https://ceph.io/en/community/)

---

> 本文档涵盖 Ceph 的核心知识体系，从基础概念到生产实践，适用于学习、运维和面试准备。建议结合实际集群操作加深理解。