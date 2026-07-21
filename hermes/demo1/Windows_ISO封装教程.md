# 手把手教你封装属于自己的 Windows ISO 镜像

> 让重装系统变得超简单 —— 系统优化、必备资料，全部内置到 ISO 镜像中

---

## 一、为什么要封装自己的 Windows ISO？

每次重装系统后，你都需要：
- 安装各种驱动
- 安装常用软件（浏览器、输入法、压缩工具等）
- 进行系统设置优化
- 安装运行库（.NET、VC++ 等）

**封装属于自己的 ISO 镜像**，就是把这些工作**一次性做完**，以后重装系统直接一步到位。

---

## 二、准备工作

### 所需工具

| 工具 | 用途 | 获取方式 |
|------|------|---------|
| **Windows ISO 镜像** | 系统安装源 | [微软官网下载](https://www.microsoft.com/zh-cn/software-download/windows11) |
| **VMware Workstation Pro** | 虚拟机环境（免费版即可） | [VMware 官网](https://www.vmware.com/products/workstation-player.html) |
| **AnyBurn**（或 NTLite） | ISO 镜像编辑工具 | [AnyBurn 官网](https://www.anyburn.com/)（免费） |
| **MSMG Toolkit**（可选） | 系统精简/集成工具 | [GitHub](https://github.com/msmg-toolkit) |

> ✅ 以上工具均为**免费**或提供免费版本

### 下载 Windows ISO 镜像

```
方法一：微软官网直接下载
  → https://www.microsoft.com/zh-cn/software-download/windows11

方法二：Media Creation Tool
  → 运行工具 → 选择"下载 ISO 文件"

方法三：第三方工具（如 Rufus）
  → 可直接从微软服务器拉取原版镜像
```

---

## 三、详细步骤

### 第一阶段：在虚拟机中安装 Windows

#### 1. 创建虚拟机

```
打开 VMware → 创建新的虚拟机 → 典型安装

关键设置：
  ├─ 客户机操作系统：Microsoft Windows
  ├─ 版本：Windows 10/11（根据你的 ISO）
  ├─ 磁盘大小：至少 60GB
  ├─ 内存：至少 4GB
  └─ 网络：NAT 模式（需要联网下载更新）
```

#### 2. 安装 Windows

```
① 加载下载的 Windows ISO 镜像到虚拟光驱
② 启动虚拟机，按正常流程安装 Windows
③ 建议安装 **Windows 专业版/企业版**（功能更全）
```

#### 3. 进入审核模式（关键步骤）

> **审核模式（Audit Mode）** 是 Windows 部署工具包提供的一种特殊模式，允许你在不创建用户账户的情况下对系统进行自定义。

**进入方法：**
```
在 OOBE（首次开机设置）界面：
  → 按 Ctrl + Shift + F3
  → 系统会自动重启并进入审核模式
  → 此时会弹出 sysprep 工具窗口（**不要关闭它！**）
```

---

### 第二阶段：自定义系统

#### 4. 安装常用软件

在审核模式下，像正常使用一样安装软件：

```
📦 必装软件清单（示例）：
  ├─ 浏览器：Chrome / Edge / Firefox
  ├─ 压缩工具：7-Zip / Bandizip
  ├─ 输入法：搜狗 / 微软拼音
  ├─ 办公软件：Office（可选）
  ├─ 影音播放：PotPlayer / VLC
  ├─ PDF 阅读：SumatraPDF / Adobe Reader
  ├─ 截图工具：Snipaste / PicPick
  ├─ 下载工具：IDM / qBittorrent
  └─ 运行库：VC++ Redistributable、.NET Framework、DirectX
```

#### 5. 系统优化设置

```ini
; === 系统优化清单 ===

; --- 隐私设置 ---
关闭：诊断数据收集
关闭：广告 ID
关闭：开始菜单应用建议
关闭：锁屏 spotlight

; --- 性能优化 ---
关闭：动画效果
关闭：透明效果
关闭：后台应用（保留必要项）
设置：性能优先

; --- 功能调整 ---
关闭：Windows Defender 实时保护（可选）
关闭：自动更新（可选，建议部署后再管理）
开启：文件扩展名显示
开启：隐藏文件显示
设置：默认应用（浏览器、图片查看器等）

; --- 任务栏优化 ---
取消固定：Cortana、Teams、Skype
固定常用：文件资源管理器、浏览器
```

#### 6. 导入注册表优化（可选）

创建 `optimize.reg` 文件，包含常用优化项：

```reg
Windows Registry Editor Version 5.00

; 关闭任务栏上的 Cortana 按钮
[HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Explorer\Advanced]
"ShowCortanaButton"=dword:00000000

; 关闭任务栏上的任务视图按钮
[HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Explorer\Advanced]
"ShowTaskViewButton"=dword:00000000

; 在文件资源管理器中显示文件扩展名
[HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Explorer\Advanced]
"HideFileExt"=dword:00000000

; 关闭 Windows 10 自动更新（专业版）
[HKEY_LOCAL_MACHINE\SOFTWARE\Policies\Microsoft\Windows\WindowsUpdate\AU]
"NoAutoUpdate"=dword:00000001
```

---

### 第三阶段：封装与捕获

#### 7. 运行 sysprep 准备封装

回到之前打开的 **sysprep 工具窗口**（或手动运行 `C:\Windows\System32\Sysprep\sysprep.exe`）：

```
Sysprep 设置：
  ├─ 操作：进入系统全新体验（OOBE）
  ├─ 通用：✅ 勾选（ generalize ）
  └─ 关机选项：关机

点击「确定」开始封装
```

> ⚠️ **重要**：封装完成后虚拟机会自动关机。**不要再次启动虚拟机**，否则需要重新封装。

#### 8. 使用 WinPE 捕获镜像

```
① 用 Windows PE 启动盘启动虚拟机
② 打开命令提示符（CMD）
③ 使用 DISM 工具捕获系统分区：

   dism /Capture-Image /ImageFile:D:\MyWindows.wim /CaptureDir:C:\ /Name:"My Custom Windows" /compress:max

④ 将生成的 MyWindows.wim 复制到宿主机
```

---

### 第四阶段：制作可启动 ISO

#### 9. 提取原版 ISO 文件

```
① 使用 AnyBurn 打开原版 Windows ISO
② 提取所有文件到文件夹，例如：C:\ISO_Workspace\
```

#### 10. 替换 install.wim

```
① 进入 C:\ISO_Workspace\sources\
② 找到 install.wim（或 install.esd）
③ 用你封装的 MyWindows.wim 替换它
④ 重命名为 install.wim
```

#### 11. 重新打包为 ISO

```
使用 AnyBurn：
  → 选择「创建 ISO 镜像」
  → 源文件夹：C:\ISO_Workspace\
  → 文件系统：UDF
  → 标签：Win10_Custom_2024
  → 点击「创建」
```

---

### 第五阶段：制作可启动 U 盘

#### 12. 使用 Rufus 写入 U 盘

```
① 下载 Rufus（https://rufus.ie/）
② 插入 U 盘（建议 16GB+）
③ 选择你刚制作的 ISO 镜像
④ 分区类型：GPT（UEFI）或 MBR（Legacy）
⑤ 点击「开始」
```

---

## 四、验证与测试

### 测试封装结果

```
① 用新制作的 U 盘启动一台真实电脑或虚拟机
② 检查安装流程是否正常
③ 进入桌面后验证：
   ├─ 软件是否已预装
   ├─ 优化是否生效
   ├─ 驱动是否正常
   └─ 系统激活状态
```

### 常见问题排查

| 问题 | 原因 | 解决方法 |
|------|------|---------|
| 安装时提示"Windows 无法安装" | install.wim 超过 4GB（FAT32 限制） | 使用 NTFS 格式的 U 盘，或分割 install.wim |
| 预装软件未出现 | 封装前未在审核模式下安装 | 重新在审核模式下安装后再次封装 |
| 系统激活失效 | 更换了硬件 | 使用数字许可证绑定的微软账号登录 |
| 安装过程卡在 OOBE | sysprep 未正确执行 | 重新封装，确保勾选 generalize |

---

## 五、进阶技巧

### 1. 使用应答文件实现全自动安装

创建 `autounattend.xml` 放在 U 盘根目录，实现**无人值守安装**：

```xml
<?xml version="1.0" encoding="utf-8"?>
<unattend xmlns="urn:schemas-microsoft-com:unattend">
    <settings pass="windowsPE">
        <!-- 自动分区、自动安装 -->
    </settings>
    <settings pass="oobeSystem">
        <!-- 自动创建用户、跳过隐私设置 -->
        <component name="Microsoft-Windows-Shell-Setup">
            <UserAccounts>
                <LocalAccounts>
                    <LocalAccount wcm:action="add">
                        <Password>
                            <Value>（加密密码）</Value>
                            <PlainText>false</PlainText>
                        </Password>
                        <DisplayName>Admin</DisplayName>
                        <Name>Admin</Name>
                        <Group>Administrators</Group>
                    </LocalAccount>
                </LocalAccounts>
            </UserAccounts>
            <AutoLogon>
                <Enabled>true</Enabled>
                <Username>Admin</Username>
            </AutoLogon>
            <OOBE>
                <HideEULAPage>true</HideEULAPage>
                <SkipMachineOOBE>true</SkipMachineOOBE>
                <SkipUserOOBE>true</SkipUserOOBE>
            </OOBE>
        </component>
    </settings>
</unattend>
```

### 2. 使用 NTLite 精简系统

```
NTLite 可以：
  ├─ 移除 Windows 自带应用（Xbox、OneDrive 等）
  ├─ 集成驱动和更新补丁
  ├─ 预设注册表优化
  └─ 直接编辑 install.wim 无需虚拟机
```

### 3. 多版本合一 ISO

```
将多个版本的 install.wim 合并：
  dism /Export-Image /SourceImageFile:install.wim /SourceIndex:1 /DestinationImageFile:combined.wim
  dism /Export-Image /SourceImageFile:install.wim /SourceIndex:2 /DestinationImageFile:combined.wim
  ...
```

---

## 六、工具下载汇总

| 工具 | 用途 | 官网 |
|------|------|------|
| **VMware Workstation Player** | 虚拟机 | https://www.vmware.com/products/workstation-player.html |
| **AnyBurn** | ISO 编辑 | https://www.anyburn.com/ |
| **Rufus** | 启动 U 盘制作 | https://rufus.ie/ |
| **NTLite** | 系统精简（付费） | https://www.ntlite.com/ |
| **MSMG Toolkit** | 系统工具包（免费） | https://msmgtoolkit.in/ |
| **Windows ADK** | 部署工具 | https://learn.microsoft.com/zh-cn/windows-hardware/get-started/adk-install |
| **DISM++** | 系统优化工具 | https://github.com/Chuyu-Team/Dism-Multi-language |

---

## 七、总结

封装自己的 Windows ISO 镜像的核心流程：

```
下载原版 ISO → 虚拟机安装 → 进入审核模式
  → 安装软件 + 优化设置 → sysprep 封装
  → 捕获 install.wim → 替换原版 → 打包 ISO → 制作启动盘
```

**优点总结：**
- ✅ **一次配置，永久使用** — 重装系统只需 20 分钟
- ✅ **无捆绑软件** — 只装你需要的，不像某些 Ghost 系统
- ✅ **安全可靠** — 基于微软原版镜像，无后门风险
- ✅ **可定制** — 按需增减软件和优化项

> **小提示**：建议每半年更新一次封装镜像，集成最新的 Windows 更新补丁和软件版本。

---

*参考来源：知乎专栏文章《手把手教你封装属于自己的Windows ISO镜像，让重装变得超简单》*
