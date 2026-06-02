# technical-analysis 文档变更记录

> 历史说明：本文件保留文档层的关键结构变更，不再承担“全部问题修复流水账”角色。  
> 新版范围与领域规范请以 [docs/spec](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec) 为准。

## 2026-06-02

### 后端 owner 底座第一批实现

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `V7__owner_scope_foundation.sql` | 新版已做 | 旧版无统一 owner 回填机制 | 核心业务表统一补 owner 并回填历史数据 | 已新增迁移脚本与系统默认归属账号 | `SYSTEM-LEGACY-OWNER` 不对外暴露 |
| `CurrentOwnerService` | 新版已做 | 旧版 controller/service 无统一 owner 获取方式 | 统一从认证上下文读取当前 owner | 已新增基础服务 | 后续 repository/service 全面接入 |
| server entity 文档同步 | 新版已做 | 文档仍描述“owner 未落地” | 实时反映首批实体字段变化 | 已同步 `02-domain-model-overview`、`10-auth-and-tenant`、`server/entity` | 继续随代码原子更新 |
| owner-aware repository/service/controller | 新版已做 | 旧版单据链路主要按全局数据工作 | 核心单据、主数据、同步、报表、AI 改为默认 owner 过滤 | 已完成核心 repository/service 改造，并补 `/v2` 首批单据控制器 | 仍需补完整 JDK21 编译验证 |
| `/v2` 单据域首批 | 新版已做 | 旧版无 `/v2` 单据接口 | 先落地销售/采购/付款三条新契约 | 已新增 `controller/v2`、`dto/v2`、`service/v2` 三层 | 其他领域后续继续扩展 |
| Android 单据规划同步 | 新版已做 | 安卓文档仍只面向 `/v1` 首版 | 明确后端 `/v2` 已落地、安卓尚未切换 | 已同步 `31-android-impact`、`data/order`、`core/model`、`feature/sales|purchases|payments` | UI 代码仍未变更 |
| `FinanceRecordRepository.search` 字段修正 | 新版已做 | 首版搜索条件存在 `remark` 命名偏差 | 关键字搜索与实体字段严格一致 | 已改为 `recordNo/category/notes` 且保持 owner 过滤 | 财务域文档已同步 |
| `HttpClientConfig` | 新版已做 | 首版未显式提供 `RestClient.Builder` Bean | 稳定支撑 AI 基础设施与后续外部 HTTP 客户端 | 已新增配置类并完成装配 | 配置层文档已同步 |
| `AgentTaskConfig` 执行器契约收口 | 新版已做 | 首版执行器类型与 service 注入期望存在偏差 | 统一返回 `ExecutorService` 并声明关闭策略 | 已完成 Bean 契约修正 | 避免 agent 任务服务启动失败 |
| 本地 Java 21 后端全量验证 | 新版已做 | 之前仅有规划口径 | 完成编译与测试双验证 | 已通过 `compileJava`、`testClasses`、`test` | `32-rollout-and-compatibility` 已同步 |

