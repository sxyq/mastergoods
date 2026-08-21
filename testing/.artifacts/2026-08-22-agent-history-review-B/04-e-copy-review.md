# 首阶段 `*-e` 临时副本核对

核对日期：2026-08-22

| 临时文件 | 对应文件 | 内容关系 | 处理 |
|---|---|---|---|
| `testing/web/功能测试/web_action_audit_20260822_B.csv-e` | `testing/web/功能测试/web_action_audit_20260822_B.csv` | 记录逐行一致；临时文件多一个末尾空行，字节和 SHA-256 不同 | 保留原文件；排除新提交 |
| `testing/web/功能测试/阶段报告-20260822-Web按钮Agent-B.md-e` | `testing/web/功能测试/阶段报告-20260822-Web按钮Agent-B.md` | 正文逐行一致；临时文件多一个末尾空行 | 保留原文件；排除新提交 |
| `testing/.artifacts/2026-08-22-web-buttons-B/04-case-register.jsonl-e` | `testing/.artifacts/2026-08-22-web-buttons-B/04-case-register.jsonl` | JSONL 记录逐行一致；临时文件多一个末尾空行 | 保留原文件；排除新提交 |

这些文件是首阶段工作产生的临时副本，不是本轮历史 Agent 原始证据。本轮没有删除或覆盖它们，也没有把它们加入 history evidence-only commit。
