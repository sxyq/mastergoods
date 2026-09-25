package com.zhihuiji.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.CurrentStoreProfile
import com.zhihuiji.data.auth.AuthRepository
import com.zhihuiji.core.datastore.SyncPreferenceStore
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
    private val syncPreferenceStore: SyncPreferenceStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        MainAccessUiState(
            isLoading = false,
            isResolved = true,
            permissions = syncPreferenceStore.peekPermissions(),
        ),
    )
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
                    syncPreferenceStore.savePermissions(permissions)
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
                            // Keep the last local permission snapshot so offline navigation remains usable.
                            permissions = it.permissions,
                            error = null,
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
private val agentViewRule = RouteAccessRule(allOf = setOf("agent:view"))
private val usersManageRule = RouteAccessRule(allOf = setOf("users:manage"))
private val topLevelRouteOrder = listOf(
    TabRoutes.HOME,
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
        normalized == TabRoutes.HOME -> unrestrictedAccessRule
        normalized == TabRoutes.AGENT -> agentViewRule
        normalized == MainRoutes.STAFF_MANAGEMENT -> usersManageRule

        normalized == DetailRoutes.DRAFT_LIST || normalized == DetailRoutes.TASK_NOTIFICATION ->
            agentViewRule
        normalized == "agent_chat" || normalized.startsWith("agent_chat/") ->
            agentViewRule
        else -> unrestrictedAccessRule
    }
}
