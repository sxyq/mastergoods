# 2026-06-30 严格完工审计快照（14:30 版本，已被 15:18 更新版补充）

## 口径

- 审计对象：`docs/spec/40-batch-development-master-plan.md` 与 `临时.md`
- 判定标准：发布可交付，不按“代码大致有了”放宽
- 证据优先级：当前工作树源码、当前命令输出、当前测试结果、当前只读服务器状态
- 保守规则：证据不够即不判完成

## 表 1：开发计划当前剩余项审计

| 条目 | 文档目标 | 发布级完成标准 | 代码现状 | 证据现状 | 缺口 | 严格结论 | 证据定位 |
|---|---|---|---|---|---|---|---|
| B06 同步与导入链路 | owner 私有同步、导入任务、迁移编排 | 真机同步、导入任务、服务端日志、现场闭环全成立 | `/v2/sync/*`、`/v2/import-jobs/*` 已在代码中 | 当前只有本地代码与服务器在线只读证据，没有真机/导入现场证据 | 缺真机同步与导入现场闭环 | 部分完成 | [40-batch-development-master-plan.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/40-batch-development-master-plan.md), [41-b11-acceptance-matrix.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/41-b11-acceptance-matrix.md) |
| B07 媒体与 AI 扩域 | 媒体附件域、AI 会话/消息/草稿合同与联调 | 真实上传链、工作台、媒体绑定、服务端日志、端到端验收成立 | 后端与前端/Android 首轮合同已落地 | 当前仅有代码与部分本地编译/测试，无真实上传链和工作台现场证据 | 缺真实上传链、AI 工作台现场、性能证据 | 部分完成 | [40-batch-development-master-plan.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/40-batch-development-master-plan.md), [41-b11-acceptance-matrix.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/41-b11-acceptance-matrix.md) |
| B08 安卓 core/data `/v2` 迁移 | core/model、network、data repository 首轮切换到 `/v2` | 当前工作树相关测试/构建通过，且真实后端联调与真机链路成立 | Android `/v2` model/network/repository 基本已接通 | `core:model`/`core:network`/多组 data 单测与构建历史通过，当前网络/datastore/debug/release 也通过 | 缺真机与真实后端完整联调 | 部分完成 | [41-b11-acceptance-matrix.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/41-b11-acceptance-matrix.md), [20260630-1326-android-web-current-status.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/android/20260630-1326-android-web-current-status.md), [20260630-1342-android-assemble-release.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/android/20260630-1342-android-assemble-release.md) |
| B09 安卓 feature `/v2` 迁移 | 商品、伙伴、单据、财务、报表、助手、设置 feature 全部切换 | 当前工作树编译通过，且真机联调、关键流程截图、业务闭环成立 | feature 层已基本切到 `/v2`，Web ID 精度辅助问题也已继续收口 | 当前有编译与 Web build 证据，但无真机业务闭环 | 缺真机登录、主流程、同步、截图 | 部分完成 | [40-batch-development-master-plan.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/40-batch-development-master-plan.md), [20260630-1326-android-web-current-status.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/android/20260630-1326-android-web-current-status.md), [20260630-1351-web-id-entityid-build.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/web/20260630-1351-web-id-entityid-build.md) |
| B10 安卓 UI 重构与设计稿贴合 | 严格按设计稿完成壳层、页面、图表、交互、视觉修整 | 真机逐页截图、设计稿贴合核对、关键流程无伪数据误导 | UI 已做多轮收口，假数据空态进一步诚实化 | 当前只有本地编译与文档说明，无最新真机逐页截图验收 | 缺真机逐页截图、视觉贴合最终核对 | 部分完成 | [40-batch-development-master-plan.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/40-batch-development-master-plan.md) |
| B11 后端 smoke | 当前工作树后端本地验收 | `backend-smoke` 在当前工作树通过 | 当前工作树下失败 3 项 | 已有今天重跑失败证据 | 需修复后端实现或后端测试后重跑 | 未完成 | [20260630-1406-backend-smoke-current-failures.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/backend/20260630-1406-backend-smoke-current-failures.md) |
| B11 Android 真机链路 | 真机登录、主流程、同步、截图、性能 | 真机被识别、安装、运行、截图、日志、性能全齐 | `adb` 可执行，但当前无设备在线 | 13:26/13:47/14:06 三次 `adb devices -l` 都为空 | 缺设备识别与真机现场证据 | 未完成 | [20260630-1326-android-web-current-status.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/android/20260630-1326-android-web-current-status.md) |
| B11 当前生产拓扑 | 当前生产环境在线与入口可达 | 公网入口、应用主机、健康检查、业务现场链路成立 | `124` 边缘与 `154` 应用主机在线 | 只读已证实服务器在线与入口可达 | 缺真实业务现场、发布流程、回滚、性能、安全头证据 | 部分完成 | [20260630-1338-124-154-readonly-status.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/backend/20260630-1338-124-154-readonly-status.md) |

