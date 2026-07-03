package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts_v2",
    indices = [Index(name = "idx_accounts_v2_owner_account", value = ["ownerUserId", "accountId"], unique = true)],
)
data class AccountV2Entity(
    @PrimaryKey val accountId: Long,
    val ownerUserId: Long,
    val code: String,
    val name: String,
    val type: Int,
    val balance: Double,
    val isDefault: Boolean,
    val status: Int,
    val sortOrder: Int,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
