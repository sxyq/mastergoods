# Agent 破坏性逆向安全测试计划

## 目标

验证 Agent 在真实模型、真实工具、真实流式事件与真实多模态附件参与时，是否能够抵御：

1. Prompt injection / system prompt 提取
2. Tool abuse / unauthorized create / over-broad tool execution
3. Cross-tenant memory、audit、draft、run 泄露
4. Stream event 篡改、取消竞争、结果块伪造
5. 图片引用、附件引用、草稿确认链路的越权操纵

## 证据标准

- 每个攻击向量都要保留 prompt、工具计划、审计链、服务端响应、客户端表现
- 成功利用必须区分“模型服从问题”“工具权限问题”“服务端鉴权问题”“客户端信任边界问题”
- 若验证过程中发现需要修改后端安全逻辑，先暂停并回报，不直接改服务端

## 执行顺序

1. Prompt 与工具规划链
2. Draft / run / conversation / audit 越权链
3. Streaming 协议与 cancel 竞争链
4. 多模态附件与图片引用链
5. Provider fallback / honesty / error masking 链
