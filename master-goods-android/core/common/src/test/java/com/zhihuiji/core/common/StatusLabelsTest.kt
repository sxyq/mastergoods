package com.zhihuiji.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class StatusLabelsTest {
    @Test
    fun statusLabels_coverPrimaryBusinessStates() {
        assertEquals("草稿", StatusLabels.saleOrderStatus(0))
        assertEquals("已完成", StatusLabels.saleOrderStatus(1))
        assertEquals("待付款", StatusLabels.payOrderStatus(0))
        assertEquals("已付款", StatusLabels.payOrderStatus(1))
        assertEquals("收入", StatusLabels.financeType(1))
        assertEquals("支出", StatusLabels.financeType(2))
    }

    @Test
    fun stockStatus_detectsOutAndLowStock() {
        assertEquals("缺货", StatusLabels.stockStatus(0.0, 5.0))
        assertEquals("低库存", StatusLabels.stockStatus(4.0, 5.0))
        assertEquals("正常", StatusLabels.stockStatus(8.0, 5.0))
    }
}
