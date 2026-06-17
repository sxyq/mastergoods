import Foundation
import SwiftUI

struct SalesOrderSummary: Identifiable, Codable, Equatable {
    let id: EntityID
    let orderNo: String
    let customerId: Int64?
    let customerName: String?
    let items: [SalesOrderItem]
    let subtotalAmount: Double
    let discountAmount: Double
    let totalAmount: Double
    let paidAmount: Double
    let notes: String?
    let status: Int
    let createdAt: Int64
    let updatedAt: Int64
}

typealias SalesOrder = SalesOrderSummary

struct SalesOrderItem: Identifiable, Codable, Equatable {
    let id: EntityID
    let orderId: EntityID
    let productId: Int64
    let productCode: String
    let productName: String
    let customerId: Int64?
    let customerName: String?
    let quantity: Double
    let unitPrice: Double
    let amount: Double
    let createdAt: Int64
}

struct SalePaymentRecord: Identifiable, Codable, Equatable {
    let id: EntityID
    let orderId: EntityID
    let amount: Double
    let method: Int
    let referenceNo: String?
    let type: Int?
    let createdAt: Int64
}

struct SalePaymentCreatePayload: Codable {
    let amount: Double
    let method: Int
    let referenceNo: String?
}

struct SaleOrderCreatePayload: Codable {
    let customerId: EntityID?
    let customerName: String?
    let items: [SaleOrderCreateItemPayload]
    let notes: String?
    let discountAmount: Double?
}

struct SaleOrderCreateItemPayload: Codable {
    let productId: EntityID
    let quantity: Double
    let unitPrice: Double
}

struct SalesReturnRecord: Identifiable, Codable, Equatable {
    let id: EntityID
    let returnNo: String
    let originalOrderId: EntityID
    let customerId: EntityID?
    let customerName: String?
    let items: [SalesReturnItem]
    let totalAmount: Double
    let refundAmount: Double
    let status: Int
    let notes: String?
    let createdAt: Int64
    let updatedAt: Int64
}

struct SalesReturnItem: Identifiable, Codable, Equatable {
    let id: EntityID
    let returnId: EntityID
    let productId: EntityID
    let productCode: String
    let productName: String
    let quantity: Double
    let unitPrice: Double
    let amount: Double
    let createdAt: Int64
}

struct SalesReturnCreatePayload: Codable {
    let originalOrderId: EntityID
    let customerId: EntityID?
    let customerName: String?
    let items: [SalesReturnCreateItemPayload]
    let notes: String?
}

struct SalesReturnCreateItemPayload: Codable {
    let productId: EntityID
    let quantity: Double
    let unitPrice: Double?
}

struct SalesReturnDraftPayload: Codable {
    let notes: String?
}

struct SalesReturnConfirmPayload: Codable {
    let notes: String?
}

struct SalesReturnRefundPayload: Codable {
    let amount: Double
    let method: Int
    let referenceNo: String?
}

enum SalePaymentMethod: Int, CaseIterable, Identifiable {
    case cash = 1
    case wechat = 2
    case alipay = 3
    case bank = 4
    case other = 5

    var id: Int { rawValue }

    var label: String {
        switch self {
        case .cash: return "现金"
        case .wechat: return "微信"
        case .alipay: return "支付宝"
        case .bank: return "银行卡"
        case .other: return "其他"
        }
    }
}

extension SalesOrderSummary {
    var outstandingAmount: Double {
        max(totalAmount - paidAmount, 0)
    }

    var statusLabel: String {
        switch status {
        case 1: return "已完成"
        case 2: return "已作废"
        case 3: return "已确认"
        default: return "草稿"
        }
    }

    var statusTint: Color {
        switch status {
        case 1, 3: return ZhihuijiTheme.ColorToken.success
        case 2: return ZhihuijiTheme.ColorToken.danger
        default: return ZhihuijiTheme.ColorToken.warning
        }
    }
}

extension SalesReturnRecord {
    var remainingRefundAmount: Double {
        max(totalAmount - refundAmount, 0)
    }

    var statusLabel: String {
        switch status {
        case 1: return "已确认"
        case 2: return "已完成"
        case 3: return "已取消"
        default: return "草稿"
        }
    }

    var statusTint: Color {
        switch status {
        case 1: return ZhihuijiTheme.ColorToken.primary
        case 2: return ZhihuijiTheme.ColorToken.success
        case 3: return ZhihuijiTheme.ColorToken.danger
        default: return ZhihuijiTheme.ColorToken.warning
        }
    }

    var canEditDraft: Bool { status == 0 }
    var canRefund: Bool { status != 3 && remainingRefundAmount > 0.009 }
    var canCancel: Bool { status != 3 }
}
