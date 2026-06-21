import SwiftUI

struct FeatureShellView: View {
    let title: String
    let subtitle: String
    let tags: [String]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(title)
                    .font(ZhihuijiTheme.Typography.pageTitle)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                Text(subtitle)
                    .font(ZhihuijiTheme.Typography.body)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)

                LazyVGrid(columns: [GridItem(.adaptive(minimum: 110), spacing: 10)], spacing: 10) {
                    ForEach(tags, id: \.self) { tag in
                        StatusChip(title: tag, tint: ZhihuijiTheme.ColorToken.primary)
                    }
                }

                EmptyStateView(title: "当前暂无明细内容", message: "这个通用容器仍可承接少量权限页或说明页，但主业务模块应优先落到专页实现。")
            }
            .padding(20)
        }
    }
}
