# Server api/common 模块分析

- 对应源码目录：`src/main/java/com/zhihuiji/backend/api/common`
- 关键源码：
  - `ApiResponse.java`
  - `BusinessException.java`
  - `GlobalExceptionHandler.java`
  - `IdGenerator.java`
  - `ParseUtils.java`
  - `PaginationUtils.java`
  - `OrderStatus.java`
  - `PartnerTypes.java`
  - `PayOrderStatus.java`
  - `PaymentStatus.java`
  - `PaymentType.java`
  - `PurchaseOrderStatus.java`

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
| `/v1` 通用响应与异常处理 | 新版已做 | 旧版无统一远程 API 包装 | 保持统一 `ApiResponse` 与异常模型 | 当前工具和枚举已存在 | 支撑所有控制器 |
| `/v2` 更完整状态枚举与分页协议 | 需重构 | 旧版无 `/v2` | 按领域扩展共用协议 | 已补 `PartnerTypes` 与轻量 `PaginationUtils`，但仍缺数据库原生分页与统一分页响应协议 | 后续跟 `/v2` 一起补 |
| `partner_type` 常量收口 | 新版已做 | 首版在多处直接写 `"customer"` / `"supplier"` 字符串 | 统一往来单位类型常量与校验入口 | 已新增 `PartnerTypes` | 主要供 `/v2` partner controller/service 复用 |
| 轻量内存分页辅助 | 新版已做 | 首版列表接口多为全量返回或各自手切分页 | 提供统一的 page/size 切片工具，避免控制器重复写边界处理 | 已新增 `PaginationUtils.slice()` | 只是过渡工具，不等于最终数据库分页能力 |
| 轻量字符串/整数状态泛用 | 需重构 | 首版快速交付更宽松 | 更强的类型安全与 owner-aware 协议 | 当前仍存在较轻的工具型实现 | 等后端正式重构 |
