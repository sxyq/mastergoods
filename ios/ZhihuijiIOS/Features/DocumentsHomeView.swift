import SwiftUI

struct DocumentsHomeView: View {
    @EnvironmentObject private var session: AppSession

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("单据中心")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

                if session.hasPermission(.salesView) {
                    NavigationLink {
                        SalesListView()
                    } label: {
                        DocumentsEntryCard(
                            title: "销售业务",
                            subtitle: "销售列表、详情、收款、退货",
                            tint: ZhihuijiTheme.ColorToken.primary
                        )
                    }
                    .buttonStyle(.plain)
                }

                if session.hasPermission(.purchaseView) {
                    NavigationLink {
                        PurchaseListView()
                    } label: {
                        DocumentsEntryCard(
                            title: "采购业务",
                            subtitle: "采购列表、详情、入库、退货",
                            tint: ZhihuijiTheme.ColorToken.warning
                        )
                    }
                    .buttonStyle(.plain)
                }

            }
            .padding(20)
        }
        .navigationTitle("单据")
    }
}

private struct DocumentsEntryCard: View {
    let title: String
    let subtitle: String
    let tint: Color

    var body: some View {
        HStack(spacing: 14) {
            Circle()
                .fill(tint.opacity(0.15))
                .frame(width: 42, height: 42)
                .overlay(
                    Image(systemName: "doc.text.fill")
                        .foregroundStyle(tint)
                )

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                Text(subtitle)
                    .font(.system(size: 13))
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            }

            Spacer()
            Image(systemName: "chevron.right")
                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
        }
        .padding(16)
        .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                .stroke(Color.white.opacity(0.5), lineWidth: 0.5)
        )
    }
}
