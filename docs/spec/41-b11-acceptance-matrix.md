# 41 B11 验收矩阵

## 文档定位

本文件是 `B11 联调、测试、性能、安全、发布验收` 的统一验收入口，供后续最小上下文 agent 复验。

当前结论必须保守：

- 自动化测试和本地编译可以作为局部通过证据。
- 真机、生产环境、性能稳定性、安全发布清单，只有拿到真实命令输出、截图、日志或报告后才能改为已完成。
- 不允许把 `assembleDebug` 或后端定向测试通过等同于发布完成。
- 2026-06-03 这批本地 PASS 证据来自 dirty worktree，统一代码态见 `docs/acceptance-evidence/b11/20260603-code-state.md`；其语义应解读为“当前在途工作树上的本地复验结果”，不是干净发布候选证明。
- 若某条结论依赖 `tools/b11_acceptance_check.sh` 内部固定 selector，而原始 `.log` 仅能直接证明 `BUILD SUCCESSFUL`，矩阵必须显式写成“脚本化定向覆盖”，不能误写成原始日志独立证明了完整覆盖面。

## 证据目录约定

后续 B11 证据统一放在：

| 类型 | 目录 | 命名建议 |
|---|---|---|
| 后端命令日志 | `docs/acceptance-evidence/b11/backend/` | 新增证据优先使用 `YYYYMMDD-HHMM-<command>.log`；本轮已补档历史日志允许保留不带 `HHMM` 的旧名，但需在摘要中说明它们是补档日志 |
| Android 构建/测试日志 | `docs/acceptance-evidence/b11/android/` | 新增证据优先使用 `YYYYMMDD-HHMM-<gradle-task>.log`；本轮已补档历史日志允许保留不带 `HHMM` 的旧名，但需在摘要中说明它们是补档日志 |
| 真机截图 | `docs/acceptance-evidence/b11/screenshots/` | `YYYYMMDD-HHMM-<device>-<flow>.png` |
| 性能记录 | `docs/acceptance-evidence/b11/performance/` | `YYYYMMDD-HHMM-<flow>-perf.md` |
| 安全发布记录 | `docs/acceptance-evidence/b11/security-release/` | `YYYYMMDD-HHMM-<checklist>.md` |

证据条目至少包含：

| 字段 | 要求 |
|---|---|
| 时间 | 使用本地时间，精确到分钟 |
| 执行人/agent | 记录本轮 agent 或人工 |
| 代码状态 | 记录 `git rev-parse --short HEAD` 与 `git status --short` 摘要 |
| 命令 | 原始命令完整保留 |
| 结果 | `PASS` / `FAIL` / `BLOCKED` / `待验证` |
| 摘要 | 记录关键输出、失败原因或阻塞条件 |
| 附件 | 截图、日志、性能 trace 或服务端日志路径 |

## B11 验收矩阵

