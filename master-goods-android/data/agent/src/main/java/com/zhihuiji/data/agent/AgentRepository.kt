package com.zhihuiji.data.agent

import androidx.annotation.VisibleForTesting
import com.zhihuiji.core.model.*
import com.zhihuiji.core.network.ZhihuijiApi
import com.zhihuiji.core.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentRepository @Inject constructor(
    private val api: ZhihuijiApi,
) {
    suspend fun getWorkbench(windowDays: Int = 7, limit: Int = 6, agingDays: Int = 15) =
        safeApiCall { api.agentWorkbench(windowDays, limit, agingDays) }

    suspend fun query(question: String) =
        safeApiCall { api.agentQuery(AgentQueryRequest(question)) }

    @VisibleForTesting
    suspend fun generateOperationDraft(instruction: String) =
        safeApiCall { api.operationDraft(OperationDraftRequest(instruction)) }

    @VisibleForTesting
    suspend fun submitOperationDraft(draft: OperationDraftDto) =
        safeApiCall { api.operationSubmit(OperationSubmitRequest(draft)) }

    @VisibleForTesting
    suspend fun createTask(request: CreateAgentTaskRequest) =
        safeApiCall { api.createAgentTask(request) }

    suspend fun listTasks() = safeApiCall { api.agentTasks() }

    @VisibleForTesting
    suspend fun getTask(taskId: Long) = safeApiCall { api.agentTask(taskId) }

    suspend fun listNotifications(unreadOnly: Boolean = false, undeliveredOnly: Boolean = false) =
        safeApiCall { api.notifications(unreadOnly, undeliveredOnly) }

    @VisibleForTesting
    suspend fun markNotificationRead(id: Long) = safeApiCall { api.markNotificationRead(id) }

    @VisibleForTesting
    suspend fun markNotificationDelivered(id: Long) = safeApiCall { api.markNotificationDelivered(id) }
}
