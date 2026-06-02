# Android data 层分析

- 对应源码目录：`master-goods-android/data/`
- 子模块：`agent / auth / customer / finance / order / product / report / supplier / sync`
- 作用：承载 Repository、在线优先缓存与后端调用封装

## 模块定位

新版 `data` 层的重点不是“把接口结果直接交给页面”，而是变成真正的**领域数据访问层**：

- 把 `/v1` 与 `/v2` 兼容期隔离开
- 默认按 owner 读取与写入
- 把扩域后的商品、往来单位、单据、财务、库存能力组织清楚
- 把同步与导入从“工具函数”升级为“业务数据通道”

## 状态图例

- `新版已做`
- `新版待做`
- `旧版存在新版未做`
- `新版需要去掉`
- `需重构`
- `待验证`

## 子目录

- [agent/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/data/agent/README.md)
- [auth/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/data/auth/README.md)
- [customer/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/data/customer/README.md)
- [finance/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/data/finance/README.md)
- [order/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/data/order/README.md)
- [product/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/data/product/README.md)
- [report/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/data/report/README.md)
- [supplier/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/data/supplier/README.md)
- [sync/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/data/sync/README.md)

## 状态表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `/v1` Repository 闭环 | 新版已做 | 旧版本地账本更偏单机 | 继续承载当前 app | 多数业务模块已有 Repository | 当前可运行 |
| `/v2` Repository 规划 | 新版待做 | 旧版无 `/v2` | 围绕新版领域重组数据访问层 | 当前主要围绕 `/v1` | 文档先行 |
| owner 默认过滤 | 需重构 | 旧版无统一账号边界 | Repository 默认按 owner 取数 | 当前多为全量或条件列表接口 | 依赖后端先落地 |
| 商品/财务/库存扩域仓储 | 旧版存在新版未做 | 旧版表域更厚 | 补齐 catalog、account、inventory 等能力 | 当前模块仍偏首版 | 未来会扩目录或扩职责 |
| 会员仓储 | 新版需要去掉 | 旧版可能存在会员扩展 | 当前新版不纳入 | 不应新增 member repository | 如恢复需重新立项 |

## 新版数据层重组方向

| 方向 | 状态 | 说明 |
|---|---|---|
| `data/auth` | 需重构 | 登录后不仅恢复 token，还要拉起 owner 初始化 |
| `data/product` | 需重构 | 后续可能扩为更完整的 catalog 仓储 |
| `data/customer + data/supplier` | 需重构 | 后续要与 partner 域更紧密对齐 |
| `data/order` | 需重构 | 当前“三仓储合一”后续会被更细的销售/采购/付款/退货场景拆分 |
| `data/finance` | 需重构 | 从轻量流水扩到账户与单据资金联动 |
| `data/report` | 需重构 | 报表取数将更依赖 owner 与扩域聚合 |
| `data/sync` | 需重构 | 要承载 owner 分桶同步与导入链路 |
| `data/agent` | 新版已做 | 当前是明显领先旧版的域，后续继续扩会话与草稿缓存 |
