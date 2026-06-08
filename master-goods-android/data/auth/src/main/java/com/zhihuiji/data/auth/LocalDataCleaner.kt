package com.zhihuiji.data.auth

import com.zhihuiji.core.database.dao.AgentNotificationDao
import com.zhihuiji.core.database.dao.CustomerDao
import com.zhihuiji.core.database.dao.FinanceRecordDao
import com.zhihuiji.core.database.dao.PayOrderDao
import com.zhihuiji.core.database.dao.ProductDao
import com.zhihuiji.core.database.dao.PurchaseOrderDao
import com.zhihuiji.core.database.dao.SaleOrderDao
import com.zhihuiji.core.database.dao.SupplierDao
import com.zhihuiji.core.database.dao.SyncCursorDao
import com.zhihuiji.core.datastore.SessionStore
import com.zhihuiji.core.datastore.SyncPreferenceStore
import com.zhihuiji.core.network.MemoryCache
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Singleton
class LocalDataCleaner @Inject constructor(
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
    private val syncCursorDao: SyncCursorDao,
) {
    suspend fun clearAll() = coroutineScope {
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
        launch { syncCursorDao.clear() }
        launch { MemoryCache.clearAllRegistered() }
    }
}
