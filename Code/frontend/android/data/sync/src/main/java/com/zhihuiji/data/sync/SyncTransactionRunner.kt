package com.zhihuiji.data.sync

import androidx.room.withTransaction
import com.zhihuiji.core.database.ZhihuijiDatabase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

interface SyncTransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}

@Singleton
class RoomSyncTransactionRunner @Inject constructor(
    private val database: ZhihuijiDatabase,
) : SyncTransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = database.withTransaction { block() }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncTransactionModule {
    @Binds
    abstract fun bindSyncTransactionRunner(
        implementation: RoomSyncTransactionRunner,
    ): SyncTransactionRunner
}
