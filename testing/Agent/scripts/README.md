# Agent 测试脚本目录（scripts）

更新日期：2026-08-28。脚本按类别放在各自子目录，并由对应 `TEST_PLAN.md` 登记用途、输入、输出与版本。

## 一、目录与定位

| 子目录 | 用途 | 登记文档 |
|---|---|---|
| `functional/` | 工具探针、提示词集、SSE 抓包、草稿确认脚本 | functional/TEST_PLAN.md |
| `security/` | 越权/注入/幂等/敏感扫描探针 | security/TEST_PLAN.md |
| `performance/` | 并发、时延、Soak 脚本 | performance/TEST_PLAN.md |
| `unit/` | 单元测试运行与报告收集 | unit/TEST_PLAN.md |
| `contract/` | 端点契约探针与序列化校验 | contract/TEST_PLAN.md |
| `integration/` | 集成链路与数据库准备 | integration/TEST_PLAN.md |
| `reliability/` | 故障注入与断线重连脚本 | reliability/TEST_PLAN.md |
| `data/` | before/after 计数与清理脚本（必须支持 `--owner`，默认拒绝无 owner 的全表清理） | data/TEST_PLAN.md |
| `client/` | ADB/Xcode 观察与 UI 证据采集 | client/TEST_PLAN.md |

## 二、登记格式

每个脚本在对应 TEST_PLAN.md 的用例行或脚本清单中登记：路径、用途、输入参数、输出位置、版本号。

## 三、安全约束

- 脚本不得包含 Token、Cookie、密码、私钥、API key、模型密钥或完整认证载荷；密钥一律通过环境变量注入。
- 数据清理脚本禁止无 owner 条件的全表删除；生产连接默认拒绝执行。
- 脚本与输出可提交 Git 前按 README 第五节脱敏规范扫描。