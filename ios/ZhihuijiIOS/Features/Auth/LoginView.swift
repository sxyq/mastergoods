import SwiftUI

struct LoginView: View {
    @EnvironmentObject private var session: AppSession
    @EnvironmentObject private var environmentStore: AppEnvironmentStore
    @Environment(\.appEnvironment) private var env
    @StateObject private var viewModel = LoginViewModel()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("智慧记")
                            .font(.system(size: 34, weight: .bold))
                            .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                        Text("iOS 原生端会沿用安卓移动端的玻璃感与业务语义，不走 Web PC 风格。")
                            .font(.system(size: 14))
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    }

                    VStack(spacing: 14) {
                        ZhihuijiTextField(title: "手机号", text: $viewModel.phone)
                        ZhihuijiTextField(title: "密码", text: $viewModel.password, secure: true)
                        if let errorMessage = viewModel.errorMessage {
                            Text(errorMessage)
                                .font(.system(size: 13, weight: .medium))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
                        }
                        PrimaryGlassButton(title: viewModel.isLoading ? "登录中..." : "登录", systemImage: "arrow.right.circle.fill", disabled: viewModel.isLoading) {
                            Task {
                                await viewModel.login(using: env.apiClient, session: session)
                            }
                        }
                    }
                    .padding(18)
                    .background(.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                            .stroke(.white.opacity(0.5), lineWidth: 0.5)
                    )

                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Text("连接设置")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                            Spacer()
                            Text(env.apiBaseURL.absoluteString)
                                .font(.system(size: 11, weight: .medium))
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                                .lineLimit(1)
                        }

                        ZhihuijiTextField(title: "后端地址", text: $environmentStore.draftBaseURL)

                        if let message = environmentStore.message {
                            Text(message)
                                .font(.system(size: 12, weight: .medium))
                                .foregroundStyle(message.contains("无效") ? ZhihuijiTheme.ColorToken.danger : ZhihuijiTheme.ColorToken.success)
                        }

                        HStack(spacing: 10) {
                            Button {
                                _ = environmentStore.applyBaseURL()
                            } label: {
                                Text("保存地址")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(.white)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                    .background(
                                        LinearGradient(
                                            colors: [ZhihuijiTheme.ColorToken.primaryBright, ZhihuijiTheme.ColorToken.primary],
                                            startPoint: .leading,
                                            endPoint: .trailing
                                        ),
                                        in: RoundedRectangle(cornerRadius: 14, style: .continuous)
                                    )
                            }
                            .buttonStyle(.plain)

                            Button {
                                environmentStore.resetBaseURL()
                            } label: {
                                Text("恢复默认")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                    .background(Color.white.opacity(0.42), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                                            .stroke(Color.white.opacity(0.5), lineWidth: 0.5)
                                    )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(18)
                    .background(.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                            .stroke(.white.opacity(0.5), lineWidth: 0.5)
                    )
                }
                .padding(20)
                .padding(.top, 80)
            }
        }
    }
}

private struct ZhihuijiTextField: View {
    let title: String
    @Binding var text: String
    var secure = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            field
        }
    }

    @ViewBuilder
    private var field: some View {
        if secure {
            SecureField("请输入\(title)", text: $text)
                .fieldStyle()
        } else {
            TextField("请输入\(title)", text: $text)
                .fieldStyle()
        }
    }
}

private extension View {
    func fieldStyle() -> some View {
        padding(.horizontal, 14)
            .padding(.vertical, 14)
            .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous)
                    .stroke(ZhihuijiTheme.ColorToken.divider, lineWidth: 1)
            )
    }
}
