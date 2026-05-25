package com.zhihuiji.core.database.dao

import androidx.room.*
import com.zhihuiji.core.database.entity.SupplierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE (:keyword IS NULL OR name LIKE '%' || :keyword || '%' OR phone LIKE '%' || :keyword || '%') AND (:status IS NULL OR status = :status) ORDER BY updatedAt DESC")
    fun search(keyword: String?, status: Int?): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun findById(id: Long): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SupplierEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SupplierEntity>)

    @Query("DELETE FROM suppliers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM suppliers")
    suspend fun clear()
}
