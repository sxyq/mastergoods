package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "finance_records_v2",
    indices = [Index(name = "idx_finance_records_v2_owner_record", value = ["ownerUserId", "recordId"], unique = true)],
)
data class FinanceRecordV2Entity(
    @PrimaryKey val recordId: Long,
    val ownerUserId: Long,
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
