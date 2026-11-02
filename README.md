# 领签 LingQian · Android

原生 Android 本地签到客户端，面向 **WorkBuddy 国内版** 与 **Qoder CN**。

> **实验性版本 v0.1.0-alpha。** APK 已在 Windows 上成功编译并通过 Android APK v2 签名校验，尚未完成真实 Android 设备启动、两平台账户授权或积分到账测试。不要把构建成功等同于签到已验证。

## 下载

前往 [GitHub Releases](https://github.com/Starlevin/LingQian-Android/releases/tag/v0.1.0-alpha)，下载 `LingQian-v0.1.0-debug.apk`，发送到 Android 手机并安装。

当前仓库为 **Private**，只有具备访问权限并登录 GitHub 的账号能打开下载页面。

## 功能（已实现代码，待真机验证）

- WorkBuddy CN：官方网页授权状态轮询、每日签到状态检查、签到、refreshToken 续期。
- Qoder CN：基于 PKCE S256 的设备授权、积分活动查询与领取。
- 多账号管理、逐账号及全部签到、一键手动领取、操作日志。
- Android Keystore AES-GCM 本地加密保存账号凭据；禁用系统自动备份。
- JobScheduler 后台定期巡检：约 6 小时周期，由系统决定具体执行时间；不保证精确整点。

## 从源码构建

环境：**JDK 17、Android SDK Platform 35**。工程使用 Gradle Wrapper 8.14.3、Android Gradle Plugin 8.8.2、Kotlin 2.0.21。

Windows PowerShell：

```powershell
git clone https://github.com/Starlevin/LingQian-Android.git
cd LingQian-Android
.\gradlew.bat :app:assembleDebug
```

Linux/macOS：

```bash
chmod +x ./gradlew
./gradlew :app:assembleDebug
```

本地编译出的 APK：`app/build/outputs/apk/debug/app-debug.apk`。

GitHub Actions 中的 `Android Debug Build` 工作流为 **手动触发**（workflow_dispatch），不会因每次推送而自动占用云端构建时间。

## 已知限制

- **未进行真实签到联调**，两个平台非官方签到接口可能调整；必须查看官方账号中的最终积分变化。
- 不支持 Qoder 国际版、WorkBuddy 国际版；Qoder 国际版需要额外设备身份校验。
- WorkBuddy 成长中心额外任务链暂未移植。
- 后台任务受 Android 省电策略、网络和有效授权状态影响；关机时无法运行。
- 授权状态轮询目前位于应用进程内，系统回收进程后需要重新发起。
- 本项目没有要求 Root、Shizuku、无障碍或长期运行电脑/云服务器。

## 来源与鸣谢

签到接口与登录协议参考以下 MIT 协议开源项目：

- [techysy/CreditDaddy](https://github.com/techysy/CreditDaddy)
- [88lin/workbuddy-auto-signin](https://github.com/88lin/workbuddy-auto-signin)

仅保存和推送应用源码、工程配置与构建工具。账号令牌、个人密钥、电脑路径配置、Gradle 缓存和调试日志均不应提交到仓库。
