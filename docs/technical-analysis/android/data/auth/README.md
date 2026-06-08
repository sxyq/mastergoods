# Android data/auth 模块分析

- 对应源码目录：`master-goods-android/data/auth`
- 关键源码：`AuthRepository.kt`

## 模块定位

`data/auth` 在新版里不仅负责登录/注册/恢复 token，还要负责：

- 登录后的 owner 上下文初始化入口
- 会话失效后的清理策略
- 与导入/同步初始化相关的首个业务跳板

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
| 登录/注册/恢复会话仓储 | 新版已做 | 旧版本地账本无当前账号体系 | 支撑当前 users/sessions 登录链路 | `AuthRepository.kt` 已存在 | 配合 `SessionStore` 使用 |
| owner-aware 登录后初始化 | 新版待做 | 旧版无统一 owner | 登录后拉起 owner 边界内的初始化流程 | 当前仍以首版会话恢复为主 | 等后端归属改造 |
| 导入前置校验 | 新版待做 | 旧版无 server import 方案 | 判断当前账号是否已导入、是否需提示迁移 | 当前无相关状态模型 | 与 sync/import 设计联动 |
| “登录成功即代表业务就绪”思路 | 新版需要去掉 | 首版常把登录成功等同于进入主页 | 登录成功后仍需 owner/bootstrap 检查 | 当前主流程仍偏首版假设 | AppState 后续承接 |

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 当前视觉真源以 Stitch 导出、`docs/spec/42-android-liquid-glass-ui-refactor-plan.md` 与 `master-goods-android/UI-DESIGN-SPEC.md` 为准；`docs/design-mockups/` 仅作历史参考。
