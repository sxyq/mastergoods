import Foundation
import SwiftUI

struct FinanceRecordSummary: Identifiable, Codable, Equatable {
    let id: EntityID
    let recordNo: String
    let type: Int
    let category: String
    let partnerName: String?
    let amount: Double
    let method: Int
    let notes: String?
    let createdAt: Int64
    let updatedAt: Int64
}

typealias FinanceRecord = FinanceRecordSummary

struct FinanceRecordCreatePayload: Codable {
    let type: Int
    let category: String
    let partnerName: String?
    let amount: Double
    let method: Int
    let notes: String?
}

struct PayOrder: Identifiable, Codable, Equatable {
    let id: EntityID
    let orderNo: String
    let supplierId: EntityID?
    let supplierName: String?
    let amount: Double
    let method: Int
    let referenceNo: String?
    let notes: String?
    let accountId: EntityID?
    let status: Int
    let createdAt: Int64
    let updatedAt: Int64
}

struct PayOrderCreatePayload: Codable {
    let supplierId: EntityID?
    let supplierName: String?
    let amount: Double
    let method: Int
    let referenceNo: String?
    let notes: String?
    let accountId: EntityID?
    let status: Int
}

struct PayOrderStatusPayload: Codable {
    let status: Int
}

struct CashChangeRecord: Identifiable, Codable, Equatable {
    let id: EntityID
    let orderType: String
    let orderId: EntityID?
    let receivable: Double
    let received: Double
    let changeAmount: Double
    let accountId: EntityID?
    let accountName: String?
    let status: Int
    let notes: String?
    let createdAt: Int64
    let updatedAt: Int64
}

struct CashChangeRecordCreatePayload: Codable {
    let orderType: String
    let orderId: EntityID?
    let receivable: Double
    let received: Double
    let accountId: EntityID?
    let status: Int
    let notes: String?
}

enum FinanceRecordType: Int, CaseIterable, Identifiable {
    case income = 1
    case expense = 2

    var id: Int { rawValue }

    var label: String {
        switch self {
        case .income: return "收入"
        case .expense: return "支出"
        }
    }
}

extension FinanceRecordSummary {
    var typeLabel: String { FinanceRecordType(rawValue: type)?.label ?? "未知" }

    var typeTint: Color {
        FinanceRecordType(rawValue: type) == .income ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.warning
    }

    var methodLabel: String {
        SalePaymentMethod(rawValue: method)?.label ?? "其他"
    }
}

extension PayOrder {
    var statusLabel: String {
        switch status {
        case 1: return "已付款"
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
}

extension CashChangeRecord {
    var statusLabel: String {
        switch status {
        case 1: return "已生效"
        case 0: return "草稿"
        default: return "未知"
        }
    }

    var statusTint: Color {
        switch status {
        case 1: return ZhihuijiTheme.ColorToken.success
        case 0: return ZhihuijiTheme.ColorToken.warning
        default: return ZhihuijiTheme.ColorToken.textTertiary
        }
    }
}
