package com.zhihuiji.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zhihuiji.feature.agent.AgentChatScreen
import com.zhihuiji.feature.agent.AgentWorkbenchScreen
import com.zhihuiji.feature.agent.DraftListScreen
import com.zhihuiji.feature.agent.TaskNotificationScreen
import com.zhihuiji.feature.customers.ContactEditScreen
import com.zhihuiji.feature.customers.CustomerContactListScreen
import com.zhihuiji.feature.customers.CustomerDetailScreen
import com.zhihuiji.feature.customers.CustomerEditScreen
import com.zhihuiji.feature.dashboard.DashboardScreen
import com.zhihuiji.feature.finance.AccountEditScreen
import com.zhihuiji.feature.finance.AccountListScreen
import com.zhihuiji.feature.finance.AccountTransferListScreen
import com.zhihuiji.feature.finance.AccountTransferScreen
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
import com.zhihuiji.feature.suppliers.SupplierContactEditScreen
import com.zhihuiji.feature.suppliers.SupplierContactListScreen
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

    const val ACCOUNT_LIST = "account_list"
    const val ACCOUNT_EDIT = "account_edit/{accountId}"
    const val ACCOUNT_CREATE = "account_create"
    const val ACCOUNT_TRANSFER_LIST = "account_transfer_list"
    const val ACCOUNT_TRANSFER = "account_transfer"

    const val CUSTOMER_CONTACT_LIST = "customer_contact_list/{customerId}"
    const val CUSTOMER_CONTACT_EDIT = "customer_contact_edit/{customerId}/{contactId}"
    const val CUSTOMER_CONTACT_CREATE = "customer_contact_create/{customerId}"

    const val SUPPLIER_CONTACT_LIST = "supplier_contact_list/{supplierId}"
    const val SUPPLIER_CONTACT_EDIT = "supplier_contact_edit/{supplierId}/{contactId}"
    const val SUPPLIER_CONTACT_CREATE = "supplier_contact_create/{supplierId}"

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
fun accountEditRoute(accountId: Long) = "account_edit/$accountId"
fun customerContactListRoute(customerId: Long) = "customer_contact_list/$customerId"
fun customerContactEditRoute(customerId: Long, contactId: Long) = "customer_contact_edit/$customerId/$contactId"
fun customerContactCreateRoute(customerId: Long) = "customer_contact_create/$customerId"
fun supplierContactListRoute(supplierId: Long) = "supplier_contact_list/$supplierId"
fun supplierContactEditRoute(supplierId: Long, contactId: Long) = "supplier_contact_edit/$supplierId/$contactId"
fun supplierContactCreateRoute(supplierId: Long) = "supplier_contact_create/$supplierId"
fun taskNotificationRoute(initialTab: Int = 0) = "${DetailRoutes.TASK_NOTIFICATION}?initialTab=$initialTab"
fun agentChatRoute(initialQuestion: String? = null, conversationId: Long? = null): String {
    val encodedQuestion = java.net.URLEncoder.encode(initialQuestion ?: "", Charsets.UTF_8.name())
    return "agent_chat?initialQuestion=$encodedQuestion&conversationId=${conversationId ?: -1L}"
}

private fun longRouteArguments(name: String): List<NamedNavArgument> =
    listOf(navArgument(name) { type = NavType.LongType })

private val initialTabArguments = listOf(
    navArgument("initialTab") {
        type = NavType.IntType
        defaultValue = 0
    }
)
private val productIdArguments = longRouteArguments("productId")
private val customerIdArguments = longRouteArguments("customerId")
private val supplierIdArguments = longRouteArguments("supplierId")
private val orderIdArguments = longRouteArguments("orderId")
private val recordIdArguments = longRouteArguments("recordId")
private val accountIdArguments = longRouteArguments("accountId")
private val contactIdArguments = longRouteArguments("contactId")
private val customerContactEditArguments = listOf(
    navArgument("customerId") { type = NavType.LongType },
    navArgument("contactId") { type = NavType.LongType }
)
private val supplierContactEditArguments = listOf(
    navArgument("supplierId") { type = NavType.LongType },
    navArgument("contactId") { type = NavType.LongType }
)
private val agentChatArguments = listOf(
    navArgument("initialQuestion") {
        type = NavType.StringType
        defaultValue = ""
    },
    navArgument("conversationId") {
        type = NavType.LongType
        defaultValue = -1L
    }
)

