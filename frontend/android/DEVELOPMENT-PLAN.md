# Android 当前开发边界

本文只保留当前状态、未完成事项和验收入口。已经实现的历史执行记录不再重复维护；实现是否成立以源码、测试命令和 `testing/` 台账为准。

## 当前状态

| 范围 | 状态 | 当前边界 |
| --- | --- | --- |
| `app`、`core/common`、`core/model`、`core/datastore`、`core/network` | 已实现 | 继续以现有源码和单元测试为准 |
| `core/database`、`data/product`、`data/customer`、`data/supplier`、`data/order`、`data/finance`、`data/report` | 已实现首版 | Room 缓存与在线刷新可用，完整离线冲突策略仍不作为已完成能力 |
| `data/auth`、`data/agent` | 已实现首版 | Agent 真实 provider、多轮工具结果回灌和异常分支仍需持续联调 |
| `data/sync` | 进行中 | 后台调度、离线回写和冲突处理尚未完成 |
| `feature/auth`、`feature/dashboard`、`feature/products`、`feature/customers`、`feature/suppliers` | 已实现首版 | 继续通过真机功能测试验证真实数据和边界态 |
| `feature/sales`、`feature/purchases`、`feature/payments`、`feature/finance`、`feature/reports` | 已实现首版 | 继续通过真机链路和发布前验收验证 |
| `feature/agent` | 进行中 | 真机主链路、过程区、正式回答、工具调用、图表按需渲染和取消仍需回归 |
| `feature/settings` | 进行中 | 缓存统计/清理和完整 store/member 权限绑定未完成 |

## 统一约束

- 视觉真源：[`docs/spec/42-android-liquid-glass-ui-refactor-plan.md`](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/42-android-liquid-glass-ui-refactor-plan.md)、Stitch 导出和 `UI-DESIGN-SPEC.md`。
- 正式源码目录：`frontend/android/`；根目录 `master-goods-android` 仅为兼容符号链接。
- 新功能必须复用 `core/designsystem` 和既有列表、详情、编辑、报表、AI、设置页面母版。
- UI 不得伪造数据；图表、表格和 KPI 只能渲染当前账户真实接口返回的结构化结果。
- 需要真机或后端现场才能确认的内容只能标为 `Blocked`/`待验证`，不能因本地编译通过而改成已完成。

## 当前待办

1. 完成 Agent provider 的多轮工具结果回灌和最终正式回答闭环。
2. 真机验证登录、session 恢复、首页、Agent 历史、草稿、审计、取消和键盘布局。
3. 补齐同步调度、离线回写和冲突处理的实现与测试。
4. 为尚未接入真实序列的数据保持空态，不将静态占位图表当作动态数据。
5. 按 `testing/安卓/` 的 Wave 台账回填实际命令、截图、logcat 和结果。

## 验证入口

```bash
cd frontend/android
./gradlew :app:compileDebugKotlin :feature:agent:compileDebugKotlin
./gradlew :app:assembleDebug
./gradlew :core:network:testDebugUnitTest
```

真机安装和 ADB 证据使用 `testing/安卓/` 现有脚本与台账；不要在本文重复复制已完成的执行日志。
