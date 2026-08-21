package com.zhihuiji.feature.dashboard

import com.zhihuiji.core.model.SalesTrendPointReportDto
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardTrendAggregationTest {
    @Test
    fun dateAggregationCombinesBucketsInOnePass() {
        val day = LocalDate.of(2026, 8, 1)
        val nextDay = day.plusDays(1)
        val points = listOf(
            point(day, 10.0),
            point(day, 2.5),
            point(nextDay, 7.0),
        )

        assertEquals(mapOf(day to 12.5, nextDay to 7.0), aggregateSalesTrendByDate(points))
    }

    @Test
    fun slotAggregationCombinesSixHourBuckets() {
        val day = LocalDate.of(2026, 8, 1)
        val points = listOf(
            point(day, 1, 3.0),
            point(day, 5, 2.0),
            point(day, 6, 4.0),
            point(day, 23, 8.0),
        )

        val amounts = aggregateSalesTrendBySlot(points)
        assertEquals(5.0, amounts[0] ?: 0.0, 0.0)
        assertEquals(4.0, amounts[1] ?: 0.0, 0.0)
        assertEquals(8.0, amounts[3] ?: 0.0, 0.0)
    }

    private fun point(day: LocalDate, amount: Double): SalesTrendPointReportDto =
        point(day, 0, amount)

    private fun point(day: LocalDate, hour: Int, amount: Double): SalesTrendPointReportDto {
        val start = day.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return SalesTrendPointReportDto(startAt = start, endAt = start, totalSalesAmount = amount)
    }
}
