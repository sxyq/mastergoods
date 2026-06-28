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

这两份文件仍然是后续所有工作的“单一事实来源”。每次读完文件或函数，都要同步回写 CSV，必要时再补一条 Markdown 进度日志。

### 2.1 当前台账真实状态

- 函数级访问闭环已经达成，不再是“还有几千个 PENDING 没看”的状态。
- 当前 CSV 统计是：
  - 总行数：`7285`
  - 文件行：`726`
  - 符号行：`6559`
  - backend：`3881`
  - android：`2738`
  - web：`666`
  - `visit_status=REVIEWED`：`7285`
  - `review_status=REVIEWED`：`5681`
  - `review_status=OPTIMIZED`：`420`
  - `review_status=VERIFIED`：`1184`
  - `review_status=BLOCKED`：`0`

### 2.2 已经落地并验证过的代表性优化

- Android：
  - `feature:auth`、`feature:customers`、`feature:finance` 都做过低风险减分配/共享 formatter/remember 缓存类优化，并有模块编译验证。
  - 更大范围的 Android/Compose/ViewModel 优化和读查已经持续回写在主台账与 `docs/performance-security-function-audit-2026-06-23.md` 的进度日志里。
- Web：
  - 已完成 EntityId 查询参数收口，避免 `Number()` 吃掉雪花 ID 精度。
  - 本轮新增修复了 `DashboardPage.vue` 的初始化时序 TDZ 问题，以及 `/403` 后无法重新登录的死锁问题。
  - 当前 `cd web && npm run build` 已再次通过。
- 后端：
  - 已完成多批 owner-scoped 查询、事务边界、权限鉴权、报表/库存/媒体/资金链路的低风险压缩与安全复核。
  - 当前 fresh H2 本地运行态下，`/v1/auth/login` 与 `/v2/stores/current` 已确认成功。

### 2.3 本轮新补的函数级台账修正

这次又把下面这些“代码变了但 CSV 还没跟上”的点补齐了：

- `web/src/main.ts`
  - 新增台账条目：
    - `router.beforeEach`
    - `router.afterEach`
    - `zhihuijiWebApiAuthHandler`
- `web/src/pages/ForbiddenPage.vue`
  - 新增台账条目：
    - `reauthenticate`
- `web/src/pages/dashboard/DashboardPage.vue`
  - 将已删除的 `buildRange` 旧符号记录，校准为当前真实存在的 `watch` 回调记录
  - 同步修正当前源码行号

## 3. 目前剩余内容

现在“剩余内容”的定义已经变了，不再是大量 `PENDING` 文件，而是下面几类真实待办：

### 3.1 运行态证据还没完全闭环

- Web：
  - 当前 3 个真实修复文件还没单独 commit：
    - `web/src/pages/dashboard/DashboardPage.vue`
    - `web/src/pages/ForbiddenPage.vue`
    - `web/src/main.ts`
  - 这 3 个文件已通过 `npm run build`，并且真实浏览器链路已验证：
    - `/403 -> 重新登录 -> /login -> /dashboard`
    - `finance/records/detail?id=1357334732875426798`
    - `archives/products/edit?id=1`
    - `inventory/adjust?productId=1`
    - `inventory/product-ledger?productId=1`
- 后端：
  - 旧本地 H2 库会触发 `store_memberships.owner_user_id` 缺列导致 500。
  - 这不是当前源码逻辑 bug，而是历史本地库漂移。
  - fresh H2 下已确认：
    - `POST /v1/auth/login` 成功
    - `GET /v2/stores/current` 成功
    - 返回 `role=OWNER`
    - 权限包含 `dashboard:view`
- Android：
  - 冷启动采样 artifacts 已经拿到，但还缺“连本地后端完成一条真实业务链路”的复验。
  - 当前 debug 默认 baseUrl 在：
    - `master-goods-android/core/datastore/src/main/java/com/zhihuiji/core/datastore/SettingsStore.kt`
    - 默认值：`http://117.72.79.106/zhihuiji/`
  - 因此 Android 现在默认打的是远端，不是本地 `10.0.2.2:18080`。

### 3.2 当前 dirty worktree 需要做 delta 审计，而不是盲提全量 commit

当前仓库不是干净状态，存在很多并行改动，尤其集中在：

- `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java`
- `src/main/java/com/zhihuiji/backend/infrastructure/ai/LongCatAnthropicClient.java`
- `src/main/java/com/zhihuiji/backend/application/service/v2/agent/...`
- `master-goods-android/feature/agent/...`
- `web/src/pages/agent/AgentPage.vue`
- `web/src/shared/api/contracts.ts`
- `web/src/style.css`
- `docs/spec/...`

这意味着：

1. 不要 `git add .`
2. 不要把“本轮审计修复”与“并行 agent 大改”混在一个提交里
3. 下一个 Agent 需要把这些 dirty 文件当成“增量再审计对象”，而不是默认继承旧台账结论

### 3.3 Android 还缺的关键下一步

最值得优先推进的是：

1. 找到现有设置入口或最小代价方式，把 debug baseUrl 切到 `http://10.0.2.2:18080/`
2. 让 Android 用本地后端完成真实登录
3. 沿商品/库存主线再抓一轮：
   - screenshot
   - `gfxinfo`
   - logcat
   - 必要时 Perfetto / simpleperf
4. 把这些运行态证据继续回写到主台账和进度文档

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

> 三端函数级访问台账已经闭环，当前主台账见 `docs/performance-security-function-audit-2026-06-23.csv`。请先 `git status --short`，不要碰工作树里的并行改动，也不要 `git add .`。优先继续做 dirty 文件的 delta 审计、Web 当前 3 个修复的独立提交准备、以及 Android baseUrl 切到 `10.0.2.2:18080` 后的本地登录与性能采样。每处理完一个文件或一批函数，立刻回写 CSV/MD，并用对应模块编译、测试或真实运行证据验证。

## 7. 当前最后确认

截至这次交接，最重要的事实是：

1. 函数级台账不是半成品，当前已经做到 `visit_status` 全 `REVIEWED`。
2. 但总目标还没完成，因为“全链路运行态复验 + dirty 增量改动再审计 + Android 本地后端链路”还没完全闭环。
3. 当前最新、最值得单独提交的一组文件是：
   - `web/src/pages/dashboard/DashboardPage.vue`
   - `web/src/pages/ForbiddenPage.vue`
   - `web/src/main.ts`
   - `docs/performance-security-function-audit-2026-06-23.csv`
   - `docs/performance-security-function-audit-2026-06-23.md`
   - `docs/agent-handoff-performance-security-audit.md`
4. 适合这组文件的中文 commit 题目是：
   - `修复 Web 看板初始化时序与 403 重登死锁，并同步审计台账`
