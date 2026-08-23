# P0 工具选择与草稿边界证据 — 2026-08-23

- 代理：backend-agent（子代理编码）+ team-lead（主控修复/验证/提交）
- 范围：计划阶段 1/2，回归用例 009/012/016/041/048/049/051/052/053/054

## 新增测试
- Code/backend/src/test/java/com/zhihuiji/backend/application/service/v2/agent/V2AgentToolSelectionRegressionTest.java（13 个测试）：
  - 009 customer_receivable_lookup 查询完成 → COMPLETED
  - 016 inventory_low_stock / 041 supplier_payable 查询完成 → COMPLETED
  - 049/048：只查依赖不创建 → EXHAUSTED（不返回成功语义；answer 带 [状态] 后缀；audit=exhausted）
  - 049/051/052/053/054：依赖查询 + 目标 CREATE_ONLY 工具 → CONFIRMATION_PENDING + 草稿保存 + 正式表无变化
  - 012：product_catalog_lookup + generate_poster_prompt（READ_ONLY 目标）→ COMPLETED
  - 缺少必填参数的 CREATE_ONLY 被跳过 → EXHAUSTED
  - confirmationPendingAnswerDoesNotClaimFormalWrite / exhaustedResponseDoesNotClaimSuccessInAnswerOrAudit

## 主控发现并修复的问题

### 1. 测试桩链式调用 bug（子代理测试代码）
4 个 create 测试桩用 `objectMapper.createObjectNode().put(...).putArray("items").addObject().put(...)` 链式结尾，整个表达式返回 items[0] 而非根对象，导致 create 工具参数只有 items 内容、缺 supplier_id/customer_id 等必填字段，被"必填参数缺失/非法字段 unit_cost"门拒绝（工具未执行，回答"已生成草稿"被 validateModelAnswer 以 unsupported_write_claim 拒绝 → FAILED）。
修复：改用 `objectMapper.valueToTree(Map.of(...))` 构造完整参数，并移除非法字段 unit_cost（Schema 只有 price）。

### 2. 产品代码 bug：poster 回答被 unsupported_write_claim 误伤（AnswerSynthesizer.java）
`generate_poster_prompt` 是 READ_ONLY 文本产物工具，回答"已生成海报提示词。"命中 UNSUPPORTED_WRITE_CLAIM 的"已+生成"分支且无草稿/可视化 → 被误拒 → FAILED。
修复：validateModelAnswer 增加 `hasSuccessfulPosterPrompt(payload)` 豁免（工具成功且 facts 为对象时视为真实事实，不当作伪造正式写入）。

### 3. poster 测试断言修正
COMPLETED 响应的 completedTools 字段为空是既有契约，断言改为通过 toolCalls 验证 generate_poster_prompt 完成。

## 验证
- `./Code/backend/gradlew -p Code/backend test --offline --tests "...V2AgentToolSelectionRegressionTest"` → BUILD SUCCESSFUL（13 tests，全绿）
- 全套件状态：见主控汇总（backend-payments 分页签名变更导致的 V2AgentAiServiceTest 回归由该子代理修复中）

## Blocked / Deferred
- 真实 Provider（deepseek flash）多轮工具选择：Deferred（单测用 stub LLM 验证服务端逻辑；模型是否续轮选择创建工具依赖真实 Provider 行为）。
- 真实跨 owner/store、同店多成员并发：Blocked。
