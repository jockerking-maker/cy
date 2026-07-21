#!/usr/bin/env python3
"""
Ceph 19.x (Squid) 全自动部署脚本
目标：3节点 MON+MGR+OSD, 3副本模式
节点: ceph1 (192.168.12.176), ceph2 (192.168.12.90), ceph3 (192.168.12.169)
"""
import paramiko
import time
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed

# ====================== 配置 ======================
NODES = {
    "ceph1": {"ip": "192.168.12.176", "data_disk": "/dev/sdb"},
    "ceph2": {"ip": "192.168.12.90",  "data_disk": "/dev/sdb"},
    "ceph3": {"ip": "192.168.12.169", "data_disk": "/dev/sdb"},
}
ROOT_PASS = "1"
PS_PASS = "1"
SSH_PORT = 22
HTTP_PROXY = "http://192.168.12.187:7897"
PROXY_ENV = f"export http_proxy={HTTP_PROXY} https_proxy={HTTP_PROXY}; "
TIMEOUT = 60
LONG_TIMEOUT = 600   # 10 min for apt operations
BOOTSTRAP_TIMEOUT = 1800  # 30 min for container pull + bootstrap


# ====================== SSH 工具 ======================
def _ssh_exec(client, cmd, timeout, label):
    prefix = f"[{label}] " if label else ""
    print(f"  {prefix}$ {cmd}", flush=True)
    stdin, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    exit_code = stdout.channel.recv_exit_status()
    out = stdout.read().decode("utf-8", errors="replace").strip()
    err = stderr.read().decode("utf-8", errors="replace").strip()
    if exit_code != 0:
        print(f"  {prefix}✗ 退出码={exit_code}", flush=True)
        if out: print(f"  {prefix}  stdout: {out[:500]}", flush=True)
        if err: print(f"  {prefix}  stderr: {err[:500]}", flush=True)
        return False, f"命令失败 [{label}]: {cmd}\n{err[:500]}"
    if out:
        for line in out.split("\n")[-3:]:
            print(f"  {prefix}  {line}", flush=True)
    return True, out


def run_cmd(host, cmds, timeout=TIMEOUT, label="", username="root", password=ROOT_PASS):
    """在远程主机上以指定用户执行一组命令"""
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        client.connect(hostname=host, port=SSH_PORT, username=username,
                       password=password, timeout=10, allow_agent=False,
                       look_for_keys=False)
        for cmd in cmds:
            if username != "root":
                cmd = f"sudo bash -c '{cmd.replace(chr(39), chr(39)+chr(34)+chr(39)+chr(34)+chr(39))}'"
            ok, msg = _ssh_exec(client, cmd, timeout, label)
            if not ok:
                client.close()
                return False, msg
        client.close()
        return True, ""
    except Exception as e:
        print(f"  [{label}] ✗ SSH异常: {e}", flush=True)
        try:
            client.close()
        except:
            pass
        return False, str(e)


def run_cmd_simple(host, cmd, timeout=TIMEOUT, label="", username="root", password=ROOT_PASS):
    return run_cmd(host, [cmd], timeout, label, username, password)


# ====================== 阶段 0：配置 root SSH 访问 ======================
def step0_enable_root_ssh(hostname, ip):
    """通过 ps 用户(有 sudo) 配置 root 密码并允许 SSH"""
    print(f"\n  → [{hostname}] 通过 ps 用户配置 root SSH 访问...", flush=True)
    
    SUDO = 'echo "1" | sudo -S'
    SSHD = "/etc/ssh/sshd_config"
    cmds = [
        # 设置 root 密码（sudo -S 读密码, chpasswd 读 stdin 分开处理）
        f'echo "1" | sudo -S bash -c \'echo "root:1" | chpasswd\'',
        # 允许 root SSH 登录
        f'{SUDO} sed -i "s/^#PermitRootLogin.*/PermitRootLogin yes/" {SSHD}',
        f'{SUDO} sed -i "s/^PermitRootLogin.*/PermitRootLogin yes/" {SSHD}',
        # 确保密码认证开启
        f'{SUDO} sed -i "s/^#PasswordAuthentication.*/PasswordAuthentication yes/" {SSHD}',
        f'{SUDO} sed -i "s/^PasswordAuthentication no/PasswordAuthentication yes/" {SSHD}',
        # Ubuntu 24.04 上 SSH 服务名为 ssh（不是 sshd）
        f'{SUDO} systemctl restart ssh',
    ]
    
    # 需要逐个执行，因为 sudo bash -c 不能正确处理多行复合命令
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        client.connect(hostname=ip, port=SSH_PORT, username="ps",
                       password=PS_PASS, timeout=10, allow_agent=False,
                       look_for_keys=False)
        for cmd in cmds:
            label_t = f"{hostname}-rootssh"
            print(f"  [{label_t}] $ {cmd}", flush=True)
            stdin, stdout, stderr = client.exec_command(cmd, timeout=TIMEOUT)
            exit_code = stdout.channel.recv_exit_status()
            out = stdout.read().decode("utf-8", errors="replace").strip()
            err = stderr.read().decode("utf-8", errors="replace").strip()
            if exit_code != 0:
                print(f"  [{label_t}] ✗ 退出码={exit_code}", flush=True)
                if out: print(f"  [{label_t}]   stdout: {out[:300]}", flush=True)
                if err: print(f"  [{label_t}]   stderr: {err[:300]}", flush=True)
                client.close()
                return hostname, False, f"{cmd} 失败: {err[:200]}"
        client.close()
        return hostname, True, ""
    except Exception as e:
        print(f"  [{hostname}] ✗ SSH异常(ps用户): {e}", flush=True)
        try: client.close()
        except: pass
        return hostname, False, str(e)


