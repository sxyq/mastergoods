# Android 脚手架说明

这个目录是基于当前后端能力反推出来的 Android 端脚手架。

约束：
- 只创建目录和开发说明，不放任何 Kotlin 业务实现代码。
- 目录命名使用真实 Android 多模块工程常见命名，便于后续直接补代码。
- 每个模块都有 `DEVELOPMENT.md`，说明这个位置需要开发的类、函数、方法与验收点。

## 目录总览

```text
master-goods-android/
  app/
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
```

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

整体推进顺序见 [DEVELOPMENT-PLAN.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/DEVELOPMENT-PLAN.md)。

UI 设计规范见 [UI-DESIGN-SPEC.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/UI-DESIGN-SPEC.md)。后续所有 Compose 页面都要以 [image doc](</Users/sunyiyang/Desktop/Project/master-goods/image doc>) 中的设计图为视觉目标。
