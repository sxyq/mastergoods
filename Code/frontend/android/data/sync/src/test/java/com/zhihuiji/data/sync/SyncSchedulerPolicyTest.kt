package com.zhihuiji.data.sync

import androidx.work.ExistingWorkPolicy
import com.zhihuiji.core.datastore.LocalAccessRevocationHandler
import com.zhihuiji.core.network.NetworkException
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.coroutines.runBlocking

class SyncSchedulerPolicyTest {

    @Test
    fun immediateSyncReplacesBackedOffWorkerInsteadOfDiscardingTheNewRequest() {
        assertEquals(ExistingWorkPolicy.REPLACE, SyncScheduler.immediateWorkPolicy())
    }

    @Test
    fun authorizationFailureClearsLocalAccessAndDoesNotEnterRetryLoop() = runBlocking {
        val handler = RecordingAccessRevocationHandler()

        val result = syncFailureWorkResult(NetworkException(401, "unauthorized"), handler)

        assertEquals(SyncFailureAction.CLEAR_LOCAL_ACCESS, syncFailureAction(NetworkException(401, "unauthorized")))
        assertEquals(1, handler.calls)
        assertEquals(androidx.work.ListenableWorker.Result.success().toString(), result.toString())
    }

    @Test
    fun forbiddenSyncScopeAlsoClearsLocalAccessAndDoesNotEnterRetryLoop() = runBlocking {
        val handler = RecordingAccessRevocationHandler()

        val result = syncFailureWorkResult(NetworkException(403, "forbidden"), handler)

        assertEquals(SyncFailureAction.CLEAR_LOCAL_ACCESS, syncFailureAction(NetworkException(403, "forbidden")))
        assertEquals(1, handler.calls)
        assertEquals(androidx.work.ListenableWorker.Result.success().toString(), result.toString())
    }

    @Test
    fun transientOrBusinessFailuresRemainRetryable() = runBlocking {
        val handler = RecordingAccessRevocationHandler()

        val result = syncFailureWorkResult(NetworkException(500, "temporary"), handler)

        assertEquals(SyncFailureAction.RETRY, syncFailureAction(NetworkException(500, "temporary")))
        assertEquals(SyncFailureAction.RETRY, syncFailureAction(NetworkException(422, "business")))
        assertEquals(0, handler.calls)
        assertEquals(androidx.work.ListenableWorker.Result.retry().toString(), result.toString())
    }

    @Test
    fun failedAccessCleanupRemainsRetryableUntilLocalStateCanBeCleared() = runBlocking {
        val result = syncFailureWorkResult(
            NetworkException(401, "unauthorized"),
            object : LocalAccessRevocationHandler {
                override suspend fun clearForAccessRevocation() {
                    error("storage unavailable")
                }
            },
        )

        assertEquals(androidx.work.ListenableWorker.Result.retry().toString(), result.toString())
    }

    private class RecordingAccessRevocationHandler : LocalAccessRevocationHandler {
        var calls = 0

        override suspend fun clearForAccessRevocation() {
            calls += 1
        }
    }
}
