# 28 AI 助手域

## 需求表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| agent_tasks | 新版已做 | 旧版无此域 | 任务中心 | 已有 | 继续增强 |
| agent_notifications | 新版已做 | 旧版无此域 | 通知中心 | 已有 | 继续增强 |
| agent_conversations | 待验证 | 旧版无此域 | 会话表 | 已新增 `agent_conversations`、`AgentConversationEntity/Repository`、`V2AgentConversationService` 与 `/v2/agent/conversations/*` | 已补 owner 维度 service/controller 回归，仍待真实工作台联调；已新增 `PUT /v2/agent/conversations/{id}`（更新标题/状态）与 `DELETE /v2/agent/conversations/{id}`（级联删除消息与草稿）；状态枚举约束为 `[active, closed, archived]`，删除时服务级与 DB 级 `ON DELETE CASCADE` 均已落地（V14 迁移） |
| agent_messages | 待验证 | 旧版无此域 | 消息表 | 已新增 `agent_messages`、`AgentMessageEntity/Repository` 与 `/v2/agent/conversations/{conversationId}/messages` | 已验证会话摘要刷新与 owner 校验，仍待真实问答链联调；已增 `AgentMessageRepository.deleteAllByOwnerUserIdAndConversationId` 批量删除方法；`closed/archived` 状态会话拒绝新消息写入 |
| agent_drafts | 待验证 | 旧版无此域 | 草稿缓存 | 已新增 `agent_drafts`、`AgentDraftEntity/Repository` 与 `/v2/agent/drafts/*` | 已验证草稿 owner 引用与更新链路，推荐结果缓存仍待后续扩展；已增 `AgentDraftRepository.deleteAllByOwnerUserIdAndConversationId` 批量删除方法；草稿状态枚举约束为 `[active, archived]` |
