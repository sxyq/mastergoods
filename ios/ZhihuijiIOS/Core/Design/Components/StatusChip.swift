import SwiftUI

struct StatusChip: View {
    let title: String
    let tint: Color

    var body: some View {
        Text(title)
            .font(.system(size: 12, weight: .semibold))
            .foregroundStyle(tint)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(tint.opacity(0.12), in: Capsule())
            .overlay(
                Capsule().strokeBorder(.white.opacity(0.4), lineWidth: 0.5)
            )
    }
}
