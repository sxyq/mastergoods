# Agent 可靠性与故障测试规划（可靠性）

更新日期：2026-08-28。覆盖 Provider 故障、SSE 断线/取消/重连、重复与异常输入、资源释放与状态机收敛。判定：失败不伪装成功、可恢复场景恢复、不可恢复场景有明确终态、允许的重试不产生重复写入。故障注入必须记录注入点、注入时刻、请求/运行 ID 和恢复或终止路径。

## 一、专项用例（AG-R-001~013，初始 `Deferred`）

| 编号 | 场景 | 故障注入 | 预期 | 验收 |
|---|---|---|---|---|
| AG-R-001 | Provider 超时 | 模型响应超时（流式发射器 180s / 压缩 20s） | 稳定超时错误；`STREAM_ERROR` 或可读终态；资源释放 | 不伪装成功；审计可追溯 |
| AG-R-002 | Provider 429/限流 | 返回 429 | 有限重试或不重试；错误终态；不重复工具写入 | 重试次数受控；无重复业务写入 |
| AG-R-003 | 空响应/非法 JSON | Provider 返回空白或非法 JSON | 规划失败/回答失败路径（`llm_unavailable`/`llm_answer_unavailable`/`MODEL_TOOL_SELECTION_FAILED`）；不注入无效摘要 | 错误码稳定；文本回答不编造 |
| AG-R-004 | 流中断线 | 首事件前/工具中/回答中断开 | 按契约恢复；只补缺失事件；不可恢复时明确终态 | 恢复后不重复旧事件；不重复写入 |
| AG-R-005 | 客户端断开（早 EOF） | 客户端在任意点断开 | 服务端释放 emitter 与活跃 run；audit 落终态 | 无资源泄漏；无残留活跃 run |
| AG-R-006 | 取消竞态 | 极早取消（run_started 后）与 Provider 中断同时发生 | `ensureRunActive` 抛取消异常；audit 收敛为 `cancelled`；不被 STREAM_ERROR 覆盖 | SSE 与 audit 终态一致（回归历史问题 7） |
| AG-R-007 | 重复事件/消息重试 | 相同 event_id、相同 SSE 事件重复投递 | 客户端去重；服务端不产生重复写入 | 重复事件不改变状态 |
| AG-R-008 | Last-Event-ID 重连 | 断点后续传 | 从断点续传；event_id/seq 单调 | 无跳号与重复 |
| AG-R-009 | 压缩失败降级 | 语义压缩超时/非法输出 | 使用确定性摘要；旧检查点不被覆盖；主回答不受影响 | 摘要可读、无敏感信息 |
| AG-R-010 | 确认中断 | 确认请求在事务中失败/断连 | 事务回滚；草稿保持 active 可重试或取消 | 无正式表半写入 |
| AG-R-011 | 异常 JSON 参数 | 模型原始参数非法 JSON | 结构化失败 `TOOL_ARGUMENTS_INVALID`（禁止用 `{}` 掩盖） | 失败记录携带 call_id 与 seq |
| AG-R-012 | 慢速下游与超时窗口 | Repository/查询慢于 SSE 寿命 | 不假死；超时可取消；审计完成 | 无泄漏、无重复 |
| AG-R-013 | 生图 Provider 故障收敛 | `image_generate` 已确认草稿；Mock 注入连接超时、读取超时、取消、空响应、非法 JSON、HTTP 错误 | Provider 调用结束后返回安全错误；草稿保持可重试；不写正式业务表；连接、线程和临时参考图资源释放；审计记录错误类型与最终状态 | 超时/取消不伪装成功；Provider 调用最多一次；资源未释放数为 0 |

## 二、故障记录与恢复判定

每个父用例至少拆成 `-B01` 首事件前、`-B02` 工具执行中、`-B03` 正式回答中、`-B04` 终态附近四个注入点；不适用的注入点登记原因。断线/重连另加客户端断开位置和 `Last-Event-ID`，取消另加取消请求与 Provider 中止的先后顺序。

必须分别记录：

- 注入前的服务、Provider、数据库和活动运行数；
- 注入后的 HTTP/SSE 状态、终态事件、错误码、工具调用数和回答内容；
- audit/run-trace 的最终状态、事件序号、丢失/失败计数；
- 活跃 run、emitter、线程、连接池和临时资源是否回落；
- 可恢复场景的补发事件，及其重复/丢失数量；
- 写入场景的业务表 before/after 与清理结果。

没有实际故障注入或服务端运行证据时记 `Deferred`；因 Provider、设备或服务能力缺失而无法注入时记 `Blocked`。不把“没有观察到异常”写成通过。

`AG-R-013` 每条记录必须包含输入、注入点、Mock 响应、预期终态、Provider 调用计数、草稿/正式表 before-after、audit、资源释放证据、清理路径和 `Passed/Failed/Blocked/Deferred` 状态；真实 Provider 只在隔离环境执行。

## 三、资源释放检查清单

- SSE emitter：`onCompletion/onTimeout/onError` 均执行 `removeRun`；`activeRun.complete()` 关闭 emitter。
- 审计写队列：队列满走 CallerRunsPolicy，丢/失败计数记入 `audit_lossy`；进程关闭 awaitTermination(2s)。
- 压缩与记忆线程池均为虚拟线程并 `shutdownNow`。
- 每个用例结束后核对：活跃 run 数为 0、无残留 emitter、连接/线程数回落。

## 四、证据存放

`可靠性/artifacts/<日期>-<波次>-<用例>/`（README 第六节文件序列，重点 03-raw-sse.log 与 05-run-audit.json）；脚本 `../脚本/可靠性/`。
