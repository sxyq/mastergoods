import SwiftUI

struct MetricCard: View {
    let title: String
    let value: String
    let subtitle: String?
    let tint: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(ZhihuijiTheme.Typography.bodyMedium)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            Text(value)
                .font(ZhihuijiTheme.Typography.tabularAmount)
                .foregroundStyle(tint)
            if let subtitle, !subtitle.isEmpty {
                Text(subtitle)
                    .font(ZhihuijiTheme.Typography.captionSemibold)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(ZhihuijiTheme.Spacing.lg)
        .background(.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                .stroke(.white.opacity(0.5), lineWidth: ZhihuijiTheme.Stroke.hairline)
        )
        .shadow(color: ZhihuijiTheme.ShadowToken.glass, radius: 12, y: 6)
    }
}
