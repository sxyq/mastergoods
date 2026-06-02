# Server resources 模块分析

- 对应源码目录：`src/main/resources`
- 当前资源：
  - `application.yml`
  - `application-local.yml`
  - `application-prod.yml`
  - `db/migration/V1__init.sql` ~ `V9__product_price_levels_and_supplier_relations.sql`
  - `static/admin-console/*`

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
| 首版配置与 Flyway 迁移 | 新版已做 | 旧版本地库不适用当前服务端资源层 | 支撑现有后端启动和表结构演进 | 现有配置与 V1-V9 迁移已存在 | 当前后端可运行 |
| owner 底座迁移脚本 | 新版已做 | 旧版无统一 owner 回填与系统默认归属账号 | 通过增量迁移落地 owner 基础设施 | 已新增 `V7__owner_scope_foundation.sql` | 为 `/v1` owner 收口与 `/v2` 扩域打底 |
| 商品/伙伴扩域迁移脚本 | 新版已做 | 旧版无当前 `/v2` 第一批扩域表 | 为商品分类/单位、伙伴分组/联系人提供结构落点 | 已新增 `V8__product_and_partner_expansion.sql` | 与 `/v2/products`、`/v2/*partners*` 首批接口配套 |
| 商品价格/供应关系扩域迁移脚本 | 新版已做 | 旧版价格和供应关系明显更厚 | 为 `product_price_levels`、`product_supplier_relations` 与商品多价格值快照提供结构落点 | 已新增 `V9__product_price_levels_and_supplier_relations.sql` | 第三阶段 `/v2/products` 扩域读写会依赖这批结构 |
| 会员相关资源脚本 | 新版需要去掉 | 旧版可能有会员相关数据域 | 当前阶段不纳入 | 不应新增 member 迁移与配置 | 恢复时再立项 |
