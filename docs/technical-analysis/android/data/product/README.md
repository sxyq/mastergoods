# Android data/product 模块分析

- 对应源码目录：`master-goods-android/data/product`
- 关键源码：`ProductRepository.kt`

## 模块定位

`data/product` 当前还是“基础商品仓储”。  
新版里，它会逐步演变成更完整的**商品目录域数据层**，承接：

- 商品基础档案
- 商品分类
- 单位与换算
- 多价格层级
- 商品-供应商关系
- 商品媒体索引

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
| 商品查询/编辑/库存调整仓储 | 新版已做 | 旧版商品域更厚 | 支撑当前商品业务链 | `ProductRepository.kt` 已实现 | 已联通列表/编辑/调库存 |
| 分类、单位、多价格、商品-供应商关系 | 旧版存在新版未做 | 旧版 `products + ptypes + punits + product_suppliers` 更完整 | 新版商品域要超过旧版 | 当前仓储仍只覆盖基础商品字段 | 后端现已具备分类、单位、价格层级、商品-供应关系两层接口 |
| owner 与 `/v2` 商品契约 | 需重构 | 旧版无统一 owner | 按 owner 查询商品并升级到 `/v2` | 后端已具备 `/v2/products`、`/v2/product-categories`、`/v2/product-units`、`/v2/product-price-levels`、`/v2/product-supplier-relations`，当前安卓仍主要消费 `/v1` | 下一步转 `core/model/v2/product` 与 `data/product` 适配 |
| “商品仓储只服务一个编辑页”思路 | 需重构 | 首版可以接受 | 新版要服务商品主数据全域 | 当前职责仍偏窄 | 代码阶段再细化 |

## `/v2` 商品数据层下一步拆分建议

| 子能力 | 状态 | 规划 |
|---|---|---|
| `products` 基础读写 | 新版待做 | 新建 `/v2` 适配层，保留 `/v1` 兼容仓储 |
| `product-categories` / `product-units` | 新版待做 | 提供选择器与缓存列表接口 |
| `product-price-levels` | 新版待做 | 提供价格层级主数据 CRUD 与商品内嵌价格值转换 |
| `product-supplier-relations` | 新版待做 | 提供供应商摘要、默认供应商、优先级关系读写 |
