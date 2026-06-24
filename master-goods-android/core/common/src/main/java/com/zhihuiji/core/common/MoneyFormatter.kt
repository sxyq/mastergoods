package com.zhihuiji.core.common

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MoneyFormatter {
    private val formatter = ThreadLocal.withInitial<DecimalFormat> {
        DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.CHINA))
    }

    fun format(amount: BigDecimal?): String {
        return "¥${formatValue(amount)}"
    }

    fun format(amount: Double?): String {
        return "¥${formatValue(amount)}"
    }

    fun formatWithoutSymbol(amount: BigDecimal?): String {
        return formatValue(amount)
    }

    fun formatWithoutSymbol(amount: Double?): String {
        return formatValue(amount)
    }

    fun formatSigned(amount: Double?): String {
        if (amount == null) return "¥0.00"
        val prefix = if (amount >= 0) "+" else ""
        return "¥$prefix${formatValue(amount)}"
    }

    private fun formatValue(amount: BigDecimal?): String {
        if (amount == null) return "0.00"
        return decimalFormat().format(amount)
    }

    private fun formatValue(amount: Double?): String {
        if (amount == null) return "0.00"
        return decimalFormat().format(amount)
    }

    private fun decimalFormat(): DecimalFormat {
        return formatter.get()
    }
}
