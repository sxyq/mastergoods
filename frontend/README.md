# 前端目录索引

本目录按客户端类型承载三个前端工程。各工程保持自己的构建系统、依赖和测试边界。

| 工程 | 目录 | 构建入口 |
|------|------|----------|
| Android | `frontend/android/` | `settings.gradle.kts`、`app/` |
| iOS | `frontend/ios/` | `ZhihuijiIOS.xcodeproj` |
| Web | `frontend/web/` | `package.json`、`src/` |

根目录的 `master-goods-android`、`ios`、`web` 是兼容符号链接。新文档、脚本和开发入口应使用 `frontend/` 下的正式路径。

测试计划、执行台账和证据统一在根目录 `testing/`；后端联调与运维脚本在 `backend/tools/`。
