# Zhihuiji iOS

## Current status

- SwiftUI native iOS app shell is in place.
- Real backend API reuse is the default path.
- Android-style mobile design tokens and page structure are the target.
- First-batch closure focuses on login, home, dashboard, AI chat, archives, settings, and core business list/detail flows.
- This is not a full parity claim. Missing capabilities stay listed below and must be completed later.

## Remaining gaps

- Media upload and binding are still client-surface only.
- Sync upload / pull / import-job lifecycle still needs full end-to-end proof.
- Cash-change records are blocked on a backend controller/service; iOS must not present this as an available flow yet.
- AI result blocks still need richer chart rendering coverage.
- Xcode build, simulator, and real-device screenshots are not yet available on this machine.

智慧记 iOS 原生端，基于 `SwiftUI + async/await + URLSession`。

## 当前目标

- 复用现有后端 API，不改 `web/`、`master-goods-android/`、`src/`
- UI 风格对齐 Android 现有移动端设计系统，而不是 Web PC
- 大 ID 一律按 `String` 处理，避免精度丢失
- 首批闭环优先：登录、首页、销售、采购、商品、库存、财务、报表、AI、员工管理

## Goal 2 拆解

### Phase 1：工程与运行底座

- `ios/ZhihuijiIOS.xcodeproj/project.pbxproj`
- `ios/ZhihuijiIOS/ZhihuijiIOSApp.swift`
- `ios/ZhihuijiIOS/App/AppEnvironment.swift`
- `ios/ZhihuijiIOS/App/AppSession.swift`
- `ios/ZhihuijiIOS/App/AppRouter.swift`
- `ios/ZhihuijiIOS/Core/API/*`
- `ios/ZhihuijiIOS/Core/Auth/*`

目标：

- 建立 SwiftUI 原生工程、环境切换、Keychain token、401/403 会话治理
- Router 按权限裁剪 Tab，不复刻 Web PC 导航
- 真实后端 API 走 `URLSession`，AI 走 `text/event-stream`

### Phase 2：Android 风格 1:1 设计基线

- `ios/ZhihuijiIOS/Core/Design/ZhihuijiTheme.swift`
- `ios/ZhihuijiIOS/Core/Design/Components/StatusChip.swift`
- `ios/ZhihuijiIOS/Core/Design/Components/MetricCard.swift`
- `ios/ZhihuijiIOS/Core/Design/Components/EmptyStateView.swift`
- `ios/ZhihuijiIOS/Core/Design/Components/LoadingStateView.swift`
- `ios/ZhihuijiIOS/Core/Design/Components/PrimaryGlassButton.swift`
- `ios/ZhihuijiIOS/Core/Design/Components/ViewStyles.swift`

目标：

- 颜色、圆角、玻璃材质、按钮、状态标签、空态、加载态全部锚定 Android 现有设计系统
- 保持移动端分层：顶部标题 / 玻璃卡片 / 渐变操作按钮 / 紧凑表单流

### Phase 3：认证、权限、门店身份

- `ios/ZhihuijiIOS/Features/Auth/*`
- `ios/ZhihuijiIOS/Features/Settings/SettingsView.swift`
- `ios/ZhihuijiIOS/Features/Settings/StaffManagementView.swift`

目标：

- 登录、登出、当前门店、当前成员、角色、权限拉通
- “一个店一个店长（总）+ 多员工”的权限语义落到 iOS 端
- 店长可管理员工；员工仅可见授权模块和写操作

### Phase 4：业务主链路

- `ios/ZhihuijiIOS/Features/Dashboard/*`
- `ios/ZhihuijiIOS/Features/DocumentsHomeView.swift`
- `ios/ZhihuijiIOS/Features/Sales/*`
- `ios/ZhihuijiIOS/Features/Purchases/*`
- `ios/ZhihuijiIOS/Features/Products/*`
- `ios/ZhihuijiIOS/Features/Inventory/*`
- `ios/ZhihuijiIOS/Features/Finance/*`
- `ios/ZhihuijiIOS/Features/Reports/*`
- `ios/ZhihuijiIOS/Features/Agent/*`
- `ios/ZhihuijiIOS/Features/ArchivesHomeView.swift`

目标：

- 每个模块先完成“真实 API + 可操作 UI + 权限收口”
- 商品、客户、供应商、销售、采购、库存、资金、报表、AI 都不保留 Web 壳页语义
- 商品编辑、员工管理、AI 会话、收款/退货等页面要形成可独立闭环

### Phase 5：测试与环境验收

- `ios/ZhihuijiIOSTests/*`
- `ios/README.md`

目标：

- 维持 Swift 源文件、Xcode target 引用与测试清单可审计；本机没有 `swiftc` 时不宣称已通过编译
- 补充大 ID、snake_case、权限模型、SSE 事件、关键 payload 测试
- 若本机补齐完整 `Xcode.app`，再跑 `xcodebuild` 模拟器 build/test

## 文件级执行序列

### Wave A：壳层与会话底座

