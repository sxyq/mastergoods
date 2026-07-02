# B11 2026-06-30 Android / Web 当前状态复验

| 字段 | 内容 |
|---|---|
| 时间 | 2026-06-30 13:26 CST（该时刻的设备状态于 13:47 CST、14:06 CST 再复核两次，结论未变；15:03-15:17 CST 已在新证据中完成真机本地 HTTPS 主链路复验） |
| 执行人/agent | Codex |
| 代码状态 | `HEAD=128a3d56`；工作树为 dirty，详见当次 `git status --short` 摘要 |
| Web 命令 | `cd web && npm run build` |
| Android 命令 | `./gradlew -p master-goods-android :core:datastore:testDebugUnitTest :core:network:testDebugUnitTest :app:assembleDebug` |
| adb 检查 | `/Users/sunyiyang/Library/Android/sdk/platform-tools/adb devices -l` |
| 结果 | PARTIAL |
| 摘要 | 该时点的 Web 生产构建通过；Android `core:datastore` / `core:network` 定向单测与 `assembleDebug` 通过；13:26-14:06 CST 三次 `adb devices -l` 复核都为空，说明当时阻塞不是“宿主缺 adb”，而是“当时没有任何已识别设备在线”。该结论已被 15:03-15:17 CST 的新真机证据进一步更新为“设备后来恢复识别，且本地 HTTPS 真机主链路已打通”。 |
| 关键输出 | `vite build` 成功产出 `dist/assets/index-a2b0d832.js`；Android 构建 `BUILD SUCCESSFUL in 6s`；`adb devices -l` 在 13:26 CST、13:47 CST、14:06 CST 三次输出均仅有 `List of devices attached` 空列表；后续真机恢复识别的最新证据见 `20260630-1508-android-device-local-https-home-smoke.md`。 |
| 已确认收口 | Web 侧已把 `session`、`RoleAccessPage`、`AgentPage` 结构化草稿、`client.ts` 共享实体 ID 签名、`live-screen-data`、`ProductArchivePage`、`PurchaseReturnPage`、`SalesReturnPage`、`PayOrderDetailPage`、`InventorySnapshotPage` 等口径统一到 `EntityId` / `readQueryId` / `sameEntityId`，并确认运行时 JSON 解析链路会先经 `preserveUnsafeIntegers()` 保住超大整型 ID 精度。Android 侧已把默认 baseUrl、legacy host 迁移、HTTPS 强制与 release trusted-host 回退逻辑落盘，并完成本地重新构建。 |
| 仍未证明 | 同步、导入、媒体上传、真实 provider AI、117 / 新边缘环境上的发布级联调；性能与安全发布现场证据。 |
| 附件 | 当前 turn 命令输出；相关源码见 `web/src/app/stores/session.ts`、`web/src/pages/settings/RoleAccessPage.vue`、`web/src/pages/agent/AgentPage.vue`、`web/src/shared/api/client.ts`、`master-goods-android/core/datastore/src/main/java/com/zhihuiji/core/datastore/SettingsStore.kt`、`master-goods-android/core/network/src/main/java/com/zhihuiji/core/network/NetworkModule.kt`；真机新增证据见 `20260630-1508-android-device-local-https-home-smoke.md` |
