# Android data/sync 模块分析

- 对应源码目录：`master-goods-android/data/sync`
- 关键源码：`SyncRepository.kt`、`SyncV2Repository.kt`

## 模块定位

新版里 `data/sync` 不再只是“定期 pull/upload”的技术模块，它还要变成：

- owner 私有同步基线的承载层
- 旧数据导入任务的客户端协调层
- 本地缓存与服务端聚合状态之间的桥接层

## 状态图例

- `新版已做`
- `新版待做`
- `旧版存在新版未做`
- `新版需要去掉`
- `需重构`
- `待验证`

## 当前实现

- 已有健康检查、手动同步、pull 结果应用、游标持久化
- 已能把部分实体类型写回 Room
- 后台自动同步、离线回写、导入上送尚未完成

## 状态表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| 基础手动同步 | 新版已做 | 旧版主要是本地账本 | 支撑在线优先架构 | `SyncRepository.kt` 已实现基础能力 | 能执行健康检查与 pull |
| `/v2` 同步与导入契约 | 待验证 | 旧版无统一 server import 方案 | 把导入、增量同步、owner 分桶设计清楚 | 已新增 `SyncV2Repository.kt`，承接 `/v2/sync/*`、`/v2/import-jobs/*` 与 `/v2/inventory/*` 读写；本轮已补 `pullApplyAndAck()`，按 `pull -> apply -> ack(next_cursor)` 执行手动同步 | 现阶段仍与 `SyncRepository` 并行；本地 apply 仅覆盖当前 Room 可承接的 `product/customer/supplier/sale_order/sale_order_item/purchase_order/pay_order/finance_record` |
| owner 分桶游标 | 待验证 | 旧版无统一 owner | 游标与同步状态按 owner 隔离 | `SyncV2Repository` 已按 opaque token 的 `cursor/pull/ack` 契约承接接口，并在手动同步链中只对 `next_cursor` 做 ack | 本地 datastore / Room owner 分桶缓存仍待后续补强 |
| 只同步部分实体类型的思路 | 新版需要去掉 | 首版范围较窄 | 新版同步要覆盖扩域后的核心实体 | 当前实现仍偏首版范围 | 但要注意 server 当前对 `inventory_ledger/snapshot/monthly_stats` 仍是 pull-only，不要在 Android 侧误设为可上传 |

## 导入链路规划

| 环节 | 状态 | 说明 |
|---|---|---|
| 客户端发起导入任务 | 新版待做 | 安卓提交 owner 归属的导入请求 |
| 导入任务轮询/订阅 | 新版待做 | 安卓显示服务端处理状态、冲突、完成度 |
| 本地缓存回填 | 新版待做 | 导入完成后按 owner 拉回数据 |
| 本地导入失败恢复 | 待验证 | 是否需要本地断点与补偿，后续再定 |

## 当前结论

- `data/sync` 已经具备“首版同步工具层”雏形。
- B06 首轮后，后端合同已经变成 `/v2/sync/* + /v2/import-jobs/*`。
- 下一阶段安卓需要补 owner 分桶本地缓存、import job 轮询和更完整的扩域实体下发应用。
- `next_cursor` / `last_cursor` 现在要按“只存储、不计算”的 token 对待，避免同一时间戳分页时漏数据。
- 安卓同步顺序应按 `pull -> 应用到本地 -> ack` 设计，不能把“拿到数据”直接等同于“已经提交游标”。

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 视觉真源固定为 `docs/design-mockups/01.png ~ 08.png` 与 `master-goods-android/UI-DESIGN-SPEC.md`。
