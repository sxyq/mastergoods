package com.zhihuiji.data.supplier

import com.zhihuiji.core.database.dao.SupplierDao
import com.zhihuiji.core.database.entity.SupplierEntity
import com.zhihuiji.core.model.v2.partner.PartnerContactV2Dto
import com.zhihuiji.core.model.v2.partner.PartnerContactWriteV2Request
import com.zhihuiji.core.model.v2.partner.PartnerGroupV2Dto
import com.zhihuiji.core.model.v2.partner.PartnerGroupWriteV2Request
import com.zhihuiji.core.model.v2.partner.SupplierV2Dto
import com.zhihuiji.core.model.v2.partner.SupplierWriteV2Request
import com.zhihuiji.core.network.ZhihuijiV2Api
import com.zhihuiji.core.network.safeApiCall
import com.zhihuiji.core.network.safeApiUnitCall
import com.zhihuiji.data.sync.LocalSyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupplierV2Repository @Inject constructor(
    private val api: ZhihuijiV2Api,
    private val supplierDao: SupplierDao,
    private val syncRepository: LocalSyncRepository,
) {
    fun observeSuppliers(
        keyword: String? = null,
        status: Int? = null,
        groupId: Long? = null,
    ): Flow<List<SupplierV2Dto>> {
        if (groupId != null) return flowOf(emptyList())
        return supplierDao.search(keyword, status).map { rows -> rows.map(SupplierEntity::toV2Dto) }
    }

    suspend fun listSuppliers(
        keyword: String? = null,
        status: Int? = null,
        groupId: Long? = null,
    ): Result<List<SupplierV2Dto>> {
        val remote = safeApiCall { api.suppliersV2(keyword, status, groupId) }
        for (supplier in remote.getOrNull().orEmpty()) {
            cacheRemoteSupplier(supplier)
        }
        return remote
    }

    suspend fun getSupplier(id: Long): Result<SupplierV2Dto> {
        val remote = safeApiCall { api.supplierV2(id) }
        remote.getOrNull()?.let { supplier ->
            cacheRemoteSupplier(supplier)
            return Result.success(supplier)
        }
        return supplierDao.findById(id)?.toV2Dto()?.let { Result.success(it) }
            ?: Result.failure(remote.exceptionOrNull() ?: IllegalStateException("supplier not found locally"))
    }

    suspend fun createSupplier(request: SupplierWriteV2Request): Result<SupplierV2Dto> {
        val id = syncRepository.nextLocalEntityId()
        val local = request.toPendingEntity(id, previous = null, now = System.currentTimeMillis())
        return syncRepository.mutateAndEnqueue(
            entityType = ENTITY_TYPE,
            entityId = id.toString(),
            operation = OPERATION_CREATE,
            payload = syncRepository.encodePayload(SupplierWriteV2Request.serializer(), request),
            baseVersion = 0L,
        ) {
            supplierDao.upsert(local)
            local.toV2Dto()
        }
    }

    suspend fun updateSupplier(id: Long, request: SupplierWriteV2Request): Result<SupplierV2Dto> {
        val previous = supplierDao.findById(id)
            ?: return Result.failure(IllegalStateException("supplier is not available locally"))
        val local = request.toPendingEntity(id, previous, System.currentTimeMillis())
        return syncRepository.mutateAndEnqueue(
            entityType = ENTITY_TYPE,
            entityId = id.toString(),
            operation = OPERATION_UPDATE,
            payload = syncRepository.encodePayload(SupplierWriteV2Request.serializer(), request),
            baseVersion = previous.syncVersion ?: 0L,
        ) {
            supplierDao.upsert(local)
            local.toV2Dto()
        }
    }

    suspend fun deleteSupplier(id: Long): Result<Unit> {
        val previous = supplierDao.findById(id)
            ?: return Result.failure(IllegalStateException("supplier is not available locally"))
        return syncRepository.mutateAndEnqueue(
            entityType = ENTITY_TYPE,
            entityId = id.toString(),
            operation = OPERATION_DELETE,
            payload = null,
            baseVersion = previous.syncVersion ?: 0L,
        ) { supplierDao.deleteById(id) }
    }

    private suspend fun cacheRemoteSupplier(supplier: SupplierV2Dto) {
        if (syncRepository.hasUnresolvedLocalChange(ENTITY_TYPE, supplier.id.toString())) return
        supplierDao.upsert(supplier.toEntity(supplierDao.findById(supplier.id)))
    }

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

private fun SupplierV2Dto.toEntity(previous: SupplierEntity? = null) = SupplierEntity(
    id = id,
    name = name,
    phone = phone,
    groupId = groupId,
    primaryContactName = primaryContactName,
    primaryContactPhone = primaryContactPhone,
    address = address,
    notes = notes,
    balance = balance,
    status = status,
    syncStatus = previous?.syncStatus,
    syncVersion = previous?.syncVersion,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun SupplierWriteV2Request.toPendingEntity(
    id: Long,
    previous: SupplierEntity?,
    now: Long,
) = SupplierEntity(
    id = id,
    name = name,
    phone = phone,
    groupId = groupId,
    primaryContactName = primaryContactName,
    primaryContactPhone = primaryContactPhone,
    address = address,
    notes = notes,
    balance = balance ?: previous?.balance ?: 0.0,
    status = status ?: previous?.status ?: 1,
    syncStatus = previous?.syncStatus ?: 1,
    syncVersion = previous?.syncVersion ?: 0L,
    createdAt = previous?.createdAt ?: now,
    updatedAt = now,
)

private fun SupplierEntity.toV2Dto() = SupplierV2Dto(
    id = id,
    name = name,
    phone = phone,
    groupId = groupId,
    primaryContactName = primaryContactName,
    primaryContactPhone = primaryContactPhone,
    address = address,
    notes = notes,
    balance = balance,
    status = status,
    createdAt = createdAt ?: 0L,
    updatedAt = updatedAt ?: 0L,
)

private const val ENTITY_TYPE = "supplier"
private const val OPERATION_CREATE = "create"
private const val OPERATION_UPDATE = "update"
private const val OPERATION_DELETE = "delete"
