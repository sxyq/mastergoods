package com.zhihuiji.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zhihuiji.feature.agent.AgentChatScreen
import com.zhihuiji.feature.agent.AgentWorkbenchScreen
import com.zhihuiji.feature.agent.DraftListScreen
import com.zhihuiji.feature.agent.TaskNotificationScreen
import com.zhihuiji.feature.customers.CustomerDetailScreen
import com.zhihuiji.feature.customers.CustomerEditScreen
import com.zhihuiji.feature.dashboard.DashboardScreen
import com.zhihuiji.feature.finance.DailyExpenseScreen
import com.zhihuiji.feature.finance.FinanceRecordDetailScreen
import com.zhihuiji.feature.payments.PayOrderDetailScreen
import com.zhihuiji.feature.products.InventoryLedgerScreen
import com.zhihuiji.feature.products.InventorySnapshotScreen
import com.zhihuiji.feature.products.ProductDetailScreen
import com.zhihuiji.feature.products.ProductEditScreen
import com.zhihuiji.feature.products.StockAdjustScreen
import com.zhihuiji.feature.purchases.PurchaseOrderDetailScreen
import com.zhihuiji.feature.purchases.PurchaseOrderEditScreen
import com.zhihuiji.feature.purchases.PurchaseReceiptScreen
import com.zhihuiji.feature.purchases.PurchaseReturnScreen
import com.zhihuiji.feature.reports.ReportScreen
import com.zhihuiji.feature.sales.SaleOrderDetailScreen
import com.zhihuiji.feature.sales.SaleOrderEditScreen
import com.zhihuiji.feature.sales.PaymentScreen
import com.zhihuiji.feature.sales.SalesReturnScreen
import com.zhihuiji.feature.suppliers.SupplierDetailScreen
import com.zhihuiji.feature.suppliers.SupplierEditScreen
import com.zhihuiji.feature.suppliers.SupplierStatementScreen
import kotlinx.coroutines.flow.Flow

object TabRoutes {
    const val HOME = "home"
    const val DOCUMENTS = "documents"
    const val ARCHIVES = "archives"
    const val REPORTS = "reports"
    const val AGENT = "agent"
}

object DetailRoutes {
    const val PRODUCT_DETAIL = "product_detail/{productId}"
    const val PRODUCT_EDIT = "product_edit/{productId}"
    const val PRODUCT_CREATE = "product_create"
    const val STOCK_ADJUST = "stock_adjust/{productId}"
    const val INVENTORY_LEDGER = "inventory_ledger/{productId}"
    const val INVENTORY_SNAPSHOT = "inventory_snapshot"

    const val CUSTOMER_DETAIL = "customer_detail/{customerId}"
    const val CUSTOMER_EDIT = "customer_edit/{customerId}"
    const val CUSTOMER_CREATE = "customer_create"

    const val SUPPLIER_DETAIL = "supplier_detail/{supplierId}"
    const val SUPPLIER_EDIT = "supplier_edit/{supplierId}"
    const val SUPPLIER_CREATE = "supplier_create"
    const val SUPPLIER_STATEMENT = "supplier_statement/{supplierId}"

    const val SALE_ORDER_DETAIL = "sale_order_detail/{orderId}"
    const val SALE_ORDER_EDIT = "sale_order_edit/{orderId}"
    const val SALE_ORDER_CREATE = "sale_order_create"
    const val SALES_RETURNS = "sales_returns"
    const val PAYMENT = "payment/{orderId}"

    const val PURCHASE_ORDER_DETAIL = "purchase_order_detail/{orderId}"
    const val PURCHASE_ORDER_EDIT = "purchase_order_edit/{orderId}"
    const val PURCHASE_ORDER_CREATE = "purchase_order_create"
    const val PURCHASE_RECEIPTS = "purchase_receipts"
    const val PURCHASE_RETURNS = "purchase_returns"

    const val PAY_ORDER_DETAIL = "pay_order_detail/{orderId}"
    const val FINANCE_RECORD_DETAIL = "finance_record_detail/{recordId}"
    const val DAILY_EXPENSE = "daily_expense"

    const val DRAFT_LIST = "draft_list"
    const val TASK_NOTIFICATION = "task_notification"
    const val AGENT_CHAT = "agent_chat?initialQuestion={initialQuestion}&conversationId={conversationId}"
}

