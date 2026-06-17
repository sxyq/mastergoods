import SwiftUI

extension View {
    func fieldBackground() -> some View {
        padding(.horizontal, 14)
            .padding(.vertical, 14)
            .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous)
                    .stroke(Color.white.opacity(0.5), lineWidth: 0.5)
            )
    }

    func glassCard(cornerRadius: CGFloat = ZhihuijiTheme.Radius.card) -> some View {
        background(
            ZhihuijiTheme.ColorToken.glassHigh,
            in: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
        )
        .overlay(
            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                .stroke(ZhihuijiTheme.ColorToken.glassBorder, lineWidth: 0.5)
        )
        .shadow(color: ZhihuijiTheme.ShadowToken.glass, radius: 14, x: 0, y: 8)
    }
}

extension Int64 {
    var dateTimeText: String {
        let date = Date(timeIntervalSince1970: TimeInterval(self) / 1000)
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.dateFormat = "yyyy-MM-dd HH:mm"
        return formatter.string(from: date)
    }

    var dateText: String {
        let date = Date(timeIntervalSince1970: TimeInterval(self) / 1000)
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }
}

extension Int {
    var boolFlag: Bool { self != 0 }
}

extension String {
    var nilIfBlank: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}
