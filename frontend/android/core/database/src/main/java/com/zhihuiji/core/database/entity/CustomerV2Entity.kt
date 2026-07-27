package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers_v2",
    indices = [Index(name = "idx_customers_v2_owner_customer", value = ["ownerUserId", "customerId"], unique = true)],
)
data class CustomerV2Entity(
    @PrimaryKey val customerId: Long,
    val ownerUserId: Long,
    val name: String,
    val phone: String,
    val level: Int,
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
