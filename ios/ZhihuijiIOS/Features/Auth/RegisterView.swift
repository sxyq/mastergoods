import SwiftUI

struct RegisterView: View {
    @EnvironmentObject private var session: AppSession
    @Environment(\.appEnvironment) private var env
    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel = RegisterViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                VStack(alignment: .leading, spacing: 10) {
                    Text("注册账号")
                        .font(ZhihuijiTheme.Typography.pageTitle)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                    Text("使用手机号注册智慧记账号，注册成功后可由店长邀请加入门店。")
                        .font(ZhihuijiTheme.Typography.body)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }

                VStack(spacing: 14) {
                    TextField("请输入手机号", text: $viewModel.phone)
                        .phoneInputKeyboard()
                        .fieldBackground()

                    SecureField("请输入密码", text: $viewModel.password)
                        .fieldBackground()

                    HStack(spacing: 10) {
                        TextField("请输入验证码", text: $viewModel.verifyCode)
                            .numberInputKeyboard()
                            .fieldBackground()

                        Button {
                            Task { await viewModel.sendVerifyCode(using: env.apiClient) }
                        } label: {
                            Text(viewModel.verifyCodeButtonTitle)
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                                .foregroundStyle(viewModel.canSendVerifyCode ? .white : ZhihuijiTheme.ColorToken.textTertiary)
                                .frame(minWidth: 110)
                                .padding(.vertical, 14)
                                .background(
                                    (viewModel.canSendVerifyCode
                                        ? LinearGradient(colors: [ZhihuijiTheme.ColorToken.primaryBright, ZhihuijiTheme.ColorToken.primary], startPoint: .leading, endPoint: .trailing)
                                        : LinearGradient(colors: [Color.white.opacity(0.54), Color.white.opacity(0.54)], startPoint: .leading, endPoint: .trailing)
                                    ),
                                    in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.field, style: .continuous)
                                )
                        }
                        .buttonStyle(.plain)
                        .disabled(!viewModel.canSendVerifyCode)
                    }

                    if let errorMessage = viewModel.errorMessage {
                        Text(errorMessage)
                            .font(ZhihuijiTheme.Typography.bodyMedium)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.danger)
                    }

                    if let successMessage = viewModel.successMessage {
                        HStack(spacing: 8) {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundStyle(ZhihuijiTheme.ColorToken.success)
                            Text(successMessage)
                                .font(ZhihuijiTheme.Typography.bodyMedium)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                        }
                    }

                    PrimaryGlassButton(
                        title: viewModel.isLoading ? "注册中..." : "注册",
                        systemImage: "person.crop.circle.badge.plus",
                        disabled: viewModel.isLoading
                    ) {
                        Task {
                            await viewModel.register(using: env.apiClient, session: session)
                        }
                    }
                }
                .padding(18)
                .glassCard()

                HStack {
                    Spacer()
                    Text("已有账号？")
                        .font(ZhihuijiTheme.Typography.body)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    Button {
                        dismiss()
                    } label: {
                        Text("返回登录")
                            .font(ZhihuijiTheme.Typography.bodyMedium)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                    }
                    .buttonStyle(.plain)
                    Spacer()
                }
            }
            .padding(20)
            .padding(.top, 40)
        }
        .navigationTitle("注册")
    }
}

@MainActor
final class RegisterViewModel: ObservableObject {
    @Published var phone = ""
    @Published var password = ""
    @Published var verifyCode = ""
    @Published private(set) var isLoading = false
    @Published var errorMessage: String?
    @Published var successMessage: String?

    @Published private var countdownSeconds: Int = 0
    private var countdownTimer: Timer?

    var canSendVerifyCode: Bool {
        countdownSeconds == 0 && !phone.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var verifyCodeButtonTitle: String {
        countdownSeconds > 0 ? "重新发送(\(countdownSeconds)s)" : "发送验证码"
    }

    func register(using client: APIClient, session: AppSession) async {
        let phone = self.phone.trimmingCharacters(in: .whitespacesAndNewlines)
        let password = self.password.trimmingCharacters(in: .whitespacesAndNewlines)
        let verifyCode = self.verifyCode.trimmingCharacters(in: .whitespacesAndNewlines)

        if phone.isEmpty || password.isEmpty || verifyCode.isEmpty {
            errorMessage = "手机号、密码和验证码都需要填写"
            successMessage = nil
            return
        }

        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            let response = try await client.register(phone: phone, password: password, verifyCode: verifyCode)
            session.updateAuth(response)
            successMessage = "注册成功，正在同步门店信息"
            errorMessage = nil
            do {
                let store = try await client.fetchCurrentStore()
                session.updateStore(store)
            } catch {
                // 新注册账号可能尚未加入门店，忽略门店同步错误
            }
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
            successMessage = nil
        }
    }

    func sendVerifyCode(using client: APIClient) async {
        let phone = self.phone.trimmingCharacters(in: .whitespacesAndNewlines)
        if phone.isEmpty {
            errorMessage = "请先输入手机号"
            successMessage = nil
            return
        }

        errorMessage = nil
        do {
            let response = try await client.issueVerifyCode(phone: phone, type: "register")
            if response.success {
                successMessage = "验证码已发送，有效期 \(response.expireSeconds) 秒"
                startCountdown(seconds: min(response.expireSeconds, 60))
            } else {
                errorMessage = "验证码发送失败，请稍后重试"
            }
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
            successMessage = nil
        }
    }

    private func startCountdown(seconds: Int) {
        countdownTimer?.invalidate()
        countdownSeconds = max(seconds, 1)
        countdownTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] timer in
            Task { @MainActor in
                guard let self else {
                    timer.invalidate()
                    return
                }
                self.countdownSeconds -= 1
                if self.countdownSeconds <= 0 {
                    timer.invalidate()
                    self.countdownSeconds = 0
                }
            }
        }
    }

    deinit {
        countdownTimer?.invalidate()
    }
}
