# 30 API 契约

本文只维护当前接口分层、未完成边界和验证入口。已实现接口的逐项实现记录已删除，接口真源以 Controller、DTO、测试和实际返回为准。

## 分层

| 层 | 当前定位 | 说明 |
|---|---|---|
| `/v1` | 兼容层 | 保留现有客户端和历史数据链路，不再扩展新的业务语义 |
| `/v2` | 正式契约层 | 新功能优先进入 `/v2`，所有业务查询必须带当前 owner 上下文 |

## 接口组状态

| 接口组 | 当前状态 | 后续验证重点 |
|---|---|---|
| auth / session / store | 已有实现 | 登录、刷新、退出、store context 和 owner 隔离真机/现场验证 |
| products / partners | 已有首版 | 真实账户数据、最小夹具、owner 隔离和 Android 联调 |
| sales / purchase / pay-orders | 已有首版 | 草稿、确认、收款、付款、退货、收货和幂等边界 |
| finance / inventory | 已有首版 | 账户余额、库存台账、快照、报表口径和最小夹具清理 |
| media | 待验证 | 真实上传、绑定、读取和客户端联调 |
| agent | 待验证 | conversation、message、chat、stream、cancel、audit、draft 及真实 LLM 工具闭环 |
| sync / import | 待验证 | pull、upload、ack、游标、任务取消/重试和 Android 顺序联调 |

## Agent 专项边界

- 非流式 `chat`、流式 `chat/stream`、运行取消、审计和草稿必须以真实账户数据验证。
- 表格、统计图和 KPI 是模型可主动调用的结构化结果工具，不得由客户端默认追加。
- 只有工具返回与用户问题相关的真实数据时，客户端才渲染对应 result block。
- 破坏性逆向安全测试必须独立登记，不计入常规接口通过率。

## 多租户要求

- 业务表使用 `owner_user_id`，Repository 查询必须显式接收 owner。
- 服务层通过当前认证上下文取得 owner，不能从客户端请求体信任 owner。
- 跨 owner 读取、修改、删除和关联对象引用必须有失败用例和真实返回证据。

## 验证入口

- 后端单元/功能/性能计划：`testing/后端/`
- Agent 单元/功能/性能计划：`testing/Agent/`
- Android 网络契约和真机计划：`testing/安卓/`
- 实际接口合同：`src/main/java/com/zhihuiji/backend/api/controller/v2/`、`src/main/java/com/zhihuiji/backend/api/dto/v2/`
- 真实执行结果：各测试类别目录的 `live_execution_ledger.csv` 和 `testing/.artifacts/`
