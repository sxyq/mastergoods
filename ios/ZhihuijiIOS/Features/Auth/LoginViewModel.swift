import Foundation

@MainActor
final class LoginViewModel: ObservableObject {
    @Published var phone = "13800000001"
    @Published var password = "123456"
    @Published private(set) var isLoading = false
    @Published var errorMessage: String?

    func login(using client: APIClient, session: AppSession) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            let response = try await client.login(phone: phone, password: password)
            session.updateAuth(response)
            let store = try await client.fetchCurrentStore()
            session.updateStore(store)
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}
