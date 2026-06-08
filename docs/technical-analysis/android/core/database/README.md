# Android core/database 模块分析

- 对应源码目录：`master-goods-android/core/database`
- 关键源码：
  - `ZhihuijiDatabase.kt`
  - `DatabaseModule.kt`
  - `EntityMappers.kt`
  - `dao/*`
  - `entity/*`

## 模块定位

新版里 `core/database` 不是旧版那种“完整单机账本复制”，也不是单纯的轻缓存。  
它要承担的是：

- owner 私有数据的本地投影
- `/v2` 领域数据的离线观察层
- 导入、同步、扩域表的持久化基础

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
| 首版 Room 缓存 | 新版已做 | 旧版本地库是完整账本 | 当前新版先做在线优先缓存 | 现有实体、DAO、mapper 已落地 | 当前可供 data 层观察 |
| 销售单本地明细图 | 新版已做 | 旧版本地账本天然保留单据与明细关系 | 当前新版至少保证销售详情可本地回退并支持商品关键字搜索 | 已新增 `SaleOrderItemEntity`、`SaleOrderWithItems`、`SaleOrderDao.findWithItemsById/replaceOrderGraph(s)` | 仍只是首版缓存图，不等于完整本地账本 |
| Room 正式迁移链路 | 新版已做 | 首版常依赖破坏式迁移换速度 | 当前新版开始为增量表扩展补 Migration | `DatabaseModule` 已新增 `1 -> 2` 迁移以落地 `sale_order_items` | 是正式迁移起点 |
| owner 归属字段本地落盘 | 新版待做 | 旧版无统一 owner | 本地表结构承载 owner 语义 | 当前实体大多未体现 owner 维度 | 与后端一起演进 |
| 多价格、账户、库存快照等表 | 旧版存在新版未做 | 旧版库表明显更多 | 新版本地缓存要覆盖更厚领域 | 当前 Room 仍是首版实体集 | 后续会增加实体与 DAO |
| `fallbackToDestructiveMigration` 思路 | 新版需要去掉 | 首版常用破坏式迁移换速度 | 新版应走正式 Migration | 当前文档层面已明确不应继续依赖此思路 | 代码阶段再清理 |

## 需要进入 Room 的新版表域

| 表域 | 状态 | 说明 |
|---|---|---|
| `owner_user_id` 维度 | 新版待做 | 现有核心实体均需要补 owner 列 |
| 商品扩域表 | 新版待做 | 分类、单位、价格层级、供应关系、媒体索引 |
| 往来单位扩域表 | 新版待做 | 联系人、分组、标签、价格策略 |
| 财务扩域表 | 新版待做 | 账户、账户余额、单据资金关联、项目、找零 |
| 库存扩域表 | 新版待做 | 账本、快照、月统计 |
| 同步扩域表 | 新版待做 | owner 分桶游标、导入任务、本地冲突记录 |

## 迁移原则

1. 继续保留现有首版缓存实体，作为兼容层。
2. 新版只走增量迁移，不做“删库重建式”设计假设。
3. Room 不是旧版 SQLite 的一比一复刻，而是 owner-aware 在线优先缓存。
4. 会员相关本地表本阶段不纳入。

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 当前视觉真源以 Stitch 导出、`docs/spec/42-android-liquid-glass-ui-refactor-plan.md` 与 `master-goods-android/UI-DESIGN-SPEC.md` 为准；`docs/design-mockups/` 仅作历史参考。
