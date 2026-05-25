package com.zhihuiji.data.customer

import com.zhihuiji.core.database.dao.CustomerDao
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
class CustomerRepository @Inject constructor(
    private val api: ZhihuijiApi,
    private val customerDao: CustomerDao,
) {
    fun observeCustomers(keyword: String): Flow<List<CustomerDto>> =
        (if (keyword.isBlank()) customerDao.observeAll() else customerDao.search(keyword)).map { list ->
            list.map { it.toDto() }
        }

    suspend fun refreshCustomers(keyword: String?) {
        val result = safeApiCall { api.customers(keyword) }
        result.onSuccess { customers ->
            customerDao.upsertAll(customers.map { it.toEntity() })
        }
    }

    suspend fun getCustomer(id: Long): Result<CustomerDto> {
        val local = customerDao.findById(id)?.toDto()
        val remote = safeApiCall { api.customer(id) }
        remote.onSuccess { customerDao.upsert(it.toEntity()) }
        return remote.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error -> local?.let(Result.Companion::success) ?: Result.failure(error) },
        )
    }

    suspend fun createCustomer(draft: CustomerDto): Result<CustomerDto> =
        safeApiCall { api.createCustomer(draft) }.also { result ->
            result.onSuccess { customerDao.upsert(it.toEntity()) }
        }

    suspend fun updateCustomer(id: Long, draft: CustomerDto): Result<CustomerDto> =
        safeApiCall { api.updateCustomer(id, draft) }.also { result ->
            result.onSuccess { customerDao.upsert(it.toEntity()) }
        }

    suspend fun deleteCustomer(id: Long): Result<Unit> =
        safeApiCall { api.deleteCustomer(id) }.also { result ->
            result.onSuccess { customerDao.deleteById(id) }
        }
}
