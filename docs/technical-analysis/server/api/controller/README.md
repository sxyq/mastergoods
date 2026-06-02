# Server api/controller 模块分析

- 对应源码目录：`src/main/java/com/zhihuiji/backend/api/controller`
- 当前控制器：
  - `/v1`: `Admin / Agent / AgentTask / Auth / Customer / FinanceRecord / PayOrder / Product / PurchaseOrder / Report / SaleOrder / Supplier / Sync`
  - `/v2`: `V2SaleOrder / V2PurchaseOrder / V2PayOrder / V2Product / V2ProductCategory / V2ProductUnit / V2ProductPriceLevel / V2ProductSupplierRelation / V2Customer / V2Supplier / V2CustomerGroup / V2SupplierGroup / V2CustomerContact / V2SupplierContact`

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
| `/v1` 业务控制器集合 | 新版已做 | 旧版无当前服务端控制器层 | 保持现有兼容接口 | 13 个控制器已存在 | 继续服务当前安卓版本 |
| `/v2` 分域控制器 | 新版已做 | 旧版无 `/v2` | 新增 products/partners/sales/purchase/finance/inventory/media/agent/sync 等新版路由 | 已建立 `/v2` 控制器目录，并落地 sale/purchase/pay/product/partner 首批路由与商品第三阶段扩域路由 | 财务、库存、媒体等后续继续补齐 |
| Entity 直接作为请求体 | 新版需要去掉 | 首版为提速做过简化 | 创建/更新改成专用 request DTO | 当前部分控制器仍保留此模式 | 属于后端首批重构项 |
| owner 归属过滤 | 新版已做 | 旧版无统一 owner | 所有列表/详情/统计按当前 owner 过滤 | 当前单据相关 controller 已完全下沉到 owner-aware service | 非单据域仍需继续清理 |
| 商品第三阶段控制器 | 新版已做 | 旧版无价格层级与供应关系路由 | 暴露价格层级 CRUD、供应关系 CRUD 与扩域商品读写 | `V2ProductPriceLevelController`、`V2ProductSupplierRelationController` 已落地，`V2ProductController` 已升级返回多价格和供应关系 | `/v1/products` 保持冻结 |
