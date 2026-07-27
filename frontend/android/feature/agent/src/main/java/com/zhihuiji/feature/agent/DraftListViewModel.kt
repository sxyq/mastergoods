package com.zhihuiji.feature.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.agent.AgentDraftDto
import com.zhihuiji.core.model.v2.agent.UpdateAgentDraftRequest
import com.zhihuiji.data.agent.AgentV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.concurrent.ConcurrentHashMap

data class DraftItem(
    val id: Long,
    val conversationId: Long? = null,
    val draftType: String,
    val typeLabel: String,
    val title: String,
    val businessNo: String,
    val partyName: String,
    val amountText: String,
    val status: String,
    val statusLabel: String,
    val contentJson: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

data class DraftListUiState(
    val isLoading: Boolean = false,
    val isArchiving: Boolean = false,
    val error: String? = null,
    val drafts: List<DraftItem> = emptyList(),
    val selectedTab: Int = 0,
)

@HiltViewModel
class DraftListViewModel @Inject constructor(
    private val repository: AgentV2Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DraftListUiState())
    val uiState: StateFlow<DraftListUiState> = _uiState.asStateFlow()

    init {
        loadDrafts()
    }

    fun loadDrafts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.listDrafts()
            result.onSuccess { dtos ->
                val drafts = ArrayList<DraftItem>(dtos.size)
                for (dto in dtos) {
                    drafts.add(dto.toDraftItem())
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        drafts = drafts,
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun archiveDraft(draftId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isArchiving = true, error = null) }
            val draft = _uiState.value.drafts.firstOrNull { it.id == draftId }
            if (draft == null) {
                _uiState.update { it.copy(isArchiving = false, error = "草稿不存在或已处理") }
                return@launch
            }
            repository.updateDraft(
                draftId,
                UpdateAgentDraftRequest(
                    conversationId = draft.conversationId,
                    draftType = draft.draftType,
                    title = draft.title,
                    contentJson = draft.contentJson,
                    status = "archived",
                )
            ).onSuccess {
                _uiState.update { state ->
                    state.copy(
                        isArchiving = false,
                        drafts = state.drafts.filter { it.id != draft.id },
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isArchiving = false, error = e.message ?: "草稿归档失败") }
            }
        }
    }

    fun deleteDraft(draftId: Long) {
        viewModelScope.launch {
            val result = repository.deleteDraft(draftId)
            result.onSuccess {
                _uiState.update { state ->
                    state.copy(drafts = state.drafts.filter { it.id != draftId })
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

private fun AgentDraftDto.toDraftItem(): DraftItem = DraftItem(
    id = id,
    conversationId = conversationId,
    draftType = draftType,
    typeLabel = when (draftType) {
        "sale" -> "销售"
        "purchase" -> "采购"
        "stock_adjust" -> "库存调整"
        else -> draftType
    },
    title = title,
    businessNo = extractJsonValue(
        contentJson,
        "orderNo",
        "order_no",
        "billNo",
        "bill_no",
        "draftNo",
        "draft_no"
    ) ?: "后端未返回业务号",
    partyName = extractJsonValue(
        contentJson,
        "customerName",
        "customer_name",
        "supplierName",
        "supplier_name",
        "partnerName",
        "partner_name",
        "partyName",
        "party_name"
    ) ?: "后端未返回往来方",
    amountText = extractAmountText(contentJson) ?: "后端未返回金额",
    status = status,
    statusLabel = when (status) {
        "active" -> "仅草稿，未执行"
        "archived" -> "已归档（未执行）"
        "deleted" -> "已删除"
        "pending", "confirmed" -> "后端状态未接入执行"
        else -> status.ifBlank { "后端未返回状态" }
    },
    contentJson = contentJson,
    createdAt = createdAt,
)

private fun extractAmountText(json: String): String? {
    val rawAmount = extractJsonValue(
        json,
        "amount",
        "totalAmount",
        "total_amount",
        "payAmount",
        "pay_amount",
        "receivable",
        "payable"
    ) ?: return null
    val amount = rawAmount.toDoubleOrNull() ?: return rawAmount.takeIf { it.isNotBlank() }
    return "¥%.2f".format(amount)
}

private fun extractJsonValue(json: String, vararg keys: String): String? {
    if (json.isBlank()) return null
    for (key in keys) {
        val patterns = jsonValuePatterns.getOrPut(key) {
            val escapedKey = Regex.escape(key)
            JsonValuePattern(
                stringRegex = Regex("\"$escapedKey\"\\s*:\\s*\"([^\"]*)\""),
                numberRegex = Regex("\"$escapedKey\"\\s*:\\s*([-+]?\\d+(?:\\.\\d+)?)"),
            )
        }
        val stringMatch = patterns.stringRegex.find(json)
        val stringValue = stringMatch?.groupValues?.getOrNull(1)
        if (!stringValue.isNullOrBlank()) {
            return stringValue
        }
        val numberMatch = patterns.numberRegex.find(json)
        val numberValue = numberMatch?.groupValues?.getOrNull(1)
        if (!numberValue.isNullOrBlank()) {
            return numberValue
        }
    }
    return null
}

private data class JsonValuePattern(
    val stringRegex: Regex,
    val numberRegex: Regex,
)

private val jsonValuePatterns = ConcurrentHashMap<String, JsonValuePattern>()
