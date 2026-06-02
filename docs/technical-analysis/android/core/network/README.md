# Android core/network 模块分析

- 对应源码目录：`master-goods-android/core/network`
- 关键源码：
  - `ZhihuijiApi.kt`
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
| `/v2` API 契约 | 新版待做 | 旧版无 `/v2` | 新版接口按领域拆分与命名 | 当前还没有 `/v2` Retrofit 契约 | 与 server spec 对齐 |
| owner-aware 请求上下文 | 需重构 | 旧版无统一 owner | 网络层能配合 owner 私有资源获取与导入状态查询 | 当前主要依赖 token 身份，不体现 owner 场景 | 后端先定义 |
| 轻量请求体与参数过载 | 需重构 | 首版为了加速开发做了简化 | 拆出更明确 Request/Query 对象 | 当前部分接口仍有 query 过载或 DTO 复用 | `/v2` 一起收敛 |
| 商品多价格/账户/库存账本接口域 | 旧版存在新版未做 | 旧版能力域更厚 | 新版网络层要覆盖更完整领域接口 | 当前 API 尚无这些接口 | 以后端扩域为准 |

## `/v2` 网络层规划

| 方向 | 状态 | 说明 |
|---|---|---|
| `ZhihuijiApi` 保留 `/v1` | 新版已做 | 当前接口集合继续兼容 |
| 新增 `/v2` 接口分组 | 新版待做 | 推荐按领域拆 `AuthV2Api / ProductV2Api / SalesV2Api ...` |
| Query 对象化 | 新版待做 | 列表筛选、分页、排序改为明确查询模型 |
| 导入/同步专用接口 | 新版待做 | 区分基础 pull/upload 与导入任务接口 |
| 错误模型分级 | 待验证 | 后续需要区分认证失败、owner 冲突、导入冲突、字段校验失败 |

## 当前安全收口结论

1. debug 仍保留联调灵活性。
2. release 已同时限制：
   - 日志输出级别
   - 明文 HTTP 基础地址
   - 非受控正式主机
   - 可选证书绑定的启用方式
3. 这套策略是现阶段 Android 端已经落地的保护层，不属于未来规划占位。
