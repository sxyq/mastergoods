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
                    val permissions = profile.permissions.toSet()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isResolved = true,
                            storeProfile = profile,
                            permissions = permissions,
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

private val unrestrictedAccessRule = RouteAccessRule()
private val dashboardViewRule = RouteAccessRule(allOf = setOf("dashboard:view"))
private val documentsViewRule = RouteAccessRule(anyOf = setOf("sales:view", "purchase:view", "finance:view"))
private val archivesViewRule = RouteAccessRule(allOf = setOf("archives:view"))
private val archivesWriteRule = RouteAccessRule(allOf = setOf("archives:write"))
private val inventoryViewRule = RouteAccessRule(allOf = setOf("inventory:view"))
private val inventoryWriteRule = RouteAccessRule(allOf = setOf("inventory:write"))
private val reportsViewRule = RouteAccessRule(allOf = setOf("reports:view"))
private val agentViewRule = RouteAccessRule(allOf = setOf("agent:view"))
private val usersManageRule = RouteAccessRule(allOf = setOf("users:manage"))
private val salesViewRule = RouteAccessRule(allOf = setOf("sales:view"))
private val salesWriteRule = RouteAccessRule(allOf = setOf("sales:write"))
private val purchaseViewRule = RouteAccessRule(allOf = setOf("purchase:view"))
private val purchaseWriteRule = RouteAccessRule(allOf = setOf("purchase:write"))
private val financeViewRule = RouteAccessRule(allOf = setOf("finance:view"))
private val financeWriteRule = RouteAccessRule(allOf = setOf("finance:write"))
private val salesPaymentRule = RouteAccessRule(allOf = setOf("sales:write", "finance:write"))
private val topLevelRouteOrder = listOf(
    TabRoutes.HOME,
    TabRoutes.DOCUMENTS,
    TabRoutes.ARCHIVES,
    TabRoutes.REPORTS,
    TabRoutes.AGENT,
)

fun MainAccessUiState.canAccessRule(rule: RouteAccessRule): Boolean {
    if (!isResolved) return false
    if (!permissions.containsAll(rule.allOf)) return false
    if (rule.anyOf.isNotEmpty() && rule.anyOf.none(permissions::contains)) return false
    return true
}

fun MainAccessUiState.canAccessRoute(route: String?): Boolean =
    canAccessRule(routeAccessRule(route))

fun MainAccessUiState.hasPermission(permission: String): Boolean =
    isResolved && permission in permissions

fun MainAccessUiState.hasAnyPermission(vararg permission: String): Boolean =
    isResolved && (permission.isEmpty() || permission.any(permissions::contains))

fun MainAccessUiState.firstAllowedTopLevelRoute(): String =
    topLevelRouteOrder.firstOrNull { canAccessRoute(it) }
        ?: TabRoutes.HOME

fun routeAccessRule(route: String?): RouteAccessRule {
    val normalized = route.orEmpty().substringBefore("?")
    return when {
        normalized == TabRoutes.HOME -> dashboardViewRule
        normalized == TabRoutes.DOCUMENTS -> documentsViewRule
        normalized == TabRoutes.ARCHIVES -> archivesViewRule
        normalized == TabRoutes.REPORTS -> reportsViewRule
        normalized == TabRoutes.AGENT -> agentViewRule
        normalized == MainRoutes.STAFF_MANAGEMENT -> usersManageRule

        normalized == DetailRoutes.PRODUCT_DETAIL || normalized.startsWith("product_detail/") ->
            archivesViewRule
        normalized == DetailRoutes.PRODUCT_EDIT || normalized.startsWith("product_edit/") || normalized == DetailRoutes.PRODUCT_CREATE ->
            archivesWriteRule
        normalized == DetailRoutes.STOCK_ADJUST || normalized.startsWith("stock_adjust/") ->
            inventoryWriteRule
        normalized == DetailRoutes.INVENTORY_LEDGER || normalized.startsWith("inventory_ledger/") || normalized == DetailRoutes.INVENTORY_SNAPSHOT ->
            inventoryViewRule

        normalized == DetailRoutes.CUSTOMER_DETAIL || normalized.startsWith("customer_detail/") ->
            archivesViewRule
        normalized == DetailRoutes.CUSTOMER_EDIT || normalized.startsWith("customer_edit/") || normalized == DetailRoutes.CUSTOMER_CREATE ->
            archivesWriteRule

        normalized == DetailRoutes.SUPPLIER_DETAIL || normalized.startsWith("supplier_detail/") ->
            archivesViewRule
        normalized == DetailRoutes.SUPPLIER_EDIT || normalized.startsWith("supplier_edit/") || normalized == DetailRoutes.SUPPLIER_CREATE ->
            archivesWriteRule
        normalized == DetailRoutes.SUPPLIER_STATEMENT || normalized.startsWith("supplier_statement/") ->
            financeViewRule

        normalized == DetailRoutes.SALE_ORDER_DETAIL || normalized.startsWith("sale_order_detail/") ->
            salesViewRule
        normalized == DetailRoutes.SALE_ORDER_EDIT || normalized.startsWith("sale_order_edit/") || normalized == DetailRoutes.SALE_ORDER_CREATE ->
            salesWriteRule
        normalized == DetailRoutes.PAYMENT || normalized.startsWith("payment/") ->
            salesPaymentRule
        normalized == DetailRoutes.SALES_RETURNS ->
            salesViewRule

        normalized == DetailRoutes.PURCHASE_ORDER_DETAIL || normalized.startsWith("purchase_order_detail/") ->
            purchaseViewRule
        normalized == DetailRoutes.PURCHASE_ORDER_EDIT || normalized.startsWith("purchase_order_edit/") || normalized == DetailRoutes.PURCHASE_ORDER_CREATE ->
            purchaseWriteRule
        normalized == DetailRoutes.PURCHASE_RECEIPTS ->
            inventoryViewRule
        normalized == DetailRoutes.PURCHASE_RETURNS ->
            purchaseViewRule

        normalized == DetailRoutes.PAY_ORDER_DETAIL || normalized.startsWith("pay_order_detail/") ->
            financeViewRule
        normalized == DetailRoutes.FINANCE_RECORD_DETAIL || normalized.startsWith("finance_record_detail/") ->
            financeViewRule
        normalized == DetailRoutes.DAILY_EXPENSE ->
            financeWriteRule

        normalized == DetailRoutes.DRAFT_LIST || normalized == DetailRoutes.TASK_NOTIFICATION ->
            agentViewRule
        normalized == "agent_chat" || normalized == DetailRoutes.AGENT_CHAT.substringBefore("?") ->
            agentViewRule
        else -> unrestrictedAccessRule
    }
}