# ====================== 步骤 1：系统初始化（并行） ======================
def step1_init_node(hostname, ip):
    """在单台节点上执行系统初始化（已用 root 连接）"""
    all_cmds = [
        f"hostnamectl set-hostname {hostname}",
        # 配置 hosts（避免复杂的 heredoc 引号问题）
        "grep -q '192.168.12.176' /etc/hosts 2>/dev/null || echo '192.168.12.176 ceph1' >> /etc/hosts",
        "grep -q '192.168.12.90' /etc/hosts 2>/dev/null || echo '192.168.12.90 ceph2' >> /etc/hosts",
        "grep -q '192.168.12.169' /etc/hosts 2>/dev/null || echo '192.168.12.169 ceph3' >> /etc/hosts",
        "swapoff -a",
        r"sed -i '/swap/s/^/#/' /etc/fstab",
        # 清除之前可能残留的错误 ceph 源（以免 apt update 404）
        'rm -f /etc/apt/sources.list.d/ceph.list',
        "apt update -y",
        "apt install -y podman lvm2",
        "apt install -y chrony",
        "systemctl enable --now chrony",
        "ufw disable || true",
        "apt install -y apt-transport-https ca-certificates curl gnupg lsb-release",
        "chronyc tracking 2>/dev/null | head -3",
    ]
    success, err = run_cmd(ip, all_cmds, LONG_TIMEOUT, hostname)
    return hostname, success, err


# ====================== 步骤 2：SSH 免密配置 ======================
def step2_ssh_keys():
    ceph1_ip = NODES["ceph1"]["ip"]
    
    # 生成密钥（先删旧密钥避免交互式覆盖提示）
    run_cmd_simple(ceph1_ip,
        'rm -f /root/.ssh/id_rsa /root/.ssh/id_rsa.pub && ssh-keygen -t rsa -N "" -f /root/.ssh/id_rsa -q',
        label="ceph1-SSH密钥")
    
    # 安装 sshpass
    run_cmd_simple(ceph1_ip, "apt install -y sshpass", LONG_TIMEOUT, "ceph1-sshpass")
    
    # 分发公钥
    for hostname in ["ceph1", "ceph2", "ceph3"]:
        ip = NODES[hostname]["ip"]
        print(f"  → 分发公钥到 {hostname} ({ip})...", flush=True)
        cmd = f'sshpass -p "{ROOT_PASS}" ssh-copy-id -o StrictHostKeyChecking=no root@{ip}'
        run_cmd_simple(ceph1_ip, cmd, label=f"ceph1→{hostname}")
    
    # 验证
    print("\n  → 验证 SSH 免密登录:", flush=True)
    for hostname in ["ceph1", "ceph2", "ceph3"]:
        ip = NODES[hostname]["ip"]
        run_cmd_simple(ceph1_ip, f"ssh -o StrictHostKeyChecking=no root@{ip} hostname",
                       label=f"验证-{hostname}")