1. `ios/ZhihuijiIOS/ZhihuijiIOSApp.swift`
2. `ios/ZhihuijiIOS/App/AppEnvironment.swift`
3. `ios/ZhihuijiIOS/App/AppSession.swift`
4. `ios/ZhihuijiIOS/App/AppRouter.swift`
5. `ios/ZhihuijiIOS/Core/API/APIClient.swift`
6. `ios/ZhihuijiIOS/Core/API/APIEndpoint.swift`
7. `ios/ZhihuijiIOS/Core/Auth/*`

交付标准：

- 登录后能恢复会话、识别当前门店和成员身份
- 401 自动清会话回登录，403 保持页面级拒绝语义
- Tab 与入口只由权限驱动，不出现 Web PC 侧栏思维

### Wave B：Android 视觉令牌与通用组件

1. `ios/ZhihuijiIOS/Core/Design/ZhihuijiTheme.swift`
2. `ios/ZhihuijiIOS/Core/Design/Components/ViewStyles.swift`
3. `ios/ZhihuijiIOS/Core/Design/Components/PrimaryGlassButton.swift`
4. `ios/ZhihuijiIOS/Core/Design/Components/StatusChip.swift`
5. `ios/ZhihuijiIOS/Core/Design/Components/MetricCard.swift`
6. `ios/ZhihuijiIOS/Core/Design/Components/EmptyStateView.swift`
7. `ios/ZhihuijiIOS/Core/Design/Components/LoadingStateView.swift`

交付标准：

- 颜色、玻璃面、描边、阴影、圆角、按钮梯度与 Android 一致
- 页头、卡片、表单、底部动作区都采用移动端层次，不照搬 PC 布局

### Wave C：角色权限与组织模型

1. `ios/ZhihuijiIOS/Features/Auth/LoginView.swift`
2. `ios/ZhihuijiIOS/Features/Auth/LoginViewModel.swift`
3. `ios/ZhihuijiIOS/Features/Settings/SettingsView.swift`
4. `ios/ZhihuijiIOS/Features/Settings/StaffManagementView.swift`
5. `ios/ZhihuijiIOSTests/AuthPermissionTests.swift`

交付标准：

- 一个店一个店长（总）+ 多员工的角色语义清晰可见
- 店长可管理员工；员工只能看到授权模块、按钮和写操作

### Wave D：核心业务页面闭环

1. `ios/ZhihuijiIOS/Features/Dashboard/*`
2. `ios/ZhihuijiIOS/Features/DocumentsHomeView.swift`
3. `ios/ZhihuijiIOS/Features/Sales/*`
4. `ios/ZhihuijiIOS/Features/Purchases/*`
5. `ios/ZhihuijiIOS/Features/Products/*`
6. `ios/ZhihuijiIOS/Features/ArchivesHomeView.swift`
7. `ios/ZhihuijiIOS/Features/Inventory/*`
8. `ios/ZhihuijiIOS/Features/Finance/*`
9. `ios/ZhihuijiIOS/Features/Reports/*`
10. `ios/ZhihuijiIOS/Features/Agent/*`

交付标准：

- 每一页都以真实 API 为主，不保留演示态假列表
- 收款、退货、收货、盘点、付款、AI 对话、员工管理都能独立进入并完成闭环
- 页面结构对齐 Android 移动稿：标题区、摘要卡、动作条、列表流、表单抽屉/弹层

### Wave E：静态测试与工程收口

1. `ios/ZhihuijiIOSTests/APIClientTests.swift`
2. `ios/ZhihuijiIOSTests/ModelDecodingTests.swift`
3. `ios/README.md`
4. `ios/ZhihuijiIOS.xcodeproj/project.pbxproj`

交付标准：

- Swift 源文件均加入 Xcode target，且本机具备 Swift/Xcode 工具链时可运行编译检查
- 大 ID、snake_case、权限模型、SSE 事件、关键业务 payload 全部有测试覆盖
- 若环境允许，再补 `xcodebuild` build/test

## Android 对齐检查清单

- 背景：浅蓝到白渐变 + Aurora 光斑
- 容器：玻璃白卡，14-16pt 圆角，0.5pt 白描边
- 主按钮：亮蓝到主蓝横向渐变，胶囊圆角
- 次按钮：轻色底 + 同色文字，不做厚重实体按钮
- 结构：标题说明在上，摘要卡在前，列表与表单顺流，下方保留动作区
- 文案：沿用安卓移动业务语义，不写 PC 管理后台口吻
- 权限：无权限时隐藏入口或展示拒绝态，不给“点进去再失败”的假动作

## 当前执行状态

- 已完成 Wave A 与 Wave B 主体
- Wave C 已基本可用，仍在继续加细员工管理与权限边界
- Wave D 已完成大部分首版页面，当前优先收口库存、档案、报表、AI 的真实业务细节
- Wave E 持续进行中；当前机器缺 `xcodebuild`、`swift` 与 `swiftc`，只能做源码结构、target 引用和 Git 静态检查

## Android 视觉对齐规则

