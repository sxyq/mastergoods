# Server infrastructure 层分析

- 对应源码目录：`src/main/java/com/zhihuiji/backend/infrastructure`
- 子目录：
  - [ai/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/server/infrastructure/ai/README.md)
  - [config/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/server/infrastructure/config/README.md)
  - [security/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/server/infrastructure/security/README.md)

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
| 安全与配置基础设施 | 新版已做 | 旧版无统一服务端基础设施 | 通过 Spring 配置后端运行时 | 安全过滤器、配置类、AI 客户端已存在 | 是后端可运行基础 |
| owner 上下文注入 | 新版待做 | 旧版无多租户上下文 | 在认证完成后把当前 owner 注入业务层 | 目前只完成 token 级别会话处理 | 将影响 filter/service/repository |
| AI 任务运行时增强 | 新版待做 | 旧版无 AI 任务基础设施 | 增强任务、草稿、缓存、通知 | 当前已有 LongCat + AgentTaskConfig | 先保持现有能力 |
| 宽松安全策略 | 需重构 | 首版更重联调效率 | 收紧权限边界并兼容 `/v1` | 当前安全基础设施仍以首版形态为主 | 先文档后代码 |
