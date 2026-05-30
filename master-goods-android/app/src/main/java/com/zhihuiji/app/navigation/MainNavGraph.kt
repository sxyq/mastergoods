package com.zhihuiji.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zhihuiji.feature.dashboard.DashboardScreen
import com.zhihuiji.feature.reports.ReportScreen
import com.zhihuiji.feature.agent.AgentWorkbenchScreen

object SubRoutes {
    const val PRODUCT_EDITOR = "product_editor"
    const val CUSTOMER_EDITOR = "customer_editor"
    const val CUSTOMER_DETAIL = "customer_detail"
    const val SUPPLIER_EDITOR = "supplier_editor"
    const val SUPPLIER_DETAIL = "supplier_detail"
    const val SALE_ORDER_EDITOR = "sale_order_editor"
    const val SALE_ORDER_DETAIL = "sale_order_detail"
    const val PURCHASE_ORDER_EDITOR = "purchase_order_editor"
    const val PURCHASE_ORDER_DETAIL = "purchase_order_detail"
    const val PAY_ORDER_EDITOR = "pay_order_editor"
    const val PAY_ORDER_DETAIL = "pay_order_detail"
}

internal fun toNullableId(rawId: Long): Long? = if (rawId > 0) rawId else null

@Composable
fun MainNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit,
    reselectSignal: (String) -> Int = { 0 },
) {
    fun navigateBack() { navController.popBackStack() }

    NavHost(
        navController = navController,
        startDestination = TopLevelRoutes.HOME,
        modifier = modifier,
    ) {
        composable(TopLevelRoutes.HOME) {
            DashboardScreen(
                onNavigateToSettings = onNavigateToSettings,
                showTopBar = false,
                reselectSignal = reselectSignal(TopLevelRoutes.HOME),
                onNavigateToSales = {
                    navController.navigate("${TopLevelRoutes.DOCUMENTS}?initialTab=0") { launchSingleTop = true }
                },
                onNavigateToProducts = {
                    navController.navigate("${TopLevelRoutes.ARCHIVES}?initialTab=0") { launchSingleTop = true }
                },
                onNavigateToCustomers = {
                    navController.navigate("${TopLevelRoutes.ARCHIVES}?initialTab=1") { launchSingleTop = true }
                },
                onNavigateToAgent = {
                    navController.navigate(TopLevelRoutes.AGENT) { launchSingleTop = true }
                },
            )
        }
        composable(
            route = "${TopLevelRoutes.DOCUMENTS}?initialTab={initialTab}",
            arguments = listOf(navArgument("initialTab") { type = NavType.IntType; defaultValue = 0 }),
        ) { backStackEntry ->
            val initialTab = backStackEntry.arguments?.getInt("initialTab") ?: 0
            DocumentsScreen(
                initialTab = initialTab,
                reselectSignal = reselectSignal(TopLevelRoutes.DOCUMENTS),
                onNavigateToSaleOrderEditor = {
                    navController.navigate(SubRoutes.SALE_ORDER_EDITOR) { launchSingleTop = true }
                },
                onNavigateToSaleOrderDetail = { orderId ->
                    navController.navigate("${SubRoutes.SALE_ORDER_DETAIL}/$orderId") { launchSingleTop = true }
                },
                onNavigateToPurchaseOrderEditor = {
                    navController.navigate(SubRoutes.PURCHASE_ORDER_EDITOR) { launchSingleTop = true }
                },
                onNavigateToPurchaseOrderDetail = { orderId ->
                    navController.navigate("${SubRoutes.PURCHASE_ORDER_DETAIL}/$orderId") { launchSingleTop = true }
                },
                onNavigateToPayOrderEditor = {
                    navController.navigate(SubRoutes.PAY_ORDER_EDITOR) { launchSingleTop = true }
                },
                onNavigateToPayOrderDetail = { orderId ->
                    navController.navigate("${SubRoutes.PAY_ORDER_DETAIL}/$orderId") { launchSingleTop = true }
                },
            )
        }
        composable(
            route = "${TopLevelRoutes.ARCHIVES}?initialTab={initialTab}",
            arguments = listOf(navArgument("initialTab") { type = NavType.IntType; defaultValue = 0 }),
        ) { backStackEntry ->
            val initialTab = backStackEntry.arguments?.getInt("initialTab") ?: 0
            ArchivesScreen(
                initialTab = initialTab,
                reselectSignal = reselectSignal(TopLevelRoutes.ARCHIVES),
                onNavigateToProductEditor = { productId ->
                    val route = if (productId != null) "${SubRoutes.PRODUCT_EDITOR}?productId=$productId" else SubRoutes.PRODUCT_EDITOR
                    navController.navigate(route) { launchSingleTop = true }
                },
                onNavigateToCustomerEditor = { customerId ->
                    val route = if (customerId != null) "${SubRoutes.CUSTOMER_EDITOR}?customerId=$customerId" else SubRoutes.CUSTOMER_EDITOR
                    navController.navigate(route) { launchSingleTop = true }
                },
                onNavigateToCustomerDetail = { customerId ->
                    navController.navigate("${SubRoutes.CUSTOMER_DETAIL}/$customerId") { launchSingleTop = true }
                },
                onNavigateToSupplierEditor = { supplierId ->
                    val route = if (supplierId != null) "${SubRoutes.SUPPLIER_EDITOR}?supplierId=$supplierId" else SubRoutes.SUPPLIER_EDITOR
                    navController.navigate(route) { launchSingleTop = true }
                },
                onNavigateToSupplierDetail = { supplierId ->
                    navController.navigate("${SubRoutes.SUPPLIER_DETAIL}/$supplierId") { launchSingleTop = true }
                },
            )
        }
        composable(TopLevelRoutes.REPORTS) {
            ReportScreen(onNavigateBack = {}, showTopBar = false, reselectSignal = reselectSignal(TopLevelRoutes.REPORTS))
        }
        composable(TopLevelRoutes.AGENT) {
            AgentWorkbenchScreen(onNavigateBack = {}, showTopBar = false, reselectSignal = reselectSignal(TopLevelRoutes.AGENT))
        }

        productEditorRoute(::navigateBack)
        customerRoutes(
            navigateBack = ::navigateBack,
            navigateToEditor = { id ->
                navController.navigate("${SubRoutes.CUSTOMER_EDITOR}?customerId=$id") { launchSingleTop = true }
            },
        )
        supplierRoutes(
            navigateBack = ::navigateBack,
            navigateToEditor = { id ->
                navController.navigate("${SubRoutes.SUPPLIER_EDITOR}?supplierId=$id") { launchSingleTop = true }
            },
        )
        saleOrderRoutes(::navigateBack)
        purchaseOrderRoutes(::navigateBack)
        payOrderRoutes(::navigateBack)
    }
}