| 验收项 | 当前状态 | 当前证据 | 仍需补齐 |
|---|---|---|---|
| 后端 `/v2` service 测试 | 本地已验证（当前工作树） | `20260630-1512-backend-recovery-summary.md` 与 `20260630-1512-backend-smoke-pass.log` 已证明修复当前工作树阻塞后，`backend-smoke` 再次通过 | 仍缺生产运行现场与发布级运行验收 |
| 后端 `/v2` controller 测试 | 本地已验证（当前工作树） | `20260630-1512-backend-recovery-summary.md` 与 `20260630-1512-backend-smoke-pass.log` 已证明脚本化 smoke 在当前工作树通过 | 仍缺更细粒度生产现场验收 |
| 后端 migration SQL 测试 | 本地已验证（脚本化 smoke 覆盖） | `20260630-1512-backend-smoke-pass.log` 证明当前工作树下 `backend-smoke` 通过；按本矩阵口径可作为当前本地门禁恢复通过的证据 | 仍建议在发布候选阶段保留更细原始日志或定向 migration 记录 |
| 后端 `/v1` compatibility 回归 | 本地已验证（当前工作树） | 当前真实响应字段为 camelCase `recordNo`，相关测试已与现行契约对齐；`20260630-1512-backend-smoke-pass.log` 已证明当前工作树下 compatibility 命中集通过 | 仍缺生产现场回归 |
| Android `/v2` model 序列化 | 本地已验证（定向单测） | `20260603-1709-android-contract.md`：最新补档日志 `20260603-android-contract.log` 输出 `BUILD SUCCESSFUL in 2s`，对应 `:core:model:testDebugUnitTest` | 如进入发布候选，仍建议追加全量 Android unit test |
| Android `/v2` network 契约 | 本地已验证（定向单测） | `20260603-1709-android-contract.md`：最新补档日志 `20260603-android-contract.log` 对应 `:core:network:testDebugUnitTest` | 仍需真实后端 HTTP 联调确认 |
| Android `/v2` repository 委派 | 本地已验证（定向单测） | `20260603-1709-android-contract.md` 已覆盖 `:data:agent:testDebugUnitTest` 与 `:data:finance:testDebugUnitTest`；本轮新增 `20260603-2354-android-repository-delegation.md`，并跑通 `:data:product:testDebugUnitTest`、`:data:customer:testDebugUnitTest`、`:data:supplier:testDebugUnitTest`、`:data:order:testDebugUnitTest`、`:data:sync:testDebugUnitTest`，补齐 `product/customer/supplier/order/sync` 的委派层定向单测；`20260630-1326-android-web-current-status.md` 与 `20260630-1351-web-id-entityid-build.md` 证明相关改动后的工作树仍可通过 Android 定向单测和 Web 构建 | 仍需真实后端 HTTP 联调，且 `20260603-2354` 证据以摘要和会话命令输出为准，未单独补档 `.log` |
| Android `assembleDebug` | 本地已验证（构建链） | `20260603-1709-android-assemble.md`：最新补档日志 `20260603-android-assemble.log` 输出 `BUILD SUCCESSFUL in 7s`；`20260604-0915-android-ui-targeted-compile.md` 补强证明最新 `dashboard/reports/agent/app` UI 收口后的定向 Kotlin 编译通过；`20260604-0227-android-ui-honesty-final-compile.md` 进一步补强证明商品/客户/供应商/销售/采购/付款这组详情编辑页诚实态收口后的定向 Kotlin 编译通过；`20260604-0930-android-assemble.md` 进一步补强证明最新 UI 收口后的整包 `android-assemble` 仍通过；`20260630-1326-android-web-current-status.md` 再次证明当前 Android 网络修复工作树上 `:core:datastore:testDebugUnitTest`、`:core:network:testDebugUnitTest` 与 `:app:assembleDebug` 通过 | 已证明 debug 构建链可用，且最新网络与 ID 收口未打坏整包 debug 构建；仍需真机安装、截图和主流程 smoke |
| Android `assembleRelease` | 本地已验证（构建链） | `20260603-1730-android-assemble-release.md`：补档日志 `20260603-android-assemble-release.log` 输出 `BUILD SUCCESSFUL in 1m 34s`；`20260604-0945-android-assemble-release.md` 进一步补强证明最新 UI 收口后的 release 构建链仍通过，包含 R8、资源收缩与 lintVital；`20260630-1342-android-assemble-release.md` 再次证明当前 Android 网络修复工作树上的 `:core:network:testDebugUnitTest`、`compileReleaseKotlin` 与 `assembleRelease` 通过 | 已证明 release 构建链在当前工作树仍可用；仍需 release 包安装、运行期截图与证书 pin 现场确认 |
| 真机登录与主流程 | 本地已验证（真机 + 本地 HTTPS 隧道） | `20260630-1508-android-device-local-https-home-smoke.md` 已证明当前 Mac 可识别设备 `d715a3a4`，App 已安装，`pm clear` 后冷启动登录链路可进入首页，并已实测打开 `单据 / 档案 / 报表 / 助手` 四个一级页签；截图已归档到 `docs/acceptance-evidence/b11/screenshots/` | 仍缺更深层业务编辑/保存、同步/导入、媒体/AI 深链路、性能采样与生产环境主流程 |
| 真机 `/v2` 同步链路 | 部分已验证 | `20260630-1552-sync-import-media-ai-local-validation.md` 已证明本地 owner 作用域下 `sync health -> upload -> pull -> ack -> 实体回查` 闭环成立；真机侧仍只有首页与一级页签 smoke | 仍需把真机实际触发同步、服务端日志与端侧落库证据串成一条现场链 |
| 后端 `bootJar` 发布构建 | 本地已验证（构建链） | `20260630-1512-backend-recovery-summary.md` 与 `20260630-1512-backend-bootjar-pass.log` 已证明当前工作树 `bootJar` 通过；历史补档见 `20260603-1732-backend-bootjar.md` | 已证明后端发布 jar 构建链可用；仍不替代生产主机运行与健康检查 |
| 当前生产拓扑只读核验 | 本地已验证（服务器只读） | `20260630-1338-124-154-readonly-status.md` 已确认 `124` 为公网边缘/Nginx 入口，`154` 为应用主机，且 `zhihuiji154-backend`、数据库、Redis、`new-api`、`filecodebox` 均在线；公网 `https://sxyq27.online/zhj-api/v1/sync/health` 与 154 本地 `http://127.0.0.1:18080/v1/sync/health` 都能返回真实后端响应 | 已证明当前不是“后端未部署”阻塞；仍需结合真机或发布流程完成业务联调、性能和发布验收 |
| 性能稳定性 | 部分已验证 | `20260630-1556-android-device-home-tabs-gfx-mem.md` 已提供真机 `d715a3a4` 首页/一级页签流的 `gfxinfo` 与 `meminfo` 基础采样 | 仍需列表、图表、同步、上传、大单据、AI 流式等更重场景的专项性能记录 |
| 安全发布清单 | 部分已验证 | 除既有 Android release / 生产拓扑只读证据外，`20260630-1552-sync-import-media-ai-local-validation.md` 已补充媒体上传超限从误报 `500` 收口到明确 `413` 的真实错误语义回归 | 仍需 release 动态验收、真实生产链路 smoke、敏感日志与安全头现场证据，并补更完整的现场元数据 |

