package com.zhihuiji.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zhihuiji.feature.customers.CustomerDetailScreen
import com.zhihuiji.feature.customers.CustomerEditorScreen
import com.zhihuiji.feature.dashboard.DashboardScreen
import com.zhihuiji.feature.payments.PayOrderDetailScreen
import com.zhihuiji.feature.payments.PayOrderEditorScreen
import com.zhihuiji.feature.products.ProductEditorScreen
import com.zhihuiji.feature.purchases.PurchaseOrderDetailScreen
import com.zhihuiji.feature.purchases.PurchaseOrderEditorScreen
import com.zhihuiji.feature.reports.ReportScreen
import com.zhihuiji.feature.sales.SaleOrderDetailScreen
import com.zhihuiji.feature.sales.SaleOrderEditorScreen
import com.zhihuiji.feature.suppliers.SupplierDetailScreen
import com.zhihuiji.feature.suppliers.SupplierEditorScreen
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

@Composable
fun MainNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit,
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
            ReportScreen(onNavigateBack = {}, showTopBar = false)
        }
        composable(TopLevelRoutes.AGENT) {
            AgentWorkbenchScreen(onNavigateBack = {}, showTopBar = false)
        }

        composable(
            route = "${SubRoutes.PRODUCT_EDITOR}?productId={productId}",
            arguments = listOf(navArgument("productId") { type = NavType.LongType; defaultValue = 0L }),
        ) { backStackEntry ->
            val rawId = backStackEntry.arguments?.getLong("productId") ?: 0L
            val productId = if (rawId > 0) rawId else null
            ProductEditorScreen(
                productId = productId,
                onNavigateBack = { navigateBack() },
            )
        }
        composable(
            route = "${SubRoutes.CUSTOMER_EDITOR}?customerId={customerId}",
            arguments = listOf(navArgument("customerId") { type = NavType.LongType; defaultValue = 0L }),
        ) { backStackEntry ->
            val rawId = backStackEntry.arguments?.getLong("customerId") ?: 0L
            val customerId = if (rawId > 0) rawId else null
            CustomerEditorScreen(
                customerId = customerId,
                onNavigateBack = { navigateBack() },
            )
        }
        composable(
            route = "${SubRoutes.CUSTOMER_DETAIL}/{customerId}",
            arguments = listOf(navArgument("customerId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getLong("customerId") ?: return@composable
            CustomerDetailScreen(
                customerId = customerId,
                onNavigateBack = { navigateBack() },
                onNavigateToEditor = { id ->
                    navController.navigate("${SubRoutes.CUSTOMER_EDITOR}?customerId=$id") { launchSingleTop = true }
                },
            )
        }
        composable(
            route = "${SubRoutes.SUPPLIER_EDITOR}?supplierId={supplierId}",
            arguments = listOf(navArgument("supplierId") { type = NavType.LongType; defaultValue = 0L }),
        ) { backStackEntry ->
            val rawId = backStackEntry.arguments?.getLong("supplierId") ?: 0L
            val supplierId = if (rawId > 0) rawId else null
            SupplierEditorScreen(
                supplierId = supplierId,
                onNavigateBack = { navigateBack() },
            )
        }
        composable(
            route = "${SubRoutes.SUPPLIER_DETAIL}/{supplierId}",
            arguments = listOf(navArgument("supplierId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val supplierId = backStackEntry.arguments?.getLong("supplierId") ?: return@composable
            SupplierDetailScreen(
                supplierId = supplierId,
                onNavigateBack = { navigateBack() },
                onNavigateToEditor = { id ->
                    navController.navigate("${SubRoutes.SUPPLIER_EDITOR}?supplierId=$id") { launchSingleTop = true }
                },
            )
        }
        composable(SubRoutes.SALE_ORDER_EDITOR) {
            SaleOrderEditorScreen(
                onNavigateBack = { navigateBack() },
            )
        }
        composable(
            route = "${SubRoutes.SALE_ORDER_DETAIL}/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: return@composable
            SaleOrderDetailScreen(
                orderId = orderId,
                onNavigateBack = { navigateBack() },
            )
        }
        composable(SubRoutes.PURCHASE_ORDER_EDITOR) {
            PurchaseOrderEditorScreen(
                onNavigateBack = { navigateBack() },
            )
        }
        composable(
            route = "${SubRoutes.PURCHASE_ORDER_DETAIL}/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: return@composable
            PurchaseOrderDetailScreen(
                orderId = orderId,
                onNavigateBack = { navigateBack() },
            )
        }
        composable(SubRoutes.PAY_ORDER_EDITOR) {
            PayOrderEditorScreen(
                onNavigateBack = { navigateBack() },
            )
        }
        composable(
            route = "${SubRoutes.PAY_ORDER_DETAIL}/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: return@composable
            PayOrderDetailScreen(
                orderId = orderId,
                onNavigateBack = { navigateBack() },
            )
        }
    }
}
