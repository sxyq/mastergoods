package com.zhihuiji.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FinanceRecordDto(
    val id: Long,
    @SerialName("record_no") val recordNo: String,
    val type: Int,
    val category: String,
    @SerialName("partner_name") val partnerName: String? = null,
    val amount: Double = 0.0,
    val method: Int,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class CreateFinanceRecordRequest(
    val type: Int,
    val category: String,
    @SerialName("partner_name") val partnerName: String? = null,
    val amount: Double,
    val method: Int? = null,
    val notes: String? = null,
)

data class FinanceFilter(
    val keyword: String? = null,
    val type: Int? = null,
    val createdAfter: String? = null,
    val createdBefore: String? = null,
)
