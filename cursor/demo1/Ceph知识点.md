# Ceph 知识点手册

> 面向运维 / 开发的 Ceph 核心知识速查

---

## 1. Ceph 是什么

Ceph 是一套**统一、分布式、开源**的存储系统，可同时提供：

| 接口类型 | 名称 | 典型用途 |
|---------|------|---------|
| 对象存储 | RADOS / RGW (S3/Swift) | 备份、网盘、云对象存储 |
| 块存储 | RBD | 虚拟机磁盘、容器持久卷 |
| 文件存储 | CephFS | 共享文件系统 |

核心特点：无单点故障、可线性扩展、数据多副本/纠删码冗余、强一致性（默认）。

---

## 2. 核心架构

```
客户端 (RBD / RGW / CephFS)
          │
          ▼
    ┌─────────────┐
    │   RADOS     │  ← Reliable Autonomic Distributed Object Store
    │ (对象存储层) │
    └─────────────┘
          │
    ┌─────┴─────┐
    ▼           ▼
  Monitor     OSD
  (集群状态)  (数据存储)
    │
    ▼
  Manager     MDS (仅 CephFS 需要)
  (监控/插件) (元数据服务)
```

### 2.1 关键组件

| 组件 | 职责 |
|------|------|
| **MON (Monitor)** | 维护集群地图（cluster map）、仲裁、认证；奇数个部署（常用 3/5） |
| **OSD (Object Storage Daemon)** | 真正存数据；处理读写、复制、恢复、再平衡 |
| **MGR (Manager)** | 监控指标、Dashboard、编排插件（如 cephadm） |
| **MDS (Metadata Server)** | 仅 CephFS 使用，管理目录树/权限等元数据 |
| **RGW (RADOS Gateway)** | 提供 S3 / Swift 兼容的 HTTP 对象存储接口 |

### 2.2 客户端访问路径

- **librados**：直接访问 RADOS
- **librbd / krbd**：块设备
- **libcephfs / ceph-fuse / kernel client**：文件系统
- **RGW**：REST API（S3/Swift）

---

## 3. 数据组织模型

### 3.1 Pool → PG → Object → OSD

```
Pool（存储池）
  └── PG（Placement Group，归置组）
        └── Object（对象）
              └── 副本/分片落到多个 OSD
```

| 概念 | 说明 |
|------|------|
| **Pool** | 逻辑隔离单元，配置副本数、纠删码、PG 数、应用类型 |
| **PG** | 对象到 OSD 的中间映射层；数量影响分布均匀与恢复粒度 |
| **Object** | 最小存储单元；RBD/CephFS/RGW 最终都落成 RADOS 对象 |
| **CRUSH** | 伪随机算法，根据 cluster map 计算对象应落在哪些 OSD |

### 3.2 CRUSH 要点

- 不查中心表，客户端本地计算数据位置 → **无元数据瓶颈**
- 层次：`root → rack → host → osd`
- 权重（weight）通常按容量设置
- 故障域（failure domain）：副本尽量跨 host/rack

### 3.3 PG 数量经验

- 经验公式（副本池）：`PG 总数 ≈ (OSD 数 × 100) / 副本数`，再向上取最接近的 2 的幂
- 过少：分布不均、恢复粗粒度
- 过多：内存与监控开销变大
- 现代版本可用 **autoscaler** 自动建议/调整 PG

---

## 4. 冗余策略

### 4.1 多副本（Replicated）

- 常见：`size=3, min_size=2`
- `size`：目标副本数
- `min_size`：低于此值则拒绝写入（保证可靠性）
- 优点：实现简单、恢复快、适合随机小 IO
- 缺点：空间放大 3 倍（三副本）

### 4.2 纠删码（Erasure Coding, EC）

- 常见：`k=4, m=2`（4 数据 + 2 校验，可用容量约 66%）
- 优点：空间效率高，适合冷数据、大对象、备份
- 缺点：写放大、CPU 开销、小随机写性能较差；部分场景不支持（如部分 RBD 特性）

---

## 5. 三类存储接口

### 5.1 RBD（块存储）