fun productDetailRoute(productId: Long) = "product_detail/$productId"
fun productEditRoute(productId: Long) = "product_edit/$productId"
fun stockAdjustRoute(productId: Long) = "stock_adjust/$productId"
fun inventoryLedgerRoute(productId: Long) = "inventory_ledger/$productId"
fun customerDetailRoute(customerId: Long) = "customer_detail/$customerId"
fun customerEditRoute(customerId: Long) = "customer_edit/$customerId"
fun supplierDetailRoute(supplierId: Long) = "supplier_detail/$supplierId"
fun supplierEditRoute(supplierId: Long) = "supplier_edit/$supplierId"
fun supplierStatementRoute(supplierId: Long) = "supplier_statement/$supplierId"
fun saleOrderDetailRoute(orderId: Long) = "sale_order_detail/$orderId"
fun saleOrderEditRoute(orderId: Long) = "sale_order_edit/$orderId"
fun paymentRoute(orderId: Long) = "payment/$orderId"
fun purchaseOrderDetailRoute(orderId: Long) = "purchase_order_detail/$orderId"
fun purchaseOrderEditRoute(orderId: Long) = "purchase_order_edit/$orderId"
fun payOrderDetailRoute(orderId: Long) = "pay_order_detail/$orderId"
fun financeRecordDetailRoute(recordId: Long) = "finance_record_detail/$recordId"
fun taskNotificationRoute(initialTab: Int = 0) = "${DetailRoutes.TASK_NOTIFICATION}?initialTab=$initialTab"
fun agentChatRoute(initialQuestion: String? = null, conversationId: Long? = null): String {
    val encodedQuestion = java.net.URLEncoder.encode(initialQuestion ?: "", Charsets.UTF_8.name())
    return "agent_chat?initialQuestion=$encodedQuestion&conversationId=${conversationId ?: -1L}"
}

