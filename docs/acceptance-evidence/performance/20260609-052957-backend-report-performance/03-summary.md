# Backend Report Performance Summary

| Endpoint | Requests | HTTP OK | Logical OK | p50 ms | p95 ms | max ms | mean ms |
|---|---:|---:|---:|---:|---:|---:|---:|
| dashboard_cashflow_summary | 5 | 5 | 5 | 1.66 | 1.87 | 1.87 | 1.65 |
| finance_records_page_for_cashflow_reconcile | 5 | 5 | 5 | 1.79 | 2.01 | 2.01 | 1.78 |
| report_profit_summary | 5 | 5 | 5 | 1.28 | 1.29 | 1.29 | 1.24 |
| report_stock_out_records | 5 | 5 | 5 | 1.44 | 1.68 | 1.68 | 1.46 |
| report_inventory_flow | 5 | 5 | 5 | 2.14 | 2.65 | 2.65 | 2.2 |
| v2_sale_orders_page | 5 | 5 | 5 | 2.58 | 4.06 | 4.06 | 2.86 |
| v2_sale_orders_filtered_page | 5 | 5 | 5 | 1.9 | 2.47 | 2.47 | 1.96 |

Captured endpoints map to the current performance-debt ledger:

- `dashboard_cashflow_summary`: Dashboard net cashflow backend aggregation.
- `finance_records_page_for_cashflow_reconcile`: finance-record page used as a cashflow reconciliation anchor.
- `report_profit_summary`: report profit scalar aggregation.
- `report_stock_out_records`: stock-out paged item/order query.
- `report_inventory_flow`: three-source inventory flow candidate query.
- `v2_sale_orders_page`: V2 sale order repository pagination plus batched items.
- `v2_sale_orders_filtered_page`: V2 sale order filtered repository pagination path.
