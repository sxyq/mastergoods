# Android feature/auth 模块分析

- 对应源码目录：`master-goods-android/feature/auth`
- 关键源码：
  - `LoginScreen.kt`
  - `RegisterScreen.kt`
  - `AuthViewModel.kt`

## 模块定位

`feature/auth` 在新版里关注的是**进入系统的业务门槛**，不是登录页长什么样。  
它要承接：

- 登录/注册
- 会话恢复失败提示
- owner 初始化前的过渡态
- 导入前置提示或首次使用引导

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
| 登录/注册首版页面 | 新版已做 | 旧版本地账本无当前账号链路 | 支撑当前 users/sessions | 登录、注册、恢复会话页面已存在 | 能进入主流程 |
| owner-aware 首次初始化与导入提示 | 新版待做 | 旧版无统一 owner | 登录后感知账号边界、数据导入状态 | 当前仍偏首版登录流 | 等后端先改 |
| “登录成功后直接视为业务就绪” | 新版需要去掉 | 首版常见简化路径 | 新版要增加 owner/bootstrap 过渡态 | 当前文档已收口 | 代码后续跟进 |

## UI 统一约束

- 本模块后续新增业务必须继续复用当前设计图对应的页面母版，不允许因为领域变厚就切换成另一套视觉语言。
- 页面结构优先落入既有模式：列表页、详情页、编辑页、报表页、AI 页、设置页。
- 视觉基线固定为：浅蓝渐变背景、玻璃卡片、蓝色主按钮、白色次按钮、统一状态标签、五栏主壳。
- 如需新增 UI 组件，先沉淀到 `core/designsystem`，再由本模块复用；不允许长期保留 feature 私有样式组件。
- 验收时同时对照：`docs/design-mockups/01.png ~ 08.png`、`master-goods-android/UI-DESIGN-SPEC.md`、`docs/technical-analysis/android/core/designsystem/README.md`。
