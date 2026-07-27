import Foundation
import Security

final class AuthTokenStore {
    private let service = "com.zhihuiji.ios.auth"
    private let accessTokenKey = "zhihuiji.ios.access-token"
    private let refreshTokenKey = "zhihuiji.ios.refresh-token"

    func readAccessToken() -> String? {
        read(for: accessTokenKey)
    }

    func readRefreshToken() -> String? {
        read(for: refreshTokenKey)
    }

    func save(accessToken: String, refreshToken: String?) {
        save(value: accessToken, for: accessTokenKey)
        if let refreshToken, !refreshToken.isEmpty {
            save(value: refreshToken, for: refreshTokenKey)
        } else {
            delete(for: refreshTokenKey)
        }
    }

    func clear() {
        delete(for: accessTokenKey)
        delete(for: refreshTokenKey)
    }

    private func read(for key: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]
        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)
        guard status == errSecSuccess,
              let data = item as? Data,
              let value = String(data: data, encoding: .utf8) else {
            return nil
        }
        return value
    }

    private func save(value: String, for key: String) {
        let data = Data(value.utf8)
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
        ]
        let update: [String: Any] = [
            kSecValueData as String: data,
        ]
        let updateStatus = SecItemUpdate(query as CFDictionary, update as CFDictionary)
        if updateStatus == errSecItemNotFound {
            let insert: [String: Any] = [
                kSecClass as String: kSecClassGenericPassword,
                kSecAttrService as String: service,
                kSecAttrAccount as String: key,
                kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
                kSecValueData as String: data,
            ]
            SecItemAdd(insert as CFDictionary, nil)
        }
    }

    private func delete(for key: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
        ]
        SecItemDelete(query as CFDictionary)
    }
}
