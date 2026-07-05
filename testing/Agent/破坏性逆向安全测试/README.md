# Agent 破坏性逆向安全测试

本目录用于记录 Agent 这一跨端能力本身的攻击式安全测试，重点覆盖提示词注入、工具滥用、跨租户泄露、流式协议篡改和多模态注入。

路由策略：

- 总编排：`attack-chain`
- Agent 专项：`llm-security`
- 接口与越权：`api-security`
- 客户端辅助：`apk-reverse` / `mobile-reverse` / `js-reverse`

目录内容：

- `TEST_PLAN.md`
- `reverse_attack_matrix.csv`
- `scripts/`
