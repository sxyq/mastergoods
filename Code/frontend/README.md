# 前端目录索引

本目录按客户端类型承载三个产品端工程，并单独承载 Agent 观测工程；各工程保持自己的构建系统、依赖和测试边界。

| 工程 | 目录 | 构建入口 |
|------|------|----------|
| Android | `Code/frontend/android/` | `settings.gradle.kts`、`app/` |
| iOS | `Code/frontend/ios/` | `ZhihuijiIOS.xcodeproj` |
| Web | `Code/frontend/web/` | `package.json`、`src/` |
| Agent 观测 | `Code/frontend/agent-observability/` | `package.json`、`public/`、`scripts/` |

三端正式代码统一位于 `Code/frontend/`，新文档、脚本和开发入口都使用这里的路径。

测试计划、执行台账和证据统一在根目录 `testing/`；后端联调与运维脚本在 `Code/backend/tools/`。
