# P1 长期记忆接入与在线搜索安全证据 — 2026-08-23/24

- 代理：team-lead（主控）
- 范围：计划第 8 节（长期记忆）、第 9 节（在线搜索）

## 长期记忆（计划 8 节）
### 接入 Agent 请求链（此前 AgentMemoryService 已实现但未接入）
- `V2AgentAiService`：
  - 新增 `@Autowired(required=false) AgentMemoryService` 字段注入（不碰构造器，保持既有测试与调用方兼容）。
  - `buildScopeDescriptionWithMemory`：请求开始时按 owner/store 召回历史记忆，附加到上下文作用域说明，明确标记"历史记忆（仅供参考，不是当前实时业务数据）"；召回失败只丢弃记忆块，不阻塞主回答。
  - `extractMemoriesAfterAnswer`：回答完成后异步提取候选记忆（非流式 chat 用保存的用户消息 id；流式 runChatStream 用 `resolveLatestUserMessageId` 尽力解析；LLM 失败/空回答不提取；服务未注入/自动学习关闭跳过）。

### 修复的产品 bug（AgentMemoryService）
1. 18 位身份证号（末位 X）未被脱敏：SENSITIVE_PATTERN 增加 `\b\d{17}[0-9Xx]\b` 分支。
2. 召回主查询异常会传播：recallMemories 主查询包 try-catch，失败返回空列表（降级不阻塞）。

### 新增测试 AgentMemoryServiceTest（12 个）
owner/store 隔离召回、limit 截断、无 store 按 owner 召回、自动学习关闭不召回/不写入、null owner 空返回、异步提取脱敏、敏感信息脱敏（手机/邮箱/身份证）、删除 owner 隔离、详情 owner 隔离、按会话清理、重复 sourceMessage 更新去重、召回异常不传播、空内容跳过提取。

## 在线搜索（计划 9 节）
### 修复的产品 bug（WebSearchUrlSafety）
Java 21 的 `URI.getHost()` 对 IPv6 字面量返回带方括号的 `"[::1]"`，导致 IPv6 环回/ULA/链路本地前缀检查失效。修复：解析主机名前去括号后再判断（含 DNS 重绑定兜底改传无括号地址）。

### 新增测试
- WebSearchUrlSafetyTest（9 个）：公开 URL 放行、非 HTTP(S) 协议拒绝、null/空/非法 URL 拒绝、localhost/.local 拒绝、环回/私有网段/链路本地/云元数据（169.254.169.254、metadata.google.internal、metadata.azure.com）拒绝、CGNAT/畸形 IPv4 拒绝、IPv6 环回/ULA/链路本地拒绝。
- WebSearchProviderContractTest（6 个）：DisabledWebSearchProvider 返回 DEFERRED 不伪造结果、状态工厂、引用编号一一对应、URL 安全委托、请求字段、limit 默认与截断。

## 验证（主控）
- `./Code/backend/gradlew -p Code/backend test --offline` → **592 tests completed, 1 failed**（唯一失败 V2BillDomainControllerTest.purchaseReceiptListReturnsSnakeCaseFields 为既有失败，与本轮无关）。
- 记忆 12/12、搜索 15/15 全绿；V2AgentAiServiceTest 75/75（接入无回归）。

## Blocked / Deferred
- 长期记忆真实 LLM 提取：Deferred（第一版为确定性规则提取，不调 LLM）。
- 在线搜索真实 Provider 调用（搜索、抓取、超时、重定向、审计）：Deferred（未配置搜索 Provider；DisabledWebSearchProvider 保证 DEFERRED 不伪造）。
- 跨会话记忆真实端到端（会话 A 写入 → 会话 B 召回）：Deferred（需部署环境 + 真实对话）。
