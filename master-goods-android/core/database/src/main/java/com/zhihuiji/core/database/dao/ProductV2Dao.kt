package com.zhihuiji.core.database.dao

import androidx.room.*
import com.zhihuiji.core.database.entity.ProductV2Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductV2Dao {
    @Query("SELECT * FROM products_v2 WHERE ownerUserId = :ownerUserId ORDER BY updatedAt DESC")
    fun observeByOwner(ownerUserId: Long): Flow<List<ProductV2Entity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProductV2Entity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ProductV2Entity>)

    @Query("DELETE FROM products_v2 WHERE ownerUserId = :ownerUserId AND productId = :productId")
    suspend fun deleteByOwnerAndId(ownerUserId: Long, productId: Long)

    @Query("DELETE FROM products_v2 WHERE ownerUserId = :ownerUserId")
    suspend fun deleteAllByOwner(ownerUserId: Long)
}
