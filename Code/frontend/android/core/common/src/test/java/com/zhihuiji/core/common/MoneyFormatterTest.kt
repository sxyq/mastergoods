package com.zhihuiji.core.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class MoneyFormatterTest {
    @Test
    fun format_handlesNullAndThousands() {
        assertEquals("¥0.00", MoneyFormatter.format(null as BigDecimal?))
        assertEquals("¥12,345.68", MoneyFormatter.format(BigDecimal("12345.678")))
    }

    @Test
    fun formatSigned_keepsPositiveAndNegativeSigns() {
        assertEquals("¥+98.50", MoneyFormatter.formatSigned(98.5))
        assertEquals("¥-20.00", MoneyFormatter.formatSigned(-20.0))
    }
}
