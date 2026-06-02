# Android feature/customers 模块分析

- 对应源码目录：`master-goods-android/feature/customers`
- 关键源码：
  - `CustomerListScreen.kt`
  - `CustomerEditorScreen.kt`
  - `CustomerDetailScreen.kt`
  - `CustomerViewModel.kt`
  - `CustomerEditorViewModel.kt`
  - `CustomerDetailViewModel.kt`

## 模块定位

`feature/customers` 在新版中承接的是客户档案域，不只是一个基础联系人列表。  
后续要覆盖：

- 客户基础档案
- 联系人与分组
- 标签与价格策略
- 与销售/应收的经营视角联动

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
| 客户列表/编辑/详情闭环 | 新版已做 | 旧版客户域更厚 | 支撑当前客户主流程 | 页面和 ViewModel 已完整存在 | 业务链已走通 |
| 联系人/分组/价格等级等 UI | 旧版存在新版未做 | 旧版客户画像更完整 | 新版客户页要覆盖更厚主数据 | 当前页面仍是基础字段集 | 依赖后端扩域 |
| `/v2` owner-aware 客户页 | 需重构 | 旧版无统一 owner | 页面逻辑要按 owner 与新版 DTO 工作 | 后端已具备 `/v2/customers`、`/v2/customer-groups`、`/v2/customer-contacts`，当前仍主要绑定 `/v1` | UI 不在本阶段修改 |
