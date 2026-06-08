# data/product 模块开发说明

- 当前状态：`ProductRepository` 已实现 `/v1` 兼容链路；`ProductV2Repository` 已实现 `/v2` 商品、分类、单位、价格层级、供应关系首轮承接。
- 实际源码目录：`data/product/src/main/java/com/zhihuiji/data/product`
- 目标：封装商品查询、编辑、库存调整和按编码查询。

## 需要创建的类

- `ProductRepository`
- `ProductV2Repository`

## 需要实现的关键函数

- `observeProducts(keyword: String): Flow<List<ProductDto>>`
- `refreshProducts(keyword: String?)`
- `getProduct(id: Long): ProductDto`
- `findProductByCode(code: String): ProductDto?`
- `createProduct(draft: ProductDraft): ProductDto`
- `updateProduct(id: Long, draft: ProductDraft): ProductDto`
- `adjustStock(id: Long, delta: BigDecimal, reason: String?, operator: String?): ProductDto`
- `deleteProduct(id: Long)`
- `/v2`：
  - `listProducts(keyword, status, categoryId, unitId)`
  - `listLowStockProducts(size)`
  - `listCategories()/createCategory()/updateCategory()/deleteCategory()`
  - `listUnits()/createUnit()/updateUnit()/deleteUnit()`
  - `listPriceLevels()/createPriceLevel()/updatePriceLevel()/deletePriceLevel()`
  - `listSupplierRelations(productId)/createSupplierRelation()/updateSupplierRelation()/deleteSupplierRelation()`

## 验收标准

- 能支持商品搜索、创建、编辑、删除、库存增减五个主链路。

## UI 设计规范支撑

- 商品列表需要提供库存、安全库存、售价、状态，支撑列表卡片和状态标签。
- 商品详情需要提供基础信息、库存信息、价格信息、创建和更新时间。
- 库存调整需要返回调整后库存，便于底部确认面板刷新。

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 当前视觉真源以 Stitch 导出、`docs/spec/42-android-liquid-glass-ui-refactor-plan.md` 与 `master-goods-android/UI-DESIGN-SPEC.md` 为准；`docs/design-mockups/` 仅作历史参考。