- 把镜像切成多个 RADOS 对象（默认对象大小 4MB）
- 特性：快照、克隆（COW）、精简配置、多层缓存
- 客户端：`krbd`（内核）或 `librbd`（用户态，可开 `rbd-nbd`）
- 典型场景：OpenStack Cinder、K8s CSI、虚拟机系统盘

常用命令：

```bash
rbd create pool/image --size 100G
rbd map pool/image
rbd snap create pool/image@snap1
rbd clone pool/image@snap1 pool/clone1
rbd info pool/image
```

### 5.2 RGW（对象存储）

- 兼容 Amazon S3、OpenStack Swift
- 概念：User / Bucket / Object / ACL / Lifecycle
- 多站点（multi-site）可做异地同步
- 适合：备份归档、静态资源、大数据湖

### 5.3 CephFS（文件系统）

- 需要 **MDS** 集群
- 支持多活 MDS（目录树分区）
- 客户端：内核客户端（性能好）或 FUSE（功能完整、易调试）
- 注意：MDS 是 CephFS 特有的复杂度与瓶颈点

---

## 6. 集群地图（Cluster Maps）

Monitor 维护的几类 map：

| Map | 作用 |
|-----|------|
| **Monitor map** | MON 成员与地址 |
| **OSD map** | OSD 上下线、权重、状态 |
| **PG map** | PG 状态、acting/up set |
| **CRUSH map** | 拓扑与规则 |
| **MDS map** | MDS 状态（CephFS） |
| **Mgr map** | Manager 状态 |

客户端拿到最新 map 后，用 CRUSH 本地计算读写目标 OSD。

---

## 7. IO 路径（简要）

以三副本写为例：

1. 客户端算 object → PG → primary OSD
2. 客户端把写请求发给 **Primary OSD**
3. Primary 同步写到副本 OSD（Replication）
4. 副本确认后，Primary 向客户端返回 ACK
5. （可选）之后再做 journal/WAL 落盘等细节优化

读通常走 Primary（也可配置均衡读，视版本与特性而定）。

---

## 8. 常用运维命令

### 8.1 集群状态

```bash
ceph -s                      # 总览
ceph health detail           # 健康详情
ceph osd tree                # OSD 拓扑
ceph osd df                  # OSD 用量
ceph df                      # 池用量
ceph mon stat
ceph mgr modules ls
```

### 8.2 Pool / PG

```bash
ceph osd pool ls detail
ceph osd pool create mypool 128      # 旧式指定 pg_num（新版本更推荐 autoscaler）
ceph osd pool set mypool size 3
ceph osd pool set mypool min_size 2
ceph pg dump
ceph pg ls-by-pool mypool
```

### 8.3 OSD 维护

```bash
ceph osd out <id>            # 踢出再平衡（数据迁走）
ceph osd in <id>
ceph osd down <id>
ceph osd crush reweight osd.<id> 0.0
systemctl stop ceph-osd@<id>
```

### 8.4 认证

```bash
ceph auth ls
ceph auth get client.admin
ceph auth get-or-create client.myuser mon 'allow r' osd 'allow rw pool=mypool'
```

---

## 9. 健康状态与常见告警

| 状态/告警 | 含义与处理方向 |
|-----------|----------------|
| `HEALTH_OK` | 正常 |
| `HEALTH_WARN` | 有问题但可服务（如 clock skew、近满、PG degraded） |
| `HEALTH_ERR` | 严重，可能已影响服务 |
| `PG_DEGRADED` | 副本不足，检查 down/out 的 OSD |
| `PG_BACKFILL / RECOVERY` | 正在恢复或回填，控制恢复带宽 |
| `OSD_NEARFULL / FULL` | 空间紧张，扩容或清数据；FULL 会拒写 |
| `SLOW_OPS` | 慢请求，查磁盘、网络、OSD 负载 |
| `CLOCK_SKEW` | 时钟不同步，配置 Chrony/NTP |
| `MON_DOWN` | Monitor 故障，保证法定人数（quorum） |

恢复相关调速示例：

