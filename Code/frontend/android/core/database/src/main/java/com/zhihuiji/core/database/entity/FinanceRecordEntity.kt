package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "finance_records")
data class FinanceRecordEntity(
    @PrimaryKey val id: Long,
    val recordNo: String,
    val type: Int,
    val category: String,
    val partnerName: String?,
    val amount: Double,
    val method: Int,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
