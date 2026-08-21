package com.zhihuiji.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerDto(
    val id: Long? = null,
    val name: String = "",
    val phone: String = "",
    val level: Int = 0,
    val address: String? = null,
    val notes: String? = null,
    val balance: Double = 0.0,
    val status: Int = 1,
    @SerialName("sync_status") val syncStatus: Int? = null,
    @SerialName("sync_version") val syncVersion: Long? = null,
    @SerialName("created_at") val createdAt: Long? = null,
    @SerialName("updated_at") val updatedAt: Long? = null,
)

@Serializable
data class SupplierDto(
    val id: Long? = null,
    val name: String = "",
    val phone: String = "",
    val address: String? = null,
    val notes: String? = null,
    val balance: Double = 0.0,
    val status: Int = 1,
    @SerialName("sync_status") val syncStatus: Int? = null,
    @SerialName("sync_version") val syncVersion: Long? = null,
    @SerialName("created_at") val createdAt: Long? = null,
    @SerialName("updated_at") val updatedAt: Long? = null,
)

@Serializable
data class CreateCustomerRequest(
    val name: String = "",
    val phone: String = "",
    val level: Int = 0,
    val address: String? = null,
    val notes: String? = null,
    val status: Int = 1,
)

@Serializable
data class UpdateCustomerRequest(
    val name: String = "",
    val phone: String = "",
    val level: Int = 0,
    val address: String? = null,
    val notes: String? = null,
    val status: Int = 1,
)

@Serializable
data class CreateSupplierRequest(
    val name: String = "",
    val phone: String = "",
    val address: String? = null,
    val notes: String? = null,
    val status: Int = 1,
)

@Serializable
data class UpdateSupplierRequest(
    val name: String = "",
    val phone: String = "",
    val address: String? = null,
    val notes: String? = null,
    val status: Int = 1,
)
