package com.zhihuiji.core.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormatter {
    private val dateFormatter = ThreadLocal.withInitial<SimpleDateFormat> {
        SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    }
    private val dateTimeFormatter = ThreadLocal.withInitial<SimpleDateFormat> {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    }
    private val timeFormatter = ThreadLocal.withInitial<SimpleDateFormat> {
        SimpleDateFormat("HH:mm", Locale.CHINA)
    }

    fun formatDate(epochMillis: Long?): String {
        return formatOrDash(epochMillis, dateFormatter)
    }

    fun formatDateTime(epochMillis: Long?): String {
        return formatOrDash(epochMillis, dateTimeFormatter)
    }

    fun formatTime(epochMillis: Long?): String {
        return formatOrDash(epochMillis, timeFormatter)
    }

    private fun formatOrDash(epochMillis: Long?, formatter: ThreadLocal<SimpleDateFormat>): String {
        if (epochMillis == null || epochMillis == 0L) return "-"
        val localFormatter = formatter.get()
        return localFormatter.format(Date(epochMillis))
    }
}
