# Android UI 全面重构 Spec

## Why

当前 Android 端 UI 实现与设计稿（`docs/design-mockups/01.png ~ 08.png`）差距显著：信息密度不均（首页过高、其他页不够简洁）、关键页面缺失、组件样式不匹配。用户要求删除现有 UI 源码后根据后端接口、设计方案和文档重新构建，并参考 BiliPai 的玻璃态实现。

## What Changes

- **BREAKING**: 删除 `feature/*` 全部现有 Screen/ViewModel 源码（保留模块目录与 build.gradle.kts）
- **BREAKING**: 删除 `app/src/main/java/com/zhihuiji/app/navigation/` 下除 `AppNavGraph.kt` 外的全部导航文件
- **BREAKING**: 删除 `core/designsystem/` 全部现有组件源码，重建为 Liquid Glass（液态玻璃）风格
- 重建所有 feature 模块 Screen + ViewModel，严格对照 8 张设计稿
- 重建 `core/designsystem`：Liquid Glass 主题、玻璃卡片、KPI 卡片、图表、状态标签等
- 重建导航体系：五栏底部导航 + 各模块子导航
- 所有数据层继续消费已落地的 `/v2` Repository，不改动 data/core 层业务契约

## Impact

- Affected specs: `docs/design-mockups/README.md`, `master-goods-android/UI-DESIGN-SPEC.md`, `docs/spec/31-android-impact.md`
- Affected code: `feature/*`, `app/navigation/*`, `core/designsystem/*`
- 不受影响的层：`core/model`, `core/network`, `core/database`, `core/datastore`, `data/*`, `backdrop/`

## ADDED Requirements

### Requirement: Liquid Glass 设计系统
The system SHALL provide a `core/designsystem` module implementing iOS-style liquid glass effects using the `backdrop` library.

#### Scenario: GlassCard
- **WHEN** any screen renders a content card
- **THEN** it uses `LiquidGlassCard` with blur, vibrancy, lens refraction, highlight, and inner shadow

#### Scenario: Theme
- **WHEN** the app launches
- **THEN** `ZhihuijiTheme` provides colors matching `#1677FF` primary, success `#18B66A`, warning `#FF9F1A`, danger `#F04438`

### Requirement: 登录/注册页
The system SHALL provide `feature/auth` pages matching design mockup 02.png.

#### Scenario: Login
- **WHEN** user opens the app unauthenticated
- **THEN** they see a centered logo, phone/password inputs with icons, blue gradient login button, "remember me" checkbox, "forgot password" link, and third-party login options

### Requirement: 首页经营看板
The system SHALL provide `feature/dashboard` matching design mockup 01.png leftmost screen.

#### Scenario: Dashboard overview
- **WHEN** user lands on home tab
- **THEN** they see "今日经营" header with date, four KPI cards (今日销售/待收款/低库存/净现金流), sales trend chart, pending reminders list, and quick action buttons

### Requirement: 单据域页面
The system SHALL provide `feature/sales`, `feature/purchases`, `feature/payments` with list/detail/editor screens matching mockups 05.png and 06.png.

#### Scenario: Sales order list
- **WHEN** user navigates to 单据 tab
- **THEN** they see status tabs (全部/待审核/待发货/待收款/已完成/已作废), search bar, filter chips, order list with status pills, and floating add button

### Requirement: 档案域页面
The system SHALL provide `feature/products`, `feature/customers`, `feature/suppliers` matching mockups 03.png and 04.png.

#### Scenario: Product list
- **WHEN** user navigates to 档案 tab
- **THEN** they see category filter chips, search bar, product list with image/name/price/stock status, and floating add button

### Requirement: 报表域页面
The system SHALL provide `feature/reports` matching mockup 07.png.

#### Scenario: Report overview
- **WHEN** user navigates to 报表 tab
- **THEN** they see date range selector, four KPI cards, sales trend chart, and tabbed report categories

### Requirement: AI 助手域页面
The system SHALL provide `feature/agent` with workbench/chat/drafts/tasks screens matching mockup 08.png.

#### Scenario: AI workbench
- **WHEN** user navigates to 助手 tab
- **THEN** they see AI greeting card, KPI mini cards, insight cards, quick actions, and suggested questions

### Requirement: 设置页
The system SHALL provide `feature/settings` matching mockup 02.png rightmost screen.

#### Scenario: Settings
- **WHEN** user opens settings
- **THEN** they see grouped cards: account security, notification settings, server address, data sync, language, cache clear, about, user agreement, privacy policy, and red logout button

## MODIFIED Requirements

### Requirement: Navigation
The `app/navigation` module SHALL use a rebuilt `MainScreen` with five-tab bottom bar and sub-graphs for each feature.

## REMOVED Requirements

### Requirement: Old designsystem components
**Reason**: Current components do not match liquid glass design target.
**Migration**: All feature screens will be rebuilt using new `core/designsystem` components.

### Requirement: Old feature screens
**Reason**: Current screens have incorrect layouts, missing pages, and poor information density.
**Migration**: ViewModels and Repository calls remain; only UI layer (Screen composables) is rewritten.
