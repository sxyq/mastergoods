package com.zhihuiji.data.supplier

import com.zhihuiji.core.model.v2.partner.PartnerContactV2Dto
import com.zhihuiji.core.model.v2.partner.PartnerContactWriteV2Request
import com.zhihuiji.core.model.v2.partner.PartnerGroupV2Dto
import com.zhihuiji.core.model.v2.partner.PartnerGroupWriteV2Request
import com.zhihuiji.core.model.v2.partner.SupplierV2Dto
import com.zhihuiji.core.model.v2.partner.SupplierWriteV2Request
import com.zhihuiji.core.network.ZhihuijiV2Api
import com.zhihuiji.core.network.safeApiCall
import com.zhihuiji.core.network.safeApiUnitCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupplierV2Repository @Inject constructor(
    private val api: ZhihuijiV2Api,
) {
    suspend fun listSuppliers(
        keyword: String? = null,
        status: Int? = null,
        groupId: Long? = null,
    ): Result<List<SupplierV2Dto>> = safeApiCall { api.suppliersV2(keyword, status, groupId) }

    suspend fun getSupplier(id: Long): Result<SupplierV2Dto> =
        safeApiCall { api.supplierV2(id) }

    suspend fun createSupplier(request: SupplierWriteV2Request): Result<SupplierV2Dto> =
        safeApiCall { api.createSupplierV2(request) }

    suspend fun updateSupplier(id: Long, request: SupplierWriteV2Request): Result<SupplierV2Dto> =
        safeApiCall { api.updateSupplierV2(id, request) }

    suspend fun deleteSupplier(id: Long): Result<Unit> =
        safeApiUnitCall { api.deleteSupplierV2(id) }

    suspend fun listGroups(): Result<List<PartnerGroupV2Dto>> =
        safeApiCall { api.supplierGroupsV2() }

    suspend fun createGroup(request: PartnerGroupWriteV2Request): Result<PartnerGroupV2Dto> =
        safeApiCall { api.createSupplierGroupV2(request) }

    suspend fun updateGroup(id: Long, request: PartnerGroupWriteV2Request): Result<PartnerGroupV2Dto> =
        safeApiCall { api.updateSupplierGroupV2(id, request) }

    suspend fun deleteGroup(id: Long): Result<Unit> =
        safeApiUnitCall { api.deleteSupplierGroupV2(id) }

    suspend fun listContacts(supplierId: Long): Result<List<PartnerContactV2Dto>> =
        safeApiCall { api.supplierContactsV2(supplierId) }

    suspend fun createContact(request: PartnerContactWriteV2Request): Result<PartnerContactV2Dto> =
        safeApiCall { api.createSupplierContactV2(request) }

    suspend fun updateContact(id: Long, request: PartnerContactWriteV2Request): Result<PartnerContactV2Dto> =
        safeApiCall { api.updateSupplierContactV2(id, request) }

    suspend fun deleteContact(id: Long): Result<Unit> =
        safeApiUnitCall { api.deleteSupplierContactV2(id) }
}