@Composable
fun MainNavGraph(
    navController: NavHostController,
    selectedIndex: Int,
    homeBottomBarScrollEvents: Flow<Float>,
    onNavigateToSettings: () -> Unit,
    accessState: MainAccessUiState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = TabRoutes.HOME
        ) {
            // 顶级页面
            permissionComposable(TabRoutes.HOME, accessState, navController) {
                DashboardScreen(
                    bottomBarScrollEvents = homeBottomBarScrollEvents,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToSales = {
                        navController.navigateIfAllowed(accessState, "${TabRoutes.DOCUMENTS}?initialTab=0")
                    },
                    onNavigateToProducts = {
                        navController.navigateIfAllowed(accessState, "${TabRoutes.ARCHIVES}?initialTab=0")
                    },
                    onNavigateToCustomers = {
                        navController.navigateIfAllowed(accessState, "${TabRoutes.ARCHIVES}?initialTab=1")
                    },
                    onNavigateToAgent = {
                        navController.navigateIfAllowed(accessState, TabRoutes.AGENT)
                    },
                    onNavigateToNotifications = {
                        navController.navigateIfAllowed(accessState, taskNotificationRoute(initialTab = 1))
                    },
                    canOpenProducts = accessState.hasPermission("archives:view"),
                    canOpenCustomers = accessState.hasPermission("sales:view"),
                    canOpenAgent = accessState.hasPermission("agent:view"),
                )
            }
            permissionComposable(
                route = "${TabRoutes.DOCUMENTS}?initialTab={initialTab}",
                accessState = accessState,
                navController = navController,
                arguments = initialTabArguments
            ) { backStackEntry ->
                DocumentsScreen(
                    accessState = accessState,
                    initialTab = backStackEntry.arguments?.getInt("initialTab") ?: 0,
                    onNavigateToSaleOrderDetail = { navController.navigateIfAllowed(accessState, saleOrderDetailRoute(it)) },
                    onNavigateToSaleOrderCreate = { navController.navigateIfAllowed(accessState, DetailRoutes.SALE_ORDER_CREATE) },
                    onNavigateToSalesReturns = { navController.navigateIfAllowed(accessState, DetailRoutes.SALES_RETURNS) },
                    onNavigateToPurchaseOrderDetail = { navController.navigateIfAllowed(accessState, purchaseOrderDetailRoute(it)) },
                    onNavigateToPurchaseOrderCreate = { navController.navigateIfAllowed(accessState, DetailRoutes.PURCHASE_ORDER_CREATE) },
                    onNavigateToPurchaseReceipts = { navController.navigateIfAllowed(accessState, DetailRoutes.PURCHASE_RECEIPTS) },
                    onNavigateToPurchaseReturns = { navController.navigateIfAllowed(accessState, DetailRoutes.PURCHASE_RETURNS) },
                    onNavigateToPayOrderDetail = { navController.navigateIfAllowed(accessState, payOrderDetailRoute(it)) },
                    onNavigateToFinanceRecordDetail = { navController.navigateIfAllowed(accessState, financeRecordDetailRoute(it)) },
                    onNavigateToDailyExpense = { navController.navigateIfAllowed(accessState, DetailRoutes.DAILY_EXPENSE) },
                    onNavigateToInventorySnapshot = { navController.navigateIfAllowed(accessState, DetailRoutes.INVENTORY_SNAPSHOT) },
                )
            }
            permissionComposable(
                route = "${TabRoutes.ARCHIVES}?initialTab={initialTab}",
                accessState = accessState,
                navController = navController,
                arguments = initialTabArguments
            ) { backStackEntry ->
                ArchivesScreen(
                    accessState = accessState,
                    canCreate = accessState.hasPermission("archives:write"),
                    initialTab = backStackEntry.arguments?.getInt("initialTab") ?: 0,
                    onNavigateToProductDetail = { navController.navigateIfAllowed(accessState, productDetailRoute(it)) },
                    onNavigateToProductCreate = { navController.navigateIfAllowed(accessState, DetailRoutes.PRODUCT_CREATE) },
                    onNavigateToCustomerDetail = { navController.navigateIfAllowed(accessState, customerDetailRoute(it)) },
                    onNavigateToCustomerCreate = { navController.navigateIfAllowed(accessState, DetailRoutes.CUSTOMER_CREATE) },
                    onNavigateToSupplierDetail = { navController.navigateIfAllowed(accessState, supplierDetailRoute(it)) },
                    onNavigateToSupplierCreate = { navController.navigateIfAllowed(accessState, DetailRoutes.SUPPLIER_CREATE) },
                )
            }
            permissionComposable(TabRoutes.REPORTS, accessState, navController) {
                ReportScreen()
            }
            permissionComposable(TabRoutes.AGENT, accessState, navController) {
                AgentWorkbenchScreen(
                    onNavigateToChat = { question ->
                        navController.navigateIfAllowed(accessState, agentChatRoute(question))
                    },
                    onNavigateToTasks = { navController.navigateIfAllowed(accessState, taskNotificationRoute()) },
                )
            }

            // 商品详情/编辑
            permissionComposable(
                route = DetailRoutes.PRODUCT_DETAIL,
                accessState = accessState,
                navController = navController,
                arguments = productIdArguments
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getLong("productId") ?: 0L
                ProductDetailScreen(
                    productId = productId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { navController.navigateIfAllowed(accessState, productEditRoute(it)) },
                    onNavigateToStockAdjust = { navController.navigateIfAllowed(accessState, stockAdjustRoute(it)) },
                    onNavigateToInventoryLedger = { navController.navigateIfAllowed(accessState, inventoryLedgerRoute(it)) }
                )
            }
            permissionComposable(
                route = DetailRoutes.PRODUCT_EDIT,
                accessState = accessState,
                navController = navController,
                arguments = productIdArguments
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getLong("productId")
                ProductEditScreen(
                    productId = productId,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }
            permissionComposable(DetailRoutes.PRODUCT_CREATE, accessState, navController) {
                ProductEditScreen(
                    productId = null,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }
            permissionComposable(
                route = DetailRoutes.STOCK_ADJUST,
                accessState = accessState,
                navController = navController,
                arguments = productIdArguments
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getLong("productId") ?: 0L
                StockAdjustScreen(
                    productId = productId,
                    onNavigateBack = { navController.popBackStack() },
                    onAdjustSuccess = { navController.popBackStack() }
                )
            }
            permissionComposable(
                route = DetailRoutes.INVENTORY_LEDGER,
                accessState = accessState,
                navController = navController,
                arguments = productIdArguments
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getLong("productId") ?: 0L
                InventoryLedgerScreen(
                    productId = productId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            permissionComposable(DetailRoutes.INVENTORY_SNAPSHOT, accessState, navController) {
                InventorySnapshotScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 客户详情/编辑
            permissionComposable(
                route = DetailRoutes.CUSTOMER_DETAIL,
                accessState = accessState,
                navController = navController,
                arguments = customerIdArguments
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
                CustomerDetailScreen(
                    customerId = customerId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { navController.navigateIfAllowed(accessState, customerEditRoute(it)) }
                )
            }
            permissionComposable(
                route = DetailRoutes.CUSTOMER_EDIT,
                accessState = accessState,
                navController = navController,
                arguments = customerIdArguments
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getLong("customerId")
                CustomerEditScreen(
                    customerId = customerId,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }
            permissionComposable(DetailRoutes.CUSTOMER_CREATE, accessState, navController) {
                CustomerEditScreen(
                    customerId = null,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }

            // 客户联系人列表/编辑
            permissionComposable(
                route = DetailRoutes.CUSTOMER_CONTACT_LIST,
                accessState = accessState,
                navController = navController,
                arguments = customerIdArguments
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
                CustomerContactListScreen(
                    customerId = customerId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { contactId ->
                        val route = if (contactId == null) {
                            customerContactCreateRoute(customerId)
                        } else {
                            customerContactEditRoute(customerId, contactId)
                        }
                        navController.navigateIfAllowed(accessState, route)
                    }
                )
            }
            permissionComposable(
                route = DetailRoutes.CUSTOMER_CONTACT_EDIT,
                accessState = accessState,
                navController = navController,
                arguments = customerContactEditArguments
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
                val contactId = backStackEntry.arguments?.getLong("contactId") ?: 0L
                ContactEditScreen(
                    customerId = customerId,
                    contactId = if (contactId > 0L) contactId else null,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }
            permissionComposable(
                route = DetailRoutes.CUSTOMER_CONTACT_CREATE,
                accessState = accessState,
                navController = navController,
                arguments = customerIdArguments
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
                ContactEditScreen(
                    customerId = customerId,
                    contactId = null,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }

            // 供应商详情/编辑
            permissionComposable(
                route = DetailRoutes.SUPPLIER_DETAIL,
                accessState = accessState,
                navController = navController,
                arguments = supplierIdArguments
            ) { backStackEntry ->
                val supplierId = backStackEntry.arguments?.getLong("supplierId") ?: 0L
                SupplierDetailScreen(
                    supplierId = supplierId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { navController.navigateIfAllowed(accessState, supplierEditRoute(it)) },
                    onNavigateToStatement = { navController.navigateIfAllowed(accessState, supplierStatementRoute(it)) }
                )
            }
            permissionComposable(
                route = DetailRoutes.SUPPLIER_STATEMENT,
                accessState = accessState,
                navController = navController,
                arguments = supplierIdArguments
            ) { backStackEntry ->
                val supplierId = backStackEntry.arguments?.getLong("supplierId") ?: 0L
                SupplierStatementScreen(
                    supplierId = supplierId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            permissionComposable(
                route = DetailRoutes.SUPPLIER_EDIT,
                accessState = accessState,
                navController = navController,
                arguments = supplierIdArguments
            ) { backStackEntry ->
                val supplierId = backStackEntry.arguments?.getLong("supplierId")
                SupplierEditScreen(
                    supplierId = supplierId,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }
            permissionComposable(DetailRoutes.SUPPLIER_CREATE, accessState, navController) {
                SupplierEditScreen(
                    supplierId = null,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }

            // 供应商联系人列表/编辑
            permissionComposable(
                route = DetailRoutes.SUPPLIER_CONTACT_LIST,
                accessState = accessState,
                navController = navController,
                arguments = supplierIdArguments
            ) { backStackEntry ->
                val supplierId = backStackEntry.arguments?.getLong("supplierId") ?: 0L
                SupplierContactListScreen(
                    supplierId = supplierId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { contactId ->
                        val route = if (contactId == null) {
                            supplierContactCreateRoute(supplierId)
                        } else {
                            supplierContactEditRoute(supplierId, contactId)
                        }
                        navController.navigateIfAllowed(accessState, route)
                    }
                )
            }
            permissionComposable(
                route = DetailRoutes.SUPPLIER_CONTACT_EDIT,
                accessState = accessState,
                navController = navController,
                arguments = supplierContactEditArguments
            ) { backStackEntry ->
                val supplierId = backStackEntry.arguments?.getLong("supplierId") ?: 0L
                val contactId = backStackEntry.arguments?.getLong("contactId") ?: 0L
                SupplierContactEditScreen(
                    supplierId = supplierId,
                    contactId = if (contactId > 0L) contactId else null,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }
            permissionComposable(
                route = DetailRoutes.SUPPLIER_CONTACT_CREATE,
                accessState = accessState,
                navController = navController,
                arguments = supplierIdArguments
            ) { backStackEntry ->
                val supplierId = backStackEntry.arguments?.getLong("supplierId") ?: 0L
                SupplierContactEditScreen(
                    supplierId = supplierId,
                    contactId = null,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }

            // 销售单详情/编辑/收款
            permissionComposable(
                route = DetailRoutes.SALE_ORDER_DETAIL,
                accessState = accessState,
                navController = navController,
                arguments = orderIdArguments
            ) {
                SaleOrderDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onEditClick = { navController.navigateIfAllowed(accessState, saleOrderEditRoute(it)) },
                    onPaymentClick = { navController.navigateIfAllowed(accessState, paymentRoute(it)) }
                )
            }
            permissionComposable(
                route = DetailRoutes.SALE_ORDER_EDIT,
                accessState = accessState,
                navController = navController,
                arguments = orderIdArguments
            ) {
                SaleOrderEditScreen(
                    onBackClick = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }
            permissionComposable(DetailRoutes.SALE_ORDER_CREATE, accessState, navController) {
                SaleOrderEditScreen(
                    onBackClick = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }
            permissionComposable(
                route = DetailRoutes.PAYMENT,
                accessState = accessState,
                navController = navController,
                arguments = orderIdArguments
            ) {
                PaymentScreen(
                    onBackClick = { navController.popBackStack() },
                    onPaySuccess = { navController.popBackStack() }
                )
            }
            permissionComposable(DetailRoutes.SALES_RETURNS, accessState, navController) {
                SalesReturnScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 采购单详情/编辑
            permissionComposable(
                route = DetailRoutes.PURCHASE_ORDER_DETAIL,
                accessState = accessState,
                navController = navController,
                arguments = orderIdArguments
            ) {
                PurchaseOrderDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onEditClick = { navController.navigateIfAllowed(accessState, purchaseOrderEditRoute(it)) },
                    onDeleteSuccess = { navController.popBackStack() }
                )
            }
            permissionComposable(
                route = DetailRoutes.PURCHASE_ORDER_EDIT,
                accessState = accessState,
                navController = navController,
                arguments = orderIdArguments
            ) {
                PurchaseOrderEditScreen(
                    onBackClick = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }
            permissionComposable(DetailRoutes.PURCHASE_ORDER_CREATE, accessState, navController) {
                PurchaseOrderEditScreen(
                    onBackClick = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }
            permissionComposable(DetailRoutes.PURCHASE_RECEIPTS, accessState, navController) {
                PurchaseReceiptScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            permissionComposable(DetailRoutes.PURCHASE_RETURNS, accessState, navController) {
                PurchaseReturnScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 付款单详情
            permissionComposable(
                route = DetailRoutes.PAY_ORDER_DETAIL,
                accessState = accessState,
                navController = navController,
                arguments = orderIdArguments
            ) {
                PayOrderDetailScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // 资金流水详情
            permissionComposable(
                route = DetailRoutes.FINANCE_RECORD_DETAIL,
                accessState = accessState,
                navController = navController,
                arguments = recordIdArguments
            ) { backStackEntry ->
                val recordId = backStackEntry.arguments?.getLong("recordId") ?: 0L
                FinanceRecordDetailScreen(
                    recordId = recordId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 日常支出
            permissionComposable(DetailRoutes.DAILY_EXPENSE, accessState, navController) {
                DailyExpenseScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onExpenseCreated = { recordId ->
                        if (accessState.canAccessRoute(financeRecordDetailRoute(recordId))) {
                            navController.navigate(financeRecordDetailRoute(recordId)) {
                                popUpTo(DetailRoutes.DAILY_EXPENSE) {
                                    inclusive = true
                                }
                            }
                        }
                    }
                )
            }

            // 资金账户列表/编辑
            permissionComposable(DetailRoutes.ACCOUNT_LIST, accessState, navController) {
                AccountListScreen(
                    onNavigateToEdit = { accountId ->
                        val route = if (accountId == null) {
                            DetailRoutes.ACCOUNT_CREATE
                        } else {
                            accountEditRoute(accountId)
                        }
                        navController.navigateIfAllowed(accessState, route)
                    }
                )
            }
            permissionComposable(
                route = DetailRoutes.ACCOUNT_EDIT,
                accessState = accessState,
                navController = navController,
                arguments = accountIdArguments
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getLong("accountId")
                AccountEditScreen(
                    accountId = accountId,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }
            permissionComposable(DetailRoutes.ACCOUNT_CREATE, accessState, navController) {
                AccountEditScreen(
                    accountId = null,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }

            // 账户转账
            permissionComposable(DetailRoutes.ACCOUNT_TRANSFER_LIST, accessState, navController) {
                AccountTransferListScreen(
                    onNavigateToTransfer = {
                        navController.navigateIfAllowed(accessState, DetailRoutes.ACCOUNT_TRANSFER)
                    }
                )
            }
            permissionComposable(DetailRoutes.ACCOUNT_TRANSFER, accessState, navController) {
                AccountTransferScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onTransferSuccess = { navController.popBackStack() }
                )
            }

            // 草稿列表
            permissionComposable(DetailRoutes.DRAFT_LIST, accessState, navController) {
                DraftListScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // 任务与通知
            permissionComposable(
                route = "${DetailRoutes.TASK_NOTIFICATION}?initialTab={initialTab}",
                accessState = accessState,
                navController = navController,
                arguments = initialTabArguments
            ) { backStackEntry ->
                TaskNotificationScreen(
                    onBackClick = { navController.popBackStack() },
                    initialTab = backStackEntry.arguments?.getInt("initialTab") ?: 0,
                )
            }

            // AI 聊天页
            permissionComposable(
                route = DetailRoutes.AGENT_CHAT,
                accessState = accessState,
                navController = navController,
                arguments = agentChatArguments
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

private fun NavGraphBuilder.permissionComposable(
    route: String,
    accessState: MainAccessUiState,
    navController: NavHostController,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit,
) {
    composable(
        route = route,
        arguments = arguments,
    ) { backStackEntry ->
        if (accessState.canAccessRoute(route)) {
            content(backStackEntry)
        } else {
            PermissionDeniedScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(accessState.firstAllowedTopLevelRoute()) {
                            launchSingleTop = true
                        }
                    }
                },
            )
        }
    }
}

private fun NavHostController.navigateIfAllowed(
    accessState: MainAccessUiState,
    route: String,
) {
    if (!accessState.canAccessRoute(route)) {
        return
    }
    navigate(route) {
        launchSingleTop = true
    }
}
