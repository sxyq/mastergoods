import SwiftUI

extension View {
    func fieldBackground() -> some View {
        padding(.horizontal, 14)
            .padding(.vertical, 14)
            .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous)
                    .stroke(Color.white.opacity(0.5), lineWidth: ZhihuijiTheme.Stroke.hairline)
            )
    }

    func glassCard(cornerRadius: CGFloat = ZhihuijiTheme.Radius.card) -> some View {
        background(
            ZhihuijiTheme.ColorToken.glassHigh,
            in: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
        )
        .overlay(
            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                .stroke(ZhihuijiTheme.ColorToken.glassBorder, lineWidth: ZhihuijiTheme.Stroke.hairline)
            )
        .shadow(color: ZhihuijiTheme.ShadowToken.glass, radius: 14, x: 0, y: 8)
    }

    @ViewBuilder
    func phoneInputKeyboard() -> some View {
        #if canImport(UIKit)
        keyboardType(.phonePad)
        #else
        self
        #endif
    }

    @ViewBuilder
    func numberInputKeyboard() -> some View {
        #if canImport(UIKit)
        keyboardType(.numberPad)
        #else
        self
        #endif
    }

    @ViewBuilder
    func decimalInputKeyboard() -> some View {
        #if canImport(UIKit)
        keyboardType(.decimalPad)
        #else
        self
        #endif
    }

    @ViewBuilder
    func inlineNavigationTitle() -> some View {
        #if canImport(UIKit)
        navigationBarTitleDisplayMode(.inline)
        #else
        self
        #endif
    }
}

struct AmountText: View {
    let value: String
    var tint: Color = ZhihuijiTheme.ColorToken.dataTextPrimary

    var body: some View {
        Text(value)
            .font(ZhihuijiTheme.Typography.tabularAmount)
            .foregroundStyle(tint)
            .monospacedDigit()
    }
}

struct TimestampText: View {
    let value: Int64?

    var body: some View {
        Text(value?.dateTimeText ?? "--")
            .font(ZhihuijiTheme.Typography.caption)
            .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
    }
}

struct GlassListRow<Leading: View, Trailing: View>: View {
    let leading: Leading
    let trailing: Trailing
    var action: (() -> Void)?

    init(
        action: (() -> Void)? = nil,
        @ViewBuilder leading: () -> Leading,
        @ViewBuilder trailing: () -> Trailing
    ) {
        self.action = action
        self.leading = leading()
        self.trailing = trailing()
    }

    var body: some View {
        rowContent
            .padding(14)
            .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
    }

    @ViewBuilder
    private var rowContent: some View {
        if let action {
            Button(action: action) {
                content
            }
            .buttonStyle(.plain)
        } else {
            content
        }
    }

    private var content: some View {
        HStack(alignment: .center, spacing: ZhihuijiTheme.Spacing.md) {
            leading
            Spacer(minLength: ZhihuijiTheme.Spacing.sm)
            trailing
        }
    }
}

extension GlassListRow where Trailing == EmptyView {
    init(
        action: (() -> Void)? = nil,
        @ViewBuilder leading: () -> Leading
    ) {
        self.action = action
        self.leading = leading()
        self.trailing = EmptyView()
    }
}

extension Int64 {
    var dateTimeText: String {
        SelfFormatters.dateTime.string(from: Date(timeIntervalSince1970: TimeInterval(self) / 1000))
    }

    var dateText: String {
        SelfFormatters.dateOnly.string(from: Date(timeIntervalSince1970: TimeInterval(self) / 1000))
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

private enum SelfFormatters {
    static let dateTime: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.dateFormat = "yyyy-MM-dd HH:mm"
        return formatter
    }()

    static let dateOnly: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }()
}
