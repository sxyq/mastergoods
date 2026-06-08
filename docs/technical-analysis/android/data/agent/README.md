# Android data/agent 模块分析

- 对应源码目录：`master-goods-android/data/agent`
- 关键源码：`AgentRepository.kt`、`AgentV2Repository.kt`、`MediaV2Repository.kt`

## 模块定位

`data/agent` 是 AI 助手的数据入口。  
新版里，它会从首版聚合接口调用，逐步演变成：

- owner 私有 AI 上下文获取
- 会话/消息获取与提交
- 草稿结果缓存
- 任务与通知读取

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
| AI 工作台/问答/任务仓储 | 新版已做 | 旧版无这类 AI 数据层 | 承载助手域接口调用 | 当前 `AgentRepository.kt` 已存在 | 是我们相对旧版的优势域 |
| 对话/草稿缓存与 `/v2` 契约 | 待验证 | 旧版无对应域 | 扩展 conversation/message/draft 数据结构 | 已新增 `AgentV2Repository.kt`、`MediaV2Repository.kt` 对接 `ZhihuijiV2Api` 的会话/消息/草稿/媒体首轮接口；B08 修复：`AgentV2Repository.deleteDraft()` 改用 `safeApiUnitCall`，`MediaV2Repository.deleteAsset()`/`deleteBinding()` 改用 `safeApiUnitCall`，API 方法名统一加 `V2` 后缀；`AgentV2RepositoryTest` 已直接调用真实 Repository 方法验证 delete/update 委派链路 | 先以后端 spec 为准，真实工作台与上传流程仍待联调 |
| B07 会话更新与删除 | 待验证 | 旧版无对应域 | 支持会话状态更新与级联删除 | `AgentV2Repository.kt` 已新增 `updateConversation()` 与 `deleteConversation()` 方法 | 对齐 `PUT/DELETE /v2/agent/conversations/{id}`；`updateConversation` 传入 `UpdateAgentConversationRequest`（status 约束 `[active, closed, archived]`）；`deleteConversation` 触发服务端级联删除消息/草稿 |
| owner 私有 AI 上下文 | 新版待做 | 旧版无对应域 | AI 结果只面向当前 owner 数据域 | 当前还未显式体现 | 需与 auth/sync 联动 |

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 当前视觉真源以 Stitch 导出、`docs/spec/42-android-liquid-glass-ui-refactor-plan.md` 与 `master-goods-android/UI-DESIGN-SPEC.md` 为准；`docs/design-mockups/` 仅作历史参考。
