import SwiftUI

struct FeatureShellView: View {
    let title: String
    let subtitle: String
    let tags: [String]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(title)
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                Text(subtitle)
                    .font(.system(size: 14))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)

                LazyVGrid(columns: [GridItem(.adaptive(minimum: 110), spacing: 10)], spacing: 10) {
                    ForEach(tags, id: \.self) { tag in
                        StatusChip(title: tag, tint: ZhihuijiTheme.ColorToken.primary)
                    }
                }

                EmptyStateView(title: "页面骨架已就位", message: "下一步将接入真实 API、状态流转和表单交互。")
            }
            .padding(20)
        }
    }
}
