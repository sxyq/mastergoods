# 智慧记 iOS 原生端

这是智慧记的 SwiftUI 原生 iOS 客户端，目标是复用现有后端 API，并保持与 Android 移动端一致的业务语义与交互方式。

## 启动入口

- App 入口：`ZhihuijiIOS/ZhihuijiIOSApp.swift`
- 路由与会话：`ZhihuijiIOS/App/AppRouter.swift`
- 会话状态：`ZhihuijiIOS/App/AppSession.swift`
- 环境配置：`ZhihuijiIOS/App/AppEnvironment.swift`

## 设计系统

- 主题令牌：`ZhihuijiIOS/Core/Design/ZhihuijiTheme.swift`
- 共享样式：`ZhihuijiIOS/Core/Design/Components/ViewStyles.swift`
- 常用组件：
  - `EmptyStateView`
  - `LoadingStateView`
  - `MetricCard`
  - `PrimaryGlassButton`
  - `StatusChip`

## API 与认证

- API 客户端：`ZhihuijiIOS/Core/API/APIClient.swift`
- API 路由：`ZhihuijiIOS/Core/API/APIEndpoint.swift`
- API 错误：`ZhihuijiIOS/Core/API/APIError.swift`
- 登录态持久化：`ZhihuijiIOS/Core/Auth/AuthTokenStore.swift`，access token 与 refresh token 使用 Keychain generic password 保存，并以 `com.zhihuiji.ios.auth` 作为 service 命名空间。

## 页面结构

- 登录：`Features/Auth`
- 底部顶级入口：`首页 / 单据 / 档案 / 报表 / 助手`，与 Android 当前移动端主导航保持一致。
- 首页：`Features/Dashboard`
- 单据：`Features/DocumentsHomeView.swift`，由 `sales:view` / `purchase:view` / `finance:view` / `inventory:view` 任一权限触发显示。
- 档案：`Features/ArchivesHomeView.swift`
- 报表：`Features/Reports`
- 助手：`Features/Agent`
- 库存：`Features/Inventory`，通过首页、档案商品详情和单据中心进入；仓库/库存角色只要有 `inventory:view` 就能看到单据入口。
- 资金：`Features/Finance`，通过首页和单据中心进入；现金调整记录可读，新增/删除由 `finance:write` 控制。
- 设置：`Features/Settings`，通过每个顶级页右上角设置按钮和首页系统设置入口进入。

## 权限与导航收口

- 页面层不再直接散写 `session.hasPermission(...)`；导航、按钮和写操作统一收口到对应的 `*ActionPolicy` / `*VisibilityPolicy` 对象。
- 相关测试已覆盖：
  - `InlinePermissionAuditTests`
  - `RolePermissionMatrixTests`
  - 各页面的 `*ActionPolicyTests`

## 约定

- 所有大 ID 都使用 `EntityID`，避免 JS / JSON 数字精度问题。
- 页面优先承接真实后端返回，不伪造本地业务数据。
- 权限通过 `Permission`、`PermissionPolicy` 以及各页面 policy 对象控制导航、按钮和写操作。
- 401 会清理会话并回到登录页，403 会展示访问受限提示。

## 备注

当前环境没有完整 Xcode 工具链时，`swiftc -typecheck` 只能作为源码级校验；完整运行验收仍建议在具备 `xcodebuild` 和模拟器 SDK 的机器上执行。
