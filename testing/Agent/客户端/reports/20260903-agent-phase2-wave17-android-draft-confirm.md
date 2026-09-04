# 2026-09-03 Android Wave 17：create_product 草稿确认实测

## 结论

本轮完成 Android App 真实输入、发送、草稿确认点击、拒绝点击和清理核对。核心确认用例为 `Failed`：确认接口返回 HTTP `422`，未完成正式商品写入；拒绝与清理作为独立子用例为 `Passed`。确认成功、重复确认和并发确认均未执行，保持 `Blocked`。

| 用例 | 范围 | 结果 |
|---|---|---|
| `AG-CLI-AND-P2-DRAFT-CONFIRM-001` | 真实生成 `create_product` 草稿并点击“允许一次” | `Failed` |
| `AG-CLI-AND-P2-DRAFT-REJECT-001-RERUN-003` | 确认失败后的真实拒绝、状态核对和清理 | `Passed` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-SUCCESS-001` | 确认成功及正式商品写入 | `Blocked` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-REPEAT-001` | 同一草稿重复确认 | `Blocked` |
| `AG-CLI-AND-P2-DRAFT-CONFIRM-CONCURRENT-001` | 同一草稿并发确认 | `Blocked` |

## 环境与前置

- 设备：`emulator-5554`，Android API 34。
- App：`com.zhihuiji.app` 1.0.0，启动 Activity 为 `com.zhihuiji.app/.MainActivity`。
- 服务：8220 公网 API；实时前置核查显示 Nginx 配置有效，API、PostgreSQL、Redis 和转发服务正常，PostgreSQL 为 `healthy`。匿名健康入口返回 HTTP `401`，属于认证保护，不作为服务故障。
- 运行时模型：`gpt-5.6-luna/chat_completions`；目标 `glm-5.3-flash` 未验证，因此模型目标前置仍为 `Blocked`。
- 账号、门店和会话身份仅以脱敏标签记录。

证据：

- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/40-server-preflight.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/41-nginx-test.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/452-server-preflight-live.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/453-public-healthz-headers-redacted.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/418-app-version-before.txt`

## 核心确认用例：Failed

### 输入和真实点击

最终在 App `EditText` 中核对的输入为：

`新增 一个 商品 商品编码 是 9172603842 名称是 阶段确认商品 先 生成 草稿 不要 直接 保存`

App 真实发送后收到 `POST /v2/agent/chat/stream` HTTP `200`、`text/event-stream`。服务端生成 run `f3cc466a-cef1-4d26-84af-49d4dd0d47fe`，conversation `175`，draft `14`。确认弹窗显示“操作确认”“新建商品：阶段确认商品”和“草稿状态：待确认”；UI tree 读取到的草稿字段为 `code=9172603842`、`name=阶段确认商品`。

依据确认前 UI tree，真实点击“允许一次”父节点 `[392,803][592,899]` 的中心 `(492,851)`，点击时间为 `2026-09-03T08:22:17+0800` 至 `08:22:18+0800`。App 随后真实调用 `POST /v2/agent/drafts/14/confirm`，返回 HTTP `422`，界面显示“请求参数未通过校验，请检查输入内容”。

证据：

- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/524-after-send-app-logcat-redacted.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/525-confirm-before-dump.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/526-confirm-before.xml`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/528-server-before-confirm-evidence.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/529-confirm-coordinate.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/530-confirm-tap-start.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/531-confirm-tap-end.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/538-after-confirm-app-logcat-redacted.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/539-server-after-confirm-failure-evidence.txt`

### 失败原因和数据边界

确认前数据库计数为：

| users | stores | memberships | products | finance_records | conversations | messages | drafts | run_audits | audit_events |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 3 | 2 | 2 | 693 | 2661 | 12 | 29 | 1 | 46 | 532 |

draft `14` 的确认内容中 `category_id` 和 `unit_id` 均为空；当前数据库分类数和单位数均为 `0`，现有商品对应的分类 ID、单位 ID 计数也均为 `0`。因此本次只确认到草稿生成和确认请求失败，没有确认成功或正式商品写入证据。服务端相关审计记录显示 run `f3cc466a-cef1-4d26-84af-49d4dd0d47fe` 保持 `confirmation_pending`，事件序号为 `1..13`。

证据：

- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/518-db-before-clean-confirm.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/539-server-after-confirm-failure-evidence.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/546-db-after-failed-confirm-and-reference-data.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/547-reference-category-unit-check.txt`

## 拒绝和清理：Passed

该结果只代表拒绝分支和测试对象清理成功，不代表确认成功。确认返回 `422` 后，依据最新 UI tree 真实点击“拒绝”父节点 `[260,830][376,926]` 的中心 `(318,878)`，点击时间为 `2026-09-03T08:25:32+0800` 至 `08:25:33+0800`。App 会话详情显示：

- `状态：cancelled · 新建商品：阶段确认商品`
- `运行取消`
- `草稿已取消，未执行任何业务写入`
- 商品编码仍为 `9172603842`，商品名称仍为 `阶段确认商品`

之后通过 App 删除 Wave 17 测试草稿和会话 `173/174/175`。服务端最终核对为：

| conversations | messages | drafts | run_audits | audit_events | products | finance_records |
|---:|---:|---:|---:|---:|---:|---:|
| 10 | 25 | 0 | 47 | 545 | 693 | 2661 |

目标商品编码记录、目标草稿和目标会话均为 `0`。`pm clear com.zhihuiji.app` 返回 `Success`，重启后的 UI tree 回到登录页，清理后的 crash buffer 无记录。正式商品数从确认前的 `693` 到最终的 `693` 未增加。

证据：

- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/536-after-confirm-3s.xml`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/540-reject-after-confirm-failure-coordinate.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/541-reject-after-confirm-failure-tap-start.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/542-reject-after-confirm-failure-tap-end.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/544-after-confirm-failure-reject.xml`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/546-db-after-failed-confirm-and-reference-data.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/572-db-after-cleanup-conversations.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/573-pm-clear.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/579-final-crash-buffer-redacted.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/580-final-login-dump-retry.txt`
- `testing/Agent/客户端/artifacts/20260903-agent-phase2-wave17-AG-CLI-AND-P2-DRAFT-CONFIRM-001/582-db-final-recount.txt`

## 未执行分支：Blocked

### 确认成功

确认请求已因 HTTP `422` 停止，当前分类和单位参考数据为空，未取得正式商品写入结果。解除条件是为当前门店提供可用的分类/单位 ID，或调整并部署确认请求契约后重新生成草稿并实测。

### 重复确认

尚未取得一次确认成功的草稿和正式写入结果，因此没有继续重复点击或重复提交。不能用本轮 `422` 结果替代重复确认证据。

### 并发确认

同一原因，本轮没有可供并发竞争的已确认草稿，未发起并发确认请求。需先完成一次有效确认，再单独采集竞争请求、HTTP 结果、正式表计数和审计证据。

三项均保持 `Blocked`，不把拒绝分支的 `Passed` 扩展为确认成功、重复或并发结论。

## 状态边界

- 本轮只覆盖 Android App 真实点击、服务端草稿确认失败、拒绝和清理；未执行生图、取消/断线、上下文压缩、性能或 iOS。
- 本轮未修改源码、线上服务、数据库或账号配置；本次交付只更新客户端报告和执行台账。
- 报告与台账只保留脱敏身份和请求元数据，未写入敏感认证字段或完整认证内容。
