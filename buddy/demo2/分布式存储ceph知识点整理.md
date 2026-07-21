# 分布式存储 Ceph 知识点整理

> **来源**：CSDN 博客（happy_king_zi，原创）
> **原文链接**：https://blog.csdn.net/happy_king_zi/article/details/140994426
> **发布时间**：2024-08-07（首次发布 / 修改）
> **本地版本**：已优化 —— 规范标题层级、补充目录与组件速查表、将长代码块转为表格、修正笔误

---

## 目录

- [一、Ceph 概述](#一ceph-概述)
  - [1.1 如何选择存储](#11-如何选择存储)
  - [1.2 存储分类](#12-存储分类)
  - [1.3 分布式存储分类](#13-分布式存储分类)
  - [1.4 分布式存储系统的特性](#14-分布式存储系统的特性)
  - [1.5 Ceph 介绍](#15-ceph-介绍)
  - [1.6 企业里的典型场景](#16-企业里的典型场景)
- [二、Ceph 部署](#二ceph-部署)
- [三、性能调优与硬件选型](#三性能调优与硬件选型)
- [四、Ceph 运维](#四ceph-运维)
- [五、Ceph MDS 性能测试分析](#五ceph-mds-性能测试分析)

---

## 一、Ceph 概述

### 1.1 如何选择存储

- 底层协议
- 兼容性
- 产品要有定位，功能有所取舍
- 针对特定市场的应用存储
- 被市场认可的存储系统
  - 稳定性是第一位的
  - 性能第二
  - 数据功能要够用

### 1.2 存储分类

#### 1.2.1 本地存储

本地的文件系统，不能在网络上用。

如：ext3、ext4、xfs、ntfs

#### 1.2.2 网络存储

网络文件系统，共享的是文件系统：

- **nfs**：网络文件系统
- **hdfs、glusterfs**：分布式网络文件系统
- **共享裸设备**：块存储 cinder、ceph（块存储 / 对象存储 / 分布式网络文件系统）、SAN（存储区域网）

### 1.3 分布式存储分类

#### 1.3.1 Hadoop HDFS（大数据分布式文件系统）

HDFS（Hadoop Distributed File System）是 Hadoop 生态系统的重要存储组件，提供高吞吐量的数据访问，非常适合大规模数据集应用。

**优点**

- 高容错性：数据自动保存多个副本，副本丢失后自动恢复
- 良好的数据访问机制：一次写入、多次读取，保证数据一致性
- 适合大数据文件存储：TB 甚至 PB 级，扩展能力很强

**缺点**

- 不适合低延迟（毫秒级）数据访问，难以应付实时交互类应用
- 海量小文件存储：占用 NameNode 大量内存
- 一个文件只能一个写入者：仅支持 append（追加）

#### 1.3.2 OpenStack object storage（Swift）

Swift 是 OpenStack 的子项目，目的是用普通硬件构建冗余、可扩展的分布式对象存储集群，容量可达 PB 级，使用 Python 开发。

**主要特点**

1. 各存储节点完全对等，是对称系统架构
2. 开发者通过 RESTful HTTP API 与对象存储系统交互
3. 无单点故障：元数据存储均匀随机分布且多副本，集群中没有单点角色
4. 不影响性能的前提下，可通过增加外部节点扩展
5. 无限可扩展：容量无限扩展，QPS / 吞吐量等性能可线性提升，扩容只需新增机器
6. 极高的数据持久性

**适用场景**

- 图片、文档存储
- 长期保存的日志文件
- 媒体库（图片、音乐、视频等）
- 视频监控文件存档

> 总结：Swift 适合存储大量、长期、需要备份的对象。

#### 1.3.3 公有云对象存储

公有云大多只提供对象存储，例如 AWS S3、阿里云 OSS、谷歌云存储等。

#### 1.3.4 GlusterFS

GlusterFS 是一种全对称的开源分布式文件系统，采用弹性哈希算法，无中心节点，所有节点平等。配置方便、稳定性好，可轻松达到 PB 级容量、数千节点。

> PB 级容量、高可用、文件系统级共享、分布式、去中心化。

**基本卷类型**：条带、复制、哈希。**复合卷**：分布式复制、分布式条带、分布式条带复制（前两种最常用）。

| 卷类型 | 说明 | 优点 | 缺点 |
|--------|------|------|------|
| 分布卷 | 文件随机存储到各节点 | 读取速度快 | 一个 brick 坏掉则文件丢失 |
| 复制卷 | 文件分别存到每台机器 | 多备份，单 brick 损坏不丢数据 | 占用资源 |
| 条带卷 | 一个文件分块存到各机器 | 大文件读写快 | 一个 brick 坏掉则文件损坏 |

#### 1.3.5 Ceph

详见下文 1.5 节。

### 1.4 分布式存储系统的特性

1. **可扩展**：可扩展到几百甚至几千台，集群规模增长时整体性能线性增长；节点扩展后旧数据自动迁移、负载均衡，扩展过程不影响业务。
2. **低成本**：自动容错、自动负载均衡使其可构建在普通 PC 上，线性扩展便于自动运维。
3. **高性能**：无论集群整体还是单台服务器都要求高性能。
4. **易用**：需提供易用对外接口，以及完善的监控、运维工具，并能与其他系统集成。
5. **易管理**：可通过简单 Web 界面配置管理，运维简便、管理成本低。

> 挑战在于数据 / 状态信息 / 持久化，要在自动迁移、容错、并发读写中保证一致性。技术主要来自**分布式系统**与**数据库**两个领域。

### 1.5 Ceph 介绍

软件定义存储（SDS）利用基于软件的方法管理数据存储，提供基于策略的数据层控制，独立于底层硬件。

- S3 Client：S3cmd — https://s3tools.org/download
- 监控控制台 ceph-dash：https://github.com/Crapworks/ceph-dash
- OpenStack RDO：http://rdo.fedorapeople.org/rdo-release.rpm

#### 1.5.1 Ceph 概要

Ceph 是一个开源、软件定义、统一的分布式存储系统：可大规模扩展、高性能、无单点故障，运行在通用商用硬件上，容量可扩展至 EB 级。它在同一底层架构上提供**块、文件、对象**三种存储，用户可自主选择。

> 我们无法停止数据的生成，但需要缩小数据生成与数据存储之间的差距。

#### 1.5.2 架构设计特性

1. 所有组件必须可扩展
2. 不能存在单点故障
3. 解决方案必须是软件定义、开源、可适用
4. 运行在通用商用硬件上
5. 所有组件尽可能自我管理

#### 1.5.3 Ceph 的好处

> **对象是 Ceph 的基础存储单元。** 任何格式的数据（块 / 对象 / 文件）都以对象形式保存在归置组（Placement Group，PG）中。

1. 满足现在和将来对非结构化数据存储的需求
2. 可将平台与硬件独立分开
3. 智能处理对象，可为每个对象创建集群副本提高可靠性
4. 无物理存储路径绑定，对象灵活且与位置无关，量级可近线性从 PB 扩展到 EB

#### 1.5.4 核心组件与概念

Ceph 支持对象存储（RADOSGW）、块存储（RBD）、文件存储（CephFS）。一个集群至少包含 Monitor、Manager、OSD；运行 CephFS 还需 MDS。

**组件速查表**

| 组件 | 全称 | 作用 |
|------|------|------|
| OSD | Object Storage Device | 存储 / 复制 / 恢复数据，心跳上报，一般由一块硬盘对应一个 OSD |
| MON | Monitor | 监视集群健康，维护 Cluster Map（OSD / Monitor / PG / CRUSH Map） |
| MDS | Metadata Server | 保存 CephFS 元数据（对象存储与块存储不需要） |
| MGR | Manager | 收集集群指标，提供 Dashboard 与 REST API（高可用至少 2 个） |
| RGW | RADOS Gateway | 基于 librados，提供 S3 / Swift 对象存储接口 |
| RADOS | Reliable Autonomic Distributed Object Store | Ceph 集群底层核心，负责数据分配与故障转移 |
| PG | Placement Group | 数据放置与定位的逻辑单元，一个 PG 包含多个 OSD |
| Object | — | 最底层存储单元，含元数据 + 原始数据 |
| CephFS | Ceph File System | 对外文件系统服务，复用同一 Ceph 存储集群 |

**OSD 详解**

OSD 负责存储、复制、平衡、恢复数据，并与其他 OSD 心跳检查、上报变化给 Monitor。一般一块硬盘对应一个 OSD（一个分区也可成为 OSD）。

> **版本提示（重要）**：本文的「文件系统 / Journal 盘」描述为旧版 **Filestore** 后端模型。自 Luminous 12.2（2017）起，官方默认且推荐的 OSD 后端是 **BlueStore**：
> - BlueStore 直接管理**裸块设备**，不再依赖 XFS/ext4 等本地文件系统（旧文「推荐 XFS」仅适用于 Filestore）；
> - 不再有独立的 journal 盘，改为用独立的 **WAL**（写前日志）与 **DB**（元数据 / omap）分区，可放在更快的 SSD/NVMe 上；
> - 下文 `filestore_*` 相关调优参数仅对 Filestore 生效，新集群无需关注。
> Filestore 已在新版本中被弃用并逐步移除，生产环境请一律使用 BlueStore。

**Journal 盘（Filestore 模型，仅作历史参考）**：写数据先落 Journal 盘，每隔一段时间（如 5 秒）刷入文件系统。Journal 盘一般用 SSD（建议 10G 以上），可缓冲突发负载、降低写时延。

**MON 详解**：维护 Cluster Map（OSD Map / Monitor Map / PG Map / CRUSH Map），是 RADOS 的关键数据结构。客户端写入时，OSD 先通过 Monitor 获取最新 Map，再结合 object id 计算最终位置。

**MGR 详解**：收集存储利用率、性能指标、系统负载，对外提供 Dashboard 与 REST API；高可用部署时至少 2 个。

**RGW 详解**：运行于 librados 之上，对外提供 S3 / Swift 兼容的对象存储 API，客户端经 cephx 认证后与集群交互。

> **版本提示**：RGW 的前端 Web 服务器自 Nautilus 起默认是 **beast**（基于 boost::asio 的异步服务器）；旧版默认的 Civetweb 仍可作为可选前端，但已非默认。

#### 1.5.5 CRUSH 算法

1. **后台计算存储 / 读取位置**，不为每个请求查元数据表
2. **动态计算元数据**，无需集中式元数据表
3. 将大计算负载分布到集群多节点，元数据管理优于传统存储
4. **基础感知能力**：可在 CRUSH Map 中自定义故障区域
5. **自我管理与自愈**：为因故障丢失的数据执行恢复

### 1.6 企业里的典型场景

| 场景 | 亮点 | 典型做法 | 用途 |
|------|------|----------|------|
| 高性能 | 低 TCO 下最高 IOPS | SSD / PCIe SSD / NVMe 高性能节点 | 块存储、高 IOPS 负载 |
| 通用 | 高吞吐、每吞吐量低功耗 | 高带宽双网络 + SSD 做日志盘 | 块 / 对象 / 文件存储 |
| 大容量 | 每 TB 低成本、机架空间成本低 | 密集服务器插满机械盘（单台 24–72TB） | 低功耗大容量对象 / 文件存储（冷存储） |

---

## 二、Ceph 部署

### 2.1 ceph-deploy 部署（已弃用）

> **版本提示**：`ceph-deploy` 已在 Nautilus 后被官方标记为弃用并停止维护，新集群不建议使用。参考仅作历史资料保留：https://blog.csdn.net/nirendao/article/details/79360629（安装 Ceph 12.x）
>
> 推荐改用 **cephadm**（官方主力，基于容器）或 **Rook**（Kubernetes 场景）。

### 2.2 cephadm 部署

参考：https://www.cnblogs.com/st2021/p/14970266.html

### 2.3 Rook 部署到 Kubernetes

参考：https://www.rook.io/docs/rook/v1.7/ceph-storage.html

**架构**

- Rook 负责初始化和管理 Ceph 集群：monitor / mgr / osd / pool / 对象存储 / 文件存储 / 监视维护状态
- Rook 提供访问驱动：Flex（旧，不推荐）、CSI、RBD 块存储、CephFS 文件存储、S3/Swift 对象存储
- 所有对象依托 Kubernetes 集群：mon、rgw、mds、osd、agent（csi-rbdplugin、csi-cephfsplugin）
- 抽象化管理，隐藏细节：pool、volumes、filesystems、buckets

---

## 三、性能调优与硬件选型

### 3.1 硬件选型

- 原则：根据存储需求与企业场景制定
- 企业诉求：TCO 低、高性能、高可靠
- 典型历程：硬件选型 → 部署调优 → 性能测试 → 架构灾备设计 → 部分业务上线测试 → 运维（故障处理、预案演练）
- 参考：https://docs.ceph.com/en/pacific/start/hardware-recommendations/
- OSD 需经 CRUSH 计算位置、复制数据、拷贝 Cluster Map；通常**每个 OSD 进程至少 1 个 CPU 核**
- SSD 选型：预算充足推荐 PCIe SSD，延迟改善明显
- BIOS：开启超线程、关闭节能
- NUMA：建议做 NUMA 亲和绑定（而非「关闭」NUMA——操作系统层面通常也无法关闭），将每个 ceph-osd 进程与其 OSD 磁盘所在 NUMA Node 的 CPU 核及本地内存绑定，避免跨节点访问带来的延迟与带宽损耗

### 3.2 系统层面优化

**Linux Kernel**

- IO 调度：SSD/NVMe 建议用 `none`（即旧版 Noop 的多队列等价实现，无额外调度开销）或 `mq-deadline`；注意新版内核（5.x+）已移除旧版单队列 CFQ/Noop/Deadline，改用多队列调度器（none / mq-deadline / bfq / kyber）
- 预读：`read_ahead_kb` 设最大值
- 进程：`pid_max` 设最大值；CPU 频率设为最大性能模式

**内存**

- 关注 SMP 与 NUMA
- SWAP：`vm_swappiness=0`
- 全闪存：增大 TCMalloc Cache 或用 jemalloc 替代 TCMalloc

**Cgroup**

- CPU 绑定 / 隔离时不要跨 CPU，以更好命中内存与缓存
- 用 Cgroup 隔离 Ceph 与其他进程，避免资源抢占
- 为 Ceph 预留充足 CPU / 内存

### 3.3 网络层面优化

**巨型帧**：以太网 MTU 默认 1500（帧 1522 = 1500 payload + 14 header + 4 CRC + 4 VLAN tag）。将 MTU 调到 9000 可减少包数量、降低包头开销、显著提升性能。**需本端与对端同时开启。** 参考：https://www.cnblogs.com/bandaoyu/p/14861151.html

**中断亲和（SMP IRQ affinity）**：默认所有网卡中断交由 CPU0，高网络 IO 时 CPU0 成瓶颈。Linux 2.4+ 支持将中断绑定到指定 CPU：

```bash
# bitmask 为十六进制 CPU 掩码，每一位代表一个核；$num 为中断号
echo "bitmask" > /proc/irq/$num/smp_affinity
```

`irqbalance` 服务每 10 秒自动均衡，但实时性不足、可能加剧负载，建议按规划手动隔离部分 CPU 处理网卡中断。

**硬件加速（TOE）**：TCP Offload Engine 处理协议校验、中断合并、减少内存拷贝。

**RDMA**：不经内核缓冲区，两主机间直接内存传输，显著降低 CPU 与延迟。

> **版本提示**：文中「Mellanox 维护的 accelio + xio 消息机制」是早期（Hammer/Jewel 时代）的 XIO Messenger，已在现代版本中废弃。当前 Ceph 通过 **AsyncMessenger** 提供 `rdma` 传输（与 `posix` 并列），在 `ms_type=async` 下选择 `rdma` 即可启用。

**DPDK**：轮询方式处理数据包，驱动收到后直接存入用户态内存，避免中断与内存拷贝、上下文切换。

### 3.4 Ceph 层面优化

**参数分类**：global、journal 相关、osd config tuning、recovery tuning、client tuning。

**PG 数量优化**

```
Total PGs = (OSD 数量 * [100-200]) / 副本数
```

> OSD 数量 ×（100~200，pool 多则取 200）÷ 副本数。

### 3.5 其他杂项参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `osd_enable_op_tracker` | true | 跟踪 op 执行时间，调优稳定后建议关 |
| `throttler_perf_counter` | true | 观察阈值是否瓶颈，稳定后建议关 |
| `cephx_sign_messages` | true | 安全要求不高可关闭 |
| `filestore_fd_cache_size` | 256 | Filestore 专用，建议 4096（BlueStore 无需关注） |
| `filestore_fd_cache_shards` | 16 | Filestore 专用，建议 256，略有提升（BlueStore 无需关注） |

---

## 四、Ceph 运维

### 4.1 运维内容概述

**手册**：运维手册、预案手册等。

**实操**：部署、预案演练、故障处理、集群扩容；保证高可用、数据不丢，并定期故障演练。

### 4.2 双活与容灾

- **双活**：对称式，两边相互影响，可能一损俱损；增加运维难度，需预防脑裂，软件不健壮反而易致脑裂。
- **异地容灾**：通过互联网 TCP/IP 将本地数据实时备份到异地，可远程恢复 / 回退。
- **传统存储双活**：经虚拟化网关实现，多一层运维；趋势偏向存储阵列自身双活。

### 4.3 日常运维

#### 4.3.1 集群监控管理

**集群整体运行状态**

```bash
[root@cephnode01 ~]# ceph -s
cluster:
    id:     8230a918-a0de-4784-9ab8-cd2a2b8671d0
    health: HEALTH_WARN
            application not enabled on 1 pool(s)

  services:
    mon: 3 daemons, quorum cephnode01,cephnode02,cephnode03 (age 27h)
    mgr: cephnode01(active, since 53m), standbys: cephnode03, cephnode02
    osd: 4 osds: 4 up (since 27h), 4 in (since 19h)
    rgw: 1 daemon active (cephnode01)

  data:
    pools:   6 pools, 96 pgs
    objects: 235 objects, 3.6 KiB
    usage:   4.0 GiB used, 56 GiB / 60 GiB avail
    pgs:     96 active+clean
```

**`ceph -s` 字段含义**

| 字段 | 含义 |
|------|------|
| id | 集群 ID |
| health | 运行状态（HEALTH_OK / WARN / ERR） |
| mon | Monitor 运行状态 |
| osd | OSD 运行状态 |
| mgr | Manager 运行状态 |
| mds | Metadata 运行状态 |
| pools | 存储池与 PG 数量 |
| objects | 存储对象数量 |
| usage | 存储理论用量 |
| pgs | PG 运行状态 |

**常用状态查询**

```bash
# 仅显示是否正常
ceph health detail

# 显示集群状态
ceph -s

# 动态观察集群
ceph -w
```

**集群标志（flag）**

| 标志 | 作用 |
|------|------|
| `noup` | OSD 启动不自标为 up（防网络抖动） |
| `nodown` | OSD 停止不标为 down（防网络抖动） |
| `noout` | 不从 CRUSH 移除 OSD（维护时防自动重平衡） |
| `noin` | 禁止数据自动分配到 OSD |
| `norecover` | 禁止集群恢复操作 |
| `nobackfill` | 禁止数据回填 |
| `noscrub` | 禁止清理（防低带宽集群 OSD 被标 down） |
| `nodeep-scrub` | 禁止深度清理 |
| `norebalance` | 禁止重平衡数据 |
| `pause` | 集群停止读写（不影响 OSD 自检） |
| `full` | 标记集群已满，拒绝写入但可读 |

```bash
# 设置 / 取消 noout
ceph osd set noout
ceph osd unset noout

# 将文件作为对象写入 pool
rados -p ssdpool put testfull /etc/ceph/ceph.conf
rados -p ssdpool ls
```

**PG 操作**

PG 常见状态：

| 状态 | 含义 |
|------|------|
| Creating | PG 正在创建（建 pool 或修改 PG 数时出现） |
| Active | 活跃，可正常读写 |
| Clean | 所有对象已复制规定副本数 |
| Down | PG 离线 |
| Replay | 某 OSD 异常后，等待客户端重发操作 |
| Splitting | PG 正在分割（PG 数增加后） |
| Scrubbing | 正在做不一致校验 |
| Degraded | 部分对象副本数未达标 |
| Inconsistent | 副本不一致，可用 `ceph pg repair` 修复 |
| Peering | 主 OSD 与副本 OSD 就对象 / 元数据达成一致 |
| Repair | 正在检查并尝试修复不一致 |
| Recovering | OSD down 后的迁移 / 同步（重平衡） |
| Backfill | 新 OSD 加入后的数据回填 |
| Backfill-wait | 等待开始回填 |
| Incomplete | PG 日志缺失关键时间段数据 |
| Stale | Monitor 未收到 PG 更新（如刚启动 Peering 前） |
| Remapped | acting set 变化后，数据从旧集迁移到新集 |

**Stuck（卡住）状态**：PG 长时间（默认 300s，`mon_pg_stuck_threshold`）处于 `inactive`（peering 问题）/`unclean`（恢复问题）/`stale`（无 OSD 上报）/`undersized`（副本数不足）会被标记。若所有 OSD 均 down+out 则 PG 不可用，可声明丢失（意味着数据丢失）。OSD 依赖 journal，journal 丢失则 OSD 停止。

```bash
# 检查 stuck 状态的 pg
ceph pg dump_stuck

# 检查阻塞在 peering 的 osd
ceph osd blocked-by

# 检查某个 pg
ceph pg dump | grep <pgid>

# 声明 pg 丢失
ceph pg <pgid> mark_unfound_lost revert|delete

# 声明 osd 丢失（需状态为 down 且 out）
ceph osd lost <osdid> --yes-i-really-mean-it
```

**pool 管理**

```bash
# 查看 pool 状态
ceph osd pool stats
ceph osd lspools

# 限制 pool 配置更改
ceph tell osd.* injectargs --osd_pool_default_flag_nodelete true   # 禁止删除
ceph tell osd.* injectargs --osd_pool_default_flag_nopgchange true # 禁止改 pg_num/pgp_num
ceph tell osd.* injectargs --osd_pool_default_flag_nosizechange true # 禁止改 size/min_size
```

**OSD 状态查询**

```bash
ceph osd stat
ceph osd status
ceph osd dump
ceph osd tree
ceph osd df
```

**Monitor 与仲裁**

```bash
ceph mon stat
ceph mon dump
ceph quorum_status
```

**集群空间**

```bash
ceph df
ceph df detail
```

#### 4.3.2 集群配置管理

```bash
# 查看运行配置
ceph daemon {daemon-type}.{id} config show
# 例：ceph daemon osd.0 config show
```

**tell 子命令**（适合对整个集群设置，用 `*` 匹配；节点异常时仅命令行报错）

```bash
# 格式：ceph tell {daemon-type}.{daemon id 或 *} injectargs --{name}={value}
ceph tell osd.0 injectargs --debug-osd 20 --debug-ms 1
```

**daemon 子命令**（逐个设置，反馈更清晰，需在对应主机执行）

```bash
# 格式：ceph daemon {daemon-type}.{id} config set {name}={value}
ceph daemon mon.ceph-monitor-1 config set mon_allow_pool_delete false
```

#### 4.3.3 集群操作

**守护进程**

```bash
systemctl start ceph.target          # 启动所有
systemctl start ceph-mgr.target
systemctl start ceph-osd@<id>
systemctl start ceph-mon.target
systemctl start ceph-mds.target
systemctl start ceph-radosgw.target
```

**添加 / 删除 OSD**

```bash
# 添加：格式化后由 ceph-deploy 在 /my-cluster 目录执行
ceph-volume lvm zap /dev/sd<id>
ceph-deploy osd create --data /dev/sd<id> $hostname

# 删除
ceph osd crush reweight osd.<ID> 0.0   # 权重置 0
systemctl stop ceph-osd@<ID>           # 停进程
ceph osd out <ID>                       # 置 out
ceph osd purge osd.<ID> --yes-i-really-mean-it  # 删数据
umount /var/lib/ceph/osd/ceph-<ID>     # 卸载磁盘
```

**扩容 PG**（需同时改 pg_num 与 pgp_num，保持大小一致才能正常 rebalancing）

```bash
ceph osd pool set {pool-name} pg_num 128
ceph osd pool set {pool-name} pgp_num 128
```

**pool 操作**

```bash
ceph osd lspools                                          # 列出
ceph osd pool create rbd 32 32                           # 创建（pg-num pgp-num）
ceph osd pool set-quota rbd max_objects 10000            # 配额
ceph osd pool delete rbd rbd --yes-i-really-really-mean-it # 删除
ceph osd pool rename {current} {new}                     # 重命名
rados df                                                 # 统计
ceph osd pool mksnap {pool-name} {snap-name}             # 快照
ceph osd pool rmsnap {pool-name} {snap-name}             # 删快照
ceph osd pool get {pool-name} {key}                      # 获取选项
ceph osd pool set {pool-name} {key} {value}              # 设选项

# 副本数
ceph osd pool set <poolname> size <num>
ceph osd pool get <poolname> size
ceph osd dump | grep 'replicated size'                   # 查对象副本数
```

**pool set 常用 key**

| key | 说明 |
|-----|------|
| size | 对象副本数（仅副本池） |
| min_size | I/O 所需最小副本数（仅副本池） |
| pg_num | 有效 PG 数，只能增大 |
| pgp_num | 有效 PGP 数，≤ PG 数 |
| hashpspool | 设置 / 取消 HASHPSPOOL 标志 |
| target_max_bytes | 达阈值触发冲洗 / 驱逐 |
| target_max_objects | 达阈值触发冲洗 / 驱逐 |
| scrub_min_interval | 低负载时最小洗刷间隔（0 则用配置） |
| scrub_max_interval | 最大洗刷间隔（0 则用配置） |
| deep_scrub_interval | 深度洗刷间隔（0 则用配置） |

**用户管理**

```bash
# 查看
ceph auth list
ceph auth get client.admin
ceph auth print-key client.admin

# 添加
ceph auth add client.john mon 'allow r' osd 'allow rw pool=liverpool'
ceph auth get-or-create client.paul mon 'allow r' osd 'allow rw pool=liverpool'
ceph auth get-or-create client.george mon 'allow r' osd 'allow rw pool=liverpool' -o george.keyring
ceph auth get-or-create-key client.ringo mon 'allow r' osd 'allow rw pool=liverpool' -o ringo.key

# 修改权限
ceph auth caps client.john mon 'allow r' osd 'allow rw pool=liverpool'
ceph auth caps client.paul mon 'allow rw' osd 'allow rwx pool=liverpool'
ceph auth caps client.brian-manager mon 'allow *' osd 'allow *'
ceph auth caps client.ringo mon ' ' osd ' '

# 删除（TYPE 为 client/osd/mon/mds，ID 为用户名或守护进程 ID）
ceph auth del {TYPE}.{ID}
```

**Monitor 增删**

> 至少 3 个，建议奇数；需多数 mon 可通信。初始建议 3 个，后续一次加 2 个。

```bash
ceph-deploy mon create $hostname     # 增加（先进入 /my-cluster 目录）
ceph-deploy mon destroy $hostname    # 删除（确保其余仍能达成一致）
```

**故障排除（nearfull / pools nearfull）**

`mon` 会监控 OSD 空间使用率。提高阈值仅能消除 WARN，实践中应分析 OSD 数据分布。

```json
"mon_osd_full_ratio": "0.95",
"mon_osd_nearfull_ratio": "0.85"
```

```bash
# 自动处理
ceph osd reweight-by-utilization
ceph osd reweight-by-pg 105 cephfs_data

# 手动处理
ceph osd reweight osd.2 0.8

# 全局均衡
ceph mgr module ls
ceph mgr module enable balancer
ceph balancer on
ceph balancer mode crush-compat
ceph config-key set "mgr/balancer/max_misplaced" "0.01"
```

#### 4.3.4 PG 与 OSD 状态

**PG 状态概述**：见 [4.3.1 集群监控管理](#431-集群监控管理) 中「PG 常见状态」表。

**OSD 状态**：每组状态互不互斥——`in/out` 表示是否在集群内，`up/down` 表示守护进程是否运行。

| 状态 | 说明 |
|------|------|
| in + up | 正常：在集群内且运行正常 |
| in + down | 在集群中但进程异常，默认 300s 后被踢出变为 out+down，PG 迁移 |
| out + up | 新增 OSD 常见：进程正常但未加入集群 |
| out + down | 不在集群内且进程异常，CRUSH 不再分配 PG |

---

## 五、Ceph MDS 性能测试分析

Ceph MDS 主处理流程为单线程，单 MDS 性能受限（最大约 8k ops/s，CPU 利用率约 140%）。但这也带来优势：

1. 单线程无需复杂锁机制，可发挥最大单 MDS 性能
2. CephFS 支持多个 active MDS（即多个 rank，各负责一部分目录子树）提供并发；增加 active MDS 数量时性能可近似线性提升。（注意 MDS 是**有状态**的——会在内存中缓存元数据，因此可部署 standby 做故障接管，并非「无状态」）
3. 当前 MDS 负载均衡实现尚不完善，可针对具体应用手动实现负载均衡

> 生产环境长稳测试：内核客户端确有约 3 倍性能，但偶有宕机；**推荐使用 fuse 客户端**，更稳定。
>
> **版本提示**：上述结论来自较早期内核（CephFS 尚未成熟时）的经验。现代 Linux 内核（5.x+）的 **in-kernel（内核态）CephFS 客户端**已同时具备高性能与高稳定性，通常是首选；fuse 客户端更适合无法加载内核模块或需最新特性的场景。选型应以实际内核版本为准。

- 测试对象：区分不同硬件，如 SSD、RAID、SAN、云硬盘（特点不同）
- 测试指标：IOPS、MBPS（吞吐率）
- 测试工具：Linux 下 Fio、dd、rados bench；Windows 下 iometer
- 测试参数：IO 大小、寻址空间、队列深度、读写模式、随机 / 顺序
- 测试方法：科学合理的测试步骤

---

> 本文由 CSDN 博主 happy_king_zi 原创，转载请附上原文出处链接及本声明。
> 原文链接：https://blog.csdn.net/happy_king_zi/article/details/140994426
