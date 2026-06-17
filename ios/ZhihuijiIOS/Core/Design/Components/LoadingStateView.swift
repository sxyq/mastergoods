import SwiftUI

struct LoadingStateView: View {
    let message: String

    var body: some View {
        VStack(spacing: 14) {
            ProgressView()
                .tint(ZhihuijiTheme.ColorToken.primary)
            Text(message)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
