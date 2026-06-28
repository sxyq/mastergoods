package com.zhihuiji.data.finance

import com.zhihuiji.core.model.v2.finance.AccountCreateV2Request
import com.zhihuiji.core.model.v2.finance.AccountTransferCreateV2Request
import com.zhihuiji.core.model.v2.finance.AccountTransferV2Dto
import com.zhihuiji.core.model.v2.finance.AccountUpdateV2Request
import com.zhihuiji.core.model.v2.finance.AccountV2Dto
import com.zhihuiji.core.network.ZhihuijiV2Api
import com.zhihuiji.core.network.safeApiCall
import com.zhihuiji.core.network.safeApiUnitCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 账户与转账 Repository（FE6/FE7 安卓端专用）。
 * 复用 [ZhihuijiV2Api] 的 v2 端点，统一走 safeApiCall/safeApiUnitCall 错误归一。
 */
@Singleton
class AccountV2Repository @Inject constructor(
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
}
