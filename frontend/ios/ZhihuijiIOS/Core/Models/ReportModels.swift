import Foundation

struct KPIBlock: Identifiable, Codable, Equatable {
    let id: String
    let title: String
    let value: String
    let subtitle: String
}

struct SalesSummaryReport: Codable, Equatable {
    let startAt: Int64
    let endAt: Int64
    let totalSalesAmount: Double
    let totalPaidAmount: Double
    let totalRefundAmount: Double
    let totalUnpaidAmount: Double
    let totalOrderCount: Int
}

struct SalesTrendPoint: Identifiable, Codable, Equatable {
    var id: String { "\(startAt)-\(endAt)" }
    let startAt: Int64
    let endAt: Int64
    let totalSalesAmount: Double
    let totalOrderCount: Int
}

struct ProfitSummaryReport: Codable, Equatable {
    let startAt: Int64
    let endAt: Int64
    let estimatedCostAmount: Double
    let estimatedProfitAmount: Double
    let estimatedProfitRate: Double
}

struct RefundRecordReport: Identifiable, Codable, Equatable {
    var id: String { "\(paymentId)" }
    let paymentId: EntityID
    let orderId: EntityID
    let orderNo: String
    let customerName: String?
    let refundAmount: Double
    let method: Int
    let referenceNo: String?
    let createdAt: Int64
}

struct TopSellingProductReport: Identifiable, Codable, Equatable {
    let productId: EntityID
    let productCode: String
    let productName: String
    let totalQuantity: Double
    let totalAmount: Double

    var id: EntityID { productId }
}

struct ProfitByProductReport: Identifiable, Codable, Equatable {
    let productId: EntityID
    let productCode: String
    let productName: String
    let totalSalesAmount: Double
    let totalCostAmount: Double
    let totalProfitAmount: Double
    let profitRate: Double

    var id: EntityID { productId }
}

struct ProfitByCustomerReport: Identifiable, Codable, Equatable {
    let customerId: EntityID?
    let customerName: String
    let totalSalesAmount: Double
    let totalCostAmount: Double
    let totalProfitAmount: Double
    let profitRate: Double

    var id: String { customerId?.rawValue ?? customerName }
}

struct CustomerSalesReport: Identifiable, Codable, Equatable {
    let customerId: EntityID?
    let customerName: String
    let totalOrders: Int
    let totalAmount: Double

    var id: String { customerId?.rawValue ?? customerName }
}

struct CustomerReceivableReport: Identifiable, Codable, Equatable {
    let customerId: EntityID
    let customerName: String
    let phone: String?
    let balance: Double

    var id: EntityID { customerId }
}

struct LowStockProductReport: Identifiable, Codable, Equatable {
    let productId: EntityID
    let productCode: String
    let productName: String
    let stock: Double
    let safeStock: Double

    var id: EntityID { productId }
}

struct StockOutRecordReport: Identifiable, Codable, Equatable {
    let orderId: EntityID
    let orderNo: String
    let customerId: EntityID?
    let customerName: String?
    let productId: EntityID
    let productCode: String
    let productName: String
    let quantity: Double
    let unitPrice: Double
    let amount: Double
    let itemCreatedAt: Int64
    let orderCreatedAt: Int64

    var id: String { "\(orderId.rawValue)-\(productId.rawValue)-\(itemCreatedAt)" }
}

struct InventoryFlowRecordReport: Identifiable, Codable, Equatable {
    let orderId: EntityID
    let orderNo: String
    let productId: EntityID
    let productCode: String
    let productName: String
    let quantity: Double
    let flowType: Int
    let flowTime: Int64
    let customerName: String?
    let sourceType: Int
    let sourceLabel: String?
    let adjustReason: String?
    let operatorName: String?

    var id: String { "\(orderId.rawValue)-\(productId.rawValue)-\(flowTime)" }
}

struct ReconciliationSummaryReport: Codable, Equatable {
    let startAt: Int64
    let endAt: Int64
    let totalReceivableAmount: Double
    let totalPayableAmount: Double
    let totalReceivableCustomerCount: Int64
    let totalPayableSupplierCount: Int64
    let totalReceivedAmount: Double
    let totalPaidAmount: Double
    let netCashFlow: Double
}

struct CashflowSummaryReport: Codable, Equatable {
    let startAt: Int64
    let endAt: Int64
    let totalIncomeAmount: Double
    let totalExpenseAmount: Double
    let netCashFlow: Double
    let totalRecordCount: Int64
}
