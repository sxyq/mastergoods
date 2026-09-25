# 2026-09-05 Wave 31-32 物理 Android / DeepSeek

## 环境

- 设备：物理 `d715a3a4`（`25010PN30C`），未启动模拟器。
- App：`com.zhihuiji.app` `1.0.0`。
- 服务端运行配置：`https://oneapi.sxyq27.online/v1`、`deepseek-v4-flash-0731`、`chat_completions`。
- API Key、密码、认证头和完整请求载荷未写入证据。

## 账号与登录

线上 PostgreSQL 中后缀 `9468`、`2002` 的两个启用账号密码已重置为用户指定值；停用的系统遗留 owner 未修改。第一次 SQL 写入因 shell 解析 BCrypt 前缀导致长度异常并被 HTTP 422 暴露，随后使用有效 `$2a$` BCrypt 哈希重新写入。

物理设备依据 UI tree 输入并点击登录，`POST /v1/auth/login` 返回 HTTP 200，App 进入首页并加载门店、报表数据；crash buffer 为 0 行。证据目录：`artifacts/20260905-agent-phase2-wave31-physical-login-reset-019/`。

## AG-CLI-AND-003 多工具与图表

- 近 30 天请求真实点击发送，HTTP 200；同一 run `5256e2b8-0b35-4cf0-b220-98010ada9068` 为 `completed`，工具顺序为 `sales_trend_lookup -> payment_lookup`，24 个连续审计事件，`audit_lossy=false`。销售区间为零值，App 未出现 `result_visualization` 图表。
- 近一年请求真实点击发送，HTTP 200；App 最终显示“暂时无法完成这次请求，请稍后重试”，无图表。该 run 的服务器收尾在 SSH 短暂不可用期间未能取得，保留为证据限制。
- 综合状态：`Failed`。前者验证了多工具真实点击，完整用例的图表展示未通过；后者增加了目标模型下的真实错误态证据。

证据目录：`artifacts/20260905-agent-phase2-wave31-physical-login-reset-019/`。

## AG-CLI-AND-004 商品草稿确认

- 使用物理 UI 真实输入并发送三次创建商品意图；服务器均返回 HTTP 200，但三次均选择 `product_catalog_lookup`，未选择 `create_product`。
- 最后一次 run `9c789c65-2d5e-4243-8b8e-b9496f0b9937` 为 `completed`，13 个连续事件，`tool_count=1`；没有 `draft_created`，`agent_drafts` 保持 0，`products` 保持 693。
- 没有进入确认、重复确认或并发确认分支，不能把这些分支标记为通过。
- 综合状态：`Failed`；重复确认、并发确认：`Blocked`（前置草稿未生成）。

证据目录：`artifacts/20260905-agent-phase2-wave32-physical-product-confirm-020/`。

## 结论

账号恢复和物理设备登录已通过。DeepSeek 运行时的只读请求可以真实完成，但当前创建意图的模型工具选择没有收敛到 `create_product`；图表请求也存在完整链路错误。未修改应用源码；仅重置了线上两个启用测试账号密码并新增本轮测试证据与文档记录。
