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

// MARK: - Account (FE6 资金账户)

struct AccountRecord: Identifiable, Codable, Equatable {
    let id: EntityID
    let code: String
    let name: String
    let type: Int
    let balance: Double
    let isDefault: Bool
    let status: Int
    let sortOrder: Int?
    let notes: String?
    let createdAt: Int64
    let updatedAt: Int64
}

struct AccountWritePayload: Codable, Equatable {
    let code: String
    let name: String
    let type: Int
    let balance: Double?
    let isDefault: Bool?
    let status: Int?
    let sortOrder: Int?
    let notes: String?
}

enum AccountType: Int, CaseIterable, Identifiable {
    case cash = 0
    case bank = 1
    case alipay = 2
    case wechat = 3

    var id: Int { rawValue }

    var label: String {
        switch self {
        case .cash: return "现金"
        case .bank: return "银行"
        case .alipay: return "支付宝"
        case .wechat: return "微信"
        }
    }

    var systemImage: String {
        switch self {
        case .cash: return "banknote.fill"
        case .bank: return "building.columns.fill"
        case .alipay: return "creditcard.fill"
        case .wechat: return "message.fill"
        }
    }
}

enum AccountStatus: Int, CaseIterable, Identifiable {
    case disabled = 0
    case active = 1

    var id: Int { rawValue }

    var label: String {
        switch self {
        case .active: return "启用"
        case .disabled: return "停用"
        }
    }
}

extension AccountRecord {
    var typeLabel: String { AccountType(rawValue: type)?.label ?? "未知" }
    var typeSystemImage: String { AccountType(rawValue: type)?.systemImage ?? "questionmark.circle" }
    var typeTint: Color {
        switch AccountType(rawValue: type) {
        case .cash: return ZhihuijiTheme.ColorToken.warning
        case .bank: return ZhihuijiTheme.ColorToken.primary
        case .alipay: return ZhihuijiTheme.ColorToken.success
        case .wechat: return ZhihuijiTheme.ColorToken.success
        case nil: return ZhihuijiTheme.ColorToken.textTertiary
        }
    }

    var statusLabel: String { AccountStatus(rawValue: status)?.label ?? "未知" }
    var statusTint: Color {
        status == 1 ? ZhihuijiTheme.ColorToken.success : ZhihuijiTheme.ColorToken.warning
    }
}

// MARK: - AccountTransfer (FE7 账户转账)

struct AccountTransferRecord: Identifiable, Codable, Equatable {
    let id: EntityID
    let transferNo: String
    let fromAccountId: EntityID
    let fromAccountName: String
    let toAccountId: EntityID
    let toAccountName: String
    let amount: Double
    let fee: Double?
    let status: Int
    let notes: String?
    let createdAt: Int64
    let updatedAt: Int64
}

struct AccountTransferCreatePayload: Codable {
    let fromAccountId: EntityID
    let toAccountId: EntityID
    let amount: Double
    let fee: Double?
    let notes: String?
}

enum AccountTransferStatus: Int, CaseIterable, Identifiable {
    case pending = 0
    case completed = 1
    case cancelled = 2

    var id: Int { rawValue }

    var label: String {
        switch self {
        case .pending: return "待处理"
        case .completed: return "已完成"
        case .cancelled: return "已取消"
        }
    }
}

extension AccountTransferRecord {
    var statusLabel: String { AccountTransferStatus(rawValue: status)?.label ?? "未知" }
    var statusTint: Color {
        switch AccountTransferStatus(rawValue: status) {
        case .completed: return ZhihuijiTheme.ColorToken.success
        case .pending: return ZhihuijiTheme.ColorToken.warning
        case .cancelled: return ZhihuijiTheme.ColorToken.danger
        case nil: return ZhihuijiTheme.ColorToken.textTertiary
        }
    }
}
