package com.zhihuiji.data.agent

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

    suspend fun generateOperationDraft(instruction: String) =
        safeApiCall { api.operationDraft(OperationDraftRequest(instruction)) }

    suspend fun submitOperationDraft(draft: OperationDraftDto) =
        safeApiCall { api.operationSubmit(OperationSubmitRequest(draft)) }

    suspend fun createTask(request: CreateAgentTaskRequest) =
        safeApiCall { api.createAgentTask(request) }

    suspend fun listTasks() = safeApiCall { api.agentTasks() }

    suspend fun getTask(taskId: Long) = safeApiCall { api.agentTask(taskId) }

    suspend fun listNotifications(unreadOnly: Boolean = false, undeliveredOnly: Boolean = false) =
        safeApiCall { api.notifications(unreadOnly, undeliveredOnly) }

    suspend fun markNotificationRead(id: Long) = safeApiCall { api.markNotificationRead(id) }

    suspend fun markNotificationDelivered(id: Long) = safeApiCall { api.markNotificationDelivered(id) }
}