### 商品域与伙伴域第二阶段第一批实体扩域

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `V8__product_and_partner_expansion.sql` | 新版已做 | 旧版这些能力在本地库中已存在更厚模型 | 第二阶段先补商品分类/单位、伙伴分组/联系人 | 已新增迁移脚本 | `/v2` 接口与服务下一批继续补 |
| `ProductCategoryEntity` / `ProductUnitEntity` | 新版已做 | 旧版有分类与单位体系 | 第二阶段先落商品主数据扩域表 | 已新增实体 | 多价格后续补 |
| `PartnerGroupEntity` / `PartnerContactEntity` | 新版已做 | 旧版有分组与联系人能力 | 第二阶段先落伙伴主数据扩域表 | 已新增实体 | tags 和价格策略后续补 |
| `ProductEntity` 扩域位 | 新版已做 | 旧版商品域比当前更厚 | 为 `/v2/products` 预留分类/单位引用 | 已新增 `categoryId/unitId` | `/v1` 通过 `@JsonIgnore` 保持冻结 |
| `CustomerEntity` / `SupplierEntity` 扩域位 | 新版已做 | 旧版客户/供应商画像更厚 | 为 `/v2/customers`、`/v2/suppliers` 预留分组与主联系人摘要 | 已新增 `groupId/contactName/contactPhone` | `/v1` 继续不暴露扩域字段 |
| `/v2` 商品域接口与服务 | 新版已做 | 旧版无 `/v2` 商品契约 | 第二阶段先落商品、分类、单位首批接口 | 已新增 `V2Product*` controller/service/dto | `/v1/products` 保持冻结兼容 |
| `/v2` 伙伴域接口与服务 | 新版已做 | 旧版无 `/v2` 伙伴契约 | 第二阶段先落客户、供应商、分组、联系人首批接口 | 已新增 `V2Customer*`、`V2Supplier*`、`V2Partner*` controller/service/dto | `/v1/customers|suppliers` 保持冻结兼容 |
| 第二阶段测试回归补齐 | 新版已做 | 新增扩域代码初次落地后仍需补迁移、service、controller、兼容回归验证 | 让商品/伙伴 `/v2` 首批能力与 `/v1` 冻结兼容同时具备可回归证据 | 已新增 `V8ProductAndPartnerExpansionSqlTest`、`V2ProductCategoryServiceTest`、`V2PartnerContactServiceTest`、`V2ProductControllerTest`、`V2PartnerControllerTest`、`V1CatalogCompatibilityControllerTest`，并修正联系人摘要测试桩顺序 | 当前以本地 JDK 21 全量测试通过为验收 |
| 安卓商品/伙伴规划同步 | 新版已做 | 安卓文档原先只面向 `/v1` 基础档案 | 明确后端商品/伙伴 `/v2` 已具备首批能力 | 已同步 `31-android-impact`、`data/product|customer|supplier`、`feature/products|customers|suppliers`、`core/model` | 本阶段不改安卓代码 |

### 安卓剩余基础模块文档深化

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `docs/technical-analysis/android/core/common/README.md` | 新版已做 | 仍偏工具类清单 | 改成金额/状态/错误三类跨域语义文档 | 已补 `BigDecimal/Double` 并存、状态集扩展、废弃兼容逻辑说明 | 更利于后续模型精度治理 |
| `docs/technical-analysis/android/core/designsystem/README.md` | 新版已做 | 偏现有 UI 组件说明 | 改成设计系统分层与领域组件规划文档 | 已补 token 层、容器层、领域组件方向、废弃兼容组件说明 | 不强调具体视觉样式 |
| `docs/technical-analysis/android/backdrop/README.md` | 新版已做 | 偏效果说明 | 改成底层渲染模块定位文档 | 已补构建信息、渲染职责、边界要求 | 明确不承载业务语义 |
| 基于 git 日志的漏文档补记 | 新版已做 | 部分代码已落地但文档只覆盖主线目标，没有补到辅助类和安全收口细节 | 按真实改动回补遗漏文档，避免技术分析与当前代码脱节 | 已补 `SessionAccessService`、`PaginationUtils`、`PartnerTypes`、`SyncCursorId`、`V7/V8` 迁移、Android 本地订单图与安全收口等文档说明 | 作为本轮“代码先于文档”的回补收口 |

### 商品域第三阶段第一批结构扩域

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `V9__product_price_levels_and_supplier_relations.sql` | 新版已做 | 旧版商品域在多价格与供应关系上更厚 | 第三阶段先补价格层级、商品-供应商关系与商品多价格值快照列 | 已新增迁移脚本 | 后续 `/v2` 服务与接口继续补 |
| `ProductPriceLevelEntity` | 新版已做 | 旧版有多价格体系 | 建立 owner 私有价格层级定义 | 已新增实体 | `code/name` 走 owner 内唯一 |
| `ProductSupplierRelationEntity` | 新版已做 | 旧版有商品-供应商关系 | 建立 owner 私有商品-供应商关系与采购偏好模型 | 已新增实体 | 包含默认供应商与采购优先级 |
| `ProductEntity.priceLevelValuesJson` | 新版已做 | 旧版多价格值维度明显更厚 | 让商品主档能挂接多价格值结构，同时不污染 `/v1` | 已新增 JSON 快照列并保持 `@JsonIgnore` | `/v2/products` 会消费该字段 |
| 商品价格/供应关系仓储 | 新版已做 | 旧版商品目录厚度更高 | 为第三阶段 `/v2/products` 扩域读写打底 | 已新增 `ProductPriceLevelRepository`、`ProductSupplierRelationRepository` | 服务与控制器下一批补上 |

