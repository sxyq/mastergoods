# Forbidden Scan Review

This review explains each production-path keyword hit from
`10-forbidden-scan.txt`. A `pass` verdict means the hit was checked
against source context and does not create mock data, fake streaming,
placeholder results, or simulated agent behavior. Unknown future hits
must remain `needs evidence` until reviewed.

| # | Location | Verdict | Reason |
|---|---|---|---|
| 1 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentChatScreen.kt:849` | `pass` | 输入框 placeholder 文案，提示用户输入经营问题；不生成 placeholder 数据或默认报表结果。 |
| 2 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentChatViewModel.kt:27` | `pass` | 仅为同文件 answer_delta 合帧节流提供 coroutine delay import；是否安全由 delay(48) 调用点约束。 |
| 3 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentChatViewModel.kt:618` | `pass` | UI 合帧节流，只在收到服务端 answer_delta 后合并刷新；不会拆分完整 answer，也不会生成本地假 token。 |
| 4 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentMarkdownText.kt:499` | `pass` | Markdown parser 的边界解析、链接、强调和行内代码切片；不参与回答拆字、假流式或补造业务数据。 |
| 5 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentMarkdownText.kt:521` | `pass` | Markdown parser 的边界解析、链接、强调和行内代码切片；不参与回答拆字、假流式或补造业务数据。 |
| 6 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentMarkdownText.kt:522` | `pass` | Markdown parser 的边界解析、链接、强调和行内代码切片；不参与回答拆字、假流式或补造业务数据。 |
| 7 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentMarkdownText.kt:524` | `pass` | Markdown parser 的边界解析、链接、强调和行内代码切片；不参与回答拆字、假流式或补造业务数据。 |
| 8 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentMarkdownText.kt:553` | `pass` | Markdown parser 的边界解析、链接、强调和行内代码切片；不参与回答拆字、假流式或补造业务数据。 |
| 9 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentMarkdownText.kt:566` | `pass` | Markdown parser 的边界解析、链接、强调和行内代码切片；不参与回答拆字、假流式或补造业务数据。 |
| 10 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentMarkdownText.kt:585` | `pass` | Markdown parser 的边界解析、链接、强调和行内代码切片；不参与回答拆字、假流式或补造业务数据。 |
| 11 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/ResultBlockRenderer.kt:1105` | `pass` | 负向防护文案：当图表缺少真实标签时停止绘制，明确避免生成模拟标签。 |
| 12 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/ResultBlockRenderer.kt:1108` | `pass` | 负向防护文案：当图表缺少真实标签时停止绘制，明确避免生成模拟标签。 |
| 13 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:1433` | `pass` | 负向安全/诚实文案，要求工具失败时不得用模拟数据替代真实查询。 |
| 14 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:1540` | `pass` | 负向安全/诚实文案，要求工具失败时不得用模拟数据替代真实查询。 |
| 15 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:1559` | `pass` | 负向安全/诚实文案，要求工具失败时不得用模拟数据替代真实查询。 |
| 16 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:1764` | `pass` | 日志、错误摘要、图表标签、conversation title 或 UI 摘要的长度裁剪；不用于本地打字机、规则摘要分块或 fake model_stream。 |
| 17 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:1838` | `pass` | 负向安全/诚实文案，要求工具失败时不得用模拟数据替代真实查询。 |
| 18 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:183` | `pass` | 负向入口说明，明确 AI 工作台不返回默认报表或模拟数据；不生成任务、通知、草稿或流式内容。 |
| 19 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:2433` | `pass` | 日志、错误摘要、图表标签、conversation title 或 UI 摘要的长度裁剪；不用于本地打字机、规则摘要分块或 fake model_stream。 |
| 20 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:2495` | `pass` | 日志、错误摘要、图表标签、conversation title 或 UI 摘要的长度裁剪；不用于本地打字机、规则摘要分块或 fake model_stream。 |
| 21 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:2598` | `pass` | 日志、错误摘要、图表标签、conversation title 或 UI 摘要的长度裁剪；不用于本地打字机、规则摘要分块或 fake model_stream。 |
| 22 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:2661` | `pass` | 日志、错误摘要、图表标签、conversation title 或 UI 摘要的长度裁剪；不用于本地打字机、规则摘要分块或 fake model_stream。 |
| 23 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:2676` | `pass` | 日志、错误摘要、图表标签、conversation title 或 UI 摘要的长度裁剪；不用于本地打字机、规则摘要分块或 fake model_stream。 |
| 24 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:706` | `pass` | 从模型规划文本中提取 JSON 对象边界；不拆分最终答案，也不补造工具结果。 |
| 25 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentConversationService.java:270` | `pass` | 会话标题/摘要长度裁剪；不改变工具查询结果，不生成回答内容或流式事件。 |
