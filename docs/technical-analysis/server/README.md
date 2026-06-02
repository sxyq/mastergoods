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
| `/v2` 领域模型与契约 | 新版待做 | 旧版无分层后端契约 | 新版能力统一在 `/v2` 承载 | 目前尚未开始代码实现 | 先由 spec 驱动 |
| owner 账号隔离 | 需重构 | 旧版无统一账号边界 | 所有核心业务表补 `owner_user_id` | 当前仅 users/sessions 独立，业务表仍偏全局 | 是导入前置条件 |
| 商品/往来/财务/库存扩域 | 旧版存在新版未做 | 旧版表域更厚 | 新版能力要超过旧版 | 当前后端仍是首版字段集 | 会带来表与接口扩容 |
| 会员体系 | 新版需要去掉 | 旧版可能有会员扩展 | 当前阶段不纳入 server 重构范围 | 不应新增 member 相关实现 | 后续恢复需单独 spec |
