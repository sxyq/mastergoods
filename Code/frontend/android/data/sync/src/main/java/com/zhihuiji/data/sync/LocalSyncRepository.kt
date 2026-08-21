package com.zhihuiji.data.sync

import kotlinx.serialization.KSerializer

/**
 * Minimal local-first contract consumed by domain repositories.
 *
 * Keeping this contract small lets repository unit tests exercise Room/outbox
 * behavior without constructing the full network synchronization graph.
 */
interface LocalSyncRepository {
    fun <T> encodePayload(serializer: KSerializer<T>, value: T): String

    fun nextLocalEntityId(): Long

    suspend fun <T> mutateAndEnqueue(
        entityType: String,
        entityId: String,
        operation: String,
        payload: String?,
        baseVersion: Long?,
        mutation: suspend () -> T,
    ): Result<T>

    suspend fun hasUnresolvedLocalChange(entityType: String, entityId: String): Boolean

    suspend fun reconcileRemoteProduct(remoteId: Long, code: String)
}
