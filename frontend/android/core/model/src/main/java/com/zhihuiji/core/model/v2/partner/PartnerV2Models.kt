package com.zhihuiji.core.model.v2.partner

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PartnerGroupV2Dto(
    val id: Long = 0L,
    @SerialName("partner_type") val partnerType: String = "",
    val name: String = "",
    val status: Int = 1,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class PartnerGroupWriteV2Request(
    val name: String,
    val status: Int? = null,
    @SerialName("sort_order") val sortOrder: Int? = null,
)

@Serializable
data class PartnerContactV2Dto(
    val id: Long = 0L,
    @SerialName("partner_type") val partnerType: String = "",
    @SerialName("partner_id") val partnerId: Long = 0L,
    val name: String = "",
    val phone: String? = null,
    val title: String? = null,
    @SerialName("is_primary") val isPrimary: Boolean = false,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class PartnerContactWriteV2Request(
    @SerialName("partner_id") val partnerId: Long,
    val name: String,
    val phone: String? = null,
    val title: String? = null,
    @SerialName("is_primary") val isPrimary: Boolean? = null,
)

@Serializable
data class CustomerV2Dto(
    val id: Long = 0L,
    val name: String = "",
    val phone: String = "",
    val level: Int = 0,
    @SerialName("group_id") val groupId: Long? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("primary_contact_name") val primaryContactName: String? = null,
    @SerialName("primary_contact_phone") val primaryContactPhone: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val balance: Double = 0.0,
    val status: Int = 1,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class CustomerWriteV2Request(
    val name: String,
    val phone: String,
    val level: Int,
    @SerialName("group_id") val groupId: Long? = null,
    @SerialName("primary_contact_name") val primaryContactName: String? = null,
    @SerialName("primary_contact_phone") val primaryContactPhone: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val balance: Double? = null,
    val status: Int? = null,
)

@Serializable
data class SupplierV2Dto(
    val id: Long = 0L,
    val name: String = "",
    val phone: String = "",
    @SerialName("group_id") val groupId: Long? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("primary_contact_name") val primaryContactName: String? = null,
    @SerialName("primary_contact_phone") val primaryContactPhone: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val balance: Double = 0.0,
    val status: Int = 1,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class SupplierWriteV2Request(
    val name: String,
    val phone: String,
    @SerialName("group_id") val groupId: Long? = null,
    @SerialName("primary_contact_name") val primaryContactName: String? = null,
    @SerialName("primary_contact_phone") val primaryContactPhone: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val balance: Double? = null,
    val status: Int? = null,
)
