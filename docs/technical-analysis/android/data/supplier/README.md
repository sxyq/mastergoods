# Android data/supplier 模块分析

- 对应源码目录：`master-goods-android/data/supplier`
- 关键源码：`SupplierRepository.kt`、`SupplierV2Repository.kt`

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
| owner 与 `/v2` 供应商契约 | 待验证 | 旧版无统一 owner | 所有供应商查询按 owner 与 `/v2` | 已新增 `SupplierV2Repository.kt`，承接 `/v2/suppliers`、`/v2/supplier-groups`、`/v2/supplier-contacts` | 现阶段与 `/v1` 并行，feature 层尚未切换 |

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 视觉真源固定为 `docs/design-mockups/01.png ~ 08.png` 与 `master-goods-android/UI-DESIGN-SPEC.md`。
