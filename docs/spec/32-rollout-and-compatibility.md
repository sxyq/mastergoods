# 32 发布与兼容

## 规则

| 项目 | 状态 | 说明 |
|---|---|---|
| /v1 兼容 | 新版已做 | 先保留 |
| /v2 单据域首批 | 新版已做 | 销售/采购/付款三条首批路由已建立 |
| /v2 商品与伙伴域首批 | 新版已做 | 商品、分类、单位、客户、供应商、分组、联系人首批路由已建立 |
| /v2 商品域第三阶段扩域 | 新版已做 | 已建立价格层级、商品-供应商关系两组路由，并升级 `/v2/products` 读写模型 |
| 数据迁移 | 新版待做 | 增量式演进 |
| 旧数据导入到服务器 | 新版待做 | 不纳入第一阶段 |
| 安卓新版 UI 切换 | 待验证 | 等 `/v2` 稳定后再做 |
| 本地 Java 21 编译验证 | 新版已做 | 之前缺匹配的 JDK 21，当前第三阶段代码已通过 `compileJava` 与 `compileTestJava` |
| 第二阶段 `/v2` 商品与伙伴测试补齐 | 新版已做 | 新扩域代码刚落地时仍缺迁移、service、controller、`/v1` 兼容回归验证；现已补齐 migration/service/controller/compatibility 测试，并修正联系人主摘要测试桩顺序，当前以 JDK 21 全量测试通过为验收基线 |
| 第三阶段商品域测试补齐 | 新版已做 | 已补 `V9` migration、`V2ProductPriceLevelService`、`V2ProductSupplierRelationService`、`V2ProductController`、`/v1` 兼容断言的测试代码 | 当前已通过本地 JDK 21 `gradlew test` |
