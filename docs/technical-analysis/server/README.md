# Server 代码目录分析

- 对应源码目录：
  - `src/main/java/com/zhihuiji/backend`
  - `src/main/resources`
- 当前主实现层级：
  - `api`
  - `application/service`
  - `domain/entity`
  - `infrastructure`
  - `resources`

## 状态图例

- `新版已做`
- `新版待做`
- `旧版存在新版未做`
- `新版需要去掉`
- `需重构`
- `待验证`

## 当前目录映射

- [api/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/server/api/README.md)
- [entity/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/server/entity/README.md)
- [repository/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/server/repository/README.md)
- [service/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/server/service/README.md)
- [infrastructure/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/server/infrastructure/README.md)
- [resources/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/server/resources/README.md)

## 状态表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `/v1` Spring Boot 后端 | 新版已做 | 旧版 app 为本地私有账本 | 提供可登录、可增删改查、可同步的后端 | 已有控制器、服务、实体、迁移脚本 | 是当前安卓联调基础 |
| `/v2` 领域模型与契约 | 新版已做 | 旧版无分层后端契约 | 新版能力统一在 `/v2` 承载 | 当前已落地商品/伙伴/单据/财务/库存，以及 B06 首轮 `/v2/sync/*` 与 `/v2/import-jobs/*` | 后续领域继续在 `/v2` 扩展；`/v2/sync` cursor 已升级为稳定 token |
| owner 账号隔离 | 新版已做 | 旧版无统一账号边界 | 所有核心业务表补 `owner_user_id` | V7迁移脚本已回填历史数据，`CurrentOwnerService` 已落地，`/v2` 接口默认 owner 过滤 | `/v1` 兼容层仍在补齐 owner 过滤 |
| 商品/往来/财务/库存/同步扩域 | 旧版存在新版未做 | 旧版表域更厚 | 新版能力要超过旧版 | 当前已落地商品/伙伴首批扩域、商品多价格与供应关系、财务与库存底座、B06 首轮同步与导入任务模型 | 媒体、AI 扩域与导入执行器仍待补 |
| 会员体系 | 新版需要去掉 | 旧版可能有会员扩展 | 当前阶段不纳入 server 重构范围 | 不应新增 member 相关实现 | 后续恢复需单独 spec |

## B11 验收状态

| 项目 | 状态 | 当前证据 | 待补 |
|---|---|---|---|
| `/v2` service/controller/migration 自动化 | 待验证 | `src/test/java` 当前已有 41 个测试文件，覆盖 `/v2` service、controller、migration SQL 与 `/v1` compatibility | 需要本轮 JDK 21 定向测试或全量测试输出 |
| 复验入口 | 新版已做 | `docs/spec/41-b11-acceptance-matrix.md` 与 `tools/b11_acceptance_check.sh backend-smoke` 已建立 | 后续把命令日志归档到 `docs/acceptance-evidence/b11/backend/` |
| 117/发布环境 smoke | 待验证 | 本文档未记录本轮 117 部署、健康检查或接口 smoke 证据 | 需要环境版本、健康检查、关键 `/v2` 接口日志 |
| 安全发布 | 待验证 | owner 边界已有测试基础，但尚未形成发布安全清单证据 | 需要鉴权、owner 隔离、安全头、敏感日志、迁移回滚与健康检查记录 |
