### 一、虚拟机数量与硬件配置要求
#### 1. 虚拟机数量
Ceph 副本模式（默认3副本）的核心要求是**副本跨节点分布**，保证单节点故障时数据不丢失，因此：
- **最低推荐（学习/测试首选）：3台虚拟机**
  每台同时承载 **MON（监控节点）+ MGR（管理节点）+ OSD（存储节点）** 三种角色，既满足 MON 高可用（3个MON符合Paxos过半机制，单MON故障集群仍正常运行），又满足3副本数据冗余要求，是最节省资源的标准测试方案。
- **极简体验（无高可用）：2台虚拟机**
  仅支持2副本，无法容忍单节点故障，仅适合快速体验功能，不推荐用于学习标准架构。
- **生产级角色分离：≥6台虚拟机**
  3台独立MON节点 + 3台以上独立OSD节点 + 可选MDS/RGW节点，适合生产环境，学习测试无需此配置。

#### 2. 单台虚拟机硬件配置
| 资源类型 | 最低可运行配置 | 推荐流畅配置 | 说明                                                         |
| -------- | -------------- | ------------ | ------------------------------------------------------------ |
| CPU      | 2核            | 4核          | 数据压缩、加密场景需更多CPU资源                              |
| 内存     | 2GB            | 4GB+         | 每个OSD进程建议至少分配1GB内存，MON/MGR额外占用约1GB         |
| 系统盘   | 30GB           | 50GB         | 安装操作系统与Ceph软件包                                     |
| 数据盘   | 1块×20GB       | 1块×50GB+    | **必须是独立的裸磁盘**（不能是系统盘的分区），每个OSD对应1块数据盘 |
| 网络     | 千兆网卡       | 千兆/万兆    | 所有节点需在同一网段，网络互通                               |

#### 3. 可用容量计算
- 核心公式：**可用逻辑容量 = 所有数据盘总容量 ÷ 副本数**
- 示例：3台虚拟机，每台配1块50GB数据盘，3副本模式下：
  总原始容量 = 3 × 50GB = 150GB
  实际可用容量 ≈ 150GB ÷ 3 ≈ 50GB
- 注意：实际可用会扣除约10%的元数据、系统预留空间，属于正常情况。

---

### 二、详细搭建步骤
以下采用官方推荐的 `cephadm` 工具部署 **Ceph 18.2（Reef 稳定版）**，操作系统使用 Ubuntu 22.04 LTS，全程命令行操作。

#### 前期规划
提前规划3台节点的基础信息，后续步骤以此为例：
| 主机名 | IP地址          | 角色        | 数据盘设备名 |
| ------ | --------------- | ----------- | ------------ |
| ceph1  | 192.168.122.101 | MON/MGR/OSD | /dev/sdb     |
| ceph2  | 192.168.122.102 | MON/MGR/OSD | /dev/sdb     |
| ceph3  | 192.168.122.103 | MON/MGR/OSD | /dev/sdb     |

> 数据盘设备名可通过 `lsblk` 命令确认，必须是未分区、未挂载的空白裸盘。

---

#### 步骤1：系统初始化（所有3台节点都执行）
所有节点统一执行基础环境配置，保证集群一致性。

1. 设置对应主机名
```bash
# ceph1节点执行
hostnamectl set-hostname ceph1
# ceph2节点执行
hostnamectl set-hostname ceph2
# ceph3节点执行
hostnamectl set-hostname ceph3
```

2. 配置主机名解析
```bash
cat >> /etc/hosts << EOF
192.168.122.101 ceph1
192.168.122.102 ceph2
192.168.122.103 ceph3
EOF
```

3. 禁用交换分区（Ceph强制要求，否则会严重影响性能和稳定性）
```bash
swapoff -a
sed -i '/ swap / s/^\(.*\)$/#\1/g' /etc/fstab
```

4. 配置时间同步（Ceph对时间误差要求≤50ms，必须同步）
```bash
apt update && apt install -y chrony
systemctl enable --now chrony
```

5. 关闭防火墙（测试环境简化，生产环境需开放对应端口）
```bash
ufw disable
```

6. 安装基础依赖工具
```bash
apt install -y apt-transport-https ca-certificates curl gnupg lsb-release
```

---

#### 步骤2：配置SSH免密登录（仅在ceph1管理节点执行）
cephadm通过SSH管理集群节点，需配置ceph1到所有节点（含自身）的root免密登录。

```bash
# 生成SSH密钥，一路回车即可
ssh-keygen -t rsa

# 分发公钥到三台节点
ssh-copy-id root@ceph1
ssh-copy-id root@ceph2
ssh-copy-id root@ceph3
```

验证：执行 `ssh ceph2 hostname`，无需输入密码直接返回主机名即为成功。

