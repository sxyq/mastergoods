# Agent Repair A - 2026-08-22

## Scope

本轮只修改 Agent 执行链、答案合成、Agent 单元测试和本目录证据。当前运行基线按 `8.220.206.9` 的 `sxyq27-zhj-api:20260818` 处理；旧 `154.217.241.207` 仅为历史对照。未读取或保存凭据、Cookie、Token、密钥或完整认证载荷。

## 修复

- `ToolPlanner`：依赖查询完成后只剩一个已分类的 create 目标时，先执行正常 `auto` Function Calling；provider 返回终止文本或空响应时，对同一单一注册工具执行一次 `required` Function Calling 兜底。执行仍经过 Schema、权限、owner/store 和 draft-only 检查，服务端不构造业务参数、不直接写业务表。
- `AnswerSynthesizer`：当已完成只读工具确实返回 `query_audit.returned_count > 0`，且 provider 首次没有返回任何正式文本时，使用工具摘要生成可追溯的非空答复。未配置、事实为空、事实不可信数字、JSON/模板或不支持的写入声称仍保持失败。
- `ToolRegistry`：本轮未需新增执行逻辑；已有 required、类型、minimum/maximum、minItems/maxItems、additionalProperties、嵌套对象和实体 ID minimum 校验均通过回归测试确认在工具执行前生效。

## 本地验证

命令：`./Code/backend/gradlew -p Code/backend test --tests 'com.zhihuiji.backend.application.service.v2.agent.*' --tests 'com.zhihuiji.backend.application.service.v2.V2AgentAiServiceTest'`

结果：`200 tests completed, 0 failed`，Gradle `BUILD SUCCESSFUL`。新增本地登记见 `01-local-regression-register.tsv`。

## 当前边界

- 8220 Provider 真实 10 case、双 owner/store、SSE/取消/审计、媒体文件自动清理仍需真实认证上下文；缺少有效 Provider 或第二 owner/store 时记 `Blocked` 或 `Deferred`，本地测试不替代它们。
- 本轮没有执行 PostgreSQL 查询计划、生产迁移、生产部署、浏览器、Android 设备或旧 154 环境测试。
