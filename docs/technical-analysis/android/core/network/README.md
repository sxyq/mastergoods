# Android core/network 模块分析

- 对应源码目录：`master-goods-android/core/network`
- 关键源码：
  - `ZhihuijiApi.kt`
  - `ZhihuijiV2Api.kt`
  - `NetworkModule.kt`
  - `NetworkConfig.kt`
  - `BaseUrlInterceptor.kt`
  - `AuthInterceptor.kt`
  - `TokenAuthenticator.kt`
  - `SafeApiCall.kt`

## 模块定位

新版里 `core/network` 不只是“能把请求发出去”，而是要成为：

- `/v1` 与 `/v2` 契约并存的入口
- owner 上下文、认证与环境策略的网络承接层
- 导入、同步、聚合读模型请求的统一网关

## 状态图例

- `新版已做`
- `新版待做`
- `旧版存在新版未做`
- `新版需要去掉`
- `需重构`
- `待验证`

## 状态表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `/v1` API 接入 | 新版已做 | 旧版无统一远程接口层 | 继续承载当前后端联调 | `ZhihuijiApi.kt` 和拦截器链已存在 | 现阶段继续使用 |
| release 主机白名单与 HTTPS 校验 | 新版已做 | 首版网络层默认更宽松，正式环境容易被改到任意地址 | 在 release 阶段拒绝非 HTTPS 与非受控正式主机 | `NetworkModule` 已通过 `ALLOW_CLEARTEXT_BASE_URL` 和 `SettingsStore.isTrustedReleaseBaseUrl()` 做双校验 | 与设置层的地址收口形成互锁 |
| 证书绑定启用入口 | 新版已做 | 首版没有安全启停入口，直接硬上 pinning 风险较高 | 在不破坏联调与未知证书链的前提下，为 release 预留可安全开启的 pinning 能力 | 已支持 `CERT_PINNING_ENABLED`、`PINNED_HOST`、`PINNED_SHA256_PINS` 构建参数，并在 `NetworkModule` 动态接入 `CertificatePinner` | 当前默认仍以“未注入真实 pin 时关闭”为策略 |
| `/v2` API 契约 | 待验证 | 旧版无 `/v2` | 新版接口按领域拆分与命名 | `ZhihuijiV2Api.kt` 已覆盖 `product/partner/order/finance/inventory/sync-import/agent/media` 首轮合同，并通过 `ZhihuijiV2ApiContractTest`；B08 修复：agent/media 方法名统一 `V2` 后缀，`@Query` 参数名已验证与后端一致并加注释，契约测试已扩展覆盖 `@Query` 值验证 | 与 server spec 对齐；`/v2/sync` cursor 已是 opaque token；B07 新增 `updateAgentConversationV2`/`deleteAgentConversationV2` 方法与 `UpdateAgentConversationRequest` |
| owner-aware 请求上下文 | 需重构 | 旧版无统一 owner | 网络层能配合 owner 私有资源获取与导入状态查询 | 当前主要依赖 token 身份，不体现 owner 场景 | 后端先定义 |
| 轻量请求体与参数过载 | 需重构 | 首版为了加速开发做了简化 | 拆出更明确 Request/Query 对象 | 当前部分接口仍有 query 过载或 DTO 复用 | `/v2` 一起收敛 |
| 商品多价格/账户/库存账本接口域 | 旧版存在新版未做 | 旧版能力域更厚 | 新版网络层要覆盖更完整领域接口 | 当前 API 尚无这些接口 | 以后端扩域为准 |

## `/v2` 网络层规划

| 方向 | 状态 | 说明 |
|---|---|---|
| `ZhihuijiApi` 保留 `/v1` | 新版已做 | 当前接口集合继续兼容 |
| 新增 `/v2` 接口分组 | 待验证 | 当前 `ZhihuijiV2Api` 已集中承接 `product/partner/order/finance/inventory/sync-import/agent/media` 首轮契约；后续可再按领域拆 `ProductV2Api / SalesV2Api / SyncV2Api ...` |
| Query 对象化 | 新版待做 | 列表筛选、分页、排序改为明确查询模型 |
| 导入/同步专用接口 | 需重构 | 已明确分成 `/v2/sync/*` 与 `/v2/import-jobs/*`，Android 侧仍未接入 | `retry/cancel` 不是通用动作，需受任务状态门控 |
| 错误模型分级 | 待验证 | 后续需要区分认证失败、owner 冲突、导入冲突、字段校验失败 |

## 当前安全收口结论

1. debug 仍保留联调灵活性。
2. release 已同时限制：
   - 日志输出级别
   - 明文 HTTP 基础地址
   - 非受控正式主机
   - 可选证书绑定的启用方式
3. 这套策略是现阶段 Android 端已经落地的保护层，不属于未来规划占位。
4. 安卓接 `/v2/sync` 时，不能在网络层把 cursor 当 long 处理，必须按字符串 token 原样透传。
5. `pull` 返回的 `next_cursor` 不能被网络层直接当成“已提交游标”；只有后续 `ack` 成功后，客户端才能把它视为已确认基线。
6. 安卓后续接 `/v2/import-jobs/*` 时，不能把 `retry/cancel` 设计成无条件动作；网络契约必须反映 `failed/cancelled -> retry`、`pending/running -> cancel` 的状态门控。
7. 当前 `ZhihuijiV2Api` 已承接 `product/partner/order/finance/inventory/sync-import/agent/media` 首轮 `/v2` 契约。
8. `SafeApiCall.kt` 已补 `safeApiUnitCall()`，用于兼容 `/v2` 中大量 `DELETE`/空响应成功路径，避免各仓储重复手写空数据判定。
9. B07 新增 `UpdateAgentConversationRequest`（含 `status` 字段）与 `updateAgentConversationV2`/`deleteAgentConversationV2` 两个 API 方法，对齐 `PUT /v2/agent/conversations/{id}` 与 `DELETE /v2/agent/conversations/{id}`；`ZhihuijiV2ApiContractTest` 已对 agent/media 全量端点补齐 HTTP 方法、路径与关键 query 参数断言。
10. B08 修复收尾：agent/media 方法名统一 `V2` 后缀（`agentConversations` → `agentConversationsV2` 等 16 个方法），`@Query` 参数名已验证与后端 `@RequestParam` 一致（bill-fund-links/inventory 为 camelCase，inventory/by-source 为 snake_case），`SafeApiCallBehaviorTest` 已验证 `safeApiUnitCall` 对 `ApiResponse<Unit>` 的正确行为；`AgentV2RepositoryTest` / `FinanceV2RepositoryTest` 已直接调用真实 Repository 方法验证 API 委派链路。

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 当前视觉真源以 Stitch 导出、`docs/spec/42-android-liquid-glass-ui-refactor-plan.md` 与 `master-goods-android/UI-DESIGN-SPEC.md` 为准；`docs/design-mockups/` 仅作历史参考。
