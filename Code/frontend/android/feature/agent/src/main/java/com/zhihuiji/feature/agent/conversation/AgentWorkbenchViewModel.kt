package com.zhihuiji.feature.agent

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.agent.AgentConversationDto
import com.zhihuiji.core.model.v2.agent.KpiCardItem
import com.zhihuiji.core.model.v2.agent.PendingDraftItem
import com.zhihuiji.core.model.v2.agent.RecentConversationItem
import com.zhihuiji.core.model.v2.agent.RiskAlertItem
import com.zhihuiji.data.agent.AgentV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class AgentWorkbenchUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRemoteSynced: Boolean = false,
    val kpiCards: List<KpiCardItem> = emptyList(),
    val quickQuestions: List<String> = emptyList(),
    val pendingDrafts: List<PendingDraftItem> = emptyList(),
    val riskAlerts: List<RiskAlertItem> = emptyList(),
    val recentConversations: List<RecentConversationItem> = emptyList(),
)

@HiltViewModel
class AgentWorkbenchViewModel @Inject constructor(
    private val repository: AgentV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AgentWorkbenchUiState())
    val uiState: StateFlow<AgentWorkbenchUiState> = _uiState.asStateFlow()

    init {
        loadWorkbench()
    }

    fun loadWorkbench() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val (workbenchResult, conversationsResult) = coroutineScope {
                val workbench = async { repository.getWorkbench() }
                val conversations = async {
                    repository.listConversations(page = 0, limit = RECENT_CONVERSATION_LIMIT)
                }
                workbench.await() to conversations.await()
            }
            workbenchResult
                .onSuccess { dto ->
                    // The workbench response is the authoritative homepage snapshot.
                    // Conversation API data remains a compatibility fallback for older servers.
                    val recentConversations = if (dto.recentConversations.isNotEmpty()) {
                        dto.recentConversations
                    } else {
                        resolveWorkbenchRecentConversations(
                            fallback = emptyList(),
                            conversations = conversationsResult.getOrNull(),
                        )
                    }
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isRemoteSynced = true,
                            kpiCards = dto.kpiCards,
                            quickQuestions = dto.quickQuestions,
                            pendingDrafts = dto.pendingDrafts,
                            riskAlerts = dto.riskAlerts,
                            recentConversations = recentConversations,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isRemoteSynced = false,
                            error = error.message ?: "AI 工作台状态同步失败，仍可打开对话入口",
                            recentConversations = resolveWorkbenchRecentConversations(
                                fallback = state.recentConversations,
                                conversations = conversationsResult.getOrNull(),
                            ),
                        )
                    }
                }
        }
    }

    companion object {
        private const val RECENT_CONVERSATION_LIMIT = 5
    }
}

/** The conversation endpoint is authoritative; workbench history is only a fallback. */
internal fun resolveWorkbenchRecentConversations(
    fallback: List<RecentConversationItem>,
    conversations: List<AgentConversationDto>?,
): List<RecentConversationItem> = conversations?.mapNotNull { conversation ->
    conversation.id.takeIf { it > 0L }?.let { id ->
        RecentConversationItem(
            id = id,
            title = conversation.title,
            lastMessageAt = conversation.lastMessageAt
                ?: conversation.updatedAt.takeIf { it > 0L }
                ?: conversation.createdAt,
            messageCount = 0,
            latestSummary = conversation.latestSummary,
        )
    }
} ?: fallback
