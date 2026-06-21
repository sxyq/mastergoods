import SwiftUI

struct PrimaryGlassButton: View {
    let title: String
    var systemImage: String? = nil
    var disabled = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let systemImage {
                    Image(systemName: systemImage)
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                }
                Text(title)
                    .font(ZhihuijiTheme.Typography.bodyMedium)
            }
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, ZhihuijiTheme.Spacing.md + 2)
            .background(
                LinearGradient(
                    colors: [ZhihuijiTheme.ColorToken.primaryBright, ZhihuijiTheme.ColorToken.primary],
                    startPoint: .leading,
                    endPoint: .trailing
                ),
                in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.pill, style: .continuous)
            )
        }
        .disabled(disabled)
        .opacity(disabled ? 0.55 : 1)
        .shadow(color: ZhihuijiTheme.ShadowToken.glass, radius: 12, y: 6)
    }
}
