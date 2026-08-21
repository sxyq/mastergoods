package com.zhihuiji.core.datastore

/**
 * Clears local account-scoped state after the server rejects the device's access.
 *
 * The interface keeps sync independent from the data module that owns the concrete
 * Room cleanup, while ensuring a revoked account never remains in a retry loop.
 */
interface LocalAccessRevocationHandler {
    suspend fun clearForAccessRevocation()
}
