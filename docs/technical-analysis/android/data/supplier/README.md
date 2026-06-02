# Android data/supplier 模块分析

- 对应源码目录：`master-goods-android/data/supplier`
- 关键源码：`SupplierRepository.kt`

## 模块定位

`data/supplier` 当前负责基础供应商档案。  
新版里，它会与客户域一起向 partner 领域靠拢，支持：

- 供应商基础档案
- 联系人
- 分组/标签
- 价格策略
- 采购联动视图
- owner 私有供应商视图

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
| 供应商列表/详情/编辑仓储 | 新版已做 | 旧版供应商字段更厚 | 支撑当前供应商业务链 | `SupplierRepository.kt` 已实现 | 已联通列表/编辑/详情页 |
| 联系人、分组、价格等级等 | 旧版存在新版未做 | 旧版 `companies` 有更细画像 | 新版供应商域补齐运营字段 | 当前仍只覆盖基础供应商信息 | 需后端扩域 |
| owner 与 `/v2` 供应商契约 | 需重构 | 旧版无统一 owner | 所有供应商查询按 owner 与 `/v2` | 后端已具备 `/v2/suppliers`、`/v2/supplier-groups`、`/v2/supplier-contacts`，当前安卓仍主要消费 `/v1` | 下一步转 `core/model/v2/partner` |
