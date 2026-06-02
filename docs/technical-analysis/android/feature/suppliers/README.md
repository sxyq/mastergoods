# Android feature/suppliers 模块分析

- 对应源码目录：`master-goods-android/feature/suppliers`
- 关键源码：
  - `SupplierListScreen.kt`
  - `SupplierEditorScreen.kt`
  - `SupplierDetailScreen.kt`
  - `SupplierViewModel.kt`
  - `SupplierEditorViewModel.kt`
  - `SupplierDetailViewModel.kt`

## 模块定位

`feature/suppliers` 在新版中承接供应商档案域。  
后续要覆盖：

- 供应商基础档案
- 联系人与分组
- 标签与价格策略
- 与采购/应付的经营联动

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
| 供应商列表/编辑/详情闭环 | 新版已做 | 旧版供应商域更厚 | 支撑当前供应商主流程 | 页面与 ViewModel 已存在 | 业务链已走通 |
| 联系人/分组/价格等级等供应商画像 | 旧版存在新版未做 | 旧版 `companies` 字段更厚 | 新版供应商页要补足画像与经营能力 | 当前仍是基础字段集 | 依赖后端扩域 |
| `/v2` owner-aware 供应商页 | 需重构 | 旧版无统一 owner | 页面改按 owner 与新版 DTO 工作 | 后端已具备 `/v2/suppliers`、`/v2/supplier-groups`、`/v2/supplier-contacts`，当前仍主要绑定 `/v1` | UI 不在本阶段修改 |
