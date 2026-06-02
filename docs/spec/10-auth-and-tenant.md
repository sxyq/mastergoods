# 10 账号与归属

## 需求表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| users / sessions | 新版已做 | 有账号与会话 | 保持 | 已存在 | 可继续沿用 |
| owner_user_id | 新版已做 | 旧版无统一归属 | 所有业务表统一归属 | 首批核心业务表与同步游标已补 | repository/service 过滤继续推进 |
| session 访问缓存与黑名单 | 新版已做 | 旧版 token 校验每次直查 session 表 | 通过短 TTL 缓存与 token 失效黑名单降低认证热点压力 | 已新增 `SessionAccessService`，统一处理 access/refresh token 的缓存、过期判断与失效回收 | 当前仍是单机内存级策略，分布式黑名单后续再补 |
| tenant/数据边界 | 需重构 | 旧版全局业务库 | 按账号隔离 | 迁移与上下文底座已补 | 这是导入前置条件 |
| 系统默认归属账号 | 新版已做 | 旧版无历史回填账号 | 承接历史全局数据 | 已定义保留账号 `SYSTEM-LEGACY-OWNER` | 不对外暴露 |

## 规则

- 默认所有查询都必须带归属过滤
- 所有写入都必须写入当前用户归属
- 导入和同步也必须按归属分桶
- 历史全局数据统一回填到系统默认归属账号，不允许长期保留空 owner
- 当前认证上下文已统一可解析“当前 owner/user”标识，供 service/repository 使用
- access token / refresh token 的校验、缓存与失效回收当前统一经过 `SessionAccessService`
- 第二阶段新增的 `product_categories`、`product_units`、`partner_groups`、`partner_contacts` 也必须按 owner 隔离
- `partner_contacts.partner_id` 虽然是逻辑关联字段，但服务层必须校验它只能指向当前 owner 的 customer/supplier
