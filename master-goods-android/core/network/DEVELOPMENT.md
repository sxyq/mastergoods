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
