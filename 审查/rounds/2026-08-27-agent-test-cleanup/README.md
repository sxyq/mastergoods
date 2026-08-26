# Agent 测试资产清理记录

本轮范围仅限 Agent 专项测试活动资产。用户在 2026-08-27 以“继续”确认推进此前已说明的清理范围。

## 删除范围

- `testing/Agent/` 下旧方案、台账、脚本、审计表、性能表、逆向测试表和阶段报告。
- `testing/scripts/` 中列出的 Agent 专用执行、并发、长会话、台账和分析脚本。
- `Code/backend/tools/` 中列出的 Agent 证据采集与设备采集脚本。
- `docs/05_测试与验收/` 中列出的四份 Agent 专用验收说明。

## 保留范围

- `testing/.artifacts/` 的原始 HTTP、SSE、审计、工具轨迹、数据库前后状态和设备证据。
- 后端、Android、iOS、Web 源码及源码测试。
- 通用测试脚本、通用测试说明和用户当前未提交的修改。
- Git 历史中的旧文件内容。删除前的 checkpoint commit 记录本清单和清理范围。

## 约束

- 不删除业务代码、数据库迁移、配置、部署文件、APK、JAR、缓存或数据目录。
- 不打印、保存或提交 Token、Cookie、密码、私钥和完整认证载荷。
- 清理后创建唯一入口 `testing/Agent/Agent综合功能与性能测试方案.md`。
