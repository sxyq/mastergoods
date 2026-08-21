package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Last complete dashboard response for one authenticated local account and sales scope.
 *
 * It is a derived cache only. Source business records remain in their own Room tables and
 * failed network refreshes must never replace this snapshot with synthetic zero values.
 */
@Entity(tableName = "dashboard_snapshots")
data class DashboardSnapshotEntity(
    @PrimaryKey val scopeKey: String,
    val salesAmount: Double,
    val salesOrderCount: Int,
    val receivableAmount: Double,
    val receivableCustomerCount: Int,
    val netCashFlow: Double,
    val salesTrendJson: String,
    val updatedAt: Long,
)
