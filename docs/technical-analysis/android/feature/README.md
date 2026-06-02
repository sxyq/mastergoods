# Android feature 层分析

- 对应源码目录：`master-goods-android/feature/`
- 子模块：`agent / auth / customers / dashboard / finance / payments / products / purchases / reports / sales / settings / suppliers`
- 作用：承载 Compose Screen、ViewModel 与业务交互流程

## 模块定位

新版 `feature` 层的重点不是“先把页面做出来”，而是让每个模块都清楚自己承接的是哪一段**领域场景**：

- 用户登录后如何进入 owner 私有上下文
- 商品/档案/单据/财务/报表/助手分别承接哪些业务能力
- 哪些现有页面只是 `/v1` 兼容页
- 哪些页面未来需要拆成更细的流程页

## 状态图例

- `新版已做`
- `新版待做`
- `旧版存在新版未做`
- `新版需要去掉`
- `需重构`
- `待验证`

## 子目录

- [agent/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/feature/agent/README.md)
- [auth/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/feature/auth/README.md)
- [customers/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/feature/customers/README.md)
- [dashboard/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/feature/dashboard/README.md)
- [finance/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/feature/finance/README.md)
- [payments/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/feature/payments/README.md)
- [products/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/feature/products/README.md)
- [purchases/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/feature/purchases/README.md)
- [reports/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/feature/reports/README.md)
- [sales/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/feature/sales/README.md)
- [settings/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/feature/settings/README.md)
- [suppliers/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/feature/suppliers/README.md)

## 状态表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| 五栏主壳下的首版页面 | 新版已做 | 旧版页面形态不可直接复用 | 继续承载现有 `/v1` 主流程 | 主要页面与若干编辑/详情页已存在 | 当前可运行 |
| `/v2` 场景页规划 | 新版待做 | 旧版无新版契约 | 根据 `/v2` 重新划分页面职责 | 当前文档刚开始同步 | 本轮只做规划 |
| 更厚的经营能力页面 | 旧版存在新版未做 | 旧版有更细的订单态、账户、库存分析 | 新版需要补足但不照抄旧版 | 当前 feature 仍偏首版闭环 | 以后端领域为准 |
| 会员入口与会员模块 | 新版需要去掉 | 旧版可推断存在会员能力 | 当前新版不纳入 | feature 下不应新增 member 模块 | 如恢复需新 spec |

## 场景拆分原则

1. **不要让一个页面吞掉整个领域**
   - 当前很多编辑页/详情页承担了过多职责
   - `/v2` 后会逐步拆成更清晰的场景页

2. **列表页、详情页、编辑页、状态页分离**
   - 销售、采购、财务最明显

3. **全局状态不留在页面里硬扛**
   - 页面只关心当前场景输入输出
   - owner、导入、同步等状态回收到 app/data 层

4. **暂不追求 UI 视觉一致性文档化**
   - 当前重点是领域规划与职责重排
