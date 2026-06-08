# Backend Report Performance Summary

| Endpoint | Requests | HTTP OK | Logical OK | p50 ms | p95 ms | max ms | mean ms |
|---|---:|---:|---:|---:|---:|---:|---:|
| dashboard_cashflow_summary | 5 | 5 | 5 | 2.68 | 3.16 | 3.16 | 2.72 |
| finance_records_page_for_cashflow_reconcile | 5 | 5 | 5 | 3.68 | 5.13 | 5.13 | 3.79 |
| report_profit_summary | 5 | 5 | 5 | 2.68 | 3.11 | 3.11 | 2.73 |
| report_stock_out_records | 5 | 5 | 5 | 3.13 | 4.53 | 4.53 | 3.33 |
| report_inventory_flow | 5 | 5 | 5 | 4.7 | 6.55 | 6.55 | 5.23 |
| v2_sale_orders_page | 5 | 5 | 5 | 4.17 | 5.56 | 5.56 | 4.21 |
| v2_sale_orders_filtered_page | 5 | 5 | 5 | 2.26 | 2.44 | 2.44 | 2.17 |

Captured endpoints map to the current performance-debt ledger:

- `dashboard_cashflow_summary`: Dashboard net cashflow backend aggregation.
- `finance_records_page_for_cashflow_reconcile`: finance-record page used as a cashflow reconciliation anchor.
- `report_profit_summary`: report profit scalar aggregation.
- `report_stock_out_records`: stock-out paged item/order query.
- `report_inventory_flow`: three-source inventory flow candidate query.
- `v2_sale_orders_page`: V2 sale order repository pagination plus batched items.
- `v2_sale_orders_filtered_page`: V2 sale order filtered repository pagination path.
