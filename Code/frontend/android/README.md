# Android 工程说明

这个目录是智慧记 Android 客户端工程，不再只是“脚手架”，当前已经包含可构建的多模块实现。

## 当前工程结构

```text
Code/frontend/android/
  app/
  backdrop/
  core/
    common/
    designsystem/
    model/
    network/
    database/
    datastore/
  data/
    auth/
    product/
    customer/
    supplier/
    order/
    finance/
    report/
    agent/
    sync/
  feature/
    auth/
    dashboard/
    products/
    customers/
    suppliers/
    sales/
    purchases/
    payments/
    finance/
    reports/
    agent/
    settings/
  DEVELOPMENT-PLAN.md
  UI-DESIGN-SPEC.md
```

## 当前定位

- `app`：应用主壳、认证流、主导航、五栏底部导航
- `backdrop`：liquid glass / 毛玻璃效果底层实现
- `core`：通用基础能力、网络、数据库、设计系统、数据存储
- `data`：Repository 与数据访问层
- `feature`：按业务域划分的页面与 ViewModel

## 关键文档

- 开发计划：
  [DEVELOPMENT-PLAN.md](/Users/sunyiyang/Desktop/Project/master-goods/Code/frontend/android/DEVELOPMENT-PLAN.md)
- UI 设计规范摘要：
  [UI-DESIGN-SPEC.md](/Users/sunyiyang/Desktop/Project/master-goods/Code/frontend/android/UI-DESIGN-SPEC.md)
- Stitch 全量 UI 重构计划：
  以当前目录的 [DEVELOPMENT-PLAN.md](./DEVELOPMENT-PLAN.md) 与 [UI-DESIGN-SPEC.md](./UI-DESIGN-SPEC.md) 为准
- Stitch 设计稿导出：
  [stitch_exports/visual-design_system_framework_14840154594131085259](/Users/sunyiyang/Desktop/Project/master-goods/Code/frontend/web/public/stitch_exports/visual-design_system_framework_14840154594131085259)
- 历史设计稿参考：
  [docs/design](/Users/sunyiyang/Desktop/Project/master-goods/docs/design)

## UI 统一基线

- Android 当前视觉真源以 Stitch 导出与 `42-android-liquid-glass-ui-refactor-plan.md` 为准。
- `UI-DESIGN-SPEC.md` 现在是摘要规范入口，不再单独承载全部细节。
- `docs/design/` 保存当前设计参考；历史设计资料若仍需保留，单独放入 `docs/archived/`。
- 具体实现必须通过 `core/designsystem` 承接，feature 页面不能长期保留私有风格组件。
- 后续新增商品、单据、财务、库存、AI、同步等业务能力时，优先落入既有列表页、详情页、编辑页、报表页、AI 页、设置页母版。
- 当前文档基线已经统一，但 B10 之前仍不能把 UI 视为设计稿级完成，后续还需要真机截图和逐页核对。

## 实际源码目录命名

- `app/src/main/java/com/zhihuiji/app`
- `core/common/src/main/java/com/zhihuiji/core/common`
- `core/designsystem/src/main/java/com/zhihuiji/core/designsystem`
- `core/model/src/main/java/com/zhihuiji/core/model`
- `core/network/src/main/java/com/zhihuiji/core/network`
- `core/database/src/main/java/com/zhihuiji/core/database`
- `core/datastore/src/main/java/com/zhihuiji/core/datastore`
- `data/auth/src/main/java/com/zhihuiji/data/auth`
- `data/product/src/main/java/com/zhihuiji/data/product`
- `data/customer/src/main/java/com/zhihuiji/data/customer`
- `data/supplier/src/main/java/com/zhihuiji/data/supplier`
- `data/order/src/main/java/com/zhihuiji/data/order`
- `data/finance/src/main/java/com/zhihuiji/data/finance`
- `data/report/src/main/java/com/zhihuiji/data/report`
- `data/agent/src/main/java/com/zhihuiji/data/agent`
- `data/sync/src/main/java/com/zhihuiji/data/sync`
- `feature/auth/src/main/java/com/zhihuiji/feature/auth`
- `feature/dashboard/src/main/java/com/zhihuiji/feature/dashboard`
- `feature/products/src/main/java/com/zhihuiji/feature/products`
- `feature/customers/src/main/java/com/zhihuiji/feature/customers`
- `feature/suppliers/src/main/java/com/zhihuiji/feature/suppliers`
- `feature/sales/src/main/java/com/zhihuiji/feature/sales`
- `feature/purchases/src/main/java/com/zhihuiji/feature/purchases`
- `feature/payments/src/main/java/com/zhihuiji/feature/payments`
- `feature/finance/src/main/java/com/zhihuiji/feature/finance`
- `feature/reports/src/main/java/com/zhihuiji/feature/reports`
- `feature/agent/src/main/java/com/zhihuiji/feature/agent`
- `feature/settings/src/main/java/com/zhihuiji/feature/settings`

## 文档使用建议

- 看整体推进顺序：先看 `DEVELOPMENT-PLAN.md`
- 看视觉目标：先看 `UI-DESIGN-SPEC.md`，再看 `DEVELOPMENT-PLAN.md` 和 Stitch 导出
- 看当前开发边界：阅读 [DEVELOPMENT-PLAN.md](/Users/sunyiyang/Desktop/Project/master-goods/Code/frontend/android/DEVELOPMENT-PLAN.md)；只有 `data/agent`、`data/sync`、`feature/agent`、`feature/settings` 保留未完成事项说明。

## 当前已知状态

- 工程可构建
- 主壳、五栏底部导航、核心列表/编辑/详情页已逐步落地
- UI 仍在持续对齐设计稿
- 本地导入链路已验证，但服务端“按账号隔离导入”仍需要后端补齐
