# master-goods 历史 Android UI 视觉参考（reference only）

本文档与本目录描述的是**历史工程的 UI 视觉语言**，供未来产品参考视觉风格使用。本目录**不属于生产源码**。

## 这是什么

`reference/ui-android/` 是一个完全独立、单模块的 Android 参考工程，从即将删除的旧工程中抽取的 UI 层：

- `app/src/main/java/com/kyant/backdrop/` — 27 个 Kotlin 文件，第三方液态玻璃渲染库，只依赖 Compose，原位置为旧 Android 工程的 `backdrop/src/main/java/`
- `app/src/main/java/com/zhihuiji/core/designsystem/` — 25 个 Kotlin 文件，旧工程全部设计系统组件，原位置为旧 Android 工程的 `core/designsystem/src/main/java/`
- `app/src/main/java/com/zhihuiji/uireference/` — 2 个新写文件（`MainActivity.kt`、`UIReferenceScreen.kt`），把 designsystem 组件串成一个可滚动的展示页

backdrop 与 designsystem 的源码直接拷入 `app` 模块的对应 package（单模块方案），不拆子模块，不以 Gradle 子模块引用方式复用原工程。

## 使用边界（重要）

- 未来产品**可以参考**这里的视觉语言：液态玻璃容器、玻璃卡片、状态色体系、KPI 卡、顶栏/底栏母版、字体与间距规范。
- **不能默认复用**旧业务的信息架构（IA）、页面流程与业务概念；旧 IA 属于将被重写的领域层，与视觉语言是两件事。
- 新生产代码**不得直接依赖**本目录：不 include、不 import、不以任何形式把 `reference/ui-android` 接入生产构建。
- 完整旧实现（含业务代码）见 `archive/pre-domain-rewrite`（tag `pre-domain-rewrite-2026-09-25`）。

## 硬性约束

- 不包含后端、HTTP 接口、鉴权、数据同步、仓储、视图模型、真实业务模型、数据库与网络代码
- 只有一个主要页面 `UIReferenceScreen`，示例数据全部为静态字符串/数字
- 依赖范围仅限：AndroidX Core KTX、Activity Compose、Compose BOM、Material3、Material Icons Extended
- 源码与 Gradle 配置中没有任何指向旧仓库路径的引用，不以 Gradle 子模块引用方式复用原工程模块

## 目录结构

```
reference/ui-android/
├── README.md                    ← 本文件
├── UI-DESIGN-SPEC.md            ← cp 自旧 Android 工程根目录的同名文件（文首加了失效说明）
├── screenshots/
│   └── android-agent-reference-collapsed.png   ← 历史 APP UI 截图（cp 自 docs/03_系统设计/UI设计/）
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml
├── gradle/wrapper/              ← gradle-wrapper.jar + gradle-wrapper.properties（gradle 8.7）
├── gradlew
├── local.properties             ← 本地 SDK 路径，gitignored，不入库
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        └── java/
            ├── com/kyant/backdrop/**            ← 拷入 27 kt（含 com/kyant/shapes/RoundedRectangularShape.kt）
            ├── com/zhihuiji/core/designsystem/** ← 拷入 25 kt
            └── com/zhihuiji/uireference/**      ← 新写 2 kt
```

## UIReferenceScreen 分节

入口是 `ZhihuijiTheme`（强制浅色方案，历史玻璃视觉以浅底为基准），外层用 `GlassScaffold` 固定顶栏与底栏，中间为滚动 `Column`：

| 分节 | 展示内容 |
|------|----------|
| 1 | Theme · Color 色板（24 个颜色令牌） |
| 2 | Theme · Typography 字体层级 + 金额专用样式 |
| 3 | Theme · Spacing 间距 / Shape 形状（含 `MainBottomBarHeight`、`roundedCardShape`、`ZhihuijiShapes`） |
| 4 | Glass surface（`LiquidGlassSurface`、`LiquidGlassCard` Low/High） |
| 5 | Top bar（`GlassTopBar`，经 `GlassScaffold.topBar` 固定于屏幕顶部，始终可见） |
| 6 | Buttons（`PrimaryButton` / `SecondaryOutlineButton` / `DangerOutlineButton`，含 disabled 态） |
| 7 | TextField（`GlassTextField`） |
| 8 | Status pill（`StatusPill` 全部 7 种 `StatusType`） |
| 9 | Filter chip / Tabs（`FilterChipRow` + `SegmentedTabs`） |
| 10 | KPI card（`KpiCard` 正向/负向/无变化 三例） |
| 11 | List item（`BusinessListItem` ×2 + `DocumentListCard` ×2） |
| 12 | Quantity stepper（`QuantityStepper`） |
| 13 | Empty state（`EmptyState` + 主按钮 action） |
| 14 | Bottom action bar（`BottomActionBar`，经 `GlassScaffold.bottomBar` 固定于屏幕底部，始终可见；含合计金额行） |

§5 清单中的组件**全部成功展示**。BusinessListItem / KpiCard / DocumentListCard / StatusPill 的签名均为纯字符串、颜色与回调，用静态 sample data 即可构造，未遇到依赖已删除类型的组件。

### 已拷入但未在 UIReferenceScreen 展示的组件

均为纯 UI 组件，源码完整可用，只是不在 §5 必选清单内：

- `ChartCard`（图表卡，需要调用方提供 chartContent）
- `SearchFilterBar`（搜索筛选条）
- `FloatingGlassActionButton`（悬浮玻璃按钮，本页未摆放悬浮位）
- `DocumentChromeSpacing` 中的 `DocumentListFabBottomPadding` / `DocumentListBottomContentPadding`（仅 `MainBottomBarHeight` 在第 3 节展示）

## 构建

```bash
cd reference/ui-android
./gradlew :app:compileDebugKotlin --console=plain
```

本地需要 Android SDK（`local.properties` 的 `sdk.dir` 已从原工程拷入，不入 git）。已验证：`BUILD SUCCESSFUL`。

## 截图

`screenshots/android-agent-reference-collapsed.png` 是历史 APP 的真实 UI 截图（原位置 `docs/03_系统设计/UI设计/`，用 cp 保留原件）。本目录不生成新截图。

## 文档

`UI-DESIGN-SPEC.md` 为原文件的完整复制，仅在文首追加了一行说明：源工程已删除、文中相对链接已失效、完整实现见 `archive/pre-domain-rewrite`（tag `pre-domain-rewrite-2026-09-25`）。
