# Master-Goods Full Coverage Test Plan Index

## Scope

This directory contains execution-oriented plans and machine-trackable ledgers for every platform:

- Backend
- Android
- iOS
- Web
- Agent

Each platform now includes four lanes:

- Unit tests
- Functional tests
- Performance tests
- Audit

## Ledger Files

- `单元测试/unit_function_coverage.csv`: 函数级单元测试覆盖台账
- `功能测试/functional_feature_matrix.csv`: 对照源码建立的功能测试台账
- `性能测试/performance_scope_matrix.csv`: 需要建立基线的性能测试台账
- `审计/audit_function_ledger.csv`: 安全/性能/复用/简化四维审计台账

## Usage

1. 所有台账默认以 `未测试` / `未审计` 初始化。
2. 每执行一个测试或完成一次审计，就在对应 CSV 中标记状态并补证据路径。
3. 每个子目录 `scripts/refresh_tables.sh` 可按端重新刷新首版台账。
4. 每个子目录还预置了对应的执行脚本占位，后续可直接补充真实命令。
