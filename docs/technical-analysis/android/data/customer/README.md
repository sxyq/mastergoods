# Android data/customer 模块分析

- 对应源码目录：`master-goods-android/data/customer`
- 关键源码：`CustomerRepository.kt`、`CustomerV2Repository.kt`

## 模块定位

`data/customer` 当前负责基础客户档案。  
新版里，它会逐步并入更完整的 partner 领域能力，支持：

- 客户基础档案
- 联系人
- 分组
- 标签
- 价格等级与折扣策略
- owner 私有客户视图

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
| 客户列表/详情/编辑仓储 | 新版已做 | 旧版客户模型更厚 | 支撑当前客户业务链 | `CustomerRepository.kt` 已实现 | 已联通列表/编辑/详情页 |
| 客户分组/联系人/价格等级/标签 | 旧版存在新版未做 | 旧版 `companies` 有更多画像字段 | 新版补齐更强客户运营能力 | 当前仓储只覆盖基础客户字段 | 后端扩域后跟进 |
| owner 过滤与 `/v2` DTO | 待验证 | 旧版无统一 owner | 所有客户查询按 owner 和 `/v2` 契约执行 | 已新增 `CustomerV2Repository.kt`，承接 `/v2/customers`、`/v2/customer-groups`、`/v2/customer-contacts` | 现阶段与 `/v1` 并行，feature 层尚未切换 |

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 视觉真源固定为 `docs/design-mockups/01.png ~ 08.png` 与 `master-goods-android/UI-DESIGN-SPEC.md`。