## 推荐本地命令

优先使用脚本统一入口：

```bash
./tools/b11_acceptance_check.sh backend-smoke
./tools/b11_acceptance_check.sh android-contract
./tools/b11_acceptance_check.sh android-assemble
./tools/b11_acceptance_check.sh android-assemble-release
./tools/b11_acceptance_check.sh backend-bootjar
```

当前脚本不会自动 `tee` 或归档日志；执行者需要手动把 stdout/stderr 归档到 `docs/acceptance-evidence/b11/*`，例如：

```bash
./tools/b11_acceptance_check.sh android-contract | tee docs/acceptance-evidence/b11/android/YYYYMMDD-HHMM-android-contract.log
```

后端直接命令（后端根目录没有独立 `./gradlew` 时，可复用 Android wrapper 作为 Gradle 启动器）：

```bash
JAVA_HOME=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home ./master-goods-android/gradlew \
  -p /Users/sunyiyang/Desktop/Project/master-goods \
  test \
  --tests 'com.zhihuiji.backend.application.service.v2.*' \
  --tests 'com.zhihuiji.backend.api.controller.V2*' \
  --tests 'com.zhihuiji.backend.api.controller.V1*CompatibilityControllerTest' \
  --tests 'com.zhihuiji.backend.infrastructure.db.*' \
  --console=plain \
  -Dorg.gradle.java.home=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home
```

Android 契约与编译（`android-contract` 是当前局部 Android `/v2` model/network 与部分 repository 测试，不代表全量 repository 覆盖）：

```bash
JAVA_HOME=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home ./master-goods-android/gradlew \
  -p /Users/sunyiyang/Desktop/Project/master-goods/master-goods-android \
  :core:model:testDebugUnitTest \
  :core:network:testDebugUnitTest \
  :data:agent:testDebugUnitTest \
  :data:finance:testDebugUnitTest \
  --console=plain \
  -Dorg.gradle.java.home=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home

JAVA_HOME=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home ./master-goods-android/gradlew \
  -p /Users/sunyiyang/Desktop/Project/master-goods/master-goods-android \
  assembleDebug \
  --console=plain \
  -Dorg.gradle.java.home=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home

JAVA_HOME=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home ./master-goods-android/gradlew \
  -p /Users/sunyiyang/Desktop/Project/master-goods/master-goods-android \
  assembleRelease \
  --console=plain \
  -Dorg.gradle.java.home=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home

JAVA_HOME=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home ./master-goods-android/gradlew \
  -p /Users/sunyiyang/Desktop/Project/master-goods \
  bootJar \
  --console=plain \
  -Dorg.gradle.java.home=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home
```

