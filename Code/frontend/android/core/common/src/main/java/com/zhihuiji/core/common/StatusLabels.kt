package com.zhihuiji.core.common

/**
 * 状态码 → 展示标签的 lookup 集合。
 *
 * REWRITE-CLEAN-SLATE-001B 清场：旧领域（销售/采购/收付款/财务/库存/客户/供应商）
 * 的状态码表已全部清空，仅保留 mapOf lookup 机制，表内容随新领域接入时补充。
 * 未知或未补充的 code 一律返回「未知」。
 */
object StatusLabels {
    private val SALE_ORDER_STATUS: Map<Int, String> = emptyMap()
    fun saleOrderStatus(code: Int): String = SALE_ORDER_STATUS[code] ?: "未知"

    private val PURCHASE_ORDER_STATUS: Map<Int, String> = emptyMap()
    fun purchaseOrderStatus(code: Int): String = PURCHASE_ORDER_STATUS[code] ?: "未知"

    private val PAY_ORDER_STATUS: Map<Int, String> = emptyMap()
    fun payOrderStatus(code: Int): String = PAY_ORDER_STATUS[code] ?: "未知"

    private val FINANCE_TYPES: Map<Int, String> = emptyMap()
    fun financeType(code: Int): String = FINANCE_TYPES[code] ?: "未知"

    private val SUPPLIER_STATUS: Map<Int, String> = emptyMap()
    fun supplierStatus(code: Int): String = SUPPLIER_STATUS[code] ?: "未知"

    private val CUSTOMER_STATUS: Map<Int, String> = emptyMap()
    fun customerStatus(code: Int): String = CUSTOMER_STATUS[code] ?: "未知"

    private val PRODUCT_STATUS: Map<Int, String> = emptyMap()
    fun productStatus(code: Int): String = PRODUCT_STATUS[code] ?: "未知"

    private val CUSTOMER_LEVELS: Map<Int, String> = emptyMap()
    fun customerLevel(code: Int): String = CUSTOMER_LEVELS[code] ?: "未知"

    private val PAYMENT_METHODS: Map<Int, String> = emptyMap()
    fun paymentMethod(code: Int): String = PAYMENT_METHODS[code] ?: "未知"

    private val PAYMENT_TYPES: Map<Int, String> = emptyMap()
    fun paymentType(code: Int): String = PAYMENT_TYPES[code] ?: "未知"

    private val INVENTORY_FLOW_TYPES: Map<Int, String> = emptyMap()
    fun inventoryFlowType(code: Int): String = INVENTORY_FLOW_TYPES[code] ?: "未知"

    private val SALES_RETURN_STATUS: Map<Int, String> = emptyMap()
    fun salesReturnStatusLabel(code: Int): String = SALES_RETURN_STATUS[code] ?: "未知"
}
