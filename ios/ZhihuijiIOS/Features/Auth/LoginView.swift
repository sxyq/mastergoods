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
                            .font(ZhihuijiTheme.Typography.pageTitle)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                        Text("iOS 原生端延续安卓移动端的玻璃感和业务语义，提供更顺手的经营操作体验。")
                            .font(ZhihuijiTheme.Typography.body)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    }

                    VStack(spacing: 14) {
                        ZhihuijiTextField(title: "手机号", text: $viewModel.phone)
                        ZhihuijiTextField(title: "密码", text: $viewModel.password, secure: true)

                        if let errorMessage = viewModel.errorMessage {
                            Text(errorMessage)
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
                        }

                        PrimaryGlassButton(
                            title: viewModel.isLoading ? "登录中..." : "登录",
                            systemImage: "arrow.right.circle.fill",
                            disabled: viewModel.isLoading
                        ) {
                            Task {
                                await viewModel.login(using: env.apiClient, session: session)
                            }
                        }
                    }
                    .padding(18)
                    .background(.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                            .stroke(.white.opacity(0.5), lineWidth: ZhihuijiTheme.Stroke.hairline)
                    )

                    HStack {
                        Spacer()
                        Text("没有账号？")
                            .font(ZhihuijiTheme.Typography.body)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                        NavigationLink {
                            RegisterView()
                        } label: {
                            Text("立即注册")
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                        }
                        Spacer()
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Text("连接设置")
                                .font(ZhihuijiTheme.Typography.cardTitle)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                            Spacer()
                            Text(env.apiBaseURL.absoluteString)
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                                .lineLimit(1)
                        }

                        ZhihuijiTextField(title: "后端地址", text: $environmentStore.draftBaseURL)

                        if let message = environmentStore.message {
                            Text(message)
                                .font(ZhihuijiTheme.Typography.caption)
                                .foregroundStyle(message.contains("无效") ? ZhihuijiTheme.ColorToken.danger : ZhihuijiTheme.ColorToken.success)
                        }

                        HStack(spacing: 10) {
                            Button {
                                _ = environmentStore.applyBaseURL()
                            } label: {
                                Text("保存地址")
                                    .font(ZhihuijiTheme.Typography.bodyMedium)
                                    .foregroundStyle(.white)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                    .background(
                                        LinearGradient(
                                            colors: [ZhihuijiTheme.ColorToken.primaryBright, ZhihuijiTheme.ColorToken.primary],
                                            startPoint: .leading,
                                            endPoint: .trailing
                                        ),
                                        in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous)
                                    )
                            }
                            .buttonStyle(.plain)

                            Button {
                                environmentStore.resetBaseURL()
                            } label: {
                                Text("恢复默认")
                                    .font(ZhihuijiTheme.Typography.bodyMedium)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                    .background(
                                        Color.white.opacity(0.42),
                                        in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous)
                                    )
                                    .overlay(
                                        RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous)
                                            .stroke(Color.white.opacity(0.5), lineWidth: ZhihuijiTheme.Stroke.hairline)
                                    )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(18)
                    .background(.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                            .stroke(.white.opacity(0.5), lineWidth: ZhihuijiTheme.Stroke.hairline)
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
                .font(ZhihuijiTheme.Typography.captionSemibold)
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
