package com.zhihuiji.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.zhihuiji.core.datastore.LocalAccessRevocationHandler
import com.zhihuiji.core.network.NetworkException
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncWorkerEntryPoint {
    fun syncRepository(): SyncV2Repository
    fun sessionStore(): com.zhihuiji.core.datastore.SessionStore
    fun syncPreferenceStore(): com.zhihuiji.core.datastore.SyncPreferenceStore
    fun localAccessRevocationHandler(): LocalAccessRevocationHandler
}

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            SyncWorkerEntryPoint::class.java,
        )
        val clientId = runCatching { entryPoint.syncPreferenceStore().requireClientId() }
            .getOrElse { return Result.success() }
        runCatching { entryPoint.sessionStore().requireAccessToken() }
            .getOrElse { return Result.success() }

        return entryPoint.syncRepository()
            .syncPendingAndPull(clientId)
            .fold(
                onSuccess = { Result.success() },
                onFailure = { throwable ->
                    syncFailureWorkResult(
                        throwable = throwable,
                        accessRevocationHandler = entryPoint.localAccessRevocationHandler(),
                    )
                },
            )
    }
}

internal enum class SyncFailureAction {
    CLEAR_LOCAL_ACCESS,
    RETRY,
}

internal fun syncFailureAction(throwable: Throwable): SyncFailureAction = when {
    throwable is NetworkException &&
        (throwable.code == 401 || throwable.code == 403) -> SyncFailureAction.CLEAR_LOCAL_ACCESS
    else -> SyncFailureAction.RETRY
}

internal suspend fun syncFailureWorkResult(
    throwable: Throwable,
    accessRevocationHandler: LocalAccessRevocationHandler,
): ListenableWorker.Result = when (syncFailureAction(throwable)) {
    SyncFailureAction.CLEAR_LOCAL_ACCESS -> runCatching {
        accessRevocationHandler.clearForAccessRevocation()
    }.fold(
        onSuccess = { ListenableWorker.Result.success() },
        onFailure = { ListenableWorker.Result.retry() },
    )
    SyncFailureAction.RETRY -> ListenableWorker.Result.retry()
}

object SyncScheduler {
    private const val UNIQUE_WORK_NAME = "master-goods-sync"
    private const val PERIODIC_WORK_NAME = "master-goods-sync-periodic"

    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            immediateWorkPolicy(),
            request,
        )
    }

    /**
     * A new local mutation must not wait behind a stale retry backoff. Outbox rows
     * are durable and operation IDs are idempotent, so replacing the delayed worker
     * cannot lose or duplicate a business operation.
     */
    internal fun immediateWorkPolicy(): ExistingWorkPolicy = ExistingWorkPolicy.REPLACE

    fun enqueuePeriodicSync(context: Context) {
        val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest,
        )
    }
}

/** Keeps mutation repositories independent from WorkManager implementation details. */
interface SyncWorkScheduler {
    fun scheduleNow()
}

@Singleton
class WorkManagerSyncWorkScheduler @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : SyncWorkScheduler {
    override fun scheduleNow() = SyncScheduler.enqueue(appContext)
}

internal object NoOpSyncWorkScheduler : SyncWorkScheduler {
    override fun scheduleNow() = Unit
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncWorkSchedulerModule {
    @Binds
    abstract fun bindSyncWorkScheduler(
        implementation: WorkManagerSyncWorkScheduler,
    ): SyncWorkScheduler
}

@Module
@InstallIn(SingletonComponent::class)
abstract class LocalSyncRepositoryModule {
    @Binds
    abstract fun bindLocalSyncRepository(
        implementation: SyncV2Repository,
    ): LocalSyncRepository
}
