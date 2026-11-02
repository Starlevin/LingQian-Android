# 领签 · 原生安卓签到客户端 v0.1.0

Kotlin + Android Framework 原生客户端，手机本地运行，无云服务器、Termux 或无障碍权限。

## 已实现
- WorkBuddy 国内版浏览器授权状态轮询、签到状态查询、签到、refreshToken 续期。
- Qoder CN 设备码 PKCE S256 授权、积分活动查询与领取。
- 多账号登录、管理、单账号/全部签到、操作日志。
- Android Keystore AES/GCM 保护账号凭据；禁止自动备份。
- JobScheduler 定期执行（网络约束、六小时轮询，不能保证整点触发）；可开关。

## 构建
JDK17，Android SDK35，Gradle8.14.3，AGP8.8.2，Kotlin2.0.21。
运行 gradle assembleDebug。
安装包：dist/LingQian-v0.1.0-debug.apk（已验证）。

## 局限
未经真实账户授权及实际领奖测试，勿将显示“成功”替代官方到账核验。接口来自第三方逆向，可能变动。
Qoder 国际版需设备风控，WorkBuddy 国际版机制不同，首版均不支持。成长中心补领链未实现。
工作后台轮询不精确，并依赖系统任务调度、省电策略、网络和有效登录态。
登录授权轮询依附进程，浏览器返回时若系统杀死应用需重新发起。
参考项目 MIT：https://github.com/techysy/CreditDaddy ，https://github.com/88lin/workbuddy-auto-signin

