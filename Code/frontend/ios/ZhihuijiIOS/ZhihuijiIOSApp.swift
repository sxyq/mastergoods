import SwiftUI

@main
struct ZhihuijiIOSApp: App {
    @StateObject private var session = AppSession()
    @StateObject private var environmentStore = AppEnvironmentStore()

    var body: some Scene {
        WindowGroup {
            AppRouter()
                .environmentObject(session)
                .environmentObject(environmentStore)
                .environment(\.appEnvironment, environmentStore.current)
                .task {
                    await session.bootstrap()
                }
        }
    }
}
