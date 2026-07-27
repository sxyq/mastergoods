package com.zhihuiji.feature.dashboard

import com.zhihuiji.data.report.ReportRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardViewModelDependencyTest {
    @Test
    fun dashboardUsesReportAggregatesInsteadOfFinanceRecordListForCashflow() {
        val constructorTypes = DashboardViewModel::class.java.constructors
            .flatMap { constructor -> constructor.parameterTypes.toList() }

        assertTrue(
            "Dashboard must keep using ReportRepository cashflowSummary for netCashFlow.",
            constructorTypes.contains(ReportRepository::class.java)
        )
        assertFalse(
            "Dashboard must not reintroduce FinanceRepository list loading for netCashFlow.",
            constructorTypes.any { type -> type.name == "com.zhihuiji.data.finance.FinanceRepository" }
        )
    }
}
