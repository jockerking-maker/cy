# Ceph 集群重启启动指南

> 适用场景：虚拟机重启后，恢复 Ceph 集群运行

---

## 一、启动顺序概述

cephadm 部署的 Ceph 所有守护进程都跑在 **Podman 容器**里，并注册为 systemd 服务。虚拟机开机后，大部分组件会自动启动。

只需按以下步骤检查即可：

```
开机 → 检查 podman 容器 → 检查集群状态 → 检查 OSD → 完成
```

---

## 二、逐步操作

### 第 1 步：开机

正常启动三台虚拟机（ceph1、ceph2、ceph3），**没有先后顺序要求**，开完即可。

### 第 2 步：检查 Podman 容器是否运行

在 **ceph1** 上执行：

```bash
podman ps
```

应该能看到类似这样的容器列表：

```
CONTAINER ID  IMAGE                          COMMAND  CREATED  STATUS  NAMES
xxx           quay.io/ceph/ceph:v19          ...      ...      Up      ceph-<fsid>-mon-ceph1
xxx           quay.io/ceph/ceph:v19          ...      ...      Up      ceph-<fsid>-mgr-ceph1
xxx           quay.io/ceph/ceph:v19          ...      ...      Up      ceph-<fsid>-osd.0
xxx           quay.io/ceph/ceph:v19          ...      ...      Up      ceph-<fsid>-osd.1
xxx           quay.io/ceph/ceph:v19          ...      ...      Up      ceph-<fsid>-osd.2
xxx           quay.io/prometheus/...         ...      ...      Up      ...
xxx           quay.io/ceph/grafana           ...      ...      Up      ...
```

所有容器 `STATUS` 应为 `Up`。如果某个容器显示 `Exited`，查看具体日志：

```bash
podman logs ceph-<fsid>-mon-ceph1
```

### 第 3 步：检查集群整体状态

```bash
ceph -s
```

正常输出示例：

```
  cluster:
    id:     8dddb28b-7e98-11f1-9107-000c29c6cc12
    health: HEALTH_OK

  services:
    mon: 3 daemons, quorum ceph1,ceph2,ceph3
    mgr: ceph1.ocwmyd(active), standbys: ceph3.ulpzzg
    osd: 3 osds: 3 up, 3 in

  data:
    pools:   1 pools, 1 pgs
    usage:   81 MiB used, 180 GiB / 180 GiB avail
    pgs:     1 active+clean
```

**重点关注**：

| 检查项 | 正常状态 |
|--------|---------|
| health | `HEALTH_OK` |
| mon | 3 daemons, quorum 三台都在 |
| osd | 3 up, 3 in |
| pgs | `active+clean` |

如果显示 `HEALTH_WARN`，看下一步。

### 第 4 步：如果 OSD 没起来

有时数据盘（`/dev/sdb`）开机后识别慢，OSD 容器可能启动失败。在 **三台节点上分别执行**：

```bash
lsblk
```

确认 `/dev/sdb` 存在。如果盘在但 OSD 没起来，在 **ceph1** 上手动重启 OSD：

```bash
systemctl restart ceph-<fsid>@osd.0
systemctl restart ceph-<fsid>@osd.1
systemctl restart ceph-<fsid>@osd.2
```

### 第 5 步：如果 MON 没起来

检查 MON 容器状态：

```bash
podman ps --filter "name=mon"
```

如果某个 MON 不在线，手动启动它的 systemd 服务（在对应节点上执行）：

```bash
# 在 ceph2 上（如果 ceph2 的 MON 没起来）
systemctl restart ceph-<fsid>@mon.ceph2
```

### 第 6 步：最终验证

```bash
ceph -s
ceph osd tree
```

都正常后，检查 Dashboard 是否可访问：

```
https://192.168.12.176:8443
```

---

## 三、如果一切正常但 ceph 命令报错

```bash
# 如果提示 "command not found" 或找不到 ceph 命令
cephadm install ceph-common
```

```bash
# 如果提示 "Unable to find ceph.conf"
# 检查配置文件是否存在
ls -l /etc/ceph/ceph.conf

# 如果不存在，从容器中复制
cephadm shell -- ceph config generate-minimal-conf > /etc/ceph/ceph.conf
```

---

## 四、一句话总结

```
开机 → ceph -s 检查 → 正常就不用管
          ↓ 异常
     podman ps 看哪个容器没起来
          ↓
     systemctl restart ceph-<fsid>@xxx
```

Ceph 集群和虚拟机是解耦的——虚拟机重启只是"断电"，磁盘数据还在，OSD 重新上线后集群自动恢复。只要不是磁盘坏了，数据不会丢。
