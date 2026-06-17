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
                        .font(.system(size: 15, weight: .semibold))
                }
                Text(title)
                    .font(.system(size: 16, weight: .semibold))
            }
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
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
