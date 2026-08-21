# Android 测试说明

## 文档信息

| 字段 | 内容 |
|---|---|
| 文档类型 | 测试验收 |
| 当前状态 | 本地已完成；真机 Blocked |
| 适用端 | Android |
| 依据源码 | `Code/frontend/android/**/src/test/`、`benchmark/` |
| 依据测试 | `testing/安卓/功能测试/TEST_PLAN.md`、`testing/安卓/单元测试/TEST_PLAN.md`、`testing/安卓/性能测试/TEST_PLAN.md` |
| 依据证据 | `testing/.artifacts/2026-08-18-8220-current-baseline/current-8220-baseline.md`（assembleDebug + 单元测试通过） |
| 最后核对 | 2026-08-20 |

## 一、单元测试覆盖（源码测试）

| 模块 | 测试 |
|---|---|
| feature/agent | `AgentChatViewModelAnswerMergeTest`、`AgentChatScreenToolStatusTest`、`AgentChatNetworkGateTest`、`AgentWorkbenchHistoryTest`、`ResultBlockRendererContractTest`、`AgentStoredResultBlockParseTest`、`AgentMarkdownTextParserTest`、`AgentResponseProvenanceTest` |
| core/network | `AgentSseClientCancellationTest`、`NetworkConfigTest`、`SafeApiCallBehaviorTest`、`ZhihuijiApiContractTest`、`ZhihuijiV2ApiContractTest` |
| core/model | `SerializationContractTest`、`V2ModelSerializationTest`、`AgentRunTraceModelsTest`、`AgentChatResponseSerializationTest`、`AgentStreamEventSerializationTest` |
| data/agent | `AgentV2RepositoryTest` |
| data 其它 | `ProductV2RepositoryTest`、`CustomerV2RepositoryTest`、`SupplierV2RepositoryTest`、`OrderV2RepositoryTest`、`FinanceV2RepositoryTest`、`AgentV2RepositoryTest` |

## 二、功能测试

- `testing/安卓/功能测试/TEST_PLAN.md` + `functional_feature_matrix.csv` + `live_execution_ledger.csv`。
- 覆盖：登录/session、Agent 对话、历史恢复、草稿、取消、首页报表等。

## 三、性能测试

- `testing/安卓/性能测试/TEST_PLAN.md`。
- `benchmark/`：AppMacrobenchmark、BaselineProfileGenerator、BenchmarkFlows。
- 已知问题 #18：32 消息长历史 jank 16.76%、PSS 增长 32688KB（154 历史证据，真机复测 Blocked）。

## 四、当前状态（8220 基线）

- Android 全量 `assembleDebug`、Agent/模型/数据单元测试通过。
- 思考完成后自动折叠、历史分页恢复首个可见消息位置、结果块按消息 part 顺序渲染——已确认。
- 真机：Blocked（本机无 `adb`）。

## 对应实现

- Android 代码：`Code/frontend/android/`
- 后端代码：不适用
- iOS 代码：不适用
- Web 代码：不适用
- Agent 代码：`feature/agent/`、`core/network/AgentSseClient.kt`

## 对应接口

- 接口路径：`/v2/*`
- 请求模型：`core/model/`
- 响应模型：同上
- SSE 事件：`AgentStreamModels.kt`

## 对应测试

- 单元测试：各模块 `src/test/`
- 功能测试：`testing/安卓/功能测试/TEST_PLAN.md`
- 性能测试：`testing/安卓/性能测试/TEST_PLAN.md`

## 当前限制

- 未完成内容：真机视觉验收、IME 修复验证、长历史性能复测
- Blocked 内容：真机（无 adb）
- Deferred 内容：多模态
- historical-only 内容：154 环境 Android 证据（d715a3a4 等）
