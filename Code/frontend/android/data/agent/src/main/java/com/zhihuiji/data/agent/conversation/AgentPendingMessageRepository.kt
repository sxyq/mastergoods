package com.zhihuiji.data.agent

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.zhihuiji.core.database.dao.PendingAgentMessageDao
import com.zhihuiji.core.database.entity.PendingAgentMessageEntity
import com.zhihuiji.core.model.v2.agent.AgentChatRequest
import com.zhihuiji.core.model.v2.agent.CreateAgentConversationRequest
import com.zhihuiji.core.network.NetworkException
import com.zhihuiji.core.datastore.SessionStore
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Singleton
class AgentPendingMessageRepository @Inject constructor(
    private val pendingDao: PendingAgentMessageDao,
    private val agentRepository: AgentV2Repository,
    private val json: Json,
    private val scheduler: AgentPendingMessageScheduler,
) {
    suspend fun enqueue(
        conversationId: Long?,
        content: String,
        imageAssetIds: List<Long>,
    ): Long {
        val queueConversationId = conversationId ?: nextLocalConversationId()
        pendingDao.insert(
            PendingAgentMessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = queueConversationId,
                content = content,
                imageAssetIdsJson = json.encodeToString(ListSerializer(Long.serializer()), imageAssetIds),
                createdAt = System.currentTimeMillis(),
            ),
        )
        scheduler.scheduleNow()
        return queueConversationId
    }

    suspend fun processPending(limit: Int = 50): FlushResult {
        for (pending in pendingDao.pending(limit)) {
            val conversationId = resolveConversationId(pending) ?: return FlushResult.Retry
            val decodedImageAssetIds = runCatching {
                json.decodeFromString(ListSerializer(Long.serializer()), pending.imageAssetIdsJson)
            }
            val imageAssetIds = decodedImageAssetIds.getOrNull()
            if (imageAssetIds == null) {
                pendingDao.markAttempt(
                    pending.id,
                    PendingAgentMessageEntity.STATE_BLOCKED,
                    "invalid pending image attachment data",
                )
                continue
            }
            val sendResult = agentRepository.chat(
                AgentChatRequest(
                    conversationId = conversationId,
                    message = pending.content,
                    stream = false,
                    imageAssetIds = imageAssetIds,
                ),
            )
            if (sendResult.isSuccess) {
                pendingDao.delete(pending.id)
                continue
            }
            val error = sendResult.exceptionOrNull() ?: IllegalStateException("agent request failed")
            if (error.isRetryableNetworkFailure()) {
                return FlushResult.Retry
            }
            pendingDao.markAttempt(
                pending.id,
                PendingAgentMessageEntity.STATE_BLOCKED,
                error.message ?: "agent request rejected",
            )
        }
        return FlushResult.Completed
    }

    private suspend fun resolveConversationId(pending: PendingAgentMessageEntity): Long? {
        val current = pending.conversationId
        if (current != null && current > 0L) return current
        val created = agentRepository.createConversation(
            CreateAgentConversationRequest(
                title = pending.content.trim().take(MAX_CONVERSATION_TITLE_LENGTH),
                status = "active",
            ),
        )
        val conversation = created.getOrElse { error ->
            if (!error.isRetryableNetworkFailure()) {
                pendingDao.markAttempt(
                    pending.id,
                    PendingAgentMessageEntity.STATE_BLOCKED,
                    error.message ?: "conversation creation rejected",
                )
            }
            return null
        }
        if (current != null && current < 0L) {
            pendingDao.replaceLocalConversationId(current, conversation.id)
        }
        return conversation.id
    }

    private fun nextLocalConversationId(): Long {
        while (true) {
            val candidate = java.security.SecureRandom().nextLong() and Long.MAX_VALUE
            if (candidate != 0L) return -candidate
        }
    }

    private fun Throwable.isRetryableNetworkFailure(): Boolean =
        this is NetworkException && (code == -1 || code >= 500)

    enum class FlushResult { Completed, Retry }

    private companion object {
        const val MAX_CONVERSATION_TITLE_LENGTH = 40
    }
}

interface AgentPendingMessageScheduler {
    fun scheduleNow()
}

@Singleton
class WorkManagerAgentPendingMessageScheduler @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : AgentPendingMessageScheduler {
    override fun scheduleNow() {
        val request = OneTimeWorkRequestBuilder<AgentPendingMessageWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            AGENT_PENDING_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AgentPendingMessageModule {
    @Binds
    abstract fun bindAgentPendingMessageScheduler(
        implementation: WorkManagerAgentPendingMessageScheduler,
    ): AgentPendingMessageScheduler
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AgentPendingMessageWorkerEntryPoint {
    fun pendingMessageRepository(): AgentPendingMessageRepository
    fun sessionStore(): SessionStore
}

class AgentPendingMessageWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            AgentPendingMessageWorkerEntryPoint::class.java,
        )
        runCatching { entryPoint.sessionStore().requireAccessToken() }
            .getOrElse { return Result.success() }
        return when (entryPoint.pendingMessageRepository().processPending()) {
            AgentPendingMessageRepository.FlushResult.Completed -> Result.success()
            AgentPendingMessageRepository.FlushResult.Retry -> Result.retry()
        }
    }
}

private const val AGENT_PENDING_WORK_NAME = "master-goods-agent-pending-messages"
