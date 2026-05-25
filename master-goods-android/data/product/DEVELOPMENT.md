# data/product 模块开发说明

- 当前状态：ProductRepository 已实现，采用在线优先 + Room 本地缓存观察。
- 实际源码目录：`data/product/src/main/java/com/zhihuiji/data/product`
- 目标：封装商品查询、编辑、库存调整和按编码查询。

## 需要创建的类

- `ProductRepository`

## 需要实现的关键函数

- `observeProducts(keyword: String): Flow<List<ProductDto>>`
- `refreshProducts(keyword: String?)`
- `getProduct(id: Long): ProductDto`
- `findProductByCode(code: String): ProductDto?`
- `createProduct(draft: ProductDraft): ProductDto`
- `updateProduct(id: Long, draft: ProductDraft): ProductDto`
- `adjustStock(id: Long, delta: BigDecimal, reason: String?, operator: String?): ProductDto`
- `deleteProduct(id: Long)`

## 验收标准

- 能支持商品搜索、创建、编辑、删除、库存增减五个主链路。

## UI 设计规范支撑

- 商品列表需要提供库存、安全库存、售价、状态，支撑列表卡片和状态标签。
- 商品详情需要提供基础信息、库存信息、价格信息、创建和更新时间。
- 库存调整需要返回调整后库存，便于底部确认面板刷新。
