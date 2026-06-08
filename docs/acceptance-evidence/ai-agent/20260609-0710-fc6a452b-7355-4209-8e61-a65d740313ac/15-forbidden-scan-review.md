# Forbidden Scan Review

This review explains each production-path keyword hit from
`10-forbidden-scan.txt`. A `pass` verdict means the hit was checked
against source context and does not create mock data, fake streaming,
placeholder results, or simulated agent behavior. Unknown future hits
must remain `needs evidence` until reviewed.

| # | Location | Verdict | Reason |
|---|---|---|---|
| 1 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentChatScreen.kt:119` | `pass` | 仅用于刷新完成工具提示的短暂可见窗口；不拆分 answer，不生成 fake model_stream，也不制造业务数据。 |
| 2 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentChatScreen.kt:51` | `pass` | 仅为完成工具提示过期时钟提供 coroutine delay import；是否安全由 delay(300) 调用点约束。 |
| 3 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentChatScreen.kt:876` | `pass` | 输入框 placeholder 文案，提示用户输入经营问题；不生成 placeholder 数据或默认报表结果。 |
| 4 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentChatViewModel.kt:27` | `pass` | 仅为同文件 answer_delta 合帧节流提供 coroutine delay import；是否安全由 delay(48) 调用点约束。 |
| 5 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentChatViewModel.kt:627` | `pass` | UI 合帧节流，只在收到服务端 answer_delta 后合并刷新；不会拆分完整 answer，也不会生成本地假 token。 |
| 6 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentMarkdownText.kt:499` | `pass` | Markdown parser 的边界解析、链接、强调和行内代码切片；不参与回答拆字、假流式或补造业务数据。 |
| 7 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentMarkdownText.kt:521` | `pass` | Markdown parser 的边界解析、链接、强调和行内代码切片；不参与回答拆字、假流式或补造业务数据。 |
| 8 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentMarkdownText.kt:522` | `pass` | Markdown parser 的边界解析、链接、强调和行内代码切片；不参与回答拆字、假流式或补造业务数据。 |
| 9 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentMarkdownText.kt:524` | `pass` | Markdown parser 的边界解析、链接、强调和行内代码切片；不参与回答拆字、假流式或补造业务数据。 |
| 10 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentMarkdownText.kt:553` | `pass` | Markdown parser 的边界解析、链接、强调和行内代码切片；不参与回答拆字、假流式或补造业务数据。 |
| 11 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentMarkdownText.kt:566` | `pass` | Markdown parser 的边界解析、链接、强调和行内代码切片；不参与回答拆字、假流式或补造业务数据。 |
| 12 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentMarkdownText.kt:585` | `pass` | Markdown parser 的边界解析、链接、强调和行内代码切片；不参与回答拆字、假流式或补造业务数据。 |
| 13 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/ResultBlockRenderer.kt:1105` | `pass` | 负向防护文案：当图表缺少真实标签时停止绘制，明确避免生成模拟标签。 |
| 14 | `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/ResultBlockRenderer.kt:1108` | `pass` | 负向防护文案：当图表缺少真实标签时停止绘制，明确避免生成模拟标签。 |
| 15 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:1444` | `pass` | 负向安全/诚实文案，要求工具失败时不得用模拟数据替代真实查询。 |
| 16 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:1540` | `pass` | 日志、错误摘要、图表标签、conversation title 或 UI 摘要的长度裁剪；不用于本地打字机、规则摘要分块或 fake model_stream。 |
| 17 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:1577` | `pass` | 负向安全/诚实文案，要求工具失败时不得用模拟数据替代真实查询。 |
| 18 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:1596` | `pass` | 负向安全/诚实文案，要求工具失败时不得用模拟数据替代真实查询。 |
| 19 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:1801` | `pass` | 日志、错误摘要、图表标签、conversation title 或 UI 摘要的长度裁剪；不用于本地打字机、规则摘要分块或 fake model_stream。 |
| 20 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:184` | `pass` | 负向入口说明，明确 AI 工作台不返回默认报表或模拟数据；不生成任务、通知、草稿或流式内容。 |
| 21 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:1875` | `pass` | 负向安全/诚实文案，要求工具失败时不得用模拟数据替代真实查询。 |
| 22 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:2470` | `pass` | 日志、错误摘要、图表标签、conversation title 或 UI 摘要的长度裁剪；不用于本地打字机、规则摘要分块或 fake model_stream。 |
| 23 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:2532` | `pass` | 日志、错误摘要、图表标签、conversation title 或 UI 摘要的长度裁剪；不用于本地打字机、规则摘要分块或 fake model_stream。 |
| 24 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:2635` | `pass` | 日志、错误摘要、图表标签、conversation title 或 UI 摘要的长度裁剪；不用于本地打字机、规则摘要分块或 fake model_stream。 |
| 25 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:2698` | `pass` | 日志、错误摘要、图表标签、conversation title 或 UI 摘要的长度裁剪；不用于本地打字机、规则摘要分块或 fake model_stream。 |
| 26 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:2713` | `pass` | 日志、错误摘要、图表标签、conversation title 或 UI 摘要的长度裁剪；不用于本地打字机、规则摘要分块或 fake model_stream。 |
| 27 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:717` | `pass` | 从模型规划文本中提取 JSON 对象边界；不拆分最终答案，也不补造工具结果。 |
| 28 | `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentConversationService.java:270` | `pass` | 会话标题/摘要长度裁剪；不改变工具查询结果，不生成回答内容或流式事件。 |
