# Android core 层分析

- 对应源码目录：`master-goods-android/core/`
- 子模块：`common / database / datastore / designsystem / model / network`
- 作用：承载通用模型、网络、存储、数据库与设计系统

## 模块定位

新版里 `core` 层要从“首版基础设施集合”升级为“长期稳定的跨域基础能力层”。  
重点不是界面样式，而是：

- `/v1` 与 `/v2` 模型边界
- owner-aware 本地持久化
- 统一金额/数量语义
- 网络契约与本地缓存策略

## 状态图例

- `新版已做`
- `新版待做`
- `旧版存在新版未做`
- `新版需要去掉`
- `需重构`
- `待验证`

## 子目录

- [common/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/core/common/README.md)
- [database/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/core/database/README.md)
- [datastore/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/core/datastore/README.md)
- [designsystem/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/core/designsystem/README.md)
- [model/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/core/model/README.md)
- [network/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/core/network/README.md)

## 状态表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| 通用基础设施 | 新版已做 | 旧版是单体 app 私有能力 | 继续作为多模块基础层 | 6 个 core 模块均已有源码 | 仍是重构承重点 |
| `/v1` 兼容模型与网络 | 新版已做 | 旧版无当前远程契约分层 | 继续支撑当前功能运行 | 当前 `core/model` 与 `core/network` 已可用 | 暂不移除 |
| `/v2` 领域基础层 | 新版待做 | 旧版无 `/v2` | 建立 owner-aware 的长期模型与契约 | 当前只在文档层规划 | 需与后端同步推进 |
| owner-aware 本地持久化 | 需重构 | 旧版无统一 owner | Room/DataStore 支撑 owner 语义 | 当前本地持久化仍偏首版 | `database + datastore` 都会变 |
| 金额/数量精度治理 | 需重构 | 旧版与当前首版都普遍使用浮点 | 新版统一精度策略 | 当前大量模型与报表仍为 `Double` | 影响范围广 |
| 会员相关基础模型 | 新版需要去掉 | 旧版可推断有扩展空间 | 当前阶段不纳入 | Android core 不应新增 member 基础模型 | 如恢复需新 spec |

## 新版 core 设计重点

1. `core/model`
   - 保留 `/v1` 模型
   - 新增 `core/model/v2/*`
   - 请求、响应、领域模型分离

2. `core/network`
   - 保留 `/v1` 契约
   - 新增 `/v2` 接口分组与 request/query 对象

3. `core/database`
   - 继续缓存首版域
   - 逐步承载 owner、账户、库存账本、扩域表

4. `core/datastore`
   - 从 session/settings/cursor 扩到 owner/import/sync baseline

5. `core/designsystem`
   - 继续承载视觉系统
   - 但本轮文档不聚焦视觉细节
