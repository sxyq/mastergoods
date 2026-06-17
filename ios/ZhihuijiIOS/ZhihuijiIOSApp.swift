import SwiftUI

@main
struct ZhihuijiIOSApp: App {
    @StateObject private var session = AppSession()

    var body: some Scene {
        WindowGroup {
            AppRouter()
                .environmentObject(session)
                .environment(\.appEnvironment, .live)
                .task {
                    await session.bootstrap()
                }
        }
    }
}
