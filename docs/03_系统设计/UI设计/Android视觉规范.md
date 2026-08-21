# Android 视觉规范

## 文档信息

| 字段 | 内容 |
|---|---|
| 文档类型 | 系统设计 |
| 当前状态 | 已完成（规范存在）；真机验证 Blocked |
| 适用端 | Android |
| 依据源码 | `Code/frontend/android/UI-DESIGN-SPEC.md`、`DEVELOPMENT-PLAN.md`、`core/designsystem/` |
| 依据测试 | `testing/安卓/功能测试/TEST_PLAN.md` |
| 依据证据 | `testing/.artifacts/2026-08-18-8220-current-baseline/current-8220-baseline.md`（参考图静态检查） |
| 最后核对 | 2026-08-20 |

## 一、色彩规范（UI-DESIGN-SPEC.md 摘要）

| 类别 | 值 |
|---|---|
| 背景顶部 | `#E1EFFF`（浅蓝极光渐变） |
| 背景过渡 | `#FFFFFF` |
| 基础背景 | `#F7F9FE` |
| 主品牌蓝 | `#005BBF` |
| 亮蓝强调 | `#1A73E8` |
| 成功 | `#34A853` |
| 警告 | `#FB8C00` |
| 错误 | `#EA4335` |
| 主文字一级 | `#181C20` |
| 主文字二级 | `#414754` |
| 数据二级 | `#6B7280` |

## 二、版式规范

| 项 | 值 |
|---|---|
| 页面左右边距 | 16dp |
| 区块垂直间距 | 12dp |
| 行内间距 | 8dp |
| 卡片内边距 | 16dp |

## 三、实现位置

- `core/designsystem/`：颜色（Color）、形状（Shape）、组件、主题。
- `app/src/main/java/com/zhihuiji/app/`：应用壳与导航。
- `backdrop/`：liquid glass / 毛玻璃底层实现（第三方视觉渲染模块）。

## 四、当前验证状态

- 8220 基线：参考图静态检查确认消息卡片、默认收起思考、展开执行过程、结果块和发送区结构已对应。
- 真机视觉验收：Blocked（本机无 adb，未执行截图/UI XML/logcat）。
- 首页顶部标题重叠已知问题 #10（历史 154 证据）、IME 顶部栏重叠 #19。

## 对应实现

- Android 代码：`core/designsystem/`、`feature/agent/`、`feature/dashboard/`
- iOS 代码：不适用
- Web 代码：不适用
- 后端代码：不适用
- Agent 代码：不适用

## 对应接口

- 接口路径：无
- 请求模型：无
- 响应模型：无
- SSE 事件：无

## 对应测试

- 视觉证据：`testing/.artifacts/2026-07-18-android-wave0/launch.png` 等
- 验收证据：`docs/05_测试与验收/验收证据/b11/screenshots/`

## 当前限制

- 未完成内容：真机视觉验收
- Blocked 内容：Android 真机（无 adb）
- Deferred 内容：无
- historical-only 内容：154 环境截图证据
