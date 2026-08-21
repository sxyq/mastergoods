package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val phone: String,
    val groupId: Long?,
    val primaryContactName: String?,
    val primaryContactPhone: String?,
    val address: String?,
    val notes: String?,
    val balance: Double,
    val status: Int,
    val syncStatus: Int?,
    val syncVersion: Long?,
    val createdAt: Long?,
    val updatedAt: Long?,
)
