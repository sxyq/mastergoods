import SwiftUI

struct EmptyStateView: View {
    let title: String
    let message: String

    var body: some View {
        VStack(spacing: 10) {
            Text(title)
                .font(ZhihuijiTheme.Typography.sectionTitle)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
            Text(message)
                .font(ZhihuijiTheme.Typography.body)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(ZhihuijiTheme.Spacing.xxl)
        .background(.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                .stroke(.white.opacity(0.5), lineWidth: ZhihuijiTheme.Stroke.hairline)
        )
    }
}