- 主品牌色：`#005BBF`
- 顶层背景：浅蓝到白的渐变底，叠加柔和 Aurora 光斑
- 主要容器：半透明白色玻璃卡片，14-16pt 圆角，细白描边，轻阴影
- 强操作按钮：亮蓝到主蓝横向渐变
- 文本层级：标题紧凑、正文克制、金额与 KPI 更粗更亮
- 页面结构沿用 Android 移动语义：顶部工具栏 + 内容流 + 底部 Tab
- 交互语义优先参考 Android 当前已落地页面，而不是 PC 管理端信息密度

## Android 参考锚点

- 颜色与背景：`master-goods-android/core/designsystem/src/main/java/com/zhihuiji/core/designsystem/ZhihuijiColors.kt`
- 玻璃壳层：`master-goods-android/core/designsystem/src/main/java/com/zhihuiji/core/designsystem/GlassScaffold.kt`
- 玻璃卡片：`master-goods-android/core/designsystem/src/main/java/com/zhihuiji/core/designsystem/LiquidGlassCard.kt`
- 输入框：`master-goods-android/core/designsystem/src/main/java/com/zhihuiji/core/designsystem/GlassTextField.kt`
- 状态与 KPI：`StatusPill.kt`、`KpiCard.kt`
- 商品编辑：`feature/products/.../ProductEditScreen.kt`
- AI：`feature/agent/.../AgentChatScreen.kt`
- 员工管理：`feature/settings/.../StaffManagementScreen.kt`

## 当前实施顺序

1. App / Session / Router / API 基础层
2. Design Tokens / 通用组件
3. 登录与权限壳
4. Dashboard / 销售 / 采购 / 商品首批页面
5. 报表 / AI / 设置与员工管理
6. SSE、测试、Xcode 工程收口

## 当前进度

- 已完成 SwiftUI 原生工程骨架、设计令牌、Keychain token、权限化 Tab Router、主要业务 API 客户端
- 已完成登录、首页、单据中心、销售、采购、商品、库存、财务、报表、AI、员工管理的 iOS 首批闭环
- AI 已接入 SSE 事件读取、停止生成请求、结构化结果块展示与运行审计面板；最终生产通过仍需要真实 provider 抓包、真机 UI 截图和 cancel 断流证据
- AI 已补上显式新建会话、删除会话、问题存草稿、草稿编辑与回填输入框的移动端工作流；会话状态按后端合同使用 `active / closed / archived`，草稿状态使用 `active / archived`
- iOS 测试已覆盖 AI 会话/草稿状态合同与关键 payload 编码，避免再回退到 `open`
- iOS 已接入 `/v2/media/*`、`/v2/sync/*`、`/v2/import-jobs/*` 客户端基础层，并在设置页提供同步/导入与媒体管理入口；这些能力仍以客户端可调用为主，后端导入 worker 与真机验收还没收口
- 当前正在补“收口层”工作：把还偏薄的页面从首批闭环收成真实业务页，优先处理商品供应关系、采购/销售退货、财务单据、Android 风格一致性
- 近期已继续统一基础组件、入口壳、销售/采购/库存/财务/报表/设置/商品详情编辑/退货页的字号、圆角和卡片层级，整体更接近 Android 移动端语义
- 当前机器未安装完整 Xcode/Swift 命令行工具，`xcodebuild`、`swift` 与 `swiftc` 都不可用；本轮只能完成源码结构、Xcode target 引用和 Git 静态检查，不能声称已通过编译

## 尚未完成 / 尚未验证

- API 覆盖：已补上 media 读写与 `/v2/sync`、`/v2/import-jobs` 的客户端基础层；`cash_change_records` 当前只有后端表/DTO/Repository 线索，未发现可复用 Controller/Service，iOS 不应声明已可用；仍缺完整库存同步与导入任务后台生命周期
- 页面字段：部分商品、采购、库存、报表字段仍是首批移动端闭环，后续需要继续对齐 Android 已落地的完整字段、筛选和批量操作
- AI 展示：当前已支持 Markdown、表格、KPI、排行、风险卡、证据卡、草稿卡、折线/柱状/环形图和未知块降级；但仍缺真实 provider 抓包、cancel 断流证据、逐块截图和坏数据回归样例
- Android 等价交互：移动端玻璃卡片、底部 Tab、权限裁剪和表单流已对齐；仍需逐页对照 Android 截图做视觉偏差清单
- 构建与真机：当前机器缺完整 Xcode/Swift 命令行工具，尚未完成 `xcodebuild`、`swiftc`、模拟器启动、真机截图、UI tree、log 证据
- 后端依赖：cash-change、完整库存同步、导入 worker 与真实对象存储上传链仍需要后端或运行时环境继续补齐，本轮 iOS 不修改后端合同或实现

## 约束

- 未经允许不提交、不推送
- 不把供应商渠道密钥、模型 URL 逻辑放到 iOS 端
- 真实接口契约以当前后端 Controller + DTO 与 Web `client.ts/contracts.ts` 为准
