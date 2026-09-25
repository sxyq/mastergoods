package com.zhihuiji.core.database

/**
 * 旧业务表已退出 Room schema，但历史设备库中仍可能存在；
 * 注销/访问撤销清理时按表名清空，避免残留本地业务数据。
 */
private val LEGACY_BUSINESS_TABLES = listOf(
    "products",
    "customers",
    "suppliers",
    "sale_orders",
    "sale_order_items",
    "purchase_orders",
    "pay_orders",
    "finance_records",
    "products_v2",
    "customers_v2",
    "suppliers_v2",
    "sale_orders_v2",
    "purchase_orders_v2",
    "pay_orders_v2",
    "accounts_v2",
    "finance_records_v2",
    "inventory_ledger_v2",
    "inventory_snapshots_v2",
    "dashboard_snapshots",
)

fun ZhihuijiDatabase.clearLegacyBusinessTables() {
    val db = openHelper.writableDatabase
    LEGACY_BUSINESS_TABLES.forEach { table ->
        db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(table),
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                db.execSQL("DELETE FROM `$table`")
            }
        }
    }
}