## 真机联调待验证清单

当前本机阻塞：

- 证据见 [20260603-1712-emulator-blocked.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/android/20260603-1712-emulator-blocked.md)
- 当前宿主现在可直接调用 Android SDK `adb`，但 `adb devices -l` 返回空列表；因此下面清单当前并非“宿主缺工具”，而是“需要让真机重新被这台 Mac 识别，或切到能看到设备的环境后继续”

| 流程 | 证据要求 | 状态 |
|---|---|---|
| 登录/会话恢复 | 登录截图、token/owner 恢复日志、重新打开 App 截图 | 待验证 |
| 商品/客户/供应商 | 列表、搜索、详情、编辑、保存后刷新截图 | 待验证 |
| 销售/采购/付款 | 创建、编辑、详情、状态变化、金额字段截图 | 待验证 |
| 财务/库存/报表 | 账户、流水、库存来源、报表数据截图 | 待验证 |
| 同步/导入 | 手动同步、`next_cursor`、`ack`、导入任务状态截图与服务端日志 | 待验证 |
| AI/媒体 | 会话、消息、草稿、上传/绑定路径截图与服务端日志 | 待验证 |

## 性能稳定性待验证清单

| 场景 | 建议指标 | 状态 |
|---|---|---|
| 大列表滚动 | 帧率、卡顿帧、内存峰值 | 待验证 |
| 图表/报表加载 | 首屏耗时、接口耗时、内存峰值 | 待验证 |
| 同步大批量数据 | pull/apply/ack 耗时、失败重试、游标正确性 | 待验证 |
| 图片上传/绑定 | 上传耗时、失败恢复、内存峰值 | 待验证 |
| 大单据编辑 | 输入响应、保存耗时、崩溃/ANR | 待验证 |

## 安全发布待验证清单

| 项目 | 要求 | 状态 |
|---|---|---|
| owner 边界 | `/v1` 与 `/v2` 关键查询、写入、同步不得跨 owner | 待验证 |
| 鉴权 | 未登录、过期 token、错误 token 有明确失败语义 | 待验证 |
| 敏感日志 | release 构建不得输出 token、密码、完整个人敏感数据 | 待验证 |
| Android 签名 | release keystore、签名配置、版本号策略明确 | 待验证 |
| 混淆/压缩 | release minify/shrink 策略与反射序列化白名单明确 | 待验证 |
| 后端发布 | 环境变量、数据库迁移、回滚方案、健康检查明确 | 待验证 |

## B11 当前收口口径

- `B11 自动化验收入口`：本文件与 `tools/b11_acceptance_check.sh` 建立后可视为已做。
- `B11 本地测试/编译结果`：2026-06-03 曾完成 `backend-smoke`、`android-contract`、`android-assemble`、`android-assemble-release`、`backend-bootjar` 五条本地复验，原始日志已补档到 `docs/acceptance-evidence/b11/`；2026-06-04 又补了四条与最新 UI 收口直接相关的证据；2026-06-30 再补了 `20260630-1326-android-web-current-status.md`、`20260630-1342-android-assemble-release.md`、`20260630-1351-web-id-entityid-build.md`、`20260630-1406-backend-smoke-current-failures.md` 与 `20260630-1512-backend-recovery-summary.md`。其中 Android/Web 构建、后端 smoke 与 bootJar 在当前工作树都已有最新通过证据。
- `B11 真机/生产环境/性能/安全发布`：当前仍待验证；但真机 UI smoke 已从“无设备在线”推进到“当前 Mac 可识别真机、可进首页并打开四个一级页签”，本地又新增拿到了 `sync/import/media` 的真实后端闭环、AI 无 provider 时的真实退化语义，以及一份真机 `gfxinfo/meminfo` 基础采样。剩余缺口集中在真机实际同步/上传/AI 深链路、重场景性能、以及生产环境发布级验收。服务器端当前已通过只读核验证明 `124`/`154` 拓扑在线，但这仍不等于发布级完成。