## 表 2：`临时.md` 增强内容当前审计

| 条目 | 期望要求 | 当前实现/现状 | 是否存在反例 | 严格结论 | 证据定位 |
|---|---|---|---|---|---|
| Web ID 安全 | Web 不得用 `Number()` 处理实体 ID，路由使用 `readQueryId`，比较用 `sameEntityId` | `ProductEditPage.vue` 当前已用 `readQueryId(route.query.id)`；共享 `id.ts` 已提供 `readQueryId`/`sameEntityId` | 本轮未扫到 `route.query.id` 的 `Number()` 反例 | 已完成 | [id.ts](/Users/sunyiyang/Desktop/Project/master-goods/web/src/shared/utils/id.ts), [ProductEditPage.vue](/Users/sunyiyang/Desktop/Project/master-goods/web/src/pages/archives/ProductEditPage.vue:45) |
| Android cleartext 禁用 | debug/release 都必须 `cleartextTrafficPermitted="false"` | debug/release network security config 均为 `false` | 本轮未发现相反配置 | 已完成 | [network_security_config.xml](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/src/debug/res/xml/network_security_config.xml:3), [network_security_config.xml](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/src/release/res/xml/network_security_config.xml:3) |
| Android 最小权限与备份禁用 | 仅最小网络权限，`allowBackup="false"`，不声明 `android:debuggable` | Manifest 仅见 `INTERNET`/`ACCESS_NETWORK_STATE`，`allowBackup="false"`，未见 `android:debuggable` | 本轮未发现反例 | 已完成 | [AndroidManifest.xml](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/src/main/AndroidManifest.xml:4), [AndroidManifest.xml](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/app/src/main/AndroidManifest.xml:9) |
| Admin console XSS 保护 | 经 `innerHTML` 注入的用户可控字段必须先 `escapeHtml()` | `renderAccounts`/`renderUsers` 中 phone/nickname/password 等插值已过 `escapeHtml()` | 本轮未发现 admin-console 中未转义的用户文本插值反例 | 已完成 | [app.js](/Users/sunyiyang/Desktop/Project/master-goods/src/main/resources/static/admin-console/app.js:97), [app.js](/Users/sunyiyang/Desktop/Project/master-goods/src/main/resources/static/admin-console/app.js:111), [app.js](/Users/sunyiyang/Desktop/Project/master-goods/src/main/resources/static/admin-console/app.js:195) |
| 多租户约束 | 新表必须 `owner_user_id`，仓储查询需含 `ownerUserId` | 文档真源仍要求该约束；仓储层搜索显示当前普遍遵循 | 未做全仓人工逐文件穷尽证明 | 不可全量证明但当前未发现直接反例 | [临时.md](/Users/sunyiyang/Desktop/Project/master-goods/临时.md:136), [临时.md](/Users/sunyiyang/Desktop/Project/master-goods/临时.md:139) |
| 构建命令为当前真源 | Web `npm run build`、Android `assembleDebug`/`assembleRelease`、后端 `test`/`bootJar` 应可作为当前真源 | Web build、Android debug/release、backend bootJar 当前可证明；backend `test` 当前不能整体证明，因为 `backend-smoke` 正失败 | 存在后端 smoke 失败这一反例 | 部分完成 | [20260630-1351-web-id-entityid-build.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/web/20260630-1351-web-id-entityid-build.md), [20260630-1342-android-assemble-release.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/android/20260630-1342-android-assemble-release.md), [20260630-1406-backend-smoke-current-failures.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/backend/20260630-1406-backend-smoke-current-failures.md) |
| known blocked finding 收口 | `ProductEditPage.vue Number(route.query.id)` 历史阻塞应已收口 | 当前文档与代码已对齐为“历史阻塞已关闭” | 本轮未见该旧反例复活 | 已完成 | [临时.md](/Users/sunyiyang/Desktop/Project/master-goods/临时.md:170), [ProductEditPage.vue](/Users/sunyiyang/Desktop/Project/master-goods/web/src/pages/archives/ProductEditPage.vue:45) |

## 压缩结论

- `40-batch-development-master-plan.md`：该 14:30 版本快照已被 `20260630-1518-strict-audit-snapshot.md` 补充更新，后端 smoke 与真机主链路状态已有新证据。
- `临时.md`：该 14:30 版本快照已被 `20260630-1518-strict-audit-snapshot.md` 补充更新，构建命令真源口径已有新证据。
