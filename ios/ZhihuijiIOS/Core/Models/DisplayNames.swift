import Foundation

enum ZhihuijiDisplayName {
    static func syncEntityType(_ raw: String) -> String {
        switch raw {
        case "product": return "商品"
        case "customer": return "客户"
        case "supplier": return "供应商"
        case "sale_order": return "销售单"
        case "sale_payment": return "销售收款"
        case "sales_return": return "销售退货"
        case "purchase_order": return "采购单"
        case "purchase_receipt": return "采购收货"
        case "purchase_return": return "采购退货"
        case "finance_record": return "资金流水"
        case "pay_order": return "付款单"
        case "inventory_snapshot": return "盘点快照"
        case "inventory_adjustment": return "库存调整"
        case "media_asset": return "媒体对象"
        case "media_binding": return "媒体绑定"
        default: return raw
        }
    }

    static func syncOperation(_ raw: String) -> String {
        switch raw {
        case "upsert": return "更新"
        case "delete": return "删除"
        case "create": return "创建"
        case "confirm": return "确认"
        case "cancel": return "取消"
        default: return raw
        }
    }

    static func importJobStatus(_ raw: String) -> String {
        switch raw {
        case "pending": return "待处理"
        case "running": return "进行中"
        case "succeeded": return "成功"
        case "failed": return "失败"
        case "cancelled": return "取消"
        default: return raw
        }
    }

    static func syncSourceType(_ raw: String) -> String {
        switch raw {
        case "legacy_sqlite": return "旧库 SQLite"
        default: return raw
        }
    }

    static func mediaAssetType(_ raw: String) -> String {
        switch raw {
        case "product_cover": return "商品主图"
        case "product_gallery": return "商品相册"
        case "customer_avatar": return "客户头像"
        case "supplier_avatar": return "供应商头像"
        default: return raw
        }
    }

    static func mediaTargetType(_ raw: String) -> String {
        switch raw {
        case "asset": return "媒体对象"
        case "product": return "商品"
        case "customer": return "客户"
        case "supplier": return "供应商"
        case "sale_order": return "销售单"
        case "purchase_order": return "采购单"
        default: return syncEntityType(raw)
        }
    }

    static func storageProvider(_ raw: String) -> String {
        switch raw {
        case "object_storage": return "对象存储"
        default: return raw
        }
    }
}
