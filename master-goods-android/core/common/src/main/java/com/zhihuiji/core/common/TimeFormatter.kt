package com.zhihuiji.core.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormatter {
    private fun createDateFormatter(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private fun createDateTimeFormatter(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    private fun createTimeFormatter(): SimpleDateFormat = SimpleDateFormat("HH:mm", Locale.CHINA)

    fun formatDate(epochMillis: Long?): String {
        if (epochMillis == null || epochMillis == 0L) return "-"
        return createDateFormatter().format(Date(epochMillis))
    }

    fun formatDateTime(epochMillis: Long?): String {
        if (epochMillis == null || epochMillis == 0L) return "-"
        return createDateTimeFormatter().format(Date(epochMillis))
    }

    fun formatTime(epochMillis: Long?): String {
        if (epochMillis == null || epochMillis == 0L) return "-"
        return createTimeFormatter().format(Date(epochMillis))
    }
}
