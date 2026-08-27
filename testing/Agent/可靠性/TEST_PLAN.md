# Agent 可靠性与故障测试规划（可靠性）

更新日期：2026-08-28。覆盖 Provider 故障、SSE 断线/取消/重连、重复与异常输入、资源释放与状态机收敛。判定：失败不伪装成功、可恢复场景恢复、不可恢复场景有明确终态、允许的重试不产生重复写入。

## 一、专项用例（AG-R-001~012，初始 `Deferred`）

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

## 二、资源释放检查清单

- SSE emitter：`onCompletion/onTimeout/onError` 均执行 `removeRun`；`activeRun.complete()` 关闭 emitter。
- 审计写队列：队列满走 CallerRunsPolicy，丢/失败计数记入 `audit_lossy`；进程关闭 awaitTermination(2s)。
- 压缩与记忆线程池均为虚拟线程并 `shutdownNow`。
- 每个用例结束后核对：活跃 run 数为 0、无残留 emitter、连接/线程数回落。

## 三、证据存放

`可靠性/artifacts/<日期>-<波次>-<用例>/`（README 第五节文件序列，重点 03-raw-sse.log 与 05-run-audit.json）；脚本 `../脚本/可靠性/`。