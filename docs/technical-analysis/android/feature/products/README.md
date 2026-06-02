# Android feature/products 模块分析

- 对应源码目录：`master-goods-android/feature/products`
- 关键源码：
  - `ProductListScreen.kt`
  - `ProductEditorScreen.kt`
  - `StockAdjustSheet.kt`
  - `ProductListViewModel.kt`
  - `ProductEditorViewModel.kt`

## 模块定位

`feature/products` 当前是基础商品页。  
新版里，它会向更完整的商品目录域界面演进，承接：

- 基础档案
- 分类与单位
- 多价格层级
- 商品-供应商关系
- 库存与媒体信息

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
| 商品列表/编辑/库存调整闭环 | 新版已做 | 旧版商品域更厚 | 支撑当前商品主流程 | 页面、弹窗、ViewModel 已存在 | 业务链已走通 |
| 多单位、多价格、供应关系页面 | 旧版存在新版未做 | 旧版商品能力明显更厚 | 新版商品页要超过旧版 | 当前编辑页仍是基础字段集 | 后端第三阶段已补 `product-price-levels` 与 `product-supplier-relations`，页面层仍未接入 |
| `/v2` owner-aware 商品页 | 需重构 | 旧版无统一 owner | 页面状态管理与表单改为新版契约 | 后端已具备 `/v2/products`、`/v2/product-categories`、`/v2/product-units`、`/v2/product-price-levels`、`/v2/product-supplier-relations`，当前页面仍主要绑定 `/v1` | UI 不在本阶段修改 |

## 后续页面拆分建议

| 页面/区域 | 状态 | 新版目标 |
|---|---|---|
| 商品列表页筛选区 | 新版待做 | 分类、单位、低库存、状态联合筛选 |
| 商品编辑页价格区 | 新版待做 | 支持基础价 + 多价格层级编辑 |
| 商品编辑页供应关系区 | 新版待做 | 支持默认供应商、优先级、最近采购价、备注 |
| 商品详情页供应链摘要 | 新版待做 | 汇总默认供应商与供应关系列表 |
