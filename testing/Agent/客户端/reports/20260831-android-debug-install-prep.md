# Android Debug 安装准备证据

日期：2026-08-31

## 范围

本次只构建、安装并启动本地 Android debug APK。未启动后端，未访问服务器，未登录，未输入账号或密码，未发送 Agent 请求；iOS、浏览器和 Agent 功能测试不在范围内。

## Git 基线

- 命令：`git status --short --branch`
- 结果：`Passed`
- 分支：`codex/publish-local-updates`，相对 `origin/codex/publish-local-updates` 为 `ahead 6`
- 工作树在本次操作前已有 backend、Android、管理后台、文档和 testing 目录修改；本次未暂存、覆盖或删除这些修改。
- 命令：`git diff --stat`
- 结果：`Passed`

## 构建

- 命令：`cd /Users/sunyiyang/Desktop/Project/master-goods/Code/frontend/android && ./gradlew :app:assembleDebug --console=plain --no-daemon`
- 结果：`Passed`
- Gradle 任务：`:app:assembleDebug`
- APK：`/Users/sunyiyang/Desktop/Project/master-goods/Code/frontend/android/app/build/outputs/apk/debug/app-debug.apk`
- APK 大小：35,141,883 bytes
- APK SHA-256：`62a7350e5284ede7ab3a4df2dda22420e47317fc7dd2d1ef09b7b29b8ca15f90`
- `output-metadata.json`：`applicationId=com.zhihuiji.app`，`variantName=debug`，`versionCode=1`，`versionName=1.0.0`
- 构建日志含 AGP 对 `compileSdk=35` 的兼容性提示；本次构建结果仍为 `Passed`。
- APK、JAR、Gradle 缓存和 build 目录未纳入本次证据提交。

## 模拟器

- ADB：`/Users/sunyiyang/Library/Android/sdk/platform-tools/adb`
- Emulator：`/Users/sunyiyang/Library/Android/sdk/emulator/emulator`
- AVD：`Zhihuiji_API34`
- Serial：`emulator-5554`
- `adb devices -l`：`Passed`，设备状态为 `device`
- `getprop sys.boot_completed`：`Passed`，值为 `1`
- `getprop dev.bootcomplete`：`Passed`，值为 `1`
- `getprop ro.build.version.sdk`：`Passed`，值为 `34`
- 安装和启动前已通过 `svc wifi disable`、`svc data disable` 关闭模拟器网络，确保本次不访问服务器。
- `settings get global wifi_on`：`Passed`，值为 `0`
- `settings get global mobile_data`：`Passed`，值为 `0`
- `dumpsys wifi`：`Passed`，状态为 `Wi-Fi is disabled`

## 安装与包信息

- 命令：`adb -s emulator-5554 install -r /Users/sunyiyang/Desktop/Project/master-goods/Code/frontend/android/app/build/outputs/apk/debug/app-debug.apk`
- 结果：`Passed`，输出为 `Success`
- 包名：`com.zhihuiji.app`
- 已安装 `versionCode`：`1`
- 已安装 `versionName`：`1.0.0`
- 已安装 APK SHA-256：`62a7350e5284ede7ab3a4df2dda22420e47317fc7dd2d1ef09b7b29b8ca15f90`
- 本地与设备内 `base.apk` SHA-256 一致。
- launcher Activity 解析：`com.zhihuiji.app/.MainActivity`
- `lastUpdateTime`：`2026-08-31 11:30:07`

## 启动证据

- 命令：`adb -s emulator-5554 shell am start -W -n com.zhihuiji.app/com.zhihuiji.app.MainActivity`
- 结果：`Passed`
- Activity 返回：`Status: ok`，`Activity: com.zhihuiji.app/.MainActivity`
- 前台 Activity：`com.zhihuiji.app/.MainActivity`
- 截图：`testing/Agent/客户端/artifacts/20260830-real-android-wave0/03-startup.png`
- UI XML：`testing/Agent/客户端/artifacts/20260830-real-android-wave0/04-startup-ui.xml`
- UI 摘要：`testing/Agent/客户端/artifacts/20260830-real-android-wave0/06-startup-ui-summary.txt`
- 崩溃缓冲区：`testing/Agent/客户端/artifacts/20260830-real-android-wave0/05-crash-buffer.log`，大小为 0 bytes，结果为 `Passed`
- 启动页为登录界面；本次没有填写或提交登录表单。

## 限制

- 本次只证明当前工作树 debug APK 可构建、可安装并可启动到登录页。
- 未进行登录后的接口、同步、Agent 对话或真实业务流程验证。
- 本次不判断 backend 或管理后台现有修改的构建和运行状态。
