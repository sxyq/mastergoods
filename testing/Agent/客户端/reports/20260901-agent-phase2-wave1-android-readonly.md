# 阶段二 Wave 1 Android 首轮真实实测

## 结论

| 范围 | 结果 |
|---|---|
| 8220 现有账号登录 | `Passed` |
| Android 首页、Agent 工作台和对话页进入 | `Passed` |
| 商品目录只读查询 | `Failed` |
| 近 30 天现金流只读查询 | `Failed` |
| 当前门店只读查询 | `Failed` |
| 本轮创建类工具、取消/断线、恢复、压缩、生图和性能 | `Blocked` |
| iOS | `Deferred` |

三个只读用例均真实进入 Agent SSE 和原生工具链，失败点一致：

```text
InvalidDataAccessApiUsageException: Query requires transaction be in progress, but no transaction is known to be in progress
```

这证明登录、SSE、工具选择、工具执行失败态、审计关联和 Android 展示链路已经走通到查询执行边界；商品、现金流和门店查询均未产生可用业务结果。当前未修改生产源码。

## 环境与账号

| 字段 | 脱敏事实 |
|---|---|
| 目标服务 | `8.220.206.9` / `https://zhj-api.sxyq27.online/` |
| API 镜像 | `sxyq27-zhj-api:20260901T014000-agent-owner-bill-c012290b` |
| 数据库 | PostgreSQL 15，Flyway V42 |
| Provider 运行时 | `gpt-5.6-luna` / `chat_completions` |
| 目标模型 | `glm-5.3-flash`，本轮未切换 |
| Android | `Zhihuiji_API34` / `emulator-5554` / Android API 34 |
| App | `com.zhihuiji.app` / versionName `1.0.0` |
| App API 地址 | `https://zhj-api.sxyq27.online/` |
| 账号范围 | 8220 已存在的两个启用账号，脱敏后缀 `9468`、`2002` |

按用户授权，两个启用账号的登录密码已在 8220 数据库中重置为用户指定值，并分别验证 `/v1/auth/login` 与 `/v2/auth/login` 返回 HTTP 200。系统遗留停用账号未纳入；未创建新账号、门店或业务夹具。密码明文、密码哈希、Session Token、Cookie、Authorization 和完整认证载荷均未写入本报告或证据目录。

## 用例结果

| test_id | 脱敏输入 | 预期工具 | 实际工具与 run | 实际观察 | 结果 |
|---|---|---|---|---|---|
| `AG-CLI-AND-P2-LOGIN-003` | Android 正常登录 | 登录后进入首页、Agent 工作台和对话页 | `/v1/auth/login`、`/v2/auth/login` 返回 200；首页加载真实门店信息；会话抽屉可打开 | 无 crash；进入 Agent 对话页 | `Passed` |
| `AG-CLI-AND-P2-RO-001` | `Show the current product list` | `product_catalog_lookup` | `product_catalog_lookup`；run `c516234a-125d-4578-9d3c-66ef53f01070` | 真实 SSE/工具执行后报事务异常；App 展示失败态和“重新生成”入口 | `Failed` |
| `AG-CLI-AND-P2-RO-002` | 近 30 天现金流查询 | `cashflow_summary_lookup` | `cashflow_summary_lookup`；run `519b96dc-e030-4f1a-b824-d1ef1919ad2d` | 真实 SSE/工具执行后报事务异常；App 展示失败态和无数据说明 | `Failed` |
| `AG-CLI-AND-P2-RO-003` | 当前门店查询 | `store_info_lookup` | `store_info_lookup`；run `ec42de07-d797-4cea-9935-ab57ec0f582b` | 真实 SSE/工具执行后报事务异常；App 展示失败态和“重新生成”入口 | `Failed` |

## 数据、审计与清理

本轮可确认的服务端观测计数如下，数值只作脱敏汇总：

| 对象 | 观测值 |
|---|---:|
| `agent_conversations` | 2 |
| `agent_messages` | 6 |
| `agent_run_audits` | 3 |
| `agent_run_audit_events` | 23 |
| `agent_drafts` | 0 |
| `agent_tasks` | 0 |
| 本轮正式业务写入 | 0（未观察到） |

两个本轮创建的 Agent 会话已执行认证 API 删除；Android 会话抽屉显示“暂无历史会话”。随后执行 `pm clear com.zhihuiji.app` 返回 `Success`，强制停止并重启 App 后回到登录页，crash buffer 为空。最终 SSH 公钥会话无法复用，因此删除会话后的 PostgreSQL 最终计数未再次确认，该项保持未确认，不能标记为 `Passed`。

## 证据

- 首页和商品失败态：`testing/Agent/客户端/artifacts/20260901-agent-phase2-wave1-AG-CLI-AND-LOGIN-003/`
- 现金流失败态 UI 摘要：同一目录 `03-cashflow-failed-ui-summary.txt`
- 门店失败态 UI 摘要：同一目录 `04-store-failed-ui-summary.txt`
- 清理后登录页与崩溃缓冲摘要：同一目录 `05-post-clear-ui-summary.txt`
- 真实工具、run、审计和数据库计数：本报告中的脱敏汇总，原始认证材料未保存

## 下一步

先修复并部署查询工具的事务边界，再从 Android 重跑 `AG-CLI-AND-P2-RO-001` 至 `003`。在此之前不继续创建类工具和后续 Wave；8220 运行模型切换到 `glm-5.3-flash` 需要单独授权。
