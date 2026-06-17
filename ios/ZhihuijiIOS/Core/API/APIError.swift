import Foundation

enum APIError: LocalizedError, Equatable {
    case invalidURL
    case invalidResponse
    case unauthorized
    case forbidden
    case server(status: Int, message: String)
    case decoding(String)
    case network(String)

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "接口地址无效"
        case .invalidResponse:
            return "接口响应无效"
        case .unauthorized:
            return "登录已失效，请重新登录"
        case .forbidden:
            return "当前账号没有权限访问该数据"
        case let .server(_, message):
            return message
        case let .decoding(message):
            return "数据解析失败：\(message)"
        case let .network(message):
            return message
        }
    }
}