# ====================== 步骤 3：安装 cephadm + bootstrap ======================
def step3_cephadm_bootstrap():
    ceph1_ip = NODES["ceph1"]["ip"]
    
    # 安装 cephadm（apt 的 jammy 包是 zipapp，需要包装脚本）
    cmds = [
        # 已有旧源则直接 apt install
        "apt install -y cephadm 2>/dev/null",
        # 如果没有，设 jammy 源再装
        'grep -q "cephadm" /usr/sbin/ 2>/dev/null || ('
        '  echo "deb [trusted=yes] https://download.ceph.com/debian-squid/ jammy main" > /etc/apt/sources.list.d/ceph.list'
        '  && apt update 2>/dev/null && apt install -y cephadm'
        ')',
        # 创建包装脚本（zipapp 无法直接执行，一行创建）
        "printf '#!/bin/bash\\nexec python3 /usr/sbin/cephadm \"$@\"\\n' > /usr/local/bin/cephadm",
        "chmod +x /usr/local/bin/cephadm",
        # 验证
        "cephadm version",
    ]
    success, err = run_cmd(ceph1_ip, cmds, LONG_TIMEOUT, "ceph1-cephadm安装")
    if not success:
        return False, f"cephadm安装失败: {err}"
    
    # bootstrap 需要拉 quay.io 容器镜像，挂代理
    print("\n  ⚠ bootstrap（代理下载 quay.io 镜像 ~1.3GB，需要几分钟）...", flush=True)
    success, err = run_cmd_simple(ceph1_ip,
        f"{PROXY_ENV} cephadm bootstrap --mon-ip 192.168.12.176",
        BOOTSTRAP_TIMEOUT, "ceph1-bootstrap")
    if not success:
        return False, f"bootstrap失败: {err}"
    
    run_cmd_simple(ceph1_ip, "cephadm shell ceph -s", label="ceph1-初始状态")
    
    # 3.5 分发 ceph.pub
    print("\n  → 分发 ceph.pub...", flush=True)
    for hostname in ["ceph2", "ceph3"]:
        ip = NODES[hostname]["ip"]
        cmd = f'sshpass -p "{ROOT_PASS}" ssh-copy-id -f -i /etc/ceph/ceph.pub -o StrictHostKeyChecking=no root@{ip}'
        run_cmd_simple(ceph1_ip, cmd, label=f"ceph.pub→{hostname}")
    
    return True, ""


# ====================== 步骤 4：添加节点 ======================
def step4_add_nodes():
    ceph1_ip = NODES["ceph1"]["ip"]
    
    run_cmd_simple(ceph1_ip, "cephadm shell ceph orch host add ceph2 192.168.12.90",
                   LONG_TIMEOUT, "ceph1-add-ceph2")
    run_cmd_simple(ceph1_ip, "cephadm shell ceph orch host add ceph3 192.168.12.169",
                   LONG_TIMEOUT, "ceph1-add-ceph3")
    run_cmd_simple(ceph1_ip, "cephadm shell ceph orch host ls", label="ceph1-主机列表")
    run_cmd_simple(ceph1_ip, "cephadm shell ceph orch apply mon 3",
                   LONG_TIMEOUT, "ceph1-扩展MON")
    
    print("\n  ⏳ 等待 MON 部署（约60秒）...", flush=True)
    for i in range(6):
        time.sleep(10)
        run_cmd_simple(ceph1_ip, "cephadm shell ceph mon stat 2>&1",
                       label=f"ceph1-mon-{i+1}")


# ====================== 步骤 5：OSD ======================
def step5_add_osds():
    ceph1_ip = NODES["ceph1"]["ip"]
    
    for hostname, info in NODES.items():
        print(f"  → 清空 {hostname} {info['data_disk']}...", flush=True)
        run_cmd_simple(info["ip"], f"wipefs -a {info['data_disk']}", label=f"{hostname}-wipefs")
    
    run_cmd_simple(ceph1_ip, "cephadm shell ceph orch device ls", label="ceph1-设备列表")
    
    for hostname, info in NODES.items():
        print(f"  → 在 {hostname} 添加 OSD ({info['data_disk']})...", flush=True)
        run_cmd_simple(ceph1_ip,
            f"cephadm shell ceph orch daemon add osd {hostname}:{info['data_disk']}",
            LONG_TIMEOUT, f"addOSD-{hostname}")
    
    print("\n  ⏳ 等待 OSD 上线（约90秒）...", flush=True)
    for i in range(9):
        time.sleep(10)
        run_cmd_simple(ceph1_ip, "cephadm shell ceph osd tree 2>&1",
                       label=f"ceph1-osd-{i+1}")


# ====================== 步骤 6：验证 ======================
def step6_verify():
    ceph1_ip = NODES["ceph1"]["ip"]
    print("\n" + "="*55, flush=True)
    print("  最终验证", flush=True)
    print("="*55, flush=True)
    run_cmd_simple(ceph1_ip, "cephadm shell ceph -s", label="ceph-s")
    run_cmd_simple(ceph1_ip, "cephadm shell ceph osd tree", label="osd-tree")
    run_cmd_simple(ceph1_ip, "cephadm shell ceph mon stat", label="mon-stat")
    run_cmd_simple(ceph1_ip, "cephadm shell ceph mgr stat", label="mgr-stat")
    run_cmd_simple(ceph1_ip, "cephadm shell ceph mgr services 2>/dev/null", label="dashboard")


