# Android data/finance 模块分析

- 对应源码目录：`master-goods-android/data/finance`
- 关键源码：`FinanceRepository.kt`、`FinanceV2Repository.kt`

## 模块定位

`data/finance` 当前只覆盖轻量资金流水。  
新版里，它会成为安卓侧财务域的数据入口，逐步扩展到：

- 账户主数据
- 账户余额
- 转账
- 单据资金关联
- 找零/零钱
- 项目维度
- owner 私有财务账本

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
| 资金流水列表与新增 | 新版已做 | 旧版资金域更厚 | 支撑当前收支流水闭环 | `FinanceRepository.kt` 已实现 | 已供列表和新增使用 |
| 账户、项目、找零、转账 | 旧版存在新版未做 | 旧版 `funds + accts + smallchange` 更完整 | 新版财务域要超过旧版 | 当前仅有轻量 `finance_records` 仓储 | 是明确扩域点 |
| owner 与 `/v2` 财务契约 | 待验证 | 旧版无统一 owner | 资金数据按 owner 过滤并升级接口 | 已新增 `FinanceV2Repository.kt`，承接 `/v2/accounts`、`/v2/account-transfers`、`/v2/bill-fund-links`；B08 修复：`deleteAccount`/`deleteBillFundLink` 已使用 `safeApiUnitCall`（与 `AgentV2Repository`/`MediaV2Repository` 修复后风格一致），`@Query` 参数名已验证与后端一致 | 轻量 `FinanceRepository` 仍保留给 `/v1` 兼容层 |

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 当前视觉真源以 Stitch 导出、`docs/spec/42-android-liquid-glass-ui-refactor-plan.md` 与 `master-goods-android/UI-DESIGN-SPEC.md` 为准；`docs/design-mockups/` 仅作历史参考。
