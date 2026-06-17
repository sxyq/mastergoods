import SwiftUI

struct DashboardView: View {
    @EnvironmentObject private var session: AppSession
    @Environment(\.appEnvironment) private var env
    @StateObject private var viewModel = DashboardViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("经营首页")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text(session.currentStore?.storeName ?? "当前门店")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }

                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    ForEach(viewModel.kpis) { item in
                        MetricCard(title: item.title, value: item.value, subtitle: item.subtitle, tint: ZhihuijiTheme.ColorToken.primary)
                    }
                }

                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "首页数据暂未拉起", message: errorMessage)
                }

                VStack(alignment: .leading, spacing: 12) {
                    Text("待处理提醒")
                        .font(.system(size: 20, weight: .semibold))
                    EmptyStateView(title: "提醒链路已就位", message: "下一步接真实 Dashboard 汇总、趋势、低库存和 AI 提醒接口。")
                }
            }
            .padding(20)
        }
        .navigationTitle("首页")
        .task {
            await viewModel.load(using: env.apiClient)
        }
    }
}
