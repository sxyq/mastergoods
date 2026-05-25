package com.zhihuiji.core.common

object StatusLabels {
    fun saleOrderStatus(code: Int): String = when (code) {
        0 -> "草稿"
        1 -> "已完成"
        2 -> "已取消"
        else -> "未知"
    }

    fun purchaseOrderStatus(code: Int): String = when (code) {
        0 -> "草稿"
        1 -> "已收货"
        else -> "未知"
    }

    fun payOrderStatus(code: Int): String = when (code) {
        0 -> "待付款"
        1 -> "已付款"
        2 -> "已取消"
        else -> "未知"
    }

    fun financeType(code: Int): String = when (code) {
        1 -> "收入"
        2 -> "支出"
        else -> "未知"
    }

    fun supplierStatus(code: Int): String = when (code) {
        1 -> "启用"
        0 -> "停用"
        else -> "未知"
    }

    fun productStatus(code: Int): String = when (code) {
        1 -> "正常"
        0 -> "停用"
        else -> "未知"
    }

    fun customerLevel(code: Int): String = when (code) {
        0 -> "普通"
        1 -> "VIP"
        2 -> "SVIP"
        else -> "未知"
    }

    fun paymentMethod(code: Int): String = when (code) {
        1 -> "现金"
        2 -> "微信"
        3 -> "支付宝"
        4 -> "银行卡"
        5 -> "其他"
        else -> "未知"
    }

    fun paymentType(code: Int): String = when (code) {
        1 -> "收款"
        2 -> "退款"
        else -> "未知"
    }

    fun inventoryFlowType(code: Int): String = when (code) {
        0 -> "出库"
        1 -> "入库"
        else -> "未知"
    }

    fun agentTaskStatus(status: String): String = when (status) {
        "queued" -> "排队中"
        "running" -> "运行中"
        "completed" -> "已完成"
        "failed" -> "失败"
        else -> "未知"
    }

    fun stockStatus(stock: Double, safeStock: Double): String = when {
        stock <= 0 -> "缺货"
        stock < safeStock -> "低库存"
        else -> "正常"
    }
}