```bash
ceph config set osd osd_max_backfill 1
ceph config set osd osd_recovery_max_active 3
```

---

## 10. 部署与管理方式

| 方式 | 说明 |
|------|------|
| **cephadm** | 官方推荐，基于容器编排，管理升级方便 |
| **Rook** | 在 Kubernetes 中运行 Ceph |
| **ceph-ansible / 手工** | 传统部署，逐步被 cephadm 取代 |

推荐生产最低规模（示意）：

- MON × 3（不同主机）
- MGR × 2（主备）
- OSD × N（每盘或每 NVMe 一个 OSD 较常见）
- 独立公共网络 + 集群网络（可选但推荐）

网络建议：

- **public network**：客户端 ↔ Ceph
- **cluster network**：OSD 复制/恢复流量隔离

---

## 11. 性能相关要点

1. **介质分层**：HDD 容量盘 + SSD/NVMe DB/WAL（BlueStore）
2. **BlueStore**：当前默认后端，对象直接落块设备，比老 FileStore 更优
3. **网络**：25GbE/40GbE 常见；恢复流量会打满网卡
4. **PG 与均衡**：不均会导致热点 OSD
5. **客户端缓存**：RBD cache、OSD op threads、CPU 与内存都要够
6. **避免**：单 OSD 过大故障域、混布过重计算、时钟漂移

查看延迟与吞吐：

```bash
ceph osd perf
ceph tell osd.* perf dump
rados bench -p mypool 30 write
```

---

## 12. 安全与权限

- 默认 **CephX** 认证
- 最小权限原则：按 pool/namespace 授权
- 管理密钥（`client.admin`）严格保管
- RGW 另有 S3 密钥体系
- 可配合 TLS、防火墙隔离 MON/OSD 端口

常见端口（默认）：

| 服务 | 端口 |
|------|------|
| MON | 3300 / 6789 |
| OSD | 6800–7300 |
| MGR Dashboard | 8443（视配置） |
| RGW | 80/443 |

---

## 13. 版本与生态（了解即可）

- 发行版以动物名命名（如 Quincy、Reef、Squid）
- 建议生产使用稳定 LTS 发行版，并关注升级路径
- 周边：Dashboard、Prometheus 模块、CSI Driver、Rook、OpenStack 集成

---

## 14. 面试 / 速记高频点

1. Ceph 统一存储：对象 / 块 / 文件三位一体，底座是 RADOS  
2. 无中心元数据服务靠 **CRUSH** 算位置  
3. **PG** 是对象到 OSD 的桥梁  
4. MON 要奇数个，保证 quorum  
5. `size` / `min_size` 决定可靠性与是否可写  
6. 副本适合热数据；纠删码适合冷数据  
7. OSD 挂了会触发 recovery/backfill，集群会自动再平衡  
8. BlueStore + 独立 WAL/DB 是性能标配思路  
9. CephFS 依赖 MDS；RBD/RGW 不需要 MDS  
10. `ceph -s` / `ceph health detail` 是排障第一入口  

---

## 15. 最小排障流程

```
1. ceph -s / ceph health detail
2. ceph osd tree / ceph osd df     → 谁 down？谁满？
3. ceph pg dump / ceph pg ls       → 哪些 PG 异常？
4. 查对应主机：磁盘 SMART、dmesg、网络、时钟
5. 看 MON/OSD 日志：/var/log/ceph/
6. 控制恢复速度，避免雪崩
7. 修复后观察 HEALTH 回到 OK，PG active+clean
```

---

## 16. 推荐学习路径

1. 弄清 RADOS / Pool / PG / CRUSH / OSD  
2. 搭一个最小集群（cephadm 或虚拟机）  
3. 亲手做：RBD 挂载、RGW 建 bucket、（可选）CephFS  
4. 故意拉掉一个 OSD，观察恢复过程  
5. 读官方文档对应版本章节 + 自己的 `ceph.conf` / CRUSH map  

官方文档：https://docs.ceph.com/

---

*文档版本：基础知识汇总，适用于日常学习与面试速查。具体参数请以所使用的 Ceph 发行版文档为准。*
