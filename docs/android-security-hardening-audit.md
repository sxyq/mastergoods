# 历史参考

> 这是历史安全加固审计记录，不再作为新版主规范。
> 新版需求请以 `docs/spec/` 为准。

# Android 安全加固审计记录

日期：2026-05-31
项目：`master-goods-android`

## 本轮加固总览

- release 服务器地址入口已收口，debug 仍保留联调能力
- `token/refreshToken` 已改为 Keystore 加密落盘
- 发布版已加入 debugger / Frida / root 高风险运行时拦截
- 发布版已加入 APK 签名完整性校验
- 网络层已加入 release 主机白名单校验
- 证书绑定已补齐可安全启用的构建入口，等待正式 pin 注入
- debug / release 构建均已验证通过

## 本次逆向视角检查

基于当前可安装 APK 与源码配置，从逆向者视角检查了以下面向：

1. APK 体量与可读性
   - `app-debug.apk` 约 19MB
   - 包内存在大量 `classes*.dex`
   - 在加固前，`release` 关闭了混淆与资源收缩，意味着正式版也会暴露完整类名、方法名与较多资源语义

2. Manifest 暴露面
   - 加固前存在 `allowBackup=true`
   - 加固前存在全局 `usesCleartextTraffic=true`
   - 这两项意味着系统备份与明文链路都处于偏宽松状态

3. 网络层暴露面
   - 加固前 OkHttp 日志在所有构建中都输出 `HEADERS`
   - 如果后续有人把基础地址改成 HTTP，正式版没有额外拦截

4. 数据面
   - 会话依赖 `DataStore`
   - 第一轮加固前 token / refreshToken 以普通字符串形式落盘
   - 第二轮已继续推进到“Keystore 加密后再落盘 + 历史明文自动迁移”

## 本次已落地的加固

### 1. Release 混淆与资源收缩

文件：
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/build.gradle.kts`
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/proguard-rules.pro`

措施：
- `release` 开启 `isMinifyEnabled = true`
- `release` 开启 `isShrinkResources = true`
- 增补 Hilt / Retrofit / kotlinx.serialization 相关保留规则

效果：
- 降低正式版静态可读性
- 减少资源语义直接暴露

### 2. 禁止系统备份与数据提取

文件：
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/src/main/AndroidManifest.xml`
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/src/main/res/xml/backup_rules.xml`
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/src/main/res/xml/data_extraction_rules.xml`

措施：
- `allowBackup=false`
- 显式排除根路径备份
- 显式排除云备份与设备迁移提取

效果：
- 降低通过系统备份、迁移、ADB 备份类通道带出应用数据的风险

### 3. 分离 debug / release 网络安全策略

文件：
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/src/debug/res/xml/network_security_config.xml`
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/src/release/res/xml/network_security_config.xml`
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/src/main/AndroidManifest.xml`

措施：
- `debug`：仅对白名单开发地址允许 HTTP
- `release`：默认仅允许受信任 HTTPS

效果：
- 调试联调不被破坏
- 正式版避免任意明文流量

### 4. 关闭正式版网络日志

文件：
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/network/build.gradle.kts`
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/network/src/main/java/com/zhihuiji/core/network/NetworkModule.kt`

措施：
- 通过 `BuildConfig` 区分 debug / release
- `release` 中 OkHttp `HttpLoggingInterceptor.Level.NONE`

效果：
- 避免请求头、鉴权信息、接口路径在正式版运行日志中暴露

### 5. 正式版强制 HTTPS 基础地址

文件：
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/network/build.gradle.kts`
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/network/src/main/java/com/zhihuiji/core/network/NetworkModule.kt`

措施：
- `release` 中如果基础地址不是 HTTPS，直接拒绝

效果：
- 降低正式版被配置到 HTTP 环境后遭遇抓包、降级、中间人风险

### 6. 正式版截图/录屏保护

