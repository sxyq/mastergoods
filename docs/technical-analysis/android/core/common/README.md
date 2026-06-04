# Android core/common 模块分析

- 对应源码目录：`master-goods-android/core/common`
- 关键源码：
  - `MoneyFormatter.kt`
  - `TimeFormatter.kt`
  - `StatusLabels.kt`
  - `ResultExt.kt`
  - `UiMessage.kt`

## 模块定位

`core/common` 在新版里承接的是**跨领域通用语义层**，而不只是几个零散工具类。  
它需要给安卓端提供统一的：

- 金额与数量表达语义
- 时间与状态文案语义
- 错误与 UI 消息语义
- 兼容层与新版层之间的过渡适配规则

## 状态图例

- `新版已做`
- `新版待做`
- `旧版存在新版未做`
- `新版需要去掉`
- `需重构`
- `待验证`

## 当前源码覆盖

| 文件 | 当前作用 | 状态 | 说明 |
|---|---|---|---|
| `MoneyFormatter.kt` | 金额格式化 | 需重构 | 已同时支持 `BigDecimal` 和 `Double`，但调用侧仍未统一 |
| `TimeFormatter.kt` | 日期/时间格式化 | 新版已做 | 提供基础日期、日期时间、时分格式化 |
| `StatusLabels.kt` | 业务状态标签 | 需重构 | 已有首版状态码映射，但覆盖范围仍偏首版 |
| `UiMessage.kt` | 页面消息模型 | 新版已做 | 已统一 `ERROR/WARNING/INFO/SUCCESS` |
| `ResultExt.kt` | 老的 `ApiResponse` 扩展 | 新版需要去掉 | 已被标记 `@Deprecated`，只作为兼容过渡 |

## 状态表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| 格式化与状态文案基础层 | 新版已做 | 旧版 app 内部分散实现 | 抽到通用基础层统一复用 | 当前已集中在 5 个工具文件 | 可持续复用 |
| 金额精度语义 | 需重构 | 旧版字段更厚且金额更敏感 | 与后端统一向 `BigDecimal` 语义过渡 | 当前 `MoneyFormatter` 已兼容 `BigDecimal`，但模型层仍大量使用 `Double` | 会波及 formatter、DTO、Room、报表 |
| 旧版更细的业务状态 | 旧版存在新版未做 | 旧版有更丰富状态/来源字段 | 扩展标签、筛选文案、错误文案体系 | 当前 `StatusLabels` 仍偏首版字段 | 等 `/v2` 状态集落地 |
| 旧 `ApiResponse.requireData()` 思路 | 新版需要去掉 | 首版为加速开发常把响应解包逻辑塞进 common | 新版统一走 `safeApiCall + Result` 链路 | `ResultExt.kt` 已被标记废弃 | 后续代码阶段彻底移除 |

## 关键字段与语义断点

### 金额与数量

| 对象 | 当前实现 | 状态 | 新版建议 |
|---|---|---|---|
| `MoneyFormatter.format(BigDecimal?)` | 已有 | 新版已做 | 作为新版金额展示主入口 |
| `MoneyFormatter.format(Double?)` | 仍大量依赖 | 需重构 | 逐步降级为兼容层 |
| `formatSigned(Double?)` | 仅 `Double` 版本 | 需重构 | 后续补 `BigDecimal` 语义版本 |
| 数量格式化 | 尚无统一数量 formatter | 新版待做 | 后续增加 `QuantityFormatter` 或等价能力 |

### 状态文案

| 能力 | 当前覆盖 | 状态 | 新版建议 |
|---|---|---|---|
| 销售/采购/付款首版状态 | 已覆盖 | 新版已做 | 继续保留兼容层 |
| 财务账户、库存账本、导入任务、owner 初始化状态 | 未覆盖 | 新版待做 | 新增更完整状态标签集 |
| AI 任务状态 | 已覆盖 `AgentTaskStatus` | 新版已做 | 是当前相对完整的域 |

### 错误与消息

| 能力 | 当前覆盖 | 状态 | 新版建议 |
|---|---|---|---|
| `UiMessage.Type` | `ERROR/WARNING/INFO/SUCCESS` | 新版已做 | 继续沿用 |
| 认证失败、owner 冲突、导入冲突、字段校验失败分级 | 未显式结构化 | 新版待做 | 后续扩展错误码到 UI 消息映射 |

## 新版 `core/common` 重构方向

1. 保留当前工具作为兼容层。
2. 新增更明确的语义工具：
   - 金额格式化统一到 `BigDecimal`
   - 数量格式化单独抽象
   - owner/bootstrap/import/sync 状态文案补齐
3. 逐步清理不再推荐的兼容能力：
   - `ResultExt.requireData()`
   - 仅 `Double` 的金额主路径

## 当前结论

- `core/common` 现在已经有一个还不错的基础雏形。
- 真正要进入新版时，这里最关键的不是“再加几个 util”，而是把**金额、状态、错误**三种跨域语义统一下来。

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 视觉真源固定为 `docs/design-mockups/01.png ~ 08.png` 与 `master-goods-android/UI-DESIGN-SPEC.md`。
