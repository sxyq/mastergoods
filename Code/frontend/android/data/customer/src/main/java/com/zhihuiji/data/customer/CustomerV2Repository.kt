package com.zhihuiji.data.customer

import com.zhihuiji.core.database.dao.CustomerDao
import com.zhihuiji.core.database.entity.CustomerEntity
import com.zhihuiji.core.model.v2.partner.CustomerV2Dto
import com.zhihuiji.core.model.v2.partner.CustomerWriteV2Request
import com.zhihuiji.core.model.v2.partner.PartnerContactV2Dto
import com.zhihuiji.core.model.v2.partner.PartnerContactWriteV2Request
import com.zhihuiji.core.model.v2.partner.PartnerGroupV2Dto
import com.zhihuiji.core.model.v2.partner.PartnerGroupWriteV2Request
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
class CustomerV2Repository @Inject constructor(
    private val api: ZhihuijiV2Api,
    private val customerDao: CustomerDao,
    private val syncRepository: LocalSyncRepository,
) {
    fun observeCustomers(
        keyword: String? = null,
        status: Int? = null,
        groupId: Long? = null,
    ): Flow<List<CustomerV2Dto>> {
        // The current local table has no group ID. A group-filtered offline result
        // would be misleading, so let the remote refresh own that query.
        if (groupId != null) return flowOf(emptyList())
        val source = if (keyword.isNullOrBlank()) {
            customerDao.observeAll()
        } else {
            customerDao.search(keyword)
        }
        return source.map { rows ->
            rows
                .asSequence()
                .filter { status == null || it.status == status }
                .map(CustomerEntity::toV2Dto)
                .toList()
        }
    }

    fun observeReceivableCustomerCount(): Flow<Int> = customerDao.observeReceivableCustomerCount()

    suspend fun listCustomers(
        keyword: String? = null,
        status: Int? = null,
        groupId: Long? = null,
    ): Result<List<CustomerV2Dto>> {
        val remote = safeApiCall { api.customersV2(keyword, status, groupId) }
        for (customer in remote.getOrNull().orEmpty()) {
            cacheRemoteCustomer(customer)
        }
        return remote
    }

    suspend fun getCustomer(id: Long): Result<CustomerV2Dto> {
        val remote = safeApiCall { api.customerV2(id) }
        remote.getOrNull()?.let { customer ->
            cacheRemoteCustomer(customer)
            return Result.success(customer)
        }
        return customerDao.findById(id)?.toV2Dto()?.let { Result.success(it) }
            ?: Result.failure(remote.exceptionOrNull() ?: IllegalStateException("customer not found locally"))
    }

    suspend fun createCustomer(request: CustomerWriteV2Request): Result<CustomerV2Dto> {
        val id = syncRepository.nextLocalEntityId()
        val local = request.toPendingEntity(id, previous = null, now = System.currentTimeMillis())
        return syncRepository.mutateAndEnqueue(
            entityType = ENTITY_TYPE,
            entityId = id.toString(),
            operation = OPERATION_CREATE,
            payload = syncRepository.encodePayload(CustomerWriteV2Request.serializer(), request),
            baseVersion = 0L,
        ) {
            customerDao.upsert(local)
            local.toV2Dto()
        }
    }

    suspend fun updateCustomer(id: Long, request: CustomerWriteV2Request): Result<CustomerV2Dto> {
        val previous = customerDao.findById(id)
            ?: return Result.failure(IllegalStateException("customer is not available locally"))
        val local = request.toPendingEntity(id, previous, System.currentTimeMillis())
        return syncRepository.mutateAndEnqueue(
            entityType = ENTITY_TYPE,
            entityId = id.toString(),
            operation = OPERATION_UPDATE,
            payload = syncRepository.encodePayload(CustomerWriteV2Request.serializer(), request),
            baseVersion = previous.syncVersion ?: 0L,
        ) {
            customerDao.upsert(local)
            local.toV2Dto()
        }
    }

    suspend fun deleteCustomer(id: Long): Result<Unit> {
        val previous = customerDao.findById(id)
            ?: return Result.failure(IllegalStateException("customer is not available locally"))
        return syncRepository.mutateAndEnqueue(
            entityType = ENTITY_TYPE,
            entityId = id.toString(),
            operation = OPERATION_DELETE,
            payload = null,
            baseVersion = previous.syncVersion ?: 0L,
        ) { customerDao.deleteById(id) }
    }

    private suspend fun cacheRemoteCustomer(customer: CustomerV2Dto) {
        if (syncRepository.hasUnresolvedLocalChange(ENTITY_TYPE, customer.id.toString())) return
        customerDao.upsert(customer.toEntity(customerDao.findById(customer.id)))
    }

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

private fun CustomerV2Dto.toEntity(previous: CustomerEntity? = null) = CustomerEntity(
    id = id,
    name = name,
    phone = phone,
    level = level,
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

private fun CustomerWriteV2Request.toPendingEntity(
    id: Long,
    previous: CustomerEntity?,
    now: Long,
) = CustomerEntity(
    id = id,
    name = name,
    phone = phone,
    level = level,
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

private fun CustomerEntity.toV2Dto() = CustomerV2Dto(
    id = id,
    name = name,
    phone = phone,
    level = level,
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

private const val ENTITY_TYPE = "customer"
private const val OPERATION_CREATE = "create"
private const val OPERATION_UPDATE = "update"
private const val OPERATION_DELETE = "delete"
