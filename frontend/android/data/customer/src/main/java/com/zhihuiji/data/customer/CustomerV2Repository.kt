package com.zhihuiji.data.customer

import com.zhihuiji.core.model.v2.partner.CustomerV2Dto
import com.zhihuiji.core.model.v2.partner.CustomerWriteV2Request
import com.zhihuiji.core.model.v2.partner.PartnerContactV2Dto
import com.zhihuiji.core.model.v2.partner.PartnerContactWriteV2Request
import com.zhihuiji.core.model.v2.partner.PartnerGroupV2Dto
import com.zhihuiji.core.model.v2.partner.PartnerGroupWriteV2Request
import com.zhihuiji.core.network.ZhihuijiV2Api
import com.zhihuiji.core.network.safeApiCall
import com.zhihuiji.core.network.safeApiUnitCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerV2Repository @Inject constructor(
    private val api: ZhihuijiV2Api,
) {
    suspend fun listCustomers(
        keyword: String? = null,
        status: Int? = null,
        groupId: Long? = null,
    ): Result<List<CustomerV2Dto>> = safeApiCall { api.customersV2(keyword, status, groupId) }

    suspend fun getCustomer(id: Long): Result<CustomerV2Dto> =
        safeApiCall { api.customerV2(id) }

    suspend fun createCustomer(request: CustomerWriteV2Request): Result<CustomerV2Dto> =
        safeApiCall { api.createCustomerV2(request) }

    suspend fun updateCustomer(id: Long, request: CustomerWriteV2Request): Result<CustomerV2Dto> =
        safeApiCall { api.updateCustomerV2(id, request) }

    suspend fun deleteCustomer(id: Long): Result<Unit> =
        safeApiUnitCall { api.deleteCustomerV2(id) }

    suspend fun listGroups(): Result<List<PartnerGroupV2Dto>> =
        safeApiCall { api.customerGroupsV2() }

    suspend fun createGroup(request: PartnerGroupWriteV2Request): Result<PartnerGroupV2Dto> =
        safeApiCall { api.createCustomerGroupV2(request) }

    suspend fun updateGroup(id: Long, request: PartnerGroupWriteV2Request): Result<PartnerGroupV2Dto> =
        safeApiCall { api.updateCustomerGroupV2(id, request) }

    suspend fun deleteGroup(id: Long): Result<Unit> =
        safeApiUnitCall { api.deleteCustomerGroupV2(id) }

    suspend fun listContacts(customerId: Long): Result<List<PartnerContactV2Dto>> =
        safeApiCall { api.customerContactsV2(customerId) }

    suspend fun createContact(request: PartnerContactWriteV2Request): Result<PartnerContactV2Dto> =
        safeApiCall { api.createCustomerContactV2(request) }

    suspend fun updateContact(id: Long, request: PartnerContactWriteV2Request): Result<PartnerContactV2Dto> =
        safeApiCall { api.updateCustomerContactV2(id, request) }

    suspend fun deleteContact(id: Long): Result<Unit> =
        safeApiUnitCall { api.deleteCustomerContactV2(id) }
}
