# core/network 模块开发说明

- 当前状态：脚手架已创建，网络层未开始。
- 实际源码目录：`core/network/src/main/java/com/zhihuiji/core/network`
- 目标：提供 Retrofit API、认证头注入、刷新 token、统一错误解析。

## 需要创建的类

- `ApiResponse`
- `ZhihuijiApi`
- `AuthInterceptor`
- `TokenAuthenticator`
- `NetworkModule`
- `NetworkConfig`

## 需要实现的关键函数

- `AuthInterceptor.intercept(chain)`
  - 自动追加 `Authorization: Bearer <token>`。
- `TokenAuthenticator.authenticate(route, response)`
  - 收到 401 后调用 `/v1/auth/refresh` 刷新并重试。
- `NetworkModule.provideOkHttpClient()`
- `NetworkModule.provideRetrofit(baseUrl: String)`
- `NetworkModule.provideZhihuijiApi()`
- `ZhihuijiApi`
  - 需要完整覆盖 `auth/products/customers/suppliers/sale-orders/purchase-orders/pay-orders/finance-records/reports/sync/agent` 所有接口。
- `safeApiCall { ... }`
  - 统一把 HTTP 错误和业务错误转换为上层可处理异常。

## 特别注意

- Agent 接口字段是 `lowerCamelCase`，其他大多是 `snake_case`。
- 销售单 PDF 下载要用单独的 `@Streaming` 方法。

## UI 设计规范支撑

- 网络错误要向上提供可展示的短错误文案，供玻璃风格 Snackbar 或 Dialog 使用。
- 列表接口的筛选参数要完整开放给 feature 层，支持设计图中的搜索栏、Tab 和筛选按钮。

## 验收标准

- 更换服务器地址后 Retrofit 能重建。
- 401 场景不会导致无限重试。

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 视觉真源固定为 `docs/design-mockups/01.png ~ 08.png` 与 `master-goods-android/UI-DESIGN-SPEC.md`。
