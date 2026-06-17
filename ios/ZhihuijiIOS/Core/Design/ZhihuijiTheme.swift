import SwiftUI

enum ZhihuijiTheme {
    enum ColorToken {
        static let primary = Color(hex: 0x005BBF)
        static let primaryBright = Color(hex: 0x1A73E8)
        static let primaryLight = Color(hex: 0xD8E2FF)
        static let success = Color(hex: 0x34A853)
        static let warning = Color(hex: 0xFB8C00)
        static let danger = Color(hex: 0xEA4335)
        static let textPrimary = Color(hex: 0x181C20)
        static let textSecondary = Color(hex: 0x414754)
        static let textTertiary = Color(hex: 0x6B7280)
        static let backgroundStart = Color(hex: 0xE8F1FF)
        static let backgroundMid = Color(hex: 0xF5F8FF)
        static let backgroundEnd = Color.white
        static let auroraBlue = Color(hex: 0x93C5FD)
        static let auroraIndigo = Color(hex: 0xA5B4FC)
        static let auroraCyan = Color(hex: 0xBAE6FD)
        static let glassLow = Color.white.opacity(0.34)
        static let glassMedium = Color.white.opacity(0.40)
        static let glassHigh = Color.white.opacity(0.58)
        static let glassBorder = Color.white.opacity(0.50)
        static let divider = Color(hex: 0xC1C6D6)
    }

    enum Radius {
        static let field: CGFloat = 14
        static let card: CGFloat = 16
        static let pill: CGFloat = 24
    }

    enum ShadowToken {
        static let glass = Color(red: 0, green: 36 / 255, blue: 104 / 255, opacity: 0.08)
    }
}

struct ZhihuijiBackground: ViewModifier {
    func body(content: Content) -> some View {
        ZStack {
            LinearGradient(
                colors: [
                    ZhihuijiTheme.ColorToken.backgroundStart,
                    ZhihuijiTheme.ColorToken.backgroundMid,
                    ZhihuijiTheme.ColorToken.backgroundEnd,
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            Circle()
                .fill(ZhihuijiTheme.ColorToken.auroraBlue.opacity(0.22))
                .frame(width: 280, height: 280)
                .blur(radius: 42)
                .offset(x: -120, y: -260)

            Circle()
                .fill(ZhihuijiTheme.ColorToken.auroraCyan.opacity(0.14))
                .frame(width: 240, height: 240)
                .blur(radius: 36)
                .offset(x: -40, y: -140)

            Circle()
                .fill(ZhihuijiTheme.ColorToken.auroraIndigo.opacity(0.16))
                .frame(width: 340, height: 340)
                .blur(radius: 56)
                .offset(x: 180, y: 280)

            content
        }
    }
}

extension View {
    func zhihuijiBackground() -> some View {
        modifier(ZhihuijiBackground())
    }
}

extension Color {
    init(hex: UInt, alpha: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: alpha
        )
    }
}