---

#### 步骤3：安装cephadm并初始化集群（仅在ceph1执行）
1. 添加Ceph官方软件源
```bash
curl -fsSL https://download.ceph.com/keys/release.asc | gpg --dearmor -o /usr/share/keyrings/ceph-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/ceph-archive-keyring.gpg] https://download.ceph.com/debian-reef/ $(lsb_release -cs) main" | tee /etc/apt/sources.list.d/ceph.list
```

2. 安装cephadm
```bash
apt update && apt install -y cephadm
```

3. 引导集群，启动第一个MON节点
```bash
cephadm bootstrap --mon-ip 192.168.122.101
```
> 将IP替换为你ceph1节点的实际IP。
> 执行完成后，终端会输出 **Dashboard访问地址、admin账号密码**，请妥善保存。

4. 验证初始集群状态
```bash
ceph -s
```
此时会显示1个MON、1个MGR，状态为 `HEALTH_WARN` 属于正常现象（尚未添加其他节点和OSD）。

---

#### 步骤4：添加剩余节点到集群（ceph1执行）
1. 将ceph2、ceph3加入集群主机列表
```bash
ceph orch host add ceph2 --labels mon
ceph orch host add ceph3 --labels mon
```

2. 查看主机列表，确认三台都已接入
```bash
ceph orch host ls
```

3. 扩展MON为3节点高可用
cephadm会自动在主机上分布MON，也可手动指定数量：
```bash
ceph orch apply mon 3
```
等待1~2分钟，MON进程会自动部署到三台节点上。

---

#### 步骤5：添加OSD数据盘
确认所有节点的数据盘为空白裸盘，若之前有分区/数据，先执行清空（以/dev/sdb为例，所有节点执行）：
```bash
wipefs -a /dev/sdb
```

在ceph1节点执行批量添加OSD命令，自动扫描所有节点的可用裸盘：
```bash
ceph orch apply osd --all-available-devices
```

等待1~2分钟，查看OSD状态：
```bash
ceph osd tree
```
输出中能看到3个OSD分别对应三台主机，状态为 `up` 即为添加成功。

---

#### 步骤6：验证集群最终状态
```bash
ceph -s
```
健康状态为 `HEALTH_OK`，且包含以下关键信息即为搭建完成：
- 3个mon节点，法定人数正常
- 3个osd，全部处于 `up`、`in` 状态
- 1个active的mgr，1个standby备用
- 默认存储池为3副本模式
 - 默认存储池为3副本模式
 
 #### 步骤7（可选）：验证副本跨节点分布
 确认3副本确实分布在不同的物理节点上，而不是落在同一台机器的多个OSD上：
 ```bash
 # 查看所有PG及其分布在哪些OSD上
 ceph pg dump pgs_brief | head -20
 
 # 查看某个具体PG的副本分布（替换PGID为实际PG编号）
 ceph pg map PGID
 ```
 正常输出中，同一个PG的3个副本应落在3台不同主机上。如果多个副本落在同一台主机，说明OSD分布不合理，需检查 `ceph osd tree` 确认各主机的OSD数量。

---

### 三、可选：功能测试（RBD块存储）
1. 创建3副本存储池
```bash
# 创建名为rbd_pool的存储池，PG数32（小规模测试适用）
ceph osd pool create rbd_pool 32 32
# 启用RBD块存储功能
ceph osd pool application enable rbd_pool rbd
```
> Ceph默认创建的存储池就是3副本，无需额外设置。

2. 创建10GB的块设备镜像
```bash
rbd create --size 10G rbd_pool/test_img
```

3. 查看镜像信息
```bash
rbd info rbd_pool/test_img
```

---

### 四、注意事项
1. 数据盘必须是独立的裸磁盘，不能使用系统盘的分区，否则cephadm不会识别。
2. 若部署出错需要重置，可执行 `cephadm rm-cluster --fsid <集群FSID> --force` 清空后重试。
3. 可视化Dashboard：访问bootstrap输出的地址（默认`https://ceph1:8443`），使用admin账号登录，可图形化管理集群。
 4. 若需重装/重置集群，销毁集群后必须清理磁盘残留数据，否则cephadm重新部署时不会识别旧盘：
 ```bash
 # 在ceph1执行，销毁集群
 cephadm rm-cluster --fsid <集群FSID> --force
 # 然后在每台节点上清理OSD磁盘头部数据
 dd if=/dev/zero of=/dev/sdb bs=1M count=100
 ```
 集群FSID可通过 `cephadm ls` 或原集群的 `ceph fsid` 命令获取。

需要我补充 CentOS 系统下的搭建步骤，或者 CephFS 文件存储、RGW 对象网关的部署教程吗？
