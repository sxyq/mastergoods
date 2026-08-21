package com.zhihuiji.core.model.v2.finance

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountV2Dto(
    val id: Long = 0L,
    val code: String = "",
    val name: String = "",
    val type: Int = 0,
    val balance: Double = 0.0,
    @SerialName("is_default") val isDefault: Boolean = false,
    val status: Int = 1,
    @SerialName("sort_order") val sortOrder: Int = 0,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class AccountCreateV2Request(
    val code: String,
    val name: String,
    val type: Int,
    val balance: Double? = null,
    @SerialName("is_default") val isDefault: Boolean? = null,
    val status: Int? = null,
    @SerialName("sort_order") val sortOrder: Int? = null,
    val notes: String? = null,
)

@Serializable
data class AccountUpdateV2Request(
    val code: String,
    val name: String,
    val type: Int,
    @SerialName("is_default") val isDefault: Boolean? = null,
    val status: Int? = null,
    @SerialName("sort_order") val sortOrder: Int? = null,
    val notes: String? = null,
)

@Serializable
data class AccountTransferV2Dto(
    val id: Long = 0L,
    @SerialName("transfer_no") val transferNo: String = "",
    @SerialName("from_account_id") val fromAccountId: Long = 0L,
    @SerialName("from_account_name") val fromAccountName: String = "",
    @SerialName("to_account_id") val toAccountId: Long = 0L,
    @SerialName("to_account_name") val toAccountName: String = "",
    val amount: Double = 0.0,
    val fee: Double? = null,
    val status: Int = 0,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class AccountTransferCreateV2Request(
    @SerialName("from_account_id") val fromAccountId: Long,
    @SerialName("to_account_id") val toAccountId: Long,
    val amount: Double,
    val fee: Double? = null,
    val notes: String? = null,
)

@Serializable
data class BillFundLinkV2Dto(
    val id: Long = 0L,
    @SerialName("bill_type") val billType: String = "",
    @SerialName("bill_id") val billId: Long = 0L,
    @SerialName("account_id") val accountId: Long = 0L,
    @SerialName("account_name") val accountName: String = "",
    val amount: Double = 0.0,
    @SerialName("link_type") val linkType: Int? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class BillFundLinkCreateV2Request(
    @SerialName("bill_type") val billType: String,
    @SerialName("bill_id") val billId: Long,
    @SerialName("account_id") val accountId: Long,
    val amount: Double,
    @SerialName("link_type") val linkType: Int? = null,
    val notes: String? = null,
)
