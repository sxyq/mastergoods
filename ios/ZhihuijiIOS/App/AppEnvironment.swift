import Foundation
import SwiftUI

struct AppEnvironment {
    let apiBaseURL: URL
    let apiClient: APIClient
    let tokenStore: AuthTokenStore
    
    static let baseURLDefaultsKey = "zhihuiji.ios.base_url"
    static let defaultBaseURLString = "http://127.0.0.1:8080"

    static var live: AppEnvironment {
        environment(from: UserDefaults.standard.string(forKey: baseURLDefaultsKey) ?? defaultBaseURLString)
            ?? environment(from: defaultBaseURLString)!
    }

    static func environment(from baseURLString: String) -> AppEnvironment? {
        let trimmed = baseURLString.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let url = URL(string: trimmed),
              let scheme = url.scheme?.lowercased(),
              ["http", "https"].contains(scheme),
              url.host?.isEmpty == false else {
            return nil
        }

        let tokenStore = AuthTokenStore()
        return AppEnvironment(
            apiBaseURL: url,
            apiClient: APIClient(baseURL: url, tokenStore: tokenStore),
            tokenStore: tokenStore
        )
    }
}

@MainActor
final class AppEnvironmentStore: ObservableObject {
    @Published private(set) var current: AppEnvironment
    @Published var draftBaseURL: String
    @Published var message: String?

    init() {
        let live = AppEnvironment.live
        current = live
        draftBaseURL = live.apiBaseURL.absoluteString
    }

    func applyBaseURL() -> Bool {
        guard let environment = AppEnvironment.environment(from: draftBaseURL) else {
            message = "接口地址无效，请输入完整的 http:// 或 https:// 地址"
            return false
        }
        current = environment
        draftBaseURL = environment.apiBaseURL.absoluteString
        UserDefaults.standard.set(environment.apiBaseURL.absoluteString, forKey: AppEnvironment.baseURLDefaultsKey)
        message = "已切换到 \(environment.apiBaseURL.absoluteString)"
        return true
    }

    func resetBaseURL() {
        draftBaseURL = AppEnvironment.defaultBaseURLString
        _ = applyBaseURL()
    }

    func clearMessage() {
        message = nil
    }
}

private struct AppEnvironmentKey: EnvironmentKey {
    static let defaultValue = AppEnvironment.live
}

extension EnvironmentValues {
    var appEnvironment: AppEnvironment {
        get { self[AppEnvironmentKey.self] }
        set { self[AppEnvironmentKey.self] = newValue }
    }
}
