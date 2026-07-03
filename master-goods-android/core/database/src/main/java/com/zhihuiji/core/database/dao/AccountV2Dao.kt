package com.zhihuiji.core.database.dao

import androidx.room.*
import com.zhihuiji.core.database.entity.AccountV2Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountV2Dao {
    @Query("SELECT * FROM accounts_v2 WHERE ownerUserId = :ownerUserId ORDER BY updatedAt DESC")
    fun observeByOwner(ownerUserId: Long): Flow<List<AccountV2Entity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AccountV2Entity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<AccountV2Entity>)

    @Query("DELETE FROM accounts_v2 WHERE ownerUserId = :ownerUserId AND accountId = :accountId")
    suspend fun deleteByOwnerAndId(ownerUserId: Long, accountId: Long)

    @Query("DELETE FROM accounts_v2 WHERE ownerUserId = :ownerUserId")
    suspend fun deleteAllByOwner(ownerUserId: Long)
}
