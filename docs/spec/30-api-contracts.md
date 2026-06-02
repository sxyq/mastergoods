# 30 API 契约

## 分层

| 层 | 状态 | 说明 |
|---|---|---|
| /v1 | 新版已做 | 兼容层，保留现有行为 |
| /v2 | 新版已做 | 新版正式契约层已建立首批单据域入口，其他领域继续补齐 |

## 新版接口组

| 接口组 | 状态 | 备注 |
|---|---|---|
| auth | 新版待做 | /v2/auth/* |
| products | 新版已做 | `/v2/products/*`、`/v2/product-categories/*`、`/v2/product-units/*`、`/v2/product-price-levels/*`、`/v2/product-supplier-relations/*` 已落地第二、三阶段首批接口 |
| partners | 新版已做 | `/v2/customers/*`、`/v2/suppliers/*`、`/v2/customer-groups/*`、`/v2/supplier-groups/*`、`/v2/customer-contacts/*`、`/v2/supplier-contacts/*` 已落地 |
| sales | 新版已做 | `/v2/sale-orders/*` 首批已落地，继续扩充退货/草稿态 |
| purchase | 新版已做 | `/v2/purchase-orders/*` 首批已落地，继续扩充订单态/收货态 |
| pay-orders | 新版已做 | `/v2/pay-orders/*` 首批已落地，仍未进入完整账户体系 |
| finance | 新版待做 | /v2/accounts/* 等 |
| inventory | 新版待做 | /v2/inventory-* |
| media | 新版待做 | /v2/media/* |
| agent | 新版已做 | 后续继续扩展 |
| sync | 新版待做 | /v2/sync/* |

## 第一阶段已落地

| 对象 | 状态 | 说明 |
|---|---|---|
| `/v2/sale-orders` | 新版已做 | 已提供列表、详情、创建、草稿更新、收款、状态更新、取消 |
| `/v2/purchase-orders` | 新版已做 | 已提供列表、详情、创建 |
| `/v2/pay-orders` | 新版已做 | 已提供列表、详情、创建、状态更新 |
| `/v2` DTO 命名空间 | 新版已做 | 已新增 `api/dto/v2/sales|purchase|pay` |
| `/v2` 领域 facade service | 新版已做 | 已新增 `application/service/v2/*`，底层复用 owner-aware 领域服务 |

## 第二阶段已落地

| 对象 | 状态 | 说明 |
|---|---|---|
| `/v2/products` | 新版已做 | 已提供列表、详情、创建、更新、删除、低库存 |
| `/v2/product-categories` | 新版已做 | 已提供列表、创建、更新、删除 |
| `/v2/product-units` | 新版已做 | 已提供列表、创建、更新、删除 |
| `/v2/customers` | 新版已做 | 已提供列表、详情、创建、更新、删除 |
| `/v2/suppliers` | 新版已做 | 已提供列表、详情、创建、更新、删除 |
| `/v2/customer-groups` / `/v2/supplier-groups` | 新版已做 | 已提供分组列表、创建、更新、删除 |
| `/v2/customer-contacts` / `/v2/supplier-contacts` | 新版已做 | 已提供联系人列表、创建、更新、删除 |
| `/v2` DTO 命名空间扩展 | 新版已做 | 已新增 `api/dto/v2/product`、`api/dto/v2/partner` |

## 第三阶段已落地

| 对象 | 状态 | 说明 |
|---|---|---|
| `/v2/product-price-levels` | 新版已做 | 已提供列表、创建、更新、删除 |
| `/v2/product-supplier-relations` | 新版已做 | 已提供按商品列表、创建、更新、删除 |
| `/v2/products` 扩域读模型 | 新版已做 | 商品详情/列表已可返回 `price_levels`、`default_supplier`、`supplier_relations` |
| `/v2/products` 扩域写模型 | 新版已做 | 商品创建/更新已支持写入 `price_levels`、`supplier_relations` |
| `/v1/products` 冻结兼容 | 新版已做 | `/v1` 不新增多价格、供应关系字段，继续服务当前安卓兼容层 |