文件：
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/src/main/java/com/zhihuiji/app/MainActivity.kt`

措施：
- 正式构建启用 `FLAG_SECURE`

效果：
- 降低敏感页面被系统截图、录屏、任务缩略图直接带出的风险

### 7. 会话令牌加密落盘

文件：
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/datastore/src/main/java/com/zhihuiji/core/datastore/SecureSessionCipher.kt`
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/datastore/src/main/java/com/zhihuiji/core/datastore/SessionStore.kt`

措施：
- 新增 Android Keystore + AES/GCM 的会话加密器
- `SessionStore` 写入 token / refreshToken 前先加密
- 读取时透明解密
- 首次读取到历史明文值时，自动迁移成密文

效果：
- 降低通过本地文件、备份残留、沙箱导出直接读取明文会话令牌的风险
- 兼容已有安装数据，不需要用户重新登录才能迁移

### 8. 发布版高风险运行时拦截

文件：
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/src/main/java/com/zhihuiji/app/security/RuntimeSecurityGuard.kt`
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/src/main/java/com/zhihuiji/app/MainActivity.kt`

措施：
- 新增调试器附加检测
- 新增 Frida 默认端口探测
- 新增 `/proc/self/maps` 中 Frida / Gum 痕迹检测
- 发布版在启动早期发现高风险运行时后，直接中止界面初始化
- root 检测能力已落地，但暂不直接作为拦截条件，避免误杀正常设备

效果：
- 提高运行时被直接附加调试、注入 Hook 的成本
- 把高置信风险场景的拦截放在比业务层更早的位置

### 9. 收紧正式环境服务器地址入口

文件：
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/datastore/build.gradle.kts`
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/datastore/src/main/java/com/zhihuiji/core/datastore/SettingsStore.kt`
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/settings/src/main/java/com/zhihuiji/feature/settings/SettingsViewModel.kt`
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/feature/settings/src/main/java/com/zhihuiji/feature/settings/SettingsScreen.kt`

措施：
- 新增构建级 `BASE_URL_EDITABLE` 开关
- debug 允许切换联调地址，release 关闭编辑能力
- release 下 `SettingsStore` 仅接受受控正式 HTTPS 主机，其余地址统一回退到默认正式地址
- 设置页在 release 构建中改为只读展示，不再暴露手工输入和保存按钮

效果：
- 降低通过本地配置、DataStore 篡改、简单 smali patch 把整套业务流量导向任意攻击者服务器的风险
- 把“可改服务器地址”能力从正式版 UI 和持久化层同时收掉

### 10. 补齐刷新链路的正式主机白名单

文件：
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/network/src/main/java/com/zhihuiji/core/network/NetworkModule.kt`
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/network/src/main/java/com/zhihuiji/core/network/TokenAuthenticator.kt`

措施：
- 基础地址拦截器在 release 下新增“受控生产主机”校验
- `TokenAuthenticator` 在发送 `/auth/refresh` 前也做同样校验
- 避免认证器单独 new `OkHttpClient` 形成绕过主拦截器的旁路

效果：
- 正式版不再只是“要求 HTTPS”，而是进一步要求“必须是受控正式主机”
- 收紧刷新 token 这一条最敏感的认证分支

### 11. APK 签名完整性校验

文件：
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/build.gradle.kts`
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/src/main/java/com/zhihuiji/app/security/SignatureIntegrityChecker.kt`
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/src/main/java/com/zhihuiji/app/MainActivity.kt`

措施：
- 构建期写入 `APP_SIGNING_SHA256`
- 运行时读取当前安装包签名并计算 SHA-256
- 发布版启动早期比对白名单签名，不匹配则直接终止界面初始化
- 正式签名摘要可通过 `ZHIHUIJI_RELEASE_SIGNING_SHA256` 注入

效果：
- 提高重打包、二改包、非预期签名替换后的运行门槛
- 把签名完整性从“人工约定”变成“客户端启动时实际校验”

### 12. 证书绑定的安全启用入口

文件：
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/network/build.gradle.kts`
- `/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/network/src/main/java/com/zhihuiji/core/network/NetworkModule.kt`

措施：
- 新增 `ZHIHUIJI_PINNED_HOST` 和 `ZHIHUIJI_CERT_PINS` 构建入口
- debug 默认关闭 pinning
- release 仅在提供真实公钥 pin 时启用 `CertificatePinner`

效果：
- 保持当前构建稳定，不在证书链状态不明确时误锁 release 包
- 一旦拿到正式证书公钥 pin，可无侵入切换到真正的证书绑定

## 仍建议后续补强

1. 注入真实证书公钥 pin
   - 当前已接好 `CertificatePinner` 入口
   - 待正式域名证书链稳定后，向 `ZHIHUIJI_CERT_PINS` 注入真实 `sha256/...` 公钥 pin 即可启用

2. Release 真机验证
   - 建议后续执行 `assembleRelease` / 安装真机
   - 验证混淆、网络策略、截图保护是否都符合预期

3. 反篡改与完整性深化
   - 当前已加签名摘要自校验
   - 后续还可联动 Play Integrity / 服务端设备校验

4. 本地业务库加密
   - 当前 token 已加密，业务库 `zhihuiji.db` 仍未加密
   - 可继续引入 SQLCipher 或高敏字段分级加密
