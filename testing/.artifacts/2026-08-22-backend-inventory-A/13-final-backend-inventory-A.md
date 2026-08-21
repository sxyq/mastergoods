# Backend Inventory A - 2026-08-22

## Current execution

- xml_files=95 test_classes=95 testcases=485 tests=485 failures=0 errors=0 skipped=0 total_time_seconds=6.652
- Gradle: `./Code/backend/gradlew -p Code/backend --rerun-tasks test`; exit `0`; 6 tasks executed; JaCoCo task executed.
- Test source/XML: 95 Java test files; 95 XML suites; 485 testcases; the parser-only declaration difference is the plural application test class.
- Controller/Service/Repository target files: 42 / 51 / 52.

## Ledger counts

- `testing/后端/测试分类总台账.csv`: rows=76; `status`={'失败': 3, 'Passed': 6, 'Blocked': 13, '部分完成': 4, '规划中': 16, '通过': 34}.
- `testing/后端/单元测试/live_execution_ledger.csv`: rows=28; `result`={'通过': 20, 'Passed': 8}.
- `testing/后端/功能测试/live_execution_ledger.csv`: rows=142; `result`={'部分完成': 3, 'Passed': 80, '解除条件:提供同店第二个有效成员或批准创建并清理临时成员夹具': 1, 'provider key was transient and absent from evidence.': 1, 'real cloned database; production container unchanged.': 1, 'model called visualization only after real sales tool.': 1, 'provider work cancelled before tool execution.': 1, 'media_upload fixture created no business row.': 1, 'Blocked': 17, '失败': 4, '通过': 21, 'Production model channel did not produce a grounded formal LLM answer; real tool data was returned and no production business row was changed.': 1, '': 3, 'real 154 execution; no schema or Java change': 1, 'Failed': 2, 'Deferred': 4}.
- `testing/后端/性能测试/live_execution_ledger.csv`: rows=33; `result`={'Failed': 1, '部分完成': 6, 'Passed': 7, 'Blocked': 11, '通过': 3, 'total_ms=13710; first_token=not_collected; token_usage=not_collected; cache_hit=not_reported': 1, 'p50=12900ms;p95=67596ms;single serial sample; no concurrency or JVM profile': 1, 'Observed': 2, 'Deferred': 1}.
- `testing/后端/单元测试/unit_function_coverage.csv`: rows=3352; `result`={'': 3305, '通过': 34, 'Passed': 12, 'Blocked': 1}.
- `testing/后端/审计/audit_function_ledger.csv`: rows=3357; `audit_status`={'已审计': 27, '未审计': 3317, '部分审计': 10, '需确认': 1, '失败': 1, '需修复': 1}.

## Remaining tests

- Deferred: real request-level PayOrder idempotency concurrency; current live cross-owner/cross-store HTTP; live HTTP pagination; end-to-end SQLite import; pagination load/P50/P95.
- Blocked: current PostgreSQL EXPLAIN/EXPLAIN ANALYZE evidence; Agent `requiredPermission` caller-level enforcement and positive/negative tests.
- Historical conflicts: old 462-count record and mixed legacy result values remain historical; current forced run is 485.

## Evidence

- Artifact directory: `/Users/sunyiyang/Desktop/Project/master-goods/testing/.artifacts/2026-08-22-backend-inventory-A`
- New evidence contains no passwords, tokens, cookies, full authentication payloads, or keys.
