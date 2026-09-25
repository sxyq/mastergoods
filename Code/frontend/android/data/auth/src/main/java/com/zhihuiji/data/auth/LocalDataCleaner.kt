package com.zhihuiji.data.auth

import android.content.Context
import androidx.work.WorkManager
import com.zhihuiji.core.database.dao.AgentNotificationDao
import com.zhihuiji.core.database.dao.AgentAuditDao
import com.zhihuiji.core.database.dao.PendingAgentMessageDao
import com.zhihuiji.core.database.dao.SyncCursorDao
import com.zhihuiji.core.database.dao.SyncOutboxDao
import com.zhihuiji.core.database.dao.SyncRemoteRecordDao
import com.zhihuiji.core.database.dao.SyncConflictDao
import com.zhihuiji.core.database.ZhihuijiDatabase
import com.zhihuiji.core.database.clearLegacyBusinessTables
import com.zhihuiji.core.datastore.LocalAccessRevocationHandler
import com.zhihuiji.core.datastore.SessionStore
import com.zhihuiji.core.datastore.SyncPreferenceStore
import com.zhihuiji.core.network.MemoryCache
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Singleton
class LocalDataCleaner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionStore: SessionStore,
    private val syncPreferenceStore: SyncPreferenceStore,
    private val agentNotificationDao: AgentNotificationDao,
    private val agentAuditDao: AgentAuditDao,
    private val pendingAgentMessageDao: PendingAgentMessageDao,
    private val syncCursorDao: SyncCursorDao,
    private val syncOutboxDao: SyncOutboxDao,
    private val syncRemoteRecordDao: SyncRemoteRecordDao,
    private val syncConflictDao: SyncConflictDao,
    private val database: ZhihuijiDatabase,
) : LocalAccessRevocationHandler {

    override suspend fun clearForAccessRevocation() {
        clearAll()
    }

    suspend fun clearAll() = coroutineScope {
        WorkManager.getInstance(context).cancelUniqueWork("master-goods-sync")
        WorkManager.getInstance(context).cancelUniqueWork("master-goods-sync-periodic")
        WorkManager.getInstance(context).cancelUniqueWork("master-goods-agent-pending-messages")
        launch { sessionStore.clearSession() }
        launch { syncPreferenceStore.clearAll() }
        launch { agentNotificationDao.clear() }
        launch { agentAuditDao.clear() }
        launch { pendingAgentMessageDao.clear() }
        launch { syncCursorDao.clear() }
        launch { syncOutboxDao.clear() }
        launch { syncRemoteRecordDao.clear() }
        launch { syncConflictDao.clear() }
        launch { database.clearLegacyBusinessTables() }
        launch { MemoryCache.clearAllRegistered() }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class LocalAccessRevocationHandlerModule {
    @Binds
    abstract fun bindLocalAccessRevocationHandler(
        implementation: LocalDataCleaner,
    ): LocalAccessRevocationHandler
}