# ====================== 清理旧集群 ======================
def cleanup_old_cluster():
    ceph1_ip = NODES["ceph1"]["ip"]
    print("  → 检查是否有旧集群...", flush=True)
    
    s, out = run_cmd_simple(ceph1_ip, "cephadm ls 2>/dev/null | head -20", label="ceph1-检查")
    if s and out:
        s2, out2 = run_cmd_simple(ceph1_ip, "ceph fsid 2>/dev/null", label="ceph1-fsid")
        fsid = out2.strip() if s2 and out2 else ""
        if fsid:
            print(f"  → 发现旧集群 FSID={fsid}，清理中...", flush=True)
            run_cmd_simple(ceph1_ip, f"cephadm rm-cluster --fsid {fsid} --force",
                           LONG_TIMEOUT, "ceph1-rm-cluster")
    
    for hostname, info in NODES.items():
        cmds = [
            'vgremove -f $(vgs --noheadings -o vg_name 2>/dev/null) 2>/dev/null; true',
            'pvremove -f $(pvs --noheadings -o pv_name 2>/dev/null) 2>/dev/null; true',
            f'wipefs -a {info["data_disk"]} 2>/dev/null; true',
        ]
        run_cmd(info["ip"], cmds, label=hostname)


# ====================== 主流程 ======================
def main():
    print("=" * 55, flush=True)
    print("  Ceph 19.x (Squid) 自动部署", flush=True)
    print("  3节点: ceph1/2/3  3副本", flush=True)
    print("=" * 55, flush=True)
    start_time = time.time()
    
    # ---- 阶段0：配置 root SSH ----
    print("\n【阶段0】配置 root SSH 登录（3节点并行）", flush=True)
    with ThreadPoolExecutor(max_workers=3) as ex:
        fs = {ex.submit(step0_enable_root_ssh, h, n["ip"]): h for h, n in NODES.items()}
        all_ok = True
        for f in as_completed(fs):
            hn, ok, err = f.result()
            s = "✓" if ok else f"✗ {err[:80]}"
            print(f"  [{hn}] root SSH 配置 {s}", flush=True)
            if not ok:
                all_ok = False
        if not all_ok:
            print("  部署终止：root SSH 配置失败", flush=True)
            sys.exit(1)
    
    time.sleep(2)  # 等 sshd 重启完成
    
    # ---- 第0步：清理旧集群 ----
    print("\n【第0步】清理旧集群残留", flush=True)
    cleanup_old_cluster()
    
    # ---- 第1步：系统初始化（并行） ----
    print("\n【第1步】系统初始化（3节点并行）", flush=True)
    with ThreadPoolExecutor(max_workers=3) as ex:
        fs = {ex.submit(step1_init_node, hn, n["ip"]): hn for hn, n in NODES.items()}
        for f in as_completed(fs):
            hn, ok, err = f.result()
            s = "✓ 完成" if ok else f"✗ 失败: {err[:80]}"
            print(f"  [{hn}] {s}", flush=True)
            if not ok:
                print(f"  部署终止", flush=True)
                sys.exit(1)
    
    # ---- 第2步：SSH 免密 ----
    print("\n【第2步】SSH 免密登录", flush=True)
    step2_ssh_keys()
    
    # ---- 第3步：cephadm + bootstrap ----
    print("\n【第3步】安装 cephadm + bootstrap", flush=True)
    ok, err = step3_cephadm_bootstrap()
    if not ok:
        print(f"  部署终止: {err}", flush=True)
        sys.exit(1)
    
    # ---- 第4步：添加节点 ----
    print("\n【第4步】添加节点 + 扩展MON", flush=True)
    step4_add_nodes()
    
    # ---- 第5步：OSD ----
    print("\n【第5步】添加 OSD", flush=True)
    step5_add_osds()
    
    # ---- 第6步：验证 ----
    print("\n【第6步】验证集群", flush=True)
    step6_verify()
    
    elapsed = time.time() - start_time
    print(f"\n{'='*55}", flush=True)
    print(f"  部署完成！耗时 {elapsed/60:.1f} 分钟", flush=True)
    print(f"  Dashboard: https://192.168.12.176:8443", flush=True)
    print(f"  用户名: admin  密码: bootstrap 输出中显示", flush=True)
    print(f"{'='*55}", flush=True)


if __name__ == "__main__":
    sys.stdout.reconfigure(line_buffering=True)
    main()
