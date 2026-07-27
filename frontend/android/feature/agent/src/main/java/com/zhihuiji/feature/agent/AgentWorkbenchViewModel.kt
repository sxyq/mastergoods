package com.zhihuiji.feature.agent

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class AgentWorkbenchUiState(
    val greeting: String = "你好，我是智慧记 AI 助手",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRemoteSynced: Boolean = false,
    val todaySummary: String? = null,
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
            repository.getWorkbench()
                .onSuccess { dto ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isRemoteSynced = true,
                            greeting = dto.greeting.ifBlank { state.greeting },
                            todaySummary = dto.todaySummary,
                            kpiCards = dto.kpiCards,
                            quickQuestions = dto.quickQuestions,
                            pendingDrafts = dto.pendingDrafts,
                            riskAlerts = dto.riskAlerts,
                            recentConversations = dto.recentConversations,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRemoteSynced = false,
                            error = e.message ?: "AI 工作台状态同步失败，仍可打开对话入口",
                        )
                    }
                }
        }
    }
}
