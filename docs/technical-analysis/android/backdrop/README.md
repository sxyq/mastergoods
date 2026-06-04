# Android backdrop 模块分析

- 对应源码目录：`master-goods-android/backdrop`
- 来源：本地引入的 liquid glass / backdrop 实现
- 关键源码：
  - `build.gradle.kts`
  - `Backdrop.kt`
  - `DrawBackdropModifier.kt`
  - `RuntimeShaderCache.kt`
  - `backdrops/*`
  - `effects/*`

## 模块定位

`backdrop` 是安卓端当前视觉技术栈里的**底层渲染模块**。  
它不承接业务领域逻辑，也不直接定义页面结构；它负责的是：

- 为 `core/designsystem` 提供玻璃/液态表面底层能力
- 承接 shader、模糊、折射等渲染细节
- 在后端重构期保持视觉底层稳定，不成为额外变量

## 状态图例

- `新版已做`
- `新版待做`
- `旧版存在新版未做`
- `新版需要去掉`
- `需重构`
- `待验证`

## 当前源码与构建信息

| 对象 | 当前实现 | 状态 | 说明 |
|---|---|---|---|
| `namespace` | `com.kyant.backdrop` | 新版已做 | 本地模块独立 namespace |
| `compileSdk` | `35` | 新版已做 | 与当前安卓工程保持一致方向 |
| `minSdk` | `26` | 新版已做 | 与主工程下限对齐 |
| `compose = true` | 已开启 | 新版已做 | 明确是 Compose 视觉底层模块 |
| 关键运行时 | `RuntimeShaderCache.kt` | 新版已做 | 当前 shader 缓存底层已存在 |
| 关键绘制入口 | `Backdrop.kt` / `DrawBackdropModifier.kt` | 新版已做 | 当前玻璃绘制底层已存在 |

## 状态表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| 液态玻璃底层能力 | 新版已做 | 旧版无此层抽象 | 为 `core/designsystem` 提供玻璃效果底层 | 本地模块已接入并被设计系统使用 | 不是业务模块 |
| 与设计稿的最终贴合度 | 待验证 | 旧版 UI 参考不同 | 在真机上验证折射、模糊、层次感 | 代码已接入，效果仍需持续微调 | 依赖设计验收 |
| 替换为其他视觉库 | 新版需要去掉 | 可能有再次更换 UI 引擎冲动 | 当前阶段保持 backdrop 稳定 | 不建议在后端重构阶段再换底层库 | 先稳住技术栈 |
| 业务域耦合到底层渲染 | 新版需要去掉 | 首版容易让视觉底层被业务组件牵着走 | backdrop 只做渲染，不做领域组件 | 当前文档已明确分层 | 代码后续继续保持边界 |

## 新版阶段对 backdrop 的要求

| 要求 | 状态 | 说明 |
|---|---|---|
| 维持稳定 | 新版已做 | 后端重构阶段不要引入新的视觉底层变量 |
| 只通过 `core/designsystem` 暴露能力 | 需重构 | 页面和业务组件不应直接依赖 backdrop 内部实现 |
| 保持与主工程 SDK/Compose 兼容 | 待验证 | 后续升级主工程时要一起检查 |

## 当前结论

- `backdrop` 现在的角色应该被清楚定义为“视觉渲染底层”。
- 在接下来的后端与安卓架构重构阶段，它最好的状态就是：**稳定、少变、不承担业务语义**。

## UI 联动约束

- `backdrop` 直接服务页面视觉底层，但只负责渐变、玻璃、模糊、折射等渲染能力，不负责业务语义。
- 后续新增业务不能因为页面变复杂就另起一套背景、阴影或玻璃技术方案；仍应通过 `core/designsystem` 复用当前视觉底层。
- 真机验收时，`backdrop` 的目标是让新页面继续保持与设计图一致的冷色渐变、轻玻璃和层次感，而不是单独追求炫技效果。
- Android 视觉真源固定为 `docs/design-mockups/01.png ~ 08.png` 与 `master-goods-android/UI-DESIGN-SPEC.md`。
