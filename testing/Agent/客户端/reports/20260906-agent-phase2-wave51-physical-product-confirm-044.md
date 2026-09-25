# 2026-09-06 Wave 51 Android 真机商品草稿确认

## 结论

线上两个启用测试账号的密码哈希经事务更新 2 行后，物理设备 `d715a3a4` 通过 Android UI 登录成功。随后在助手新建对话中真实发送三次商品草稿请求，均收到 Agent SSE HTTP 200。前两次输入因 Android 文本输入编码使工具名变形，结果不作为工具链结论；第三次 UI 中明确包含 `create_product`，服务端仍只执行 `product_catalog_lookup`，最终回答明确说明当前没有商品创建工具，未生成 `draft_created`、确认弹窗或正式商品写入。

因此本轮 `AG-CLI-AND-004` 商品草稿创建为 `Failed`；确认成功、重复确认和并发确认未到达，记为 `Blocked`。HTTP 200 和普通回答不作为创建成功依据。

## 真实 UI 操作与证据

- 设备为物理 Android `d715a3a4`，Android 16，未启动模拟器；登录、进入助手、新建对话、输入和发送均通过 UI tree bounds 真实点击。
- 登录后进入助手，输入框 bounds 为 `[355,2164][887,2311]`，发送控件 bounds 为 `[903,2175][1029,2301]`；第三次请求的 UI tree 保留了完整 `create_product` 文本。
- 第三次请求收到 `POST /v2/agent/chat/stream` HTTP 200、`text/event-stream`；UI 展示 1 个查询步骤 `product_catalog_lookup`，回答说明没有创建商品工具，未展示草稿卡片或确认按钮。
- 三个服务端 run 均为 `completed`、`audit_lossy=false`：`eef9f37c-e4c4-48b5-a428-a89505a0e5a1`（1 工具、4 事件，首轮编码错误）、`b22e2f02-3731-44d6-b4c5-7fe2328413b2`（1 工具、11 事件，第二轮编码错误）、`01d677f5-43d3-4fa9-bfb5-fbc113ea9918`（1 工具、18 事件，正确包含工具名但仍路由查询）。事件摘要分别包含 `error/run_failed`、查询完成和回答完成；没有 `draft_created` 或确认事件。
- App 日志已脱敏保存，Agent SSE HTTP 200；crash buffer 为 0 行。运行模型和 API Key 未写入证据。

## 数据、账号修复与清理

账号修复仅更新了两个后缀为 `9468`、`2002` 且状态为启用的用户密码哈希及更新时间，事务结果为 `UPDATE 2`；没有修改账号状态、其他用户、业务表或 API Key。数据库 BCrypt 校验通过，但公网登录端点的账号值与此前历史候选不同，使用数据库真实账号后 Android 登录成功。

发送后计数为 `users=3`、`stores=2`、`products=693`、`customers=84`、`finance_records=2661`、`agent_conversations=18`、`agent_messages=53`、`agent_drafts=0`、`agent_run_audits=101`、`agent_run_audit_events=1587`、`agent_context_checkpoints=0`。正式业务表无变化。App 已通过真实 UI 返回助手列表并刷新；本轮新会话未在可见会话列表中出现，未使用接口删除，故服务端新增会话/消息保留为待清理边界。随后 `pm clear com.zhihuiji.app` 返回 `Success`，最终登录页可见，crash buffer 为 0 行。

证据目录：`testing/Agent/客户端/artifacts/20260906-agent-phase2-wave51-physical-product-confirm-044/`。关键文件为 `01-before-input-ui.xml`、`03-after-send-3s-ui.xml`、`07-retry-after-send-16s-ui.xml`、`10-exact-tool-after-send-16s-ui.xml`、`11-logcat-safe.txt`、`12-crash-buffer.txt`、`13-server-counts-after.txt`、`19-session-list-ui.xml`、`25-agent-list-final-ui.xml`。

