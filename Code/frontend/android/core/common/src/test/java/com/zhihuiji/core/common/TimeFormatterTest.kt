package com.zhihuiji.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeFormatterTest {
    @Test
    fun formattersReturnDashForMissingOrZeroTimestamp() {
        assertEquals("-", TimeFormatter.formatDate(null))
        assertEquals("-", TimeFormatter.formatDateTime(0L))
        assertEquals("-", TimeFormatter.formatTime(0L))
    }

    @Test
    fun formattersProduceExpectedDateAndTimeShapes() {
        val timestamp = 1_722_470_400_000L

        assertTrue(TimeFormatter.formatDate(timestamp).matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        assertTrue(TimeFormatter.formatDateTime(timestamp).matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")))
        assertTrue(TimeFormatter.formatTime(timestamp).matches(Regex("\\d{2}:\\d{2}")))
    }
}
