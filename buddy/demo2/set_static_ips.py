#!/usr/bin/env python3
"""
将三台 Ceph 节点的网络从 DHCP 改为静态 IP，保持 IP 不变。
按 MAC 地址匹配接口，确保配置精确绑定。
"""
import paramiko
import time

NODES = [
    {
        "hostname": "ceph1",
        "ip": "192.168.12.176",
        "mac": "00:0c:29:c6:cc:12",
        "iface": "ens33",
    },
    {
        "hostname": "ceph2",
        "ip": "192.168.12.90",
        "mac": "00:0c:29:76:d9:09",
        "iface": "ens33",
    },
    {
        "hostname": "ceph3",
        "ip": "192.168.12.169",
        "mac": "00:0c:29:4b:de:87",
        "iface": "ens33",
    },
]

GATEWAY = "192.168.12.254"
DNS = ["202.96.209.5", "202.96.199.133"]
NETMASK = 24

def netplan_config(node):
    """生成 netplan YAML 配置"""
    return f"""network:
  version: 2
  renderer: NetworkManager
  ethernets:
    {node['iface']}:
      dhcp4: no
      dhcp6: no
      addresses:
        - {node['ip']}/{NETMASK}
      routes:
        - to: default
          via: {GATEWAY}
      nameservers:
        addresses: [{', '.join(f'\"{d}\"' for d in DNS)}]
      match:
        mac: \"{node['mac']}\"
      set-name: \"{node['iface']}\"
"""


def main():
    for node in NODES:
        hostname = node["hostname"]
        ip = node["ip"]
        print(f"\n{'='*50}")
        print(f"  配置 {hostname} ({ip})")
        print(f"{'='*50}")

        client = paramiko.SSHClient()
        client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        client.connect(ip, port=22, username="root", password="1", timeout=10)

        # 1. 备份现有 netplan 配置
        print(f"  → 备份现有 netplan 配置...")
        stdin, stdout, stderr = client.exec_command(
            "cp /etc/netplan/01-netcfg.yaml /etc/netplan/01-netcfg.yaml.bak 2>/dev/null; "
            "ls /etc/netplan/",
            timeout=10
        )
        print(f"     /etc/netplan/ 内容: {stdout.read().decode().strip()}")

        # 2. 写入新的静态 IP 配置
        print(f"  → 写入静态 IP 配置...")
        config = netplan_config(node)
        with client.open_sftp() as sftp:
            with sftp.open("/etc/netplan/01-netcfg.yaml", "w") as f:
                f.write(config)
        print(f"     配置已写入 /etc/netplan/01-netcfg.yaml")

        # 3. 验证配置语法
        print(f"  → 验证 netplan 语法...")
        stdin, stdout, stderr = client.exec_command("netplan generate 2>&1", timeout=15)
        out = stdout.read().decode().strip()
        err = stderr.read().decode().strip()
        if err and "Warning" not in err:
            print(f"     ✗ netplan 语法错误: {err}")
            print(f"     跳过此节点")
            client.close()
            continue
        print(f"     ✓ netplan 语法正确")

        # 4. 应用配置
        print(f"  → 应用 netplan 配置（网络会短暂中断）...")
        stdin, stdout, stderr = client.exec_command("netplan apply 2>&1", timeout=30)
        out = stdout.read().decode().strip()
        err = stderr.read().decode().strip()
        if err:
            print(f"     提示: {err[:200]}")

        client.close()
        print(f"  ✓ {hostname} 配置完成")

    # 5. 等待各节点网络恢复
    print(f"\n{'='*50}")
    print(f"  等待网络恢复（10 秒）...")
    time.sleep(10)

    # 6. 验证所有节点
    print(f"{'='*50}")
    print(f"  验证所有节点")
    print(f"{'='*50}")
    all_ok = True
    for node in NODES:
        hostname = node["hostname"]
        ip = node["ip"]
        try:
            client = paramiko.SSHClient()
            client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
            client.connect(ip, port=22, username="root", password="1", timeout=10)

            # 验证 IP
            stdin, stdout, stderr = client.exec_command(
                "ip -4 addr show ens33 | grep inet | awk '{print $2}'", timeout=10
            )
            current_ip = stdout.read().decode().strip()
            expected = f"{ip}/{NETMASK}"

            # 验证默认路由
            stdin2, stdout2, stderr2 = client.exec_command(
                "ip route show default | awk '{print $3}'", timeout=10
            )
            gateway = stdout2.read().decode().strip()

            # 验证连通性（ping 网关）
            stdin3, stdout3, stderr3 = client.exec_command(
                f"ping -c 1 -W 2 {GATEWAY} 2>&1 | grep '1 received' || echo 'FAIL'", timeout=10
            )
            ping_gw = stdout3.read().decode().strip()

            status = "✓" if current_ip == expected else "✗"
            print(f"  [{hostname}] {status} IP: {current_ip} (期望: {expected})")
            print(f"             路由: 网关 {gateway}")
            print(f"             连通: ping 网关 {'OK' if '1 received' in ping_gw else 'FAIL'}")

            if current_ip != expected:
                all_ok = False

            client.close()
        except Exception as e:
            print(f"  [{hostname}] ✗ 无法连接: {e}")
            all_ok = False

    print(f"\n{'='*50}")
    if all_ok:
        print(f"  ✓ 全部节点静态 IP 配置成功！")
    else:
        print(f"  ⚠ 部分节点验证失败，请手动检查")
    print(f"{'='*50}")


if __name__ == "__main__":
    main()
