# 2026-09-04 Android Wave 22：客户删除同步复测

## 结论

本报告记录 Wave 21 修复后的独立 Android 真实重测，不改写 Wave 21 的历史 `Failed` 记录。

| 用例 | 范围 | 状态 |
|---|---|---|
| `AG-CLI-AND-P2-CUSTOMER-DELETE-SYNC-012` | App 客户删除、同步 Worker、本地 Room 与服务端删除结果 | `Passed` |

客户 `95` 从 Android 客户详情页真实点击删除后，App 发起同步上传、cursor、pull 和 ack 请求；所有请求返回 HTTP `200`，Worker 返回 `SUCCESS`。本地客户行和 outbox 均已清除，服务端客户行消失并生成 `customer/95` tombstone，服务端删除操作为 `applied`。本轮满足客户删除同步用例的通过条件。

## 环境与测试对象

- 设备：`emulator-5554`，Android API 34。
- App：`com.zhihuiji.app`，`versionName=1.0.0`，Activity 为 `com.zhihuiji.app/.MainActivity`。
- API：`sxyq27-zhj-api:20260904T1316-customer-sync-version-48532083`，容器内 JAR SHA-256 为 `dd18ff7de2a3078b3266b2754631a44cbe64227db78ca31559f864a0ae6e9e7d`。
- 数据库迁移：Flyway `43`；PostgreSQL 容器 healthy；公网 `/healthz` 返回 `ok`。
- 测试对象：App 创建的客户名称 `92226012`，服务端客户 ID `95`；手机号为合成测试输入，报告正文不展开，认证字段均已脱敏。
- 删除前服务端目标客户 `sync_version=1`；删除前本地 Room 目标客户 `syncVersion=1`。
- 使用已授权测试账号；账号、密码、Token、Cookie 和 Authorization 载荷未写入报告或证据。

环境与版本证据：`47-environment-final.txt`、`41-server-after-real-delete.txt`。

## 真实点击与同步调用

1. App 通过“档案管理 → 客户”进入客户列表并创建本轮测试客户。第一次点击到本地临时负数 ID，详情请求返回 HTTP `422`，该尝试未计入结果；随后依据 `34-server-card-target-ui.xml` 重新选择服务端客户 ID `95`。
2. `35-server-customer-detail-ui.xml` 和 `36-server-before-real-delete.txt` 确认详情页目标为客户 `95`，详情请求 HTTP `200`。删除控件父节点 bounds 为 `[592,152][688,248]`，按 UI tree 计算中心坐标 `(640,200)`，在 App 内真实点击。
3. 删除后 App 返回客户列表。`43-after-sync-list-ui.xml` 和 `43-after-sync-list.png` 显示搜索目标手机号时为“暂无客户 / 没有匹配的真实客户”。
4. App 日志记录 WorkManager 启动 `com.zhihuiji.data.sync.SyncWorker`，随后实际调用：
   - `POST /v2/sync/upload`：HTTP `200`；
   - `GET /v2/sync/cursor/{clientId}`：HTTP `200`；
   - `POST /v2/sync/pull`：HTTP `200`；
   - `POST /v2/sync/cursor/ack`：HTTP `200`；
   - Worker 结果：`SUCCESS`。

同步日志证据：`40-after-real-delete-logcat-redacted.txt`。其中保留了旧临时 ID 的 `422` 记录，正式删除结果以服务端 ID `95` 的操作和后续数据库核对为准。

## 本地与服务端结果

本地 Room 删除后：

- `customers` 和 `customers_v2` 中均没有目标 ID `95`；
- `sync_outbox` 中没有目标删除操作；
- `sync_conflicts` 中没有目标冲突；
- `sync_remote_records` 保留 `customer|95|delete|...|isDeleted=1` 删除标记。

服务端删除后：

- `customers` 中没有 ID `95`；
- `sync_tombstones` 有一条 `customer|95` 删除记录；
- `sync_change_log` 有一条目标 `delete` 变更；
- `sync_operation_log` 的目标删除操作状态为 `applied`；
- API 容器仍为当前修复镜像，健康检查为 `ok`。

证据：`42-local-after-real-delete.txt`、`41-server-after-real-delete.txt`。服务端查询为只读核对，没有执行补偿删除或数据库写入。

## 清理与最终状态

- 删除结果确认后执行 `pm clear com.zhihuiji.app`，返回 `Success`；
- 重启 `MainActivity` 后回到登录页，`45-final-login-ui.xml` 和 `45-final-login.png` 可见手机号/密码空输入框；
- `46-final-crash-buffer.txt` 行数为 `0`；
- 本轮测试客户已由 App 删除同步链路从服务端移除，没有额外服务端清理请求。

## 证据索引

- 登录和入口：`00-login-ui.xml`、`03-after-login.png`、`10-after-login-clean.png`。
- 创建和选择目标：`21-customers-list-before-create-ui.xml`、`25-after-customer-create-ui.xml`、`34-server-card-target-ui.xml`、`35-server-customer-detail-ui.xml`、`36-server-before-real-delete.txt`、`37-local-before-real-delete.txt`。
- 删除后 App：`38-detail-load-log-redacted.txt`、`39-current-ui.xml`、`39-current.png`、`40-after-real-delete-logcat-redacted.txt`、`43-after-sync-list-ui.xml`、`43-after-sync-list.png`。
- 数据库和运行环境：`41-server-after-real-delete.txt`、`42-local-after-real-delete.txt`、`47-environment-final.txt`。
- 清理收尾：`44-pm-clear.txt`、`45-final-login-ui.xml`、`45-final-login.png`、`46-final-crash-buffer.txt`。

## 验证边界

- 本轮验证的是当前 Android 客户删除同步分支；没有执行 Agent 模型调用、SSE、生图、取消/断线、上下文压缩、性能或 iOS 流程。
- 运行时目标模型 `glm-5.3-flash` 仍未成为实际模型，相关状态继续记为 `Blocked`；这不影响本轮同步删除用例的判定。
- 本轮未修改产品源码；线上只做健康检查、容器和 PostgreSQL 只读核对，模拟器执行了测试数据清理。
