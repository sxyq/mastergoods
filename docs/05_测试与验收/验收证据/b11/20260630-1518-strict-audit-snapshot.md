# 2026-06-30 严格完工审计快照（15:18 更新）

## 口径

- 审计对象：`docs/spec/40-batch-development-master-plan.md` 与 `临时.md`
- 判定标准：发布可交付，不按“代码大致有了”放宽
- 证据优先级：当前工作树源码、当前命令输出、当前测试结果、当前真机截图/日志、当前只读服务器状态
- 保守规则：证据不够即不判完成

## 表 1：开发计划当前剩余项审计

| 条目 | 文档目标 | 发布级完成标准 | 代码现状 | 证据现状 | 缺口 | 严格结论 | 证据定位 |
|---|---|---|---|---|---|---|---|
| B06 同步与导入链路 | owner 私有同步、导入任务、迁移编排 | 真机同步、导入任务、服务端日志、现场闭环全成立 | `/v2/sync/*`、`/v2/import-jobs/*` 已在代码中 | 当前已拿到本地 `sync health -> upload -> pull -> ack -> 实体回查` 闭环，以及 `import-jobs pending -> succeeded` worker 执行闭环证据 | 真机实际同步触发、更多旧库样本覆盖、生产现场仍缺 | 部分完成 | [20260630-1552-sync-import-media-ai-local-validation.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/backend/20260630-1552-sync-import-media-ai-local-validation.md) |
| B07 媒体与 AI 扩域 | 媒体附件域、AI 会话/消息/草稿合同与联调 | 真实上传链、工作台、媒体绑定、服务端日志、端到端验收成立 | 后端与前端/Android 首轮合同已落地 | 当前已拿到媒体上传/绑定/内容读取真闭环，且 AI `/v2/agent/chat` 已验证无 provider 时的真实工具查询退化语义 | 真实 provider 对话/流式联调仍缺；AI 不能按全部完成记账 | 部分完成 | [20260630-1552-sync-import-media-ai-local-validation.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/backend/20260630-1552-sync-import-media-ai-local-validation.md) |
| B08 安卓 core/data `/v2` 迁移 | core/model、network、data repository 首轮切换到 `/v2` | 当前工作树相关测试/构建通过，且真实后端联调与真机链路成立 | Android `/v2` model/network/repository 已接通 | 定向单测、debug/release 构建、本地 HTTPS 真机主链路均已拿到证据 | 缺同步/上传/更多业务详情页现场覆盖 | 部分完成 | [41-b11-acceptance-matrix.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/41-b11-acceptance-matrix.md), [20260630-1508-android-device-local-https-home-smoke.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/android/20260630-1508-android-device-local-https-home-smoke.md) |
| B09 安卓 feature `/v2` 迁移 | 商品、伙伴、单据、财务、报表、助手、设置 feature 全部切换 | 当前工作树编译通过，且真机联调、关键流程截图、业务闭环成立 | feature 层已基本切到 `/v2` | 真机已完成登录首页、`单据 / 档案 / 报表 / 助手` 顶层页签打开与真实 `/v2` 请求返回 `200 OK` | 缺更深层编辑/保存/同步/上传现场闭环 | 部分完成 | [20260630-1508-android-device-local-https-home-smoke.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/android/20260630-1508-android-device-local-https-home-smoke.md) |
| B10 安卓 UI 重构与设计稿贴合 | 严格按设计稿完成壳层、页面、图表、交互、视觉修整 | 真机逐页截图、设计稿贴合核对、关键流程无伪数据误导 | UI 已做多轮收口 | 当前已补登录页、首页、旧状态权限页截图，并验证一级页签可打开 | 缺逐页设计稿贴合核对与更多业务页截图 | 部分完成 | [20260630-1508-android-device-local-https-home-smoke.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/android/20260630-1508-android-device-local-https-home-smoke.md), [screenshots](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/screenshots) |
| B11 后端 smoke | 当前工作树后端本地验收 | `backend-smoke` 在当前工作树通过 | 本轮已修复阻塞并重跑通过 | 今日通过日志已归档 | 仍缺生产运行现场与发布级运行验收 | 已完成（本地门禁） | [20260630-1512-backend-recovery-summary.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/backend/20260630-1512-backend-recovery-summary.md) |
| B11 Android 真机链路 | 真机登录、主流程、同步、截图、性能 | 真机被识别、安装、运行、截图、日志、性能全齐 | 真机已识别、App 已安装、登录首页与四个一级页签已验证，并补到一份 `gfxinfo/meminfo` 基础采样 | 今日真机截图、日志、设备识别与基础性能采样证据已齐 | 缺真机同步、导入、媒体、AI 深链路与更重性能场景 | 部分完成 | [20260630-1508-android-device-local-https-home-smoke.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/android/20260630-1508-android-device-local-https-home-smoke.md), [20260630-1556-android-device-home-tabs-gfx-mem.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/performance/20260630-1556-android-device-home-tabs-gfx-mem.md) |
| B11 当前生产拓扑 | 当前生产环境在线与入口可达 | 公网入口、应用主机、健康检查、业务现场链路成立 | `124` 边缘与 `154` 应用主机在线 | 只读已证实服务器在线与入口可达 | 缺真实业务现场、发布流程、回滚、性能、安全头证据 | 部分完成 | [20260630-1338-124-154-readonly-status.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/backend/20260630-1338-124-154-readonly-status.md) |

