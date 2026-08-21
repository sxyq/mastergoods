package com.zhihuiji.data.auth

import android.content.Context
import androidx.work.WorkManager
import com.zhihuiji.core.database.dao.AgentNotificationDao
import com.zhihuiji.core.database.dao.AgentAuditDao
import com.zhihuiji.core.database.dao.PendingAgentMessageDao
import com.zhihuiji.core.database.dao.CustomerDao
import com.zhihuiji.core.database.dao.DashboardSnapshotDao
import com.zhihuiji.core.database.dao.FinanceRecordDao
import com.zhihuiji.core.database.dao.PayOrderDao
import com.zhihuiji.core.database.dao.ProductDao
import com.zhihuiji.core.database.dao.PurchaseOrderDao
import com.zhihuiji.core.database.dao.SaleOrderDao
import com.zhihuiji.core.database.dao.SupplierDao
import com.zhihuiji.core.database.dao.SyncCursorDao
import com.zhihuiji.core.database.dao.SyncOutboxDao
import com.zhihuiji.core.database.dao.SyncRemoteRecordDao
import com.zhihuiji.core.database.dao.SyncConflictDao
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
    private val productDao: ProductDao,
    private val customerDao: CustomerDao,
    private val supplierDao: SupplierDao,
    private val saleOrderDao: SaleOrderDao,
    private val purchaseOrderDao: PurchaseOrderDao,
    private val payOrderDao: PayOrderDao,
    private val financeRecordDao: FinanceRecordDao,
    private val agentNotificationDao: AgentNotificationDao,
    private val agentAuditDao: AgentAuditDao,
    private val pendingAgentMessageDao: PendingAgentMessageDao,
    private val syncCursorDao: SyncCursorDao,
    private val syncOutboxDao: SyncOutboxDao,
    private val syncRemoteRecordDao: SyncRemoteRecordDao,
    private val syncConflictDao: SyncConflictDao,
    private val dashboardSnapshotDao: DashboardSnapshotDao,
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
        launch { productDao.clear() }
        launch { customerDao.clear() }
        launch { supplierDao.clear() }
        launch { saleOrderDao.clear() }
        launch { purchaseOrderDao.clear() }
        launch { payOrderDao.clear() }
        launch { financeRecordDao.clear() }
        launch { agentNotificationDao.clear() }
        launch { agentAuditDao.clear() }
        launch { pendingAgentMessageDao.clear() }
        launch { syncCursorDao.clear() }
        launch { syncOutboxDao.clear() }
        launch { syncRemoteRecordDao.clear() }
        launch { syncConflictDao.clear() }
        launch { dashboardSnapshotDao.clear() }
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
