# RBD 挂载重启丢失分析报告

> **问题**：手动挂载的 `/mnt/ceph-rbd` 在虚拟机重启后消失，目录为空。

---

## 一、诊断结论

经过排查，丢失的根本原因有 **三层**：

### 第一层：rbd 内核模块未自动加载

```bash
lsmod | grep rbd   # 重启后无输出 → 模块未加载
```

之前手动执行的 `modprobe rbd` 仅对**当前运行的内核生效**，重启后丢失。

预期文件 `/etc/modules-load.d/rbd.conf` **不存在**（`ls /etc/modules-load.d/` 确认），因此开机时系统不会自动加载 rbd 模块。

### 第二层：rbd map 映射未持久化

```bash
rbd showmapped   # 重启后无输出 → 映射已消失
```

`rbd map` 创建的映射关系是**内核级临时状态**，存在 `/sys/bus/rbd/` 和 `/dev/rbd*` 中，重启后全部清空。没有自动重新映射的机制。

### 第三层：/etc/fstab 未配置持久挂载

```bash
cat /etc/fstab   # 无任何 RBD 相关条目
```

`mount /dev/rbd0 /mnt/ceph-rbd` 是临时命令，重启后不会自动执行。

### 核心原因总结

| 层级 | 重启后状态 | 缺失项 |
|------|-----------|--------|
| 内核模块 | rbd 未加载 | `/etc/modules-load.d/rbd.conf` |
| 设备映射 | `rbd map` 映射丢失| 自动映射脚本 |
| 文件系统挂载 | mount 丢失 | `/etc/fstab` 条目或 systemd 服务 |

---

## 二、修复方案

### 方案 A：systemd 服务（推荐，可靠）

创建一个 systemd 服务，按顺序执行：等待 Ceph 就绪 → 加载模块 → 映射 RBD → 挂载。

#### 步骤 1：创建挂载脚本

```bash
cat > /usr/local/bin/ceph-rbd-mount.sh << 'SCRIPT'
#!/bin/bash
# Ceph RBD 自动映射与挂载脚本
# 依赖：Ceph 集群必须已正常运行

set -e

# 1. 加载 rbd 内核模块
modprobe rbd 2>/dev/null || echo "rbd module already loaded or unavailable"

# 2. 等待 Ceph 集群就绪（超时 60 秒）
for i in $(seq 1 12); do
    if ceph health 2>/dev/null | grep -q HEALTH_OK; then
        break
    fi
    echo "waiting for Ceph cluster... (${i}/12)"
    sleep 5
done

# 3. 映射 RBD 镜像（如果尚未映射）
if ! rbd showmapped 2>/dev/null | grep -q mypool/my-disk; then
    rbd map mypool/my-disk
    echo "RBD image mapped"
else
    echo "RBD image already mapped"
fi

# 4. 等待设备文件出现
RBD_DEV=""
for i in $(seq 1 10); do
    RBD_DEV=$(rbd showmapped 2>/dev/null | grep "mypool.*my-disk" | awk '{print $5}')
    if [ -n "$RBD_DEV" ] && [ -b "$RBD_DEV" ]; then
        break
    fi
    sleep 2
done

# 5. 挂载（如果尚未挂载）
if [ -n "$RBD_DEV" ] && [ -b "$RBD_DEV" ]; then
    mkdir -p /mnt/ceph-rbd
    if ! mount | grep -q "/mnt/ceph-rbd"; then
        mount "$RBD_DEV" /mnt/ceph-rbd
        echo "Mounted $RBD_DEV -> /mnt/ceph-rbd"
    else
        echo "Already mounted"
    fi
else
    echo "ERROR: RBD device not found"
    exit 1
fi
SCRIPT

chmod +x /usr/local/bin/ceph-rbd-mount.sh
```

#### 步骤 2：创建 systemd 服务单元

```bash
cat > /etc/systemd/system/ceph-rbd-mount.service << 'SERVICE'
[Unit]
Description=Mount Ceph RBD block device
After=network-online.target
Wants=network-online.target
# 在 Ceph 容器服务之后启动（如果启用了 systemd 跟踪）
After=ceph.target 2>/dev/null || true

[Service]
Type=oneshot
ExecStart=/usr/local/bin/ceph-rbd-mount.sh
RemainAfterExit=yes
TimeoutStartSec=90

[Install]
WantedBy=multi-user.target
SERVICE
```

#### 步骤 3：启用并启动服务

```bash
systemctl daemon-reload
systemctl enable ceph-rbd-mount.service
systemctl start ceph-rbd-mount.service
```

#### 步骤 4：验证

```bash
systemctl status ceph-rbd-mount.service
# 应显示 active (exited)

mount | grep ceph-rbd
# 应显示 /dev/rbd0 on /mnt/ceph-rbd

df -h /mnt/ceph-rbd
# 15G 容量正常显示

echo "Persistent mount test" > /mnt/ceph-rbd/after-reboot.txt
```

---

### 方案 B：fstab + _netdev（精简，但可靠性较低）

> 仅靠 fstab 不够，因为 rbd 映射必须在挂载前完成。以下方案需要结合方案 A 的脚本。

