# Android app 模块分析

- 对应源码目录：`master-goods-android/app`
- 关键源码：
  - `MainActivity.kt`
  - `ZhihuijiApp.kt`
  - `navigation/AppNavGraph.kt`
  - `navigation/MainScreen.kt`
  - `navigation/MainNavGraph.kt`
  - `security/RuntimeSecurityGuard.kt`
  - `security/SignatureIntegrityChecker.kt`

## 模块定位

`app` 模块在新版中的职责不是“承载具体业务页面样式”，而是：

- 管理启动与会话恢复
- 管理认证流与主流程切换
- 组织全局导航与全局状态
- 承接 owner 私有上下文、导入任务、同步状态、环境策略

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
| 应用入口与主壳 | 新版已做 | 旧版不是当前多模块 Compose 主壳 | 保持 `MainActivity + AppNavGraph + MainNavGraph` 总体结构 | 已完成认证流与五栏主壳 | 当前可构建运行 |
| 会话恢复 | 新版已做 | 旧版无当前账号体系 | 恢复当前用户会话 | 当前 `AuthViewModel + SessionStore` 已承担基础恢复 | 只恢复到了 token 级别 |
| 发布版运行时防护 | 新版已做 | 首版缺少启动早期的运行时风险阻断 | 在 release 启动期拦截高风险调试/注入/重签名场景 | `MainActivity` 已接入 `FLAG_SECURE`、`RuntimeSecurityGuard`、`SignatureIntegrityChecker` | 保护链路已前移到业务 UI 之前 |
| 备份与数据提取收口 | 新版已做 | 首版更接近默认 Android 应用配置 | 显式关闭系统备份与设备迁移导出应用数据 | `AndroidManifest.xml`、`backup_rules.xml`、`data_extraction_rules.xml` 已落地 | 与会话加密一起降低离线提取风险 |
| owner bootstrap | 需重构 | 旧版无统一 owner 语义 | 登录后建立 owner 私有数据上下文 | 当前没有显式 owner bootstrap 过程 | 新版 app state 核心任务 |
| 全局导入/同步状态 | 新版待做 | 旧版无 server import | App 级统一感知导入任务、同步基线、异常诊断 | 当前设置和同步仍偏局部 | 需要 AppState |
| release 环境策略 | 新版已做 | 首版联调阶段更宽松 | 正式版只允许受控主机和受控策略 | 当前已有安全收口 | 后续继续与 `/v2` 对齐 |
| “页面自身管理全部业务全局状态”思路 | 新版需要去掉 | 首版为了快常让页面自己扛更多状态 | 全局状态回收到 app/data 层 | 当前部分逻辑仍散在页面 | 后续重构重点 |

## 新版 AppState 规划

| AppState 片段 | 状态 | 说明 |
|---|---|---|
| `sessionState` | 新版已做 | 当前已有基础登录态恢复语义 |
| `ownerScopeState` | 新版待做 | 当前账号所对应的数据边界、初始化完成度 |
| `syncState` | 新版待做 | 最近同步时间、队列状态、失败原因、owner 分桶游标 |
| `importState` | 新版待做 | 旧数据导入任务、服务端处理状态、冲突提示 |
| `environmentState` | 需重构 | debug/release 环境切换、受控主机、诊断信息 |

## 导航层新版影响

| 对象 | 状态 | 当前实现 | 新版影响 |
|---|---|---|---|
| `AppNavGraph` | 需重构 | 负责认证流与主流程切换 | 需要在登录后插入 owner/bootstrap 判断 |
| `MainNavGraph` | 需重构 | 主要组织五栏与子路由 | 需要适配 `/v2` 场景拆分与 owner 私有页面输入 |
| `Settings` 入口 | 需重构 | 目前偏首版设置项 | 新版要承接账号、同步、导入、环境、安全 |
| 顶层 reselect / 返回栈策略 | 待验证 | 当前服务现有五栏主壳 | 新版等场景拆分后需再核对 |

## 本模块在本阶段不做的事

- 不改具体导航 UI 样式
- 不改底栏设计细节
- 不改页面视觉

本阶段只把 `app` 模块的**职责变化与状态变化**文档化。
