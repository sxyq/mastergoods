# Agent 数据一致性与清理测试规划（data）

更新日期：2026-08-28。覆盖草稿边界、正式表变化、重复确认、失败回滚、同 key 幂等、测试数据清理与迁移数据核对。每条用例记录业务表 before/after 差异，清理后无预期外残留。

## 一、判定通则

- 草稿阶段（active/confirming/cancelled）不修改任何正式业务表；正式写入只发生在 confirm 成功之后。
- 同一草稿重复确认、并发确认或重放只能产生一次正式写入；失败必须回滚。
- 幂等语义：同 owner/store + 同 key + 同 payload 返回同一结果；同 key 不同 payload 返回明确冲突（付款 409）。

## 二、专项用例（AG-D-001~012，初始 `Deferred`）

| 编号 | 场景 | 输入/操作 | before/after 断言 | 边界 |
|---|---|---|---|---|
| AG-D-001 | 只读工具表不变 | 46 个 RO 逐项 | 会话/消息/audit 增加；业务表零差异 | 空数据、分页、大结果 |
| AG-D-002 | 草稿阶段零写入 | CREATE_ONLY 生成草稿 | 仅 `agent_drafts`(active) 增加 | 14 工具逐个 |
| AG-D-003 | 拒绝零写入 | 草稿 → cancel | draft=cancelled；正式表零差异 | 拒绝后不可再确认 |
| AG-D-004 | 确认单笔写入 | 草稿 → confirm | 正式表 +1；draft=confirmed；审计含确认者与业务 ID | 14 工具逐个 |
| AG-D-005 | 重复确认幂等 | 已 confirmed 再 confirm | 第二次返回稳定已处理结果；正式表仍 1 条 | 顺序与并发 |
| AG-D-006 | 确认失败回滚 | 构造业务失败（库存不足/唯一冲突/账户不存在） | 全部回滚；draft=active 可重试；无半写入 | 事务边界 |
| AG-D-007 | 并发确认/取消竞争 | 同 draft 并发 confirm+cancel | 乐观锁 `updateStatusIfCurrent` 唯一胜者；正式表≤1 | 无 500 |
| AG-D-008 | 付款幂等键 | 同 key 不同 payload | 第一笔成功；第二笔 409；不重复付款 | 跨 owner 不互相命中 |
| AG-D-009 | 会话删除级联 | 删除会话 | 消息/草稿/检查点/audit 关联按设计清理 | 不删除其他 owner |
| AG-D-010 | 检查点数据 | 压缩后读 checkpoint | 唯一 active；owner 隔离；失效后重建 revision 递增 | 并发压缩 |
| AG-D-011 | 记忆数据 | 回答后 / 删除记忆 | `agent_memories` 去重更新；删除停止召回 | 脱敏字段 |
| AG-D-012 | 测试数据清理 | 全部用例跑完 | 删除测试会话/草稿/临时媒体/授权清理的测试业务数据 | 清理有结果；正式数据符合预期 |

## 三、清理实现

- 会话与草稿可通过 API（DELETE /v2/agent/conversations/{id}、/v2/agent/drafts/{id}）清理；业务测试数据按既有业务接口或数据库脚本（受控、带 owner 条件）清理。
- 清理脚本只放 `../scripts/data/`，必须支持 `--owner` 参数并默认拒绝无 owner 条件的全表删除。
- before/after 快照：`06-database-before.json`、`07-database-after.json`、`09-cleanup.json` 三份文件为必填证据。

## 四、证据存放

`data/artifacts/<日期>-<波次>-<用例>/`；SQL/脚本与计数查询 → `data/logs/`；报告 → `data/reports/`。