#### 步骤 1：确保 rbd 内核模块开机加载

```bash
echo 'rbd' > /etc/modules-load.d/rbd.conf
```

#### 步骤 2：加入 fstab 条目

```bash
# 先确认设备名
RBD_DEV=$(rbd showmapped | grep "mypool.*my-disk" | awk '{print $5}')
echo "设备路径: $RBD_DEV"

# 获取 UUID（可选，更稳定）
blkid "$RBD_DEV"

# 追加到 fstab（使用 _netdev 标记网络设备）
echo "$RBD_DEV /mnt/ceph-rbd ext4 defaults,_netdev,nofail 0 0" >> /etc/fstab
```

> ⚠ **局限**：先有 `rbd map` 才有 `/dev/rbd0`。fstab 无法驱动 rbd map。**即使配了 fstab，仍需要映射脚本先执行**。因此方案 A 更完整。

---

## 三、完整排查流程

| 排查步骤 | 命令 | 判断依据 |
|---------|------|---------|
| 1. 检查挂载 | `mount \| grep rbd` | 无输出则未挂载 |
| 2. 检查 RBD 映射 | `rbd showmapped` | 无输出则未映射 |
| 3. 检查内核模块 | `lsmod \| grep rbd` | 无输出则模块未加载 |
| 4. 检查 Ceph 状态 | `ceph -s` | HEALTH_OK 是挂载的前提 |
| 5. 检查模块自加载 | `cat /etc/modules-load.d/rbd.conf` | 文件不存在或内容不对 |
| 6. 检查 fstab | `cat /etc/fstab \| grep rbd` | 无对应条目 |
| 7. 检查系统日志 | `dmesg \| grep -i rbd` | 查看内核是否有相关错误 |
| 8. 检查启动日志 | `journalctl -b | grep -i "rbd\|ceph-rbd"` | 查看服务启动顺序 |

---

## 四、各配置项的持久性对比

| 操作 | 生命周期 | 持久化方式 | 重启后是否保留 |
|------|---------|-----------|--------------|
| `modprobe rbd` | 当前运行态 | 写入 `/etc/modules-load.d/rbd.conf` | ✅ 是 |
| `rbd map ...` | 当前运行态 | 需要脚本或服务在启动时自动执行 | ❌ 否 |
| `mount /dev/rbd0 /mnt/ceph-rbd` | 当前运行态 | fstab 条目 + 前置映射 | ❌ 否（缺映射） |
| `mkdir -p /mnt/ceph-rbd` | 文件系统 | 目录本身持久化 | ✅ 是（但空挂载点） |

---

## 五、完整一键配置（汇总）

将以下内容一次性执行，即可完成全套持久化配置：

```bash
# ===== 1. 设置内核模块自动加载 =====
echo 'rbd' > /etc/modules-load.d/rbd.conf

# ===== 2. 创建挂载脚本 =====
cat > /usr/local/bin/ceph-rbd-mount.sh << 'SCRIPT'
#!/bin/bash
set -e
modprobe rbd 2>/dev/null || true
for i in $(seq 1 12); do
    if ceph health 2>/dev/null | grep -q HEALTH_OK; then break; fi
    sleep 5
done
if ! rbd showmapped 2>/dev/null | grep -q mypool/my-disk; then
    rbd map mypool/my-disk
fi
RBD_DEV=$(rbd showmapped 2>/dev/null | grep "mypool.*my-disk" | awk '{print $5}')
if [ -n "$RBD_DEV" ] && [ -b "$RBD_DEV" ]; then
    mkdir -p /mnt/ceph-rbd
    mount "$RBD_DEV" /mnt/ceph-rbd || true
fi
SCRIPT
chmod +x /usr/local/bin/ceph-rbd-mount.sh

# ===== 3. 创建 systemd 服务 =====
cat > /etc/systemd/system/ceph-rbd-mount.service << 'SERVICE'
[Unit]
Description=Mount Ceph RBD block device
After=network-online.target
Wants=network-online.target

[Service]
Type=oneshot
ExecStart=/usr/local/bin/ceph-rbd-mount.sh
RemainAfterExit=yes
TimeoutStartSec=90

[Install]
WantedBy=multi-user.target
SERVICE

# ===== 4. 启用服务 =====
systemctl daemon-reload
systemctl enable ceph-rbd-mount.service
systemctl start ceph-rbd-mount.service

# ===== 5. 验证 =====
echo "=== 挂载状态 ==="
mount | grep ceph-rbd
echo "=== 映射状态 ==="
rbd showmapped 2>/dev/null | grep my-disk
echo "=== 服务状态 ==="
systemctl is-enabled ceph-rbd-mount.service
```

---

## 六、测试验证

配置完成后，可以模拟重启验证：

```bash
# 方法一：不重启，先卸载再触发服务（验证脚本逻辑）
umount /mnt/ceph-rbd
rbd unmap mypool/my-disk
systemctl start ceph-rbd-mount.service
mount | grep ceph-rbd    # 应恢复

# 方法二：完全重启验证
reboot
# 重启后检查
mount | grep ceph-rbd    # 应自动挂载
df -h /mnt/ceph-rbd     # 15G 可用
```
