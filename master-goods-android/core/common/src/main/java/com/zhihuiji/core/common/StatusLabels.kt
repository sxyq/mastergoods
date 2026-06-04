package com.zhihuiji.core.common

import com.zhihuiji.core.model.AgentTaskStatus

object StatusLabels {
    object Codes {
        const val SALE_DRAFT = 0
        const val SALE_COMPLETED = 1
        const val SALE_CANCELLED = 2
        const val SALE_CONFIRMED = 3
        const val PURCHASE_DRAFT = 0
        const val PURCHASE_RECEIVED = 1
        const val PAY_PENDING = 0
        const val PAY_PAID = 1
        const val PAY_CANCELLED = 2
        const val FINANCE_INCOME = 1
        const val FINANCE_EXPENSE = 2
        const val ENTITY_ACTIVE = 1
        const val ENTITY_DISABLED = 0
        const val CUSTOMER_STATUS_DISABLED = 0
        const val CUSTOMER_STATUS_ACTIVE = 1
        const val CUSTOMER_NORMAL = 0
        const val CUSTOMER_VIP = 1
        const val CUSTOMER_SVIP = 2
        const val METHOD_CASH = 1
        const val METHOD_WECHAT = 2
        const val METHOD_ALIPAY = 3
        const val METHOD_BANK = 4
        const val METHOD_OTHER = 5
        const val PAYMENT_COLLECT = 1
        const val PAYMENT_REFUND = 2
        const val INVENTORY_OUT = 0
        const val INVENTORY_IN = 1
    }

    fun saleOrderStatus(code: Int): String = when (code) {
        Codes.SALE_DRAFT -> "草稿"
        Codes.SALE_COMPLETED -> "已完成"
        Codes.SALE_CANCELLED -> "已取消"
        Codes.SALE_CONFIRMED -> "已确认"
        else -> "未知"
    }

    fun purchaseOrderStatus(code: Int): String = when (code) {
        Codes.PURCHASE_DRAFT -> "草稿"
        Codes.PURCHASE_RECEIVED -> "已收货"
        else -> "未知"
    }

    fun payOrderStatus(code: Int): String = when (code) {
        Codes.PAY_PENDING -> "待付款"
        Codes.PAY_PAID -> "已付款"
        Codes.PAY_CANCELLED -> "已取消"
        else -> "未知"
    }

    fun financeType(code: Int): String = when (code) {
        Codes.FINANCE_INCOME -> "收入"
        Codes.FINANCE_EXPENSE -> "支出"
        else -> "未知"
    }

    fun supplierStatus(code: Int): String = when (code) {
        Codes.ENTITY_ACTIVE -> "启用"
        Codes.ENTITY_DISABLED -> "停用"
        else -> "未知"
    }

    fun customerStatus(code: Int): String = when (code) {
        Codes.CUSTOMER_STATUS_ACTIVE -> "正常"
        Codes.CUSTOMER_STATUS_DISABLED -> "已停用"
        else -> "未知"
    }

    fun customerListStatus(code: Int, balance: Double): String = when {
        code == Codes.CUSTOMER_STATUS_DISABLED -> "已停用"
        balance > EPSILON -> "欠款"
        code == Codes.CUSTOMER_STATUS_ACTIVE -> "正常"
        else -> "未知"
    }

    fun productStatus(code: Int): String = when (code) {
        Codes.ENTITY_ACTIVE -> "正常"
        Codes.ENTITY_DISABLED -> "停用"
        else -> "未知"
    }

    fun customerLevel(code: Int): String = when (code) {
        Codes.CUSTOMER_NORMAL -> "普通"
        Codes.CUSTOMER_VIP -> "VIP"
        Codes.CUSTOMER_SVIP -> "SVIP"
        else -> "未知"
    }

    fun paymentMethod(code: Int): String = when (code) {
        Codes.METHOD_CASH -> "现金"
        Codes.METHOD_WECHAT -> "微信"
        Codes.METHOD_ALIPAY -> "支付宝"
        Codes.METHOD_BANK -> "银行卡"
        Codes.METHOD_OTHER -> "其他"
        else -> "未知"
    }

    fun paymentType(code: Int): String = when (code) {
        Codes.PAYMENT_COLLECT -> "收款"
        Codes.PAYMENT_REFUND -> "退款"
        else -> "未知"
    }

    fun inventoryFlowType(code: Int): String = when (code) {
        Codes.INVENTORY_OUT -> "出库"
        Codes.INVENTORY_IN -> "入库"
        else -> "未知"
    }

    fun agentTaskStatus(status: AgentTaskStatus): String = when (status) {
        AgentTaskStatus.QUEUED -> "排队中"
        AgentTaskStatus.RUNNING -> "运行中"
        AgentTaskStatus.COMPLETED -> "已完成"
        AgentTaskStatus.FAILED -> "失败"
    }

    fun stockStatus(stock: Double, safeStock: Double): String = when {
        stock <= EPSILON -> "缺货"
        stock < safeStock - EPSILON -> "低库存"
        else -> "正常"
    }

    private const val EPSILON = 1e-10
}
