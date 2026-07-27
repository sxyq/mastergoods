package com.zhihuiji.data.finance

import com.zhihuiji.core.model.v2.finance.AccountCreateV2Request
import com.zhihuiji.core.model.v2.finance.AccountTransferCreateV2Request
import com.zhihuiji.core.model.v2.finance.AccountTransferV2Dto
import com.zhihuiji.core.model.v2.finance.AccountUpdateV2Request
import com.zhihuiji.core.model.v2.finance.AccountV2Dto
import com.zhihuiji.core.model.v2.finance.BillFundLinkCreateV2Request
import com.zhihuiji.core.model.v2.finance.BillFundLinkV2Dto
import com.zhihuiji.core.network.ZhihuijiV2Api
import com.zhihuiji.core.network.safeApiCall
import com.zhihuiji.core.network.safeApiUnitCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceV2Repository @Inject constructor(
    private val api: ZhihuijiV2Api,
) {
    suspend fun listAccounts(): Result<List<AccountV2Dto>> =
        safeApiCall { api.accountsV2() }

    suspend fun getAccount(id: Long): Result<AccountV2Dto> =
        safeApiCall { api.accountV2(id) }

    suspend fun createAccount(request: AccountCreateV2Request): Result<AccountV2Dto> =
        safeApiCall { api.createAccountV2(request) }

    suspend fun updateAccount(id: Long, request: AccountUpdateV2Request): Result<AccountV2Dto> =
        safeApiCall { api.updateAccountV2(id, request) }

    suspend fun deleteAccount(id: Long): Result<Unit> =
        safeApiUnitCall { api.deleteAccountV2(id) }

    suspend fun listTransfers(): Result<List<AccountTransferV2Dto>> =
        safeApiCall { api.accountTransfersV2() }

    suspend fun getTransfer(id: Long): Result<AccountTransferV2Dto> =
        safeApiCall { api.accountTransferV2(id) }

    suspend fun createTransfer(request: AccountTransferCreateV2Request): Result<AccountTransferV2Dto> =
        safeApiCall { api.createAccountTransferV2(request) }

    suspend fun listBillFundLinks(
        billType: String? = null,
        billId: Long? = null,
        accountId: Long? = null,
    ): Result<List<BillFundLinkV2Dto>> = safeApiCall { api.billFundLinksV2(billType, billId, accountId) }

    suspend fun createBillFundLink(request: BillFundLinkCreateV2Request): Result<BillFundLinkV2Dto> =
        safeApiCall { api.createBillFundLinkV2(request) }

    suspend fun deleteBillFundLink(id: Long): Result<Unit> =
        safeApiUnitCall { api.deleteBillFundLinkV2(id) }
}
