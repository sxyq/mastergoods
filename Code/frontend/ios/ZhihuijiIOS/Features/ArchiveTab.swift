import Foundation

enum ArchiveTab: String, CaseIterable, Identifiable {
    case products
    case customers
    case suppliers

    var id: String { rawValue }

    var title: String {
        switch self {
        case .products: return "商品"
        case .customers: return "客户"
        case .suppliers: return "供应商"
        }
    }

    var searchPlaceholder: String {
        switch self {
        case .products: return "搜索商品名称 / 编码"
        case .customers: return "搜索客户名称 / 手机号"
        case .suppliers: return "搜索供应商名称 / 手机号"
        }
    }

    var emptyMessage: String {
        switch self {
        case .products: return "当前没有商品档案数据。"
        case .customers: return "当前没有客户档案数据。"
        case .suppliers: return "当前没有供应商档案数据。"
        }
    }
}
