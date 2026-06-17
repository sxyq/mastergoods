package com.zhihuiji.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.CurrentStoreProfile
import com.zhihuiji.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainAccessUiState(
    val isLoading: Boolean = false,
    val isResolved: Boolean = false,
    val storeProfile: CurrentStoreProfile? = null,
    val permissions: Set<String> = emptySet(),
    val error: String? = null,
)

@HiltViewModel
class MainAccessViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainAccessUiState(isLoading = true))
    val uiState: StateFlow<MainAccessUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.fetchCurrentStore().fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isResolved = true,
                            storeProfile = profile,
                            permissions = profile.permissions.toSet(),
                            error = null,
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isResolved = true,
                            permissions = emptySet(),
                            error = throwable.message ?: "门店权限上下文读取失败",
                        )
                    }
                },
            )
        }
    }
}

data class RouteAccessRule(
    val allOf: Set<String> = emptySet(),
    val anyOf: Set<String> = emptySet(),
)

fun MainAccessUiState.canAccessRule(rule: RouteAccessRule): Boolean {
    if (!isResolved) return true
    if (!permissions.containsAll(rule.allOf)) return false
    if (rule.anyOf.isNotEmpty() && rule.anyOf.none(permissions::contains)) return false
    return true
}

fun MainAccessUiState.canAccessRoute(route: String?): Boolean =
    canAccessRule(routeAccessRule(route))

fun MainAccessUiState.hasPermission(permission: String): Boolean =
    canAccessRule(RouteAccessRule(allOf = setOf(permission)))

fun MainAccessUiState.hasAnyPermission(vararg permission: String): Boolean =
    canAccessRule(RouteAccessRule(anyOf = permission.toSet()))

fun MainAccessUiState.firstAllowedTopLevelRoute(): String =
    listOf(TabRoutes.HOME, TabRoutes.DOCUMENTS, TabRoutes.ARCHIVES, TabRoutes.REPORTS, TabRoutes.AGENT)
        .firstOrNull { canAccessRoute(it) }
        ?: TabRoutes.HOME

fun routeAccessRule(route: String?): RouteAccessRule {
    val normalized = route.orEmpty().substringBefore("?")
    return when {
        normalized == TabRoutes.HOME -> RouteAccessRule(allOf = setOf("dashboard:view"))
        normalized == TabRoutes.DOCUMENTS -> RouteAccessRule(
            anyOf = setOf("sales:view", "purchase:view", "finance:view")
        )
        normalized == TabRoutes.ARCHIVES -> RouteAccessRule(allOf = setOf("archives:view"))
        normalized == TabRoutes.REPORTS -> RouteAccessRule(allOf = setOf("reports:view"))
        normalized == TabRoutes.AGENT -> RouteAccessRule(allOf = setOf("agent:view"))
        normalized == MainRoutes.STAFF_MANAGEMENT -> RouteAccessRule(allOf = setOf("users:manage"))

        normalized == DetailRoutes.PRODUCT_DETAIL || normalized.startsWith("product_detail/") ->
            RouteAccessRule(allOf = setOf("archives:view"))
        normalized == DetailRoutes.PRODUCT_EDIT || normalized.startsWith("product_edit/") || normalized == DetailRoutes.PRODUCT_CREATE ->
            RouteAccessRule(allOf = setOf("archives:write"))
        normalized == DetailRoutes.STOCK_ADJUST || normalized.startsWith("stock_adjust/") ->
            RouteAccessRule(allOf = setOf("inventory:write"))
        normalized == DetailRoutes.INVENTORY_LEDGER || normalized.startsWith("inventory_ledger/") || normalized == DetailRoutes.INVENTORY_SNAPSHOT ->
            RouteAccessRule(allOf = setOf("inventory:view"))

        normalized == DetailRoutes.CUSTOMER_DETAIL || normalized.startsWith("customer_detail/") ->
            RouteAccessRule(allOf = setOf("archives:view"))
        normalized == DetailRoutes.CUSTOMER_EDIT || normalized.startsWith("customer_edit/") || normalized == DetailRoutes.CUSTOMER_CREATE ->
            RouteAccessRule(allOf = setOf("archives:write"))

        normalized == DetailRoutes.SUPPLIER_DETAIL || normalized.startsWith("supplier_detail/") ->
            RouteAccessRule(allOf = setOf("archives:view"))
        normalized == DetailRoutes.SUPPLIER_EDIT || normalized.startsWith("supplier_edit/") || normalized == DetailRoutes.SUPPLIER_CREATE ->
            RouteAccessRule(allOf = setOf("archives:write"))
        normalized == DetailRoutes.SUPPLIER_STATEMENT || normalized.startsWith("supplier_statement/") ->
            RouteAccessRule(allOf = setOf("finance:view"))

        normalized == DetailRoutes.SALE_ORDER_DETAIL || normalized.startsWith("sale_order_detail/") ->
            RouteAccessRule(allOf = setOf("sales:view"))
        normalized == DetailRoutes.SALE_ORDER_EDIT || normalized.startsWith("sale_order_edit/") || normalized == DetailRoutes.SALE_ORDER_CREATE ->
            RouteAccessRule(allOf = setOf("sales:write"))
        normalized == DetailRoutes.PAYMENT || normalized.startsWith("payment/") ->
            RouteAccessRule(allOf = setOf("sales:write", "finance:write"))
        normalized == DetailRoutes.SALES_RETURNS ->
            RouteAccessRule(allOf = setOf("sales:view"))

        normalized == DetailRoutes.PURCHASE_ORDER_DETAIL || normalized.startsWith("purchase_order_detail/") ->
            RouteAccessRule(allOf = setOf("purchase:view"))
        normalized == DetailRoutes.PURCHASE_ORDER_EDIT || normalized.startsWith("purchase_order_edit/") || normalized == DetailRoutes.PURCHASE_ORDER_CREATE ->
            RouteAccessRule(allOf = setOf("purchase:write"))
        normalized == DetailRoutes.PURCHASE_RECEIPTS ->
            RouteAccessRule(allOf = setOf("inventory:view"))
        normalized == DetailRoutes.PURCHASE_RETURNS ->
            RouteAccessRule(allOf = setOf("purchase:view"))

        normalized == DetailRoutes.PAY_ORDER_DETAIL || normalized.startsWith("pay_order_detail/") ->
            RouteAccessRule(allOf = setOf("finance:view"))
        normalized == DetailRoutes.FINANCE_RECORD_DETAIL || normalized.startsWith("finance_record_detail/") ->
            RouteAccessRule(allOf = setOf("finance:view"))
        normalized == DetailRoutes.DAILY_EXPENSE ->
            RouteAccessRule(allOf = setOf("finance:write"))

        normalized == DetailRoutes.DRAFT_LIST || normalized == DetailRoutes.TASK_NOTIFICATION ->
            RouteAccessRule(allOf = setOf("agent:view"))
        normalized == "agent_chat" || normalized == DetailRoutes.AGENT_CHAT.substringBefore("?") ->
            RouteAccessRule(allOf = setOf("agent:view"))
        else -> RouteAccessRule()
    }
}
