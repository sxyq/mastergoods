# iOS 视觉规范

## 文档信息

| 字段 | 内容 |
|---|---|
| 文档类型 | 系统设计 |
| 当前状态 | 待验证 |
| 适用端 | iOS |
| 依据源码 | `Code/frontend/ios/ZhihuijiIOS/Core/Design/ZhihuijiTheme.swift`、`Core/Design/Components/`（PrimaryGlassButton、MetricCard、StatusChip、EmptyStateView、LoadingStateView、ViewStyles）、`Core/Models/DisplayNames.swift` |
| 依据测试 | `Code/frontend/ios/ZhihuijiIOSTests/ModelDecodingTests.swift` 等 |
| 依据证据 | 无 8220 运行证据 |
| 最后核对 | 2026-08-20 |

## 一、设计系统组件（真实源码）

| 组件 | 源码 | 用途 |
|---|---|---|
| `ZhihuijiTheme` | `Core/Design/ZhihuijiTheme.swift` | 主题色（ColorToken） |
| `PrimaryGlassButton` | `Core/Design/Components/PrimaryGlassButton.swift` | 毛玻璃主按钮 |
| `MetricCard` | `Core/Design/Components/MetricCard.swift` | 指标卡片 |
| `StatusChip` | `Core/Design/Components/StatusChip.swift` | 状态标签 |
| `EmptyStateView` | `Core/Design/Components/EmptyStateView.swift` | 空态 |
| `LoadingStateView` | `Core/Design/Components/LoadingStateView.swift` | 加载态 |
| `ViewStyles` | `Core/Design/Components/ViewStyles.swift` | 通用样式 |

## 二、视觉规范要点

- 与 Android 移动端对齐：底部导航"首页/单据/档案/报表/助手"（PAGE_MAP.md）。
- 使用 SwiftUI（`struct ... : View` + `@StateObject`）。
- 角色显示名：`DisplayNames.swift`（`ASSISTANT` → "AI/只读助理"）。

## 三、当前状态

- 组件源码存在；单元测试覆盖模型解码与策略（`ModelDecodingTests`、`AuthPermissionTests`）。
- Agent 视觉规范在真机/模拟器的运行验证：待验证（本轮未展开 iOS 主流程）。

## 对应实现

- iOS 代码：`Core/Design/`、`Features/Agent/`
- Android 代码：不适用
- Web 代码：不适用
- 后端代码：不适用
- Agent 代码：不适用

## 对应接口

- 接口路径：无
- 请求模型：无
- 响应模型：无
- SSE 事件：无

## 对应测试

- 单元测试：`ZhihuijiIOSTests/`（ModelDecoding、策略测试）
- 功能测试：`testing/ios/功能测试/TEST_PLAN.md`

## 当前限制

- 未完成内容：iOS 视觉运行验证
- Blocked 内容：无
- Deferred 内容：无
- historical-only 内容：无
