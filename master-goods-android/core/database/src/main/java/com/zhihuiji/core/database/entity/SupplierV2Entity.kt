package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "suppliers_v2",
    indices = [Index(name = "idx_suppliers_v2_owner_supplier", value = ["ownerUserId", "supplierId"], unique = true)],
)
data class SupplierV2Entity(
    @PrimaryKey val supplierId: Long,
    val ownerUserId: Long,
    val name: String,
    val phone: String,
    val groupId: Long?,
    val groupName: String?,
    val primaryContactName: String?,
    val primaryContactPhone: String?,
    val address: String?,
    val notes: String?,
    val balance: Double,
    val status: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
