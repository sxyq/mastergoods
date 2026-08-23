# P1 付款幂等与分页下推证据 — 2026-08-23

- 代理：backend-payments（子代理编码）+ team-lead（主控修复回归/验证/提交）
- 范围：计划阶段 3 + P1 付款幂等

## backend-payments 产出（子代理）
- 付款幂等：V2PayOrderServiceTest 新增幂等键场景测试（顺序重复/并发/不同 owner/不同 payload 等，+99 行）。
- 分页下推：9 个 Repository 新增过滤/分页方法（findReceivablesByOwnerUserIdAndFilters、findPayablesByOwnerUserIdAndFilters、findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc、findByOwnerUserIdAndFilters(InventoryLedger/InventorySnapshot)、findByOwnerUserIdAndOrderIdIn、findAllByOwnerUserIdAndStatusOrderByUpdatedAtDesc、findByOwnerUserIdAndCreatedAtBetween、purchaseSummaryAggregate、findByOwnerUserIdAndOriginalOrderIdInOrderByCreatedAtDesc、findAllByOwnerUserIdAndProductIdOrderByIsDefaultDescPurchasePriorityAscCreatedAtAsc 等）。
- 10 个 readonly 工具改用新分页方法（CustomerReceivable/SupplierPayable/ProductCatalog/CustomerProfile/InventoryLedger/InventorySnapshot/Payment/ImportJob/CrossAnalysis/ProductSupplierRelation），消灭 N+1（CustomerProfile 批量加载 payments/returns）。
- V2ProductDtos 长字段相关调整。
- 新增测试：V2PayOrderServiceTest 幂等场景；修改 ProductCatalogLookupToolTest/CustomerProfileLookupToolTest。

## 主控修复（子代理分页改动引起的测试回归）
backend-payments 把工具调用切换到新 Repository 签名后，V2AgentAiServiceTest 15 个测试桩/verify 不匹配。主控修复：
1. 44 处桩：findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc/findPayablesByOwnerUserIdAndFilters、findAllByOwnerUserIdOrderByNameAsc → findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc（any() 匹配）。
2. 5 处 verify：旧方法 → 新方法（含 times(1)/times(2) 变体）。
3. inventoryPanorama 测试恢复旧桩（InventoryPanoramaLookupTool 仍调 findAllByOwnerUserIdOrderByNameAsc）。
4. customerProfile 测试：payment/return 桩改为批量方法 findByOwnerUserIdAndOrderIdIn/findByOwnerUserIdAndOriginalOrderIdInOrderByCreatedAtDesc；saleOrderRepository.search 桩补 Pageable 参数（工具新增分页参数）。

## 验证（主控）
- `./Code/backend/gradlew -p Code/backend test --offline` → **566 tests completed, 1 failed**（唯一失败 V2BillDomainControllerTest.purchaseReceiptListReturnsSnakeCaseFields 为既有失败，已在本轮开始前于 HEAD(f5f6efe9) worktree 确认同样失败，与本轮无关，未触碰）。
- V2AgentAiServiceTest 75/75 通过；V2PayOrderServiceTest、各工具测试通过。

## Blocked / Deferred
- PostgreSQL EXPLAIN / EXPLAIN ANALYZE：**Deferred**（无 PostgreSQL 环境；H2 仅验证查询语义；分页 SQL 已在 Repository @Query 中下推）。
- 真实并发幂等（多请求同时打同一 key）：**Deferred**（唯一约束 + 事务处理已由 V30/V31 迁移与 service 实现，真实并发需部署环境）。
- /v2/payments 404 排查：由子代理确认路由（结果以子代理汇报为准，主控抽查）。