## 表 2：`临时.md` 增强内容当前审计

| 条目 | 期望要求 | 当前实现/现状 | 是否存在反例 | 严格结论 | 证据定位 |
|---|---|---|---|---|---|
| Web ID 安全 | Web 不得用 `Number()` 处理实体 ID，路由使用 `readQueryId`，比较用 `sameEntityId` | `ProductEditPage.vue` 当前已用 `readQueryId(route.query.id)`；共享 `id.ts` 已提供 `readQueryId`/`sameEntityId` | 本轮未扫到 `route.query.id` 的 `Number()` 反例 | 已完成 | [id.ts](/Users/sunyiyang/Desktop/Project/master-goods/web/src/shared/utils/id.ts), [ProductEditPage.vue](/Users/sunyiyang/Desktop/Project/master-goods/web/src/pages/archives/ProductEditPage.vue:45) |
| Android cleartext 禁用 | debug/release 都必须 `cleartextTrafficPermitted="false"` | debug/release network security config 均为 `false` | 本轮未发现相反配置 | 已完成 | [network_security_config.xml](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/src/debug/res/xml/network_security_config.xml:3), [network_security_config.xml](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/src/release/res/xml/network_security_config.xml:3) |
| Android 最小权限与备份禁用 | 仅最小网络权限，`allowBackup="false"`，不声明 `android:debuggable` | Manifest 仅见 `INTERNET`/`ACCESS_NETWORK_STATE`，`allowBackup="false"`，未见 `android:debuggable` | 本轮未发现反例 | 已完成 | [AndroidManifest.xml](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/src/main/AndroidManifest.xml:4), [AndroidManifest.xml](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/src/main/AndroidManifest.xml:9) |
| Admin console XSS 保护 | 经 `innerHTML` 注入的用户可控字段必须先 `escapeHtml()` | `renderAccounts`/`renderUsers` 中 phone/nickname/password 等插值已过 `escapeHtml()` | 本轮未发现 admin-console 中未转义的用户文本插值反例 | 已完成 | [app.js](/Users/sunyiyang/Desktop/Project/master-goods/src/main/resources/static/admin-console/app.js:97), [app.js](/Users/sunyiyang/Desktop/Project/master-goods/src/main/resources/static/admin-console/app.js:111), [app.js](/Users/sunyiyang/Desktop/Project/master-goods/src/main/resources/static/admin-console/app.js:195) |
| 多租户约束 | 新表必须 `owner_user_id`，仓储查询需含 `ownerUserId` | 文档真源仍要求该约束；仓储层搜索显示当前普遍遵循 | 未做全仓人工逐文件穷尽证明 | 不可全量证明但当前未发现直接反例 | [临时.md](/Users/sunyiyang/Desktop/Project/master-goods/临时.md:136), [临时.md](/Users/sunyiyang/Desktop/Project/master-goods/临时.md:139) |
| 构建命令为当前真源 | Web `npm run build`、Android `assembleDebug`/`assembleRelease`、后端 `test`/`bootJar` 应可作为当前真源 | Web build、Android debug/release、backend smoke、backend bootJar 当前均已拿到本轮通过证据 | 当前未见这四类命令的直接反例 | 已完成（当前本地门禁口径） | [20260630-1351-web-id-entityid-build.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/web/20260630-1351-web-id-entityid-build.md), [20260630-1342-android-assemble-release.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/android/20260630-1342-android-assemble-release.md), [20260630-1512-backend-recovery-summary.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/backend/20260630-1512-backend-recovery-summary.md) |
| known blocked finding 收口 | `ProductEditPage.vue Number(route.query.id)` 历史阻塞应已收口 | 当前文档与代码已对齐为“历史阻塞已关闭” | 本轮未见该旧反例复活 | 已完成 | [临时.md](/Users/sunyiyang/Desktop/Project/master-goods/临时.md:170), [ProductEditPage.vue](/Users/sunyiyang/Desktop/Project/master-goods/web/src/pages/archives/ProductEditPage.vue:45) |

## 压缩结论

- `40-batch-development-master-plan.md`：仍未全部完成；本地 `sync/import/media` 已从“只有代码”推进到“有真实闭环证据”，剩余内容主要集中在真机深链路、真实 provider AI、重场景性能与生产发布级证据。
- `临时.md`：当前可核到的硬约束与构建真源口径已基本收口；仍有“多租户全仓长期约束”这种不能仅靠当前抽样证据宣称全量闭环的保守项。
