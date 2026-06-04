# B11 Android Release Security Checklist

| 字段 | 内容 |
|---|---|
| 时间 | 2026-06-03 17:20 |
| 执行人/agent | Codex |
| 代码状态 | 执行当时未单独归档 `git rev-parse --short HEAD` 与 `git status --short`；当前仅保留本 Markdown 摘要，无法事后精确回填。 |
| 结果 | PASS（静态配置检查） |
| 范围 | Android release 构建配置、签名完整性、混淆、主机白名单、运行时防护、证书绑定入口 |
| 附件 | 无独立截图或运行日志；当前证据由本 Markdown 摘要、下方代码引用与 `20260603-1730-android-assemble-release.md` 组成。 |

## 已核对项

| 项目 | 结论 | 证据 |
|---|---|---|
| release 开启混淆 | 已做 | [app/build.gradle.kts](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/build.gradle.kts) 中 `release { isMinifyEnabled = true }` |
| release 开启资源收缩 | 已做 | [app/build.gradle.kts](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/build.gradle.kts) 中 `release { isShrinkResources = true }` |
| release 签名摘要构建注入 | 已做 | [app/build.gradle.kts](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/build.gradle.kts) 中 `ZHIHUIJI_RELEASE_SIGNING_SHA256` -> `APP_SIGNING_SHA256` |
| APK 签名完整性校验 | 已做 | [SignatureIntegrityChecker.kt](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/src/main/java/com/zhihuiji/app/security/SignatureIntegrityChecker.kt) |
| release 基础地址不可任意编辑 | 已做 | [SettingsStore.kt](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/datastore/src/main/java/com/zhihuiji/core/datastore/SettingsStore.kt) 中 `BASE_URL_EDITABLE` 与 `isTrustedReleaseBaseUrl()` |
| release 仅允许受控 HTTPS 主机 | 已做 | [NetworkModule.kt](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/network/src/main/java/com/zhihuiji/core/network/NetworkModule.kt) 中 `ALLOW_CLEARTEXT_BASE_URL` + `isTrustedReleaseBaseUrl()` 双校验 |
| release 网络日志关闭 | 已做 | [NetworkModule.kt](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/network/src/main/java/com/zhihuiji/core/network/NetworkModule.kt) 中 `NETWORK_LOGGING_ENABLED` 为 false 时 `HttpLoggingInterceptor.Level.NONE` |
| 运行时高风险拦截 | 已做 | [RuntimeSecurityGuard.kt](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/src/main/java/com/zhihuiji/app/security/RuntimeSecurityGuard.kt) 已覆盖 debugger / Frida / root 检测 |
| 证书绑定启用入口 | 已做 | [NetworkModule.kt](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/core/network/src/main/java/com/zhihuiji/core/network/NetworkModule.kt) 中 `buildCertificatePinner()` 与构建常量入口 |

## 仍未由本机动态证明的事项

- 未安装 release 包到真机
- 未验证 release 证书 pin 是否已注入真实值
- 未采集 release 运行期截图、日志或崩溃行为

## 本机已补的动态构建证据

- [20260603-1730-android-assemble-release.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/android/20260603-1730-android-assemble-release.md)

## 备注

- 本证据是静态源码与构建配置检查，不替代真机 release 验收。
- 历史安全加固背景文档见 [android-security-hardening-audit.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/android-security-hardening-audit.md)。
