import Foundation

struct LoginRequest: Codable {
    let phone: String
    let password: String
}

struct RefreshRequest: Codable {
    let refreshToken: String
}

struct AuthPayload: Codable, Equatable {
    let userId: Int64
    let token: String
    let refreshToken: String?
    let expiresIn: Int?
}

struct UserProfile: Codable, Equatable {
    let id: Int64
    let phone: String
    let nickname: String
    let status: Int
}

struct APIEnvelope<T: Codable>: Codable {
    let code: Int?
    let message: String?
    let data: T
}
