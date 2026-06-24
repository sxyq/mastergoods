# 三端函数级审计与性能安全优化交接说明

## 1. 项目介绍

这个仓库是一个混合三端项目，目标是同时审计并优化：

- 后端：Spring Boot 服务，主要在 `src/main/java`、`src/main/resources`、`src/test/java`
- Android：主工程在 `master-goods-android/`
- Web：管理端在 `web/`

当前总目标不是单点修 bug，而是做一轮全量的、函数级别的审计和低风险收缩：

1. 精确到每个文件、每个函数、每个组件、每个路由
2. 记录访问痕迹，确保“确实看过”
3. 在不改变功能和 UI 语义的前提下精简代码
4. 提升性能，包括：
   - 后端查询性能
   - Android 手机运行性能
   - Android 滑动/重组流畅度
   - Web 交互与渲染性能
5. 同步做全链路安全风险检查：
   - 鉴权
   - 权限
   - 数据隔离
   - 配置
   - 上传
   - SSE / AI
   - 缓存
   - 日志

## 2. 当前工作状态

当前函数级审计主台账是：

- `docs/performance-security-function-audit-2026-06-23.csv`
- `docs/performance-security-function-audit-2026-06-23.md`

这两份文件是后续所有工作的“单一事实来源”。每次读完文件或函数，都要同步回写 CSV，必要时再补一条 Markdown 进度日志。

### 已完成并验证的 Android 低风险优化

#### auth

- `master-goods-android/feature/auth/src/main/java/com/zhihuiji/feature/auth/AuthViewModel.kt`
- `master-goods-android/feature/auth/src/main/java/com/zhihuiji/feature/auth/LoginScreen.kt`
- `master-goods-android/feature/auth/src/main/java/com/zhihuiji/feature/auth/RegisterScreen.kt`

做过的事：

- login / register 复用共享 auth coroutine 包装
- logout 的 loading 清理改成 finally 语义
- 登录/注册页把背景刷、圆角形状提成文件级常量

验证：

- `./gradlew :feature:auth:compileDebugKotlin`

#### customers

- `master-goods-android/feature/customers/src/main/java/com/zhihuiji/feature/customers/CustomerListViewModel.kt`
- `master-goods-android/feature/customers/src/main/java/com/zhihuiji/feature/customers/CustomerDetailScreen.kt`
- `master-goods-android/feature/customers/src/main/java/com/zhihuiji/feature/customers/CustomerEditScreen.kt`

做过的事：

- 列表页改成单次预分配循环
- 详情页统一使用共享 `MoneyFormatter`
- 详情页余额文本做记忆化
- 编辑页表单状态按加载 snapshot 键控同步，避免 stale blank fields

验证：

- `./gradlew :feature:customers:compileDebugKotlin`

#### finance

已处理并验证的文件：

- `master-goods-android/feature/finance/src/main/java/com/zhihuiji/feature/finance/FinanceViewModel.kt`
- `master-goods-android/feature/finance/src/main/java/com/zhihuiji/feature/finance/DailyExpenseViewModel.kt`
- `master-goods-android/feature/finance/src/main/java/com/zhihuiji/feature/finance/FinanceRecordListScreen.kt`
- `master-goods-android/feature/finance/src/main/java/com/zhihuiji/feature/finance/DailyExpenseScreen.kt`
- `master-goods-android/feature/finance/src/main/java/com/zhihuiji/feature/finance/FinanceRecordDetailScreen.kt`

做过的事：

- `FinanceViewModel`
  - 缓存 filter 快照
  - 预分配 `ArrayList`
  - 抽出 `buildFinanceTitle`
  - 统一金额格式化为共享 `MoneyFormatter`
- `DailyExpenseViewModel`
  - `amountText` 改用共享 `MoneyFormatter`
- `FinanceRecordListScreen`
  - meta 文本从 `listOf + filter + joinToString` 改为直接分支拼接
- `DailyExpenseScreen`
  - 复用文件级 `roundedCardShape`
  - 预计算类别行、账户行、账户标签
  - 减少 card / selector / attachment / chip 的重复 shape 分配
- `FinanceRecordDetailScreen`
  - `remember(uiState.records, recordId)` 缓存当前记录查找
  - 缓存 amount color，减少重组时重复扫描

验证：

- `./gradlew :feature:finance:compileDebugKotlin`

## 3. 目前剩余内容

当前最新台账统计：

- 剩余 `PENDING`：`7164`
- 剩余文件：`705`
- 分布：
  - backend：`3879`
  - android：`2623`
  - web：`662`

结论很直接：现在只完成了很小一部分，剩余仍然很多，不能缩窄成“只做某几个模块”。

### 还没开始的大块内容

大多数剩余工作仍在这几类区域：

- Android 其他 feature 模块
- Android core / data / app 导航 / 安全 / database / network
- Web 的 `web/src/app`、`web/src/pages`、`web/src/shared`
- 后端 controller / service / repository / DTO / migration / test

