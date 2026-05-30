package com.zhihuiji.core.common

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MoneyFormatter {
    private fun createFormatter(): DecimalFormat =
        DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.CHINA))

    fun format(amount: BigDecimal?): String {
        if (amount == null) return "¥0.00"
        return "¥${createFormatter().format(amount)}"
    }

    fun format(amount: Double?): String {
        if (amount == null) return "¥0.00"
        return "¥${createFormatter().format(amount)}"
    }

    fun formatWithoutSymbol(amount: BigDecimal?): String {
        if (amount == null) return "0.00"
        return createFormatter().format(amount)
    }

    fun formatWithoutSymbol(amount: Double?): String {
        if (amount == null) return "0.00"
        return createFormatter().format(amount)
    }

    fun formatSigned(amount: Double?): String {
        if (amount == null) return "¥0.00"
        val prefix = if (amount >= 0) "+" else ""
        return "¥$prefix${createFormatter().format(amount)}"
    }
}
