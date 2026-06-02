# Server service 模块分析

- 对应源码目录：`src/main/java/com/zhihuiji/backend/application/service`
- 当前服务数：20+
- 覆盖：认证、商品、客户、供应商、销售、采购、付款、财务、报表、同步、AI、管理端、演示数据，以及 `application/service/v2` 下的首批单据 facade

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
| 首版业务 Service 集合 | 新版已做 | 旧版没有当前服务端服务层 | 支撑当前 `/v1` 业务闭环 | 17 个 Service 已存在 | 当前安卓依赖这层 |
| owner-aware 事务边界 | 新版已做 | 旧版无统一 owner | 所有单据主链路都按 owner 处理 | 商品、客户、供应商、销售、采购、付款、财务、同步、报表、AI 已接入 `CurrentOwnerService` | admin/global 口径继续清理 |
| 会话访问缓存与黑名单 | 新版已做 | 旧版认证链每次直查 session 表，也没有失效 token 的短期黑名单 | 统一处理 access/refresh token 的缓存、过期判断与失效回收 | 已新增 `SessionAccessService`，并接入 `AuthService` 与 `TokenAuthenticationFilter` | 当前是单实例内存级策略，分布式部署后仍需扩展 |
| `/v2` 单据域 facade service | 新版已做 | 旧版无 `/v2` service 分层 | 新版控制器通过独立 facade 访问领域服务 | 已新增 `application/service/v2` 下销售/采购/付款 facade | 避免 `/v1` DTO 与 `/v2` DTO 混用 |
| `/v2` 商品与伙伴域服务 | 新版已做 | 旧版无 `/v2` 商品/伙伴服务层 | 第二阶段先建立商品、分类、单位、客户、供应商、分组、联系人服务 | 已新增 `V2ProductService`、`V2ProductCategoryService`、`V2ProductUnitService`、`V2CustomerService`、`V2SupplierService`、`V2PartnerGroupService`、`V2PartnerContactService` | `/v1` 基础 service 保持冻结兼容职责 |
| 商品第三阶段扩域服务 | 新版已做 | 旧版无价格层级与供应关系 service 分层 | 为 `/v2/products` 提供多价格与供应关系主逻辑 | 已新增 `V2ProductPriceLevelService`、`V2ProductSupplierRelationService`，并升级 `V2ProductService` 读写扩域字段 | 当前已补 service 单测 |
| 联系人主摘要镜像 | 新版已做 | 旧版客户/供应商联系人摘要未沉淀到新版主档字段 | 联系人主联系人变化后自动刷新 `customers` / `suppliers` 摘要字段 | `V2PartnerContactService` 已在 create/update/delete/sync 路径同步 `contactName/contactPhone` | 已补 service 测试覆盖主联系人刷新行为 |
| 同步上传只覆盖部分实体类型 | 新版需要去掉 | 首版同步范围较窄 | 新版同步应覆盖扩域后的核心实体 | 当前 SyncService 仍偏首版同步范围 | 随 `/v2/sync` 重构 |
| 销售订单态/采购订单态/账户/库存统计等服务 | 旧版存在新版未做 | 旧版能力域更厚 | 新版服务层要超过旧版 | 当前服务仍偏首版业务闭环 | 会新增多个领域服务 |