@Composable
fun MainNavGraph(
    navController: NavHostController,
    selectedIndex: Int,
    homeBottomBarScrollEvents: Flow<Float>,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = TabRoutes.HOME
        ) {
            // 顶级页面
            composable(TabRoutes.HOME) {
                DashboardScreen(
                    bottomBarScrollEvents = homeBottomBarScrollEvents,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToSales = {
                        navController.navigate("${TabRoutes.DOCUMENTS}?initialTab=0") { launchSingleTop = true }
                    },
                    onNavigateToProducts = {
                        navController.navigate("${TabRoutes.ARCHIVES}?initialTab=0") { launchSingleTop = true }
                    },
                    onNavigateToCustomers = {
                        navController.navigate("${TabRoutes.ARCHIVES}?initialTab=1") { launchSingleTop = true }
                    },
                    onNavigateToAgent = {
                        navController.navigate(TabRoutes.AGENT) { launchSingleTop = true }
                    },
                    onNavigateToNotifications = {
                        navController.navigate(taskNotificationRoute(initialTab = 1)) { launchSingleTop = true }
                    },
                )
            }
            composable(
                route = "${TabRoutes.DOCUMENTS}?initialTab={initialTab}",
                arguments = listOf(navArgument("initialTab") { type = NavType.IntType; defaultValue = 0 })
            ) { backStackEntry ->
                DocumentsScreen(
                    initialTab = backStackEntry.arguments?.getInt("initialTab") ?: 0,
                    onNavigateToSaleOrderDetail = { navController.navigate(saleOrderDetailRoute(it)) },
                    onNavigateToSaleOrderCreate = { navController.navigate(DetailRoutes.SALE_ORDER_CREATE) },
                    onNavigateToSalesReturns = { navController.navigate(DetailRoutes.SALES_RETURNS) },
                    onNavigateToPurchaseOrderDetail = { navController.navigate(purchaseOrderDetailRoute(it)) },
                    onNavigateToPurchaseOrderCreate = { navController.navigate(DetailRoutes.PURCHASE_ORDER_CREATE) },
                    onNavigateToPurchaseReceipts = { navController.navigate(DetailRoutes.PURCHASE_RECEIPTS) },
                    onNavigateToPurchaseReturns = { navController.navigate(DetailRoutes.PURCHASE_RETURNS) },
                    onNavigateToPayOrderDetail = { navController.navigate(payOrderDetailRoute(it)) },
                    onNavigateToFinanceRecordDetail = { navController.navigate(financeRecordDetailRoute(it)) },
                    onNavigateToDailyExpense = { navController.navigate(DetailRoutes.DAILY_EXPENSE) },
                    onNavigateToInventorySnapshot = { navController.navigate(DetailRoutes.INVENTORY_SNAPSHOT) },
                )
            }
            composable(
                route = "${TabRoutes.ARCHIVES}?initialTab={initialTab}",
                arguments = listOf(navArgument("initialTab") { type = NavType.IntType; defaultValue = 0 })
            ) { backStackEntry ->
                ArchivesScreen(
                    initialTab = backStackEntry.arguments?.getInt("initialTab") ?: 0,
                    onNavigateToProductDetail = { navController.navigate(productDetailRoute(it)) },
                    onNavigateToProductCreate = { navController.navigate(DetailRoutes.PRODUCT_CREATE) },
                    onNavigateToCustomerDetail = { navController.navigate(customerDetailRoute(it)) },
                    onNavigateToCustomerCreate = { navController.navigate(DetailRoutes.CUSTOMER_CREATE) },
                    onNavigateToSupplierDetail = { navController.navigate(supplierDetailRoute(it)) },
                    onNavigateToSupplierCreate = { navController.navigate(DetailRoutes.SUPPLIER_CREATE) },
                )
            }
            composable(TabRoutes.REPORTS) {
                ReportScreen()
            }
            composable(TabRoutes.AGENT) {
                AgentWorkbenchScreen(
                    onNavigateToChat = { question ->
                        navController.navigate(agentChatRoute(question))
                    },
                    onNavigateToTasks = { navController.navigate(taskNotificationRoute()) },
                )
            }

            // 商品详情/编辑
            composable(
                route = DetailRoutes.PRODUCT_DETAIL,
                arguments = listOf(navArgument("productId") { type = NavType.LongType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getLong("productId") ?: 0L
                ProductDetailScreen(
                    productId = productId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { navController.navigate(productEditRoute(it)) },
                    onNavigateToStockAdjust = { navController.navigate(stockAdjustRoute(it)) },
                    onNavigateToInventoryLedger = { navController.navigate(inventoryLedgerRoute(it)) }
                )
            }
            composable(
                route = DetailRoutes.PRODUCT_EDIT,
                arguments = listOf(navArgument("productId") { type = NavType.LongType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getLong("productId")
                ProductEditScreen(
                    productId = productId,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }
            composable(DetailRoutes.PRODUCT_CREATE) {
                ProductEditScreen(
                    productId = null,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }
            composable(
                route = DetailRoutes.STOCK_ADJUST,
                arguments = listOf(navArgument("productId") { type = NavType.LongType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getLong("productId") ?: 0L
                StockAdjustScreen(
                    productId = productId,
                    onNavigateBack = { navController.popBackStack() },
                    onAdjustSuccess = { navController.popBackStack() }
                )
            }
            composable(
                route = DetailRoutes.INVENTORY_LEDGER,
                arguments = listOf(navArgument("productId") { type = NavType.LongType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getLong("productId") ?: 0L
                InventoryLedgerScreen(
                    productId = productId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(DetailRoutes.INVENTORY_SNAPSHOT) {
                InventorySnapshotScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 客户详情/编辑
            composable(
                route = DetailRoutes.CUSTOMER_DETAIL,
                arguments = listOf(navArgument("customerId") { type = NavType.LongType })
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
                CustomerDetailScreen(
                    customerId = customerId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { navController.navigate(customerEditRoute(it)) }
                )
            }
            composable(
                route = DetailRoutes.CUSTOMER_EDIT,
                arguments = listOf(navArgument("customerId") { type = NavType.LongType })
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getLong("customerId")
                CustomerEditScreen(
                    customerId = customerId,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }
            composable(DetailRoutes.CUSTOMER_CREATE) {
                CustomerEditScreen(
                    customerId = null,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }

            // 供应商详情/编辑
            composable(
                route = DetailRoutes.SUPPLIER_DETAIL,
                arguments = listOf(navArgument("supplierId") { type = NavType.LongType })
            ) { backStackEntry ->
                val supplierId = backStackEntry.arguments?.getLong("supplierId") ?: 0L
                SupplierDetailScreen(
                    supplierId = supplierId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { navController.navigate(supplierEditRoute(it)) },
                    onNavigateToStatement = { navController.navigate(supplierStatementRoute(it)) }
                )
            }
            composable(
                route = DetailRoutes.SUPPLIER_STATEMENT,
                arguments = listOf(navArgument("supplierId") { type = NavType.LongType })
            ) { backStackEntry ->
                val supplierId = backStackEntry.arguments?.getLong("supplierId") ?: 0L
                SupplierStatementScreen(
                    supplierId = supplierId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = DetailRoutes.SUPPLIER_EDIT,
                arguments = listOf(navArgument("supplierId") { type = NavType.LongType })
            ) { backStackEntry ->
                val supplierId = backStackEntry.arguments?.getLong("supplierId")
                SupplierEditScreen(
                    supplierId = supplierId,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }
            composable(DetailRoutes.SUPPLIER_CREATE) {
                SupplierEditScreen(
                    supplierId = null,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }

            // 销售单详情/编辑/收款
            composable(
                route = DetailRoutes.SALE_ORDER_DETAIL,
                arguments = listOf(navArgument("orderId") { type = NavType.LongType })
            ) {
                SaleOrderDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onEditClick = { navController.navigate(saleOrderEditRoute(it)) },
                    onPaymentClick = { navController.navigate(paymentRoute(it)) }
                )
            }
            composable(
                route = DetailRoutes.SALE_ORDER_EDIT,
                arguments = listOf(navArgument("orderId") { type = NavType.LongType })
            ) {
                SaleOrderEditScreen(
                    onBackClick = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }
            composable(DetailRoutes.SALE_ORDER_CREATE) {
                SaleOrderEditScreen(
                    onBackClick = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }
            composable(
                route = DetailRoutes.PAYMENT,
                arguments = listOf(navArgument("orderId") { type = NavType.LongType })
            ) {
                PaymentScreen(
                    onBackClick = { navController.popBackStack() },
                    onPaySuccess = { navController.popBackStack() }
                )
            }
            composable(DetailRoutes.SALES_RETURNS) {
                SalesReturnScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 采购单详情/编辑
            composable(
                route = DetailRoutes.PURCHASE_ORDER_DETAIL,
                arguments = listOf(navArgument("orderId") { type = NavType.LongType })
            ) {
                PurchaseOrderDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onEditClick = { navController.navigate(purchaseOrderEditRoute(it)) },
                    onDeleteSuccess = { navController.popBackStack() }
                )
            }
            composable(
                route = DetailRoutes.PURCHASE_ORDER_EDIT,
                arguments = listOf(navArgument("orderId") { type = NavType.LongType })
            ) {
                PurchaseOrderEditScreen(
                    onBackClick = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }
            composable(DetailRoutes.PURCHASE_ORDER_CREATE) {
                PurchaseOrderEditScreen(
                    onBackClick = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }
            composable(DetailRoutes.PURCHASE_RECEIPTS) {
                PurchaseReceiptScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(DetailRoutes.PURCHASE_RETURNS) {
                PurchaseReturnScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 付款单详情
            composable(
                route = DetailRoutes.PAY_ORDER_DETAIL,
                arguments = listOf(navArgument("orderId") { type = NavType.LongType })
            ) {
                PayOrderDetailScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // 资金流水详情
            composable(
                route = DetailRoutes.FINANCE_RECORD_DETAIL,
                arguments = listOf(navArgument("recordId") { type = NavType.LongType })
            ) { backStackEntry ->
                val recordId = backStackEntry.arguments?.getLong("recordId") ?: 0L
                FinanceRecordDetailScreen(
                    recordId = recordId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 日常支出
            composable(DetailRoutes.DAILY_EXPENSE) {
                DailyExpenseScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onExpenseCreated = { recordId ->
                        navController.navigate(financeRecordDetailRoute(recordId)) {
                            popUpTo(DetailRoutes.DAILY_EXPENSE) {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            // 草稿列表
            composable(DetailRoutes.DRAFT_LIST) {
                DraftListScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // 任务与通知
            composable(
                route = "${DetailRoutes.TASK_NOTIFICATION}?initialTab={initialTab}",
                arguments = listOf(navArgument("initialTab") { type = NavType.IntType; defaultValue = 0 })
            ) { backStackEntry ->
                TaskNotificationScreen(
                    onBackClick = { navController.popBackStack() },
                    initialTab = backStackEntry.arguments?.getInt("initialTab") ?: 0,
                )
            }

            // AI 聊天页
            composable(
                route = DetailRoutes.AGENT_CHAT,
                arguments = listOf(
                    navArgument("initialQuestion") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("conversationId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStackEntry ->
                val initialQuestion = backStackEntry.arguments?.getString("initialQuestion")?.takeIf { it.isNotBlank() }
                val conversationId = backStackEntry.arguments?.getLong("conversationId")?.takeIf { it > 0 }
                AgentChatScreen(
                    initialQuestion = initialQuestion,
                    conversationId = conversationId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
