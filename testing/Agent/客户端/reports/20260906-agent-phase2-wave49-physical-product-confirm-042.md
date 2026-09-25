# 2026-09-06 Wave 49 Android 真机商品草稿确认

## 结论

物理设备 `d715a3a4` 恢复后成功通过 Android UI 登录，并真实进入助手、新建对话和发送两次商品草稿请求。两次请求均被模型路由为查询工具，未生成 `create_product` 草稿；中间一次重试准备阶段设备短暂从 ADB 消失，但恢复后第二次请求仍完成。因此 `AG-CLI-AND-004` 商品草稿创建本轮为 `Failed`；重复确认和并发确认未到达，继续为 `Blocked`。

## 真实 UI 操作

- 设备：物理 Android `d715a3a4`，Android 16；未启动模拟器。
- 通过 UI tree 定位并点击“助手”和“新建对话”。
- 第一次输入 `Create a new product with code P2REAL041 and name Physical Product 041. Generate a draft only, do not save it.`，真实点击发送中心 `(966,2238)`。
- 2 秒 UI 显示处理中；约 10 秒后显示 `sale_order_lookup`、`已完成` 和“未匹配到销售单”，没有“操作确认”或草稿卡片。
- 第二次准备输入 `Add product code P2REAL042 name Physical Product 042. Draft only. Do not save.` 时设备短暂从 ADB 消失；恢复后重新进入 Agent，发送 `Use the create_product tool...P2REAL043...`，最终仍执行商品查询，未出现草稿。

## 证据与数据

- 第一次 Agent 请求成功进入 SSE 处理并完成普通查询；本轮没有 `create_product`、`draft_created`、确认按钮或确认请求。
- 设备掉线证据：`testing/Agent/客户端/artifacts/20260906-agent-phase2-wave49-physical-product-confirm-042/11-after-retry-adb-recheck.txt`。
- UI 证据：`01-product-input-ui.xml`、`03-after-send-2s-ui.xml`、`05-after-send-10s-ui.xml`、`07-product-retry-input-ui.xml`；对应截图同目录保存。
- 两次发送后服务端只读计数为 `18/49/0/96/1523/0`；业务表为 `products=693`、`customers=84`、`finance_records=2661`。两条 run 均为 `completed`，分别为 `e1956909-4138-44df-b9f6-0cc6ec37402a`（9 个事件）和 `2c8a0e91-c7c1-4341-941c-26af4987a7d5`（24 个事件），事件中没有 `draft_created` 或确认事件。未执行确认、重复确认、并发确认和业务写入。
- 未保存密码、Token、Cookie、API Key 或完整认证载荷；未执行账号密码重置。

## 清理与后续

设备恢复后通过 App 会话列表依据 UI tree 真实删除本轮普通查询会话；最终服务端计数为 `17/47/0/96/1523/0`，业务表仍为 `693/84/2661`。`pm clear` 返回 `Success`，登录页可见，crash buffer 为 0 行。当前 `AG-CLI-AND-004` 的确认成功、重复确认和并发确认仍未收敛。
