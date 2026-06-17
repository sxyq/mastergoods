import Foundation
import SwiftUI

struct PurchaseOrderSummary: Identifiable, Codable, Equatable {
    let id: EntityID
    let orderNo: String
    let supplierId: EntityID?
    let supplierName: String?
    let items: [PurchaseOrderItem]
    let totalAmount: Double
    let paidAmount: Double
    let receivedAmount: Double
    let notes: String?
    let status: Int
    let createdAt: Int64
    let updatedAt: Int64
}

typealias PurchaseOrder = PurchaseOrderSummary

struct PurchaseOrderCreatePayload: Codable {
    let supplierId: EntityID?
    let supplierName: String?
    let items: [PurchaseOrderCreateItemPayload]
    let notes: String?
    let status: Int?
}

struct PurchaseOrderCreateItemPayload: Codable {
    let productId: EntityID?
    let productCode: String?
    let productName: String?
    let quantity: Double
    let unitCost: Double
}

struct PurchaseOrderItem: Identifiable, Codable, Equatable {
    let id: EntityID
    let orderId: EntityID
    let productId: EntityID?
    let productCode: String?
    let productName: String
    let quantity: Double
    let unitCost: Double
    let amount: Double
    let createdAt: Int64
}

extension PurchaseOrderSummary {
    var outstandingAmount: Double {
        max(totalAmount - paidAmount, 0)
    }

    var pendingReceiptAmount: Double {
        max(totalAmount - receivedAmount, 0)
    }

    var statusLabel: String {
        switch status {
        case 1: return "已入库"
        case 2: return "已确认"
        default: return "草稿"
        }
    }

    var statusTint: Color {
        switch status {
        case 1: return ZhihuijiTheme.ColorToken.success
        case 2: return ZhihuijiTheme.ColorToken.primary
        default: return ZhihuijiTheme.ColorToken.warning
        }
    }
}

struct PurchaseReceiptRecord: Identifiable, Codable, Equatable {
    let id: EntityID
    let receiptNo: String
    let purchaseOrderId: EntityID?
    let supplierId: EntityID?
    let supplierName: String?
    let items: [PurchaseReceiptItem]
    let totalAmount: Double
    let status: Int
    let notes: String?
    let createdAt: Int64
    let updatedAt: Int64
}

struct PurchaseReceiptItem: Identifiable, Codable, Equatable {
    let id: EntityID
    let receiptId: EntityID
    let productId: EntityID?
    let productCode: String?
    let productName: String
    let quantity: Double
    let unitCost: Double
    let amount: Double
    let createdAt: Int64
}

struct PurchaseReceiptCreatePayload: Codable {
    let purchaseOrderId: EntityID?
    let supplierId: EntityID?
    let supplierName: String?
    let items: [PurchaseReceiptCreateItemPayload]
    let notes: String?
}

struct PurchaseReceiptCreateItemPayload: Codable {
    let productId: EntityID?
    let productCode: String?
    let productName: String?
    let quantity: Double
    let unitCost: Double
}

struct PurchaseReceiptDraftPayload: Codable {
    let notes: String?
}

struct PurchaseReturnRecord: Identifiable, Codable, Equatable {
    let id: EntityID
    let returnNo: String
    let purchaseOrderId: EntityID?
    let supplierId: EntityID?
    let supplierName: String?
    let items: [PurchaseReturnItem]
    let refunds: [PurchaseReturnRefund]
    let totalAmount: Double
    let refundAmount: Double
    let status: Int
    let notes: String?
    let createdAt: Int64
    let updatedAt: Int64
}

struct PurchaseReturnItem: Identifiable, Codable, Equatable {
    let id: EntityID
    let returnId: EntityID
    let productId: EntityID?
    let productCode: String?
    let productName: String
    let quantity: Double
    let unitCost: Double
    let amount: Double
    let createdAt: Int64
}

struct PurchaseReturnRefund: Identifiable, Codable, Equatable {
    let id: EntityID
    let returnId: EntityID
    let amount: Double
    let method: Int
    let referenceNo: String?
    let createdAt: Int64
}

struct PurchaseReturnCreatePayload: Codable {
    let purchaseOrderId: EntityID?
    let supplierId: EntityID?
    let supplierName: String?
    let items: [PurchaseReturnCreateItemPayload]
    let notes: String?
}

struct PurchaseReturnCreateItemPayload: Codable {
    let productId: EntityID?
    let productCode: String?
    let productName: String?
    let quantity: Double
    let unitCost: Double?
}

struct PurchaseReturnDraftPayload: Codable {
    let notes: String?
}

struct PurchaseReturnConfirmPayload: Codable {
    let notes: String?
}

struct PurchaseReturnRefundPayload: Codable {
    let amount: Double
    let method: Int
    let referenceNo: String?
}

extension PurchaseReceiptRecord {
    var statusLabel: String {
        switch status {
        case 1: return "已确认"
        case 2: return "已取消"
        default: return "草稿"
        }
    }

    var statusTint: Color {
        switch status {
        case 1: return ZhihuijiTheme.ColorToken.success
        case 2: return ZhihuijiTheme.ColorToken.danger
        default: return ZhihuijiTheme.ColorToken.warning
        }
    }

    var canEditDraft: Bool { status == 0 }
    var canCancel: Bool { status != 2 }
}

extension PurchaseReturnRecord {
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
