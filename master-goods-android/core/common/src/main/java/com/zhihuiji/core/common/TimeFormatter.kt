package com.zhihuiji.core.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormatter {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.CHINA)

    fun formatDate(epochMillis: Long?): String {
        if (epochMillis == null || epochMillis == 0L) return "-"
        return dateFormatter.format(Date(epochMillis))
    }

    fun formatDateTime(epochMillis: Long?): String {
        if (epochMillis == null || epochMillis == 0L) return "-"
        return dateTimeFormatter.format(Date(epochMillis))
    }

    fun formatTime(epochMillis: Long?): String {
        if (epochMillis == null || epochMillis == 0L) return "-"
        return timeFormatter.format(Date(epochMillis))
    }
}
