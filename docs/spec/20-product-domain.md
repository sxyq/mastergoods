# 20 商品域

## 需求表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| products | 需重构 | 多单位、多价格、库存预警 | 完整商品主档 | 已补 `category_id/unit_id/price_level_values_json` 扩域位，仍保留 `category/unit` 兼容字段 | `/v2/products` 将消费新字段，`/v1` 继续冻结 |
| product_categories | 新版已做 | 旧版有分类体系 | 独立分类表 | 已新增迁移、实体、接口 | 第二阶段已落地 |
| product_units | 新版已做 | 旧版有单位体系 | 独立单位表 | 已新增迁移、实体、接口 | 第二阶段已落地 |
| product_price_levels | 新版已做 | 旧版有多价格 | owner 私有价格层级主数据 | 已新增 `V9` 迁移、实体、repository、service、controller 与测试 | `/v2/product-price-levels/*` 已可用 |
| product_supplier_relations | 新版已做 | 旧版有供应关系 | owner 私有商品-供应商关系 | 已新增 `V9` 迁移、实体、repository、service、controller 与测试 | `/v2/product-supplier-relations/*` 已可用 |
| product_media | 新版待做 | 旧版有图片资源 | 商品图片/附件 | 未做 | 需要补 |

## 第三阶段当前落点

- `products` 新增：
  - `category_id`
  - `unit_id`
- `products` 继续新增：
  - `price_level_values_json`
- 历史 `products.category` 已规划按 owner 自动归并到 `product_categories`
- 历史 `products.unit` 已规划按 owner 自动归并到 `product_units`
- `product_price_levels` 已建 owner 私有价格层级表
- `product_supplier_relations` 已建 owner 私有商品-供应商关系表
- 当前阶段仍不进入：
  - 商品媒体
  - 更完整的多单位换算

## 第三阶段数据关系说明

1. `product_price_levels` 保存 owner 级价格层级定义，要求 `code/name` 在 owner 内唯一。
2. `products.price_level_values_json` 保存某个商品对应的多价格值快照，并引用 owner 自己的价格层级定义。
3. `product_supplier_relations` 保存某个商品与 owner 自己供应商之间的关系，并承载：
   - 默认供应商
   - 采购优先级
   - 最近采购价
   - 关系备注
4. `/v2/products` 已升级为返回：
   - 基础商品主档
   - `price_levels`
   - `default_supplier`
   - `supplier_relations`
5. `/v1/products` 继续只暴露基础字段，不暴露这些扩域位。
