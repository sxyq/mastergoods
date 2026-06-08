# Android feature/agent 模块分析

- 对应源码目录：`master-goods-android/feature/agent`
- 关键源码：
  - `AgentWorkbenchScreen.kt`
  - `AgentChatScreen.kt`
  - `OperationDraftScreen.kt`
  - `AgentTaskScreen.kt`
  - `AgentViewModel.kt`

## 模块定位

`feature/agent` 是当前新版相对旧版最明确的能力优势。  
在后续规划里，它的重点不是“再多做一些视觉页面”，而是：

- 让 AI 与 owner 私有经营数据建立清晰边界
- 承接对商品、单据、财务、库存的跨域查询与建议
- 从首版聚合页升级成更完整的 AI 子域

## 状态图例

- `新版已做`
- `新版待做`
- `旧版存在新版未做`
- `新版需要去掉`
- `需重构`
- `待验证`

## 状态表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| AI 工作台/问答/草稿/任务首版页面 | 待验证 | 旧版没有这一域 | 保持并继续增强 | 工作台、问答、草稿、任务/通知页面与 ViewModel 已存在 | 会话/消息/草稿链已落地，但工作台仍含占位聚合，任务/通知暂无真实 `/v2` 端点 |
| owner 私有上下文感知 | 待验证 | 旧版无对应域 | AI 只能读取当前 owner 可见数据 | 已切到 `AgentV2Repository + AgentConversationDto/AgentMessageDto/AgentDraftDto` | 当前仅会话/消息/草稿链切到 `/v2`；任务/通知仍待后端补端点 |
| 更细的会话、草稿、通知详情 | 待验证 / 新版待做 | 旧版无对应域 | 拆出更完整的 AI 子域模型和页面职责 | 会话与草稿已切到 V2 Dto，通知暂无 V2 端点 | 草稿部分待验证；通知仍为新版待做 |
| “AI 页面主要看视觉贴图是否像设计稿”思路 | 新版需要去掉 | 容易把重点放偏 | 当前阶段先看场景、数据、职责和契约 | 当前文档已改为规划视角 | 视觉细化后置 |

## 新版功能规划重点

| 方向 | 状态 | 说明 |
|---|---|---|
| `conversation/message` 模型 | 待验证 | 与 server `agent` 会话域对齐 | B07 已补会话状态更新（active/closed/archived）与级联删除能力，closed/archived 会话拒绝新消息 |
| AI 草稿提交闭环 | 新版待做 | 草稿不只是页面展示，还要成为业务动作入口 |
| 任务与通知 owner 隔离 | 新版待做 | 所有 AI 任务与通知都归属于当前 owner |
| 跨域经营建议 | 待验证 | 与商品/单据/财务/库存扩域一起演进 |

## UI 统一约束

- 本模块后续新增业务必须继续复用当前设计图对应的页面母版，不允许因为领域变厚就切换成另一套视觉语言。
- 页面结构优先落入既有模式：列表页、详情页、编辑页、报表页、AI 页、设置页。
- 视觉基线固定为：浅蓝渐变背景、玻璃卡片、蓝色主按钮、白色次按钮、统一状态标签、五栏主壳。
- 如需新增 UI 组件，先沉淀到 `core/designsystem`，再由本模块复用；不允许长期保留 feature 私有样式组件。
- 验收时当前优先对照：`docs/spec/42-android-liquid-glass-ui-refactor-plan.md`、Stitch 导出清单、`master-goods-android/UI-DESIGN-SPEC.md`、`docs/technical-analysis/android/core/designsystem/README.md`；`docs/design-mockups/` 仅作历史参考。
