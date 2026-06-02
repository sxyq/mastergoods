package com.zhihuiji.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zhihuiji.feature.customers.CustomerDetailScreen
import com.zhihuiji.feature.customers.CustomerEditorScreen
import com.zhihuiji.feature.payments.PayOrderDetailScreen
import com.zhihuiji.feature.payments.PayOrderEditorScreen
import com.zhihuiji.feature.products.ProductEditorScreen
import com.zhihuiji.feature.purchases.PurchaseOrderDetailScreen
import com.zhihuiji.feature.purchases.PurchaseOrderEditorScreen
import com.zhihuiji.feature.sales.SaleOrderDetailScreen
import com.zhihuiji.feature.sales.SaleOrderEditorScreen
import com.zhihuiji.feature.suppliers.SupplierDetailScreen
import com.zhihuiji.feature.suppliers.SupplierEditorScreen

fun NavGraphBuilder.productEditorRoute(navigateBack: () -> Unit) {
    composable(
        route = "${SubRoutes.PRODUCT_EDITOR}?productId={productId}",
        arguments = listOf(navArgument("productId") { type = NavType.LongType; defaultValue = 0L }),
    ) { backStackEntry ->
        val rawId = backStackEntry.arguments?.getLong("productId") ?: 0L
        val productId = toNullableId(rawId)
        ProductEditorScreen(
            productId = productId,
            onNavigateBack = { navigateBack() },
        )
    }
}

fun NavGraphBuilder.customerRoutes(navigateBack: () -> Unit, navigateToEditor: (Long) -> Unit) {
    composable(
        route = "${SubRoutes.CUSTOMER_EDITOR}?customerId={customerId}",
        arguments = listOf(navArgument("customerId") { type = NavType.LongType; defaultValue = 0L }),
    ) { backStackEntry ->
        val rawId = backStackEntry.arguments?.getLong("customerId") ?: 0L
        val customerId = toNullableId(rawId)
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
            onNavigateToEditor = { id -> navigateToEditor(id) },
        )
    }
}

fun NavGraphBuilder.supplierRoutes(navigateBack: () -> Unit, navigateToEditor: (Long) -> Unit) {
    composable(
        route = "${SubRoutes.SUPPLIER_EDITOR}?supplierId={supplierId}",
        arguments = listOf(navArgument("supplierId") { type = NavType.LongType; defaultValue = 0L }),
    ) { backStackEntry ->
        val rawId = backStackEntry.arguments?.getLong("supplierId") ?: 0L
        val supplierId = toNullableId(rawId)
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
            onNavigateToEditor = { id -> navigateToEditor(id) },
        )
    }
}

fun NavGraphBuilder.saleOrderRoutes(
    navigateBack: () -> Unit,
    navigateToEditor: (Long?) -> Unit,
) {
    composable(
        route = "${SubRoutes.SALE_ORDER_EDITOR}?orderId={orderId}",
        arguments = listOf(navArgument("orderId") { type = NavType.LongType; defaultValue = 0L }),
    ) { backStackEntry ->
        val rawId = backStackEntry.arguments?.getLong("orderId") ?: 0L
        val orderId = toNullableId(rawId)
        SaleOrderEditorScreen(
            orderId = orderId,
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
            onNavigateToEdit = { navigateToEditor(orderId) },
        )
    }
}

fun NavGraphBuilder.purchaseOrderRoutes(navigateBack: () -> Unit) {
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
}

fun NavGraphBuilder.payOrderRoutes(navigateBack: () -> Unit) {
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
