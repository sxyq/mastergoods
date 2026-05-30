package com.zhihuiji.data.supplier

import com.zhihuiji.core.database.dao.SupplierDao
import com.zhihuiji.core.database.toDto
import com.zhihuiji.core.database.toEntity
import com.zhihuiji.core.model.*
import com.zhihuiji.core.network.ZhihuijiApi
import com.zhihuiji.core.network.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupplierRepository @Inject constructor(
    private val api: ZhihuijiApi,
    private val supplierDao: SupplierDao,
) {
    fun observeSuppliers(keyword: String, status: Int?): Flow<List<SupplierDto>> =
        supplierDao.search(keyword.ifBlank { null }, status).map { list ->
            list.map { it.toDto() }
        }

    suspend fun refreshSuppliers(keyword: String?, status: Int?) {
        val result = safeApiCall { api.suppliers(keyword, status) }
        result.onSuccess { suppliers ->
            supplierDao.upsertAll(suppliers.mapNotNull { it.toEntity() })
        }
    }

    suspend fun getSupplier(id: Long): Result<SupplierDto> {
        val local = supplierDao.findById(id)?.toDto()
        val remote = safeApiCall { api.supplier(id) }
        remote.onSuccess { it.toEntity()?.let { entity -> supplierDao.upsert(entity) } }
        return remote.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error -> local?.let(Result.Companion::success) ?: Result.failure(error) },
        )
    }

    suspend fun createSupplier(draft: SupplierDto): Result<SupplierDto> =
        safeApiCall { api.createSupplier(CreateSupplierRequest(name = draft.name, phone = draft.phone, address = draft.address, notes = draft.notes, status = draft.status)) }.also { result ->
            result.onSuccess { it.toEntity()?.let { entity -> supplierDao.upsert(entity) } }
        }

    suspend fun updateSupplier(id: Long, draft: SupplierDto): Result<SupplierDto> =
        safeApiCall { api.updateSupplier(id, UpdateSupplierRequest(name = draft.name, phone = draft.phone, address = draft.address, notes = draft.notes, status = draft.status)) }.also { result ->
            result.onSuccess { it.toEntity()?.let { entity -> supplierDao.upsert(entity) } }
        }

    suspend fun deleteSupplier(id: Long): Result<Unit> =
        safeApiCall { api.deleteSupplier(id) }.also { result ->
            result.onSuccess { supplierDao.deleteById(id) }
        }
}
