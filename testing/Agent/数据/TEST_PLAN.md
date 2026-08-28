# Agent 数据一致性与清理测试规划（数据）

更新日期：2026-08-28。覆盖草稿边界、正式表变化、重复确认、失败回滚、同 key 幂等、测试数据清理与迁移数据核对。每条父用例都要按正常、失败、重复/并发和清理动作形成独立记录，记录业务表 before/after 差异，清理后无预期外残留。

## 一、判定通则

- 草稿阶段（active/confirming/cancelled）不修改任何正式业务表；正式写入只发生在 confirm 成功之后。
- 同一草稿重复确认、并发确认或重放只能产生一次正式写入；失败必须回滚。
- 幂等语义：同 owner/store + 同 key + 同 payload 返回同一结果；同 key 不同 payload 返回明确冲突（付款 409）。

## 二、专项用例（AG-D-001~013，初始 `Deferred`）

| 编号 | 场景 | 输入/操作 | before/after 断言 | 边界 |
|---|---|---|---|---|
| AG-D-001 | 只读工具表不变 | 46 个 RO 逐项 | 会话/消息/audit 增加；业务表零差异 | 空数据、分页、大结果 |
| AG-D-002 | 草稿阶段零写入 | CREATE_ONLY 生成草稿 | 仅 `agent_drafts`(active) 增加 | 15 工具逐个 |
| AG-D-003 | 拒绝零写入 | 草稿 → cancel | draft=cancelled；正式表零差异 | 拒绝后不可再确认 |
| AG-D-004 | 确认单笔写入 | 草稿 → confirm | 正式表 +1；draft=confirmed；审计含确认者与业务 ID | 14 工具逐个 |
| AG-D-005 | 重复确认幂等 | 已 confirmed 再 confirm | 第二次返回稳定已处理结果；正式表仍 1 条 | 顺序与并发 |
| AG-D-006 | 确认失败回滚 | 构造业务失败（库存不足/唯一冲突/账户不存在） | 全部回滚；draft=active 可重试；无半写入 | 事务边界 |
| AG-D-007 | 并发确认/取消竞争 | 同 draft 并发 confirm+cancel | 乐观锁 `updateStatusIfCurrent` 唯一胜者；正式表≤1 | 无 500 |
| AG-D-008 | 付款幂等键 | 直接付款 API 使用同 owner/store、同 key 的相同/不同 payload；Agent 草稿确认另记录其生成的 `agent-pay-<run_id>` key | 相同 key+payload 只产生一个付款结果；不同 payload 返回明确冲突；不重复付款 | 缺失/空白/非法 key、跨 owner 同 key、唯一约束竞争、失败重试 |
| AG-D-009 | 会话删除级联 | 删除会话 | 消息/草稿/检查点/audit 关联按设计清理 | 不删除其他 owner |
| AG-D-010 | 检查点数据 | 压缩后读 checkpoint | 唯一 active；owner 隔离；失效后重建 revision 递增 | 并发压缩 |
| AG-D-011 | 记忆数据 | 回答后 / 删除记忆 | `agent_memories` 去重更新；删除停止召回 | 脱敏字段 |
| AG-D-012 | 测试数据清理 | 全部用例跑完 | 删除测试会话/草稿/临时媒体/授权清理的测试业务数据 | 清理有结果；正式数据符合预期 |
| AG-D-013 | 生图结果与清理 | `image_generate` 草稿生成、拒绝、确认成功/失败/超时/取消；Provider Mock 返回 `url`/`b64_json` | 工具阶段业务表与 Provider 调用均为 0；拒绝无正式写入；确认结果可按草稿/owner 对齐；失败可重试；临时参考图和测试草稿按 owner 清理 | b64/URL 不进快照原文；真实 Provider 资源与生产 PG 结论另记 `Blocked`/`Deferred` |

## 三、数据差异记录规则

每条记录的 before/after 至少包含：对象类型、owner/store 脱敏标签、记录数量、关键状态数量、测试创建 ID 的脱敏列表、关联对象数量和校验时间。金额、库存和数量记录汇总值；不保存其他用户的完整业务行。

| 场景 | 必须比较的对象 | 通过条件 |
|---|---|---|
| 只读工具 | 业务表、会话、消息、audit/event | 业务表差异为 0；运行记录只属于当前 owner/run |
| 草稿生成/拒绝 | `agent_drafts` 与正式业务表 | 生成只增加 active 草稿；拒绝只改变草稿状态；正式表差异为 0 |
| 确认成功 | 草稿、目标正式表、关联明细/库存/流水 | 目标记录数量和金额/数量与草稿一致；同一草稿最多一份正式结果 |
| 确认失败/竞争 | 草稿状态、全部目标正式表、关联明细 | 无半写入；竞争结果可解释；后续重试不重复 |
| 会话删除 | 会话、消息、草稿、检查点、audit/event | 按当前删除设计清理关联对象；其他 owner 数据不变 |
| 测试清理 | 测试创建的全部对象和临时媒体 | 清理脚本带 owner 条件；清理结果可复核；无全表删除 |

同一场景需要比较 SQLite、H2 或 PostgreSQL 时分别记录数据库类型和迁移版本。SQLite/H2 结果不能代替 PostgreSQL 查询计划或生产数据一致性结论。Agent 付款测试必须区分“用户请求级 key”和“工具按 run_id 生成的草稿 key”，不能仅因草稿状态幂等就判定请求级幂等。

`AG-D-013` 每条记录必须包含输入、预期结果、Provider Mock 响应类型、owner/store 标签、草稿/正式表 before-after、Provider 调用计数、审计摘要、清理结果、证据位置和 `Passed/Failed/Blocked/Deferred` 状态。

## 四、清理实现

- 会话与草稿可通过 API（DELETE /v2/agent/conversations/{id}、/v2/agent/drafts/{id}）清理；业务测试数据按既有业务接口或数据库脚本（受控、带 owner 条件）清理。
- 清理脚本只放 `../脚本/数据/`，必须支持 `--owner` 参数并默认拒绝无 owner 条件的全表删除。
- before/after 快照：`06-database-before.json`、`07-database-after.json`、`09-cleanup.json` 三份文件为必填证据。

## 五、证据存放

`数据/artifacts/<日期>-<波次>-<用例>/`；SQL/脚本与计数查询 → `数据/logs/`；报告 → `数据/reports/`。证据文件按 README 第六节命名，`09-cleanup.json`缺失时不得标记通过。
