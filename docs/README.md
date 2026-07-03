# 智慧记新版文档中心

这里是当前项目的正式文档入口。

## 当前优先阅读顺序

1. [40-batch-development-master-plan.md](./spec/40-batch-development-master-plan.md)
2. [42-android-liquid-glass-ui-refactor-plan.md](./spec/42-android-liquid-glass-ui-refactor-plan.md)
3. [43-ai-assistant-requirements.md](./spec/43-ai-assistant-requirements.md)
4. [00-product-overview.md](./spec/00-product-overview.md)
5. [01-status-taxonomy.md](./spec/01-status-taxonomy.md)
6. [02-domain-model-overview.md](./spec/02-domain-model-overview.md)
7. [10-auth-and-tenant.md](./spec/10-auth-and-tenant.md)
8. 业务域 spec：`20 ~ 32`

## 文档分层

- `docs/spec/`
  - 新版正式规范中心，后续开发以此为准
- `docs/acceptance-evidence/`
  - 验收证据归档（ai-agent、performance、b11）

## 当前 Android UI 真源

- 正式计划：
  - [42-android-liquid-glass-ui-refactor-plan.md](./spec/42-android-liquid-glass-ui-refactor-plan.md)
- Stitch 导出清单（位于 web/public 下，前端可直接访问）：
  - [manifest.tsv](/Users/sunyiyang/Desktop/Project/master-goods/web/public/stitch_exports/visual-design_system_framework_14840154594131085259/manifest.tsv)
- Android 摘要规范入口：
  - [master-goods-android/UI-DESIGN-SPEC.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/UI-DESIGN-SPEC.md)

## 当前总控入口

- [40-batch-development-master-plan.md](./spec/40-batch-development-master-plan.md)
  - 这是跨后端、Android、联调与验收的总控文档
- [42-android-liquid-glass-ui-refactor-plan.md](./spec/42-android-liquid-glass-ui-refactor-plan.md)
  - 这是当前 Android 全量 UI 重构的专项总计划
- [43-ai-assistant-requirements.md](./spec/43-ai-assistant-requirements.md)
  - 这是 AI 助手真实 agentic、无模拟数据、Markdown / 图表、流式体验与后续审查的需求基线
- [AI_AGENT_P0_EVIDENCE_MATRIX.md](./acceptance-evidence/ai-agent/AI_AGENT_P0_EVIDENCE_MATRIX.md)
  - 这是 AI 助手 AGT-P0-001..019 当前证据、缺口和下一步的统一总表

## 状态字段

- `新版已做`
- `新版待做`
- `旧版存在新版未做`
- `新版需要去掉`
- `需重构`
- `待验证`

## 说明

- 会员体系当前不纳入新版范围，已在 spec 中标记为 `新版需要去掉`
- `docs/technical-analysis/`、`docs/design-mockups/` 与根下历史审计 .md 已删除，源码与 spec 为唯一真源
- Stitch 设计导出位于 `web/public/stitch_exports/`，前端可直接访问
- 后续每完成一个批次或一组代码改动，更新对应 spec 文档外，还必须回写 `40-batch-development-master-plan.md`