继续工作时，以 CSV 里的 `PENDING` 顺序为准，不要凭印象挑文件。

## 4. 工作流程

### 4.1 开始前

1. 先跑 `git status --short`
2. 确认当前工作树里已有很多非本次任务的改动
3. 不要回滚、不相关不碰
4. 把 `docs/performance-security-function-audit-2026-06-23.csv` 当成主线

### 4.2 每个文件的处理顺序

建议固定成下面这个节奏：

1. 找到一个 `PENDING` 文件
2. 读完整个文件，连同所有函数/方法/组件/路由一起看
3. 在脑中判断它属于：
   - 纯审读
   - 可做低风险优化
   - 需要保守回避
4. 如果可优化，只做功能/UI 不变的低风险改动
5. 回写 CSV：
   - `visit_status`
   - `review_status`
   - `visited_at`
   - `reviewer`
   - `performance_risk`
   - `security_risk`
   - `action`
   - `validation`
   - `notes`
6. 必要时补 Markdown 进度日志
7. 跑对应模块验证

### 4.3 状态字段建议

- `PENDING`
  - 还没看
- `VISITED`
  - 已经读完，但还没形成明确结论
- `REVIEWED`
  - 已形成审读结论，功能/安全/性能判断完成
- `OPTIMIZED`
  - 已实际改动源码，且属于低风险优化
- `VERIFIED`
  - 改动有编译/测试/运行证据
- `BLOCKED`
  - 真正缺环境、缺账号、缺设备、缺外部状态才能继续

### 4.4 代码改动原则

只接受下面这类改动：

- 单次遍历替代 map/filter/reduce 的多次遍历
- 预分配集合
- 抽出共享 formatter / helper
- 文件级常量复用
- `remember` / `derivedStateOf` 缓存派生值
- 减少重复字符串拼接、重复 parse、重复查找
- 后端补索引、收口查询、减少 N+1、减少临时对象
- 安全上收紧鉴权、权限、参数校验、日志泄露

不要做这些：

- 不改变 UI 语义的前提下乱改布局
- 为了“看起来高级”引入大重构
- 为了节省一点点代码把语义改掉
- 迁移架构时顺手改产品行为

## 5. 分平台细则

### Android

优先看这些区域：

- `master-goods-android/app/src/main/java/com/zhihuiji/app`
- `master-goods-android/core/common`
- `master-goods-android/core/network`
- `master-goods-android/core/database`
- `master-goods-android/data/*`
- `master-goods-android/feature/*`

重点关注：

- 长列表
- 大屏幕/详情页的重复格式化
- Compose 重组热点
- remember / derivedStateOf / snapshot 读法
- 预分配、单次遍历、懒加载
- 真实设备上的流畅度、滚动卡顿、首帧和启动

验证优先级：

- 轻量改动：模块编译
- 较大改动：加测试
- 性能敏感改动：必要时上 adb / gfxinfo / Perfetto / simpleperf

### Web

优先看这些区域：

- `web/src/app`
- `web/src/pages`
- `web/src/shared`
- `web/vite.config.ts`

重点关注：

- 大页面的重复计算
- 不必要的响应式开销
- API client / session store / routing
- 金额、ID、权限、角色相关的精度和安全性
- `npm run build`

### 后端

优先看这些区域：

- `src/main/java/com/zhihuiji/backend/api/controller`
- `src/main/java/com/zhihuiji/backend/application/service`
- `src/main/java/com/zhihuiji/backend/infrastructure/repository`
- `src/main/resources/db/migration`
- `src/test/java`

重点关注：

- 查询路径
- 索引
- 事务边界
- 权限和 owner-scoped 数据隔离
- controller 输入校验
- 日志/异常泄露
- 任何可能的 N+1 查询

验证优先级：

- 目标模块编译
- 单元测试
- 需要时跑 SpringBootTest 或定向接口验证

## 6. 交接给下一个 Agent 的建议句式

可以直接这么交接：

> 继续按 `docs/performance-security-function-audit-2026-06-23.csv` 的 `PENDING` 顺序审计三端所有文件和函数。目标仍然是函数级全覆盖记录 + 功能/UI 不变的低风险性能优化 + 安全风险检查。先 `git status --short`，不要碰工作树里的无关既有改动。每处理完一个文件或一批函数，立刻回写 CSV/MD，并用对应模块编译或测试验证。

## 7. 当前最后确认

最近已经验证通过的 Android 改动包括：

- `feature:auth`
- `feature:customers`
- `feature:finance`

最新 finance 相关收尾文件是：

- `DailyExpenseScreen.kt`
- `FinanceRecordDetailScreen.kt`

这两个文件已经回写到台账，并且 `./gradlew :feature:finance:compileDebugKotlin` 通过。