### 商品域第三阶段第二批 `/v2` 契约落地

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `V2ProductPriceLevelService` | 新版已做 | 旧版没有当前 `/v2` 服务层 | 为 owner 私有价格层级提供 CRUD、唯一性校验、引用校验 | 已新增 service | 已补单测 |
| `V2ProductSupplierRelationService` | 新版已做 | 旧版没有当前 `/v2` 服务层 | 为商品-供应商关系提供 CRUD、默认供应商约束与 owner 校验 | 已新增 service | 已补单测 |
| `V2ProductPriceLevelController` | 新版已做 | 旧版无该接口 | 暴露 `/v2/product-price-levels/*` | 已新增 controller | snake_case 契约已锁定 |
| `V2ProductSupplierRelationController` | 新版已做 | 旧版无该接口 | 暴露 `/v2/product-supplier-relations/*` | 已新增 controller | 按 `product_id` 列表查询 |
| `V2ProductService` 扩域读写 | 新版已做 | 旧版商品读模型较薄 | 让 `/v2/products` 返回多价格、默认供应商、供应关系列表，并支持回写 | 已升级 service 与 `V2ProductDtos` | `/v1/products` 继续冻结 |
| 第三阶段测试补齐 | 新版已做 | 新扩域代码刚落地时仍缺第三阶段回归证据 | 为迁移、service、controller、`/v1` 兼容提供测试护栏 | 已新增 `V9ProductPriceLevelAndSupplierRelationSqlTest`、`V2ProductPriceLevelServiceTest`、`V2ProductSupplierRelationServiceTest`，并升级 `V2ProductControllerTest` 与 `V1CatalogCompatibilityControllerTest` | 当前已通过本地 JDK 21 全量测试 |
| 商品域与安卓规划文档同步 | 新版已做 | 文档仍停留在第二阶段口径 | 把多价格与供应关系写入 spec、server technical-analysis、android planning | 已同步 `20-product-domain`、`30-api-contracts`、`31-android-impact`、`32-rollout-and-compatibility` 以及 product 相关技术分析文档 | 本阶段不改安卓 UI 代码 |

## 2026-06-01

### 文档结构重建

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `docs/technical-analysis/android` | 新版已做 | 目录存在但说明不完整 | 与 `master-goods-android` 目录一一对应 | 已补齐目录级 README | 统一接入六态状态表 |
| `docs/technical-analysis/server` | 新版已做 | 目录存在但与真实服务端结构有偏差 | 对齐 `src/main/java/com/zhihuiji/backend` 与 `src/main/resources` | 已补齐目录级 README | 重点服务于后端先行重构 |
| 旧问题式 README | 新版需要去掉 | 大量文档仍按历史缺陷罗列 | 改成“当前实现 + spec 差距 + 下一阶段动作” | 已完成首轮切换 | 后续逐步补字段矩阵 |
| 会员体系 | 新版需要去掉 | 旧版可能存在会员扩展空间 | 当前新版不纳入范围 | 已在相关文档统一标记 | 后续若恢复需单独立项 |

### 安卓文档同步到新版规划

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `docs/spec/31-android-impact.md` | 新版已做 | 只有极简占位表 | 改成 Android `/v2`、owner、扩域影响总表 | 已重写为新版迁移规范 | 不涉及 UI 视觉细节 |
| `docs/technical-analysis/android/README.md` | 新版已做 | 仍偏首版说明 | 改成 Android 新版迁移总览 | 已同步到后端 entity/dto 方向 | 作为安卓文档入口 |
| `app/core/data/feature` 总文档 | 新版已做 | 仍偏“页面已做完”口径 | 改成职责、迁移、owner、导入视角 | 已统一重写 | 更利于后续重构 |
| Android 核心子模块 README | 新版已做 | 多数仍以 `/v1` 首版闭环为核心 | 改成面向 `/v2` 和扩域能力的规划文档 | 已同步 model/network/database/datastore/auth/order/sync 等关键模块 | 后续继续字段级深化 |
| Android 业务域子模块 README | 新版已做 | 更强调页面是否能跑通 | 改成领域职责与场景拆分说明 | 已同步商品、档案、销售、采购、付款、财务、报表、助手、设置等模块 | 本轮不改代码 |

### 后续维护规则

1. 先更新 `docs/spec/`
2. 再更新对应 `docs/technical-analysis/*/README.md`
3. 仅在文档结构或状态发生变化时更新本文件
