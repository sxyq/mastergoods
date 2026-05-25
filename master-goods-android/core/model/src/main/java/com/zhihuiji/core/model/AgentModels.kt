package com.zhihuiji.core.model

import kotlinx.serialization.Serializable

@Serializable
data class AgentWorkbenchDto(
    val kpis: List<AgentKpi> = emptyList(),
    val insights: List<AgentInsight> = emptyList(),
    val receivableReminders: List<AgentReceivableReminder> = emptyList(),
    val stockAlerts: List<AgentStockAlert> = emptyList(),
    val quickActions: List<AgentQuickAction> = emptyList(),
)

@Serializable
data class AgentKpi(
    val label: String,
    val value: String,
    val trend: String? = null,
    val tone: String? = null,
)

@Serializable
data class AgentInsight(
    val title: String,
    val content: String,
    val severity: String? = null,
)

@Serializable
data class AgentReceivableReminder(
    val customerName: String,
    val amount: Double,
    val agingDays: Int,
)

@Serializable
data class AgentStockAlert(
    val productName: String,
    val currentStock: Double,
    val safeStock: Double,
)

@Serializable
data class AgentQuickAction(
    val label: String,
    val actionType: String,
    val params: Map<String, String> = emptyMap(),
)

@Serializable
data class AgentQueryRequest(
    val query: String,
)

@Serializable
data class AgentAnswerDto(
    val query: String = "",
    val intent: String = "",
    val answer: String = "",
    val highlights: List<String> = emptyList(),
    val columns: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList(),
    val suggestedActions: List<String> = emptyList(),
)

@Serializable
data class OperationDraftRequest(
    val instruction: String,
)

@Serializable
data class OperationDraftDto(
    val operationType: String = "",
    val summary: String = "",
    val partnerRole: String = "",
    val partnerId: Long? = null,
    val partnerName: String = "",
    val items: List<OperationDraftItemDto> = emptyList(),
    val notes: String? = null,
    val canSubmit: Boolean = false,
    val warnings: List<String> = emptyList(),
    val suggestedActions: List<String> = emptyList(),
)

@Serializable
data class OperationDraftItemDto(
    val productId: Long? = null,
    val productCode: String = "",
    val productName: String = "",
    val quantity: Double = 0.0,
    val unitPrice: Double = 0.0,
    val amount: Double = 0.0,
    val currentStock: Double = 0.0,
)

@Serializable
data class OperationSubmitRequest(
    val draft: OperationDraftDto,
)

@Serializable
data class OperationSubmitResultDto(
    val operationType: String = "",
    val orderId: Long? = null,
    val orderNo: String? = null,
    val message: String = "",
    val nextAction: String = "",
)

@Serializable
data class CreateAgentTaskRequest(
    val taskType: String,
    val title: String,
    val input: String? = null,
)

@Serializable
data class AgentTaskSummaryDto(
    val id: Long = 0,
    val taskType: String = "",
    val title: String = "",
    val status: String = "",
    val progress: Int = 0,
    val createdAt: Long = 0L,
    val completedAt: Long? = null,
)

@Serializable
data class AgentTaskDetailDto(
    val id: Long = 0,
    val taskType: String = "",
    val title: String = "",
    val status: String = "",
    val progress: Int = 0,
    val input: String? = null,
    val result: String? = null,
    val error: String? = null,
    val createdAt: Long = 0L,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
)

@Serializable
data class AgentNotificationDto(
    val id: Long = 0,
    val type: String = "",
    val title: String = "",
    val content: String = "",
    val isRead: Boolean = false,
    val isDelivered: Boolean = false,
    val createdAt: Long = 0L,
)
