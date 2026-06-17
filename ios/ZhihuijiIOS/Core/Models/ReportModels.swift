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

struct LowStockProductReport: Identifiable, Codable, Equatable {
    let productId: EntityID
    let productCode: String
    let productName: String
    let stock: Double
    let safeStock: Double

    var id: EntityID { productId }
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
