import SwiftUI

struct MetricCard: View {
    let title: String
    let value: String
    let subtitle: String
    let tint: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            Text(value)
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(tint)
            Text(subtitle)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                .stroke(.white.opacity(0.5), lineWidth: 0.5)
        )
        .shadow(color: ZhihuijiTheme.ShadowToken.glass, radius: 12, y: 6)
    }
}
