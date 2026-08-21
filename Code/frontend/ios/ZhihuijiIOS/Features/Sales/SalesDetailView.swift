import Foundation
import SwiftUI

#if canImport(UIKit)
import UIKit
#endif

struct SalesDetailView: View {
    @Environment(\.appEnvironment) private var env
    @EnvironmentObject private var session: AppSession
    let orderId: EntityID
    @StateObject private var viewModel = SalesDetailViewModel()
    @State private var isReceiptExporting = false
    @State private var receiptMessage: String?

    private var actionPolicy: SalesDetailActionPolicy {
        SalesDetailActionPolicy.resolve(for: session.permissions)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if let errorMessage = viewModel.errorMessage {
                    EmptyStateView(title: "销售详情读取失败", message: errorMessage)
                } else if let order = viewModel.order {
                    if let receiptMessage {
                        Text(receiptMessage)
                            .font(ZhihuijiTheme.Typography.caption)
                            .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                            .padding(12)
                            .background(
                                Color.white.opacity(0.42),
                                in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.cardSmall, style: .continuous)
                            )
                    }
                    header(order: order)

                    VStack(alignment: .leading, spacing: 12) {
                        Text("商品明细")
                            .font(ZhihuijiTheme.Typography.sectionTitle)
                        ForEach(order.items) { item in
                            VStack(alignment: .leading, spacing: 6) {
                                Text(item.productName)
                                    .font(ZhihuijiTheme.Typography.cardTitle)
                                Text(item.productCode)
                                    .font(ZhihuijiTheme.Typography.caption)
                                    .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                                HStack {
                                    Text("数量 \(String(format: "%.2f", item.quantity))")
                                    Spacer()
                                    Text(item.amount.currencyText)
                                }
                                .font(ZhihuijiTheme.Typography.body)
                                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                            }
                            .padding(14)
                            .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
                        }
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Text("收款记录")
                                .font(ZhihuijiTheme.Typography.sectionTitle)
                            Spacer()
                            if actionPolicy.canOpenPayment {
                                NavigationLink {
                                    SalesPaymentView(initialOrderId: order.id)
                                } label: {
                                    StatusChip(title: "去收款", tint: ZhihuijiTheme.ColorToken.primary)
                                }
                                .buttonStyle(.plain)
                            }
                        }

                        if viewModel.payments.isEmpty {
                            EmptyStateView(title: "暂无收款记录", message: "当前销售单还没有收款记录。")
                        } else {
                            ForEach(viewModel.payments) { payment in
                                HStack {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(payment.amount.currencyText)
                                            .font(ZhihuijiTheme.Typography.cardTitle)
                                        Text(payment.createdAt.dateTimeText)
                                            .font(ZhihuijiTheme.Typography.caption)
                                            .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                                    }
                                    Spacer()
                                    VStack(alignment: .trailing, spacing: 4) {
                                        Text(SalePaymentMethod(rawValue: payment.method)?.label ?? "其他")
                                            .font(ZhihuijiTheme.Typography.bodyMedium)
                                        if let referenceNo = payment.referenceNo, !referenceNo.isEmpty {
                                            Text(referenceNo)
                                                .font(ZhihuijiTheme.Typography.caption)
                                                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                                        }
                                    }
                                }
                                .padding(14)
                                .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
                            }
                        }
                    }
                } else {
                    LoadingStateView(message: "正在加载销售详情...")
                }
            }
            .padding(20)
        }
        .navigationTitle("销售详情")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button {
                        Task { await printReceipt(order: viewModel.order) }
                    } label: {
                        Label("打印或存储 PDF", systemImage: "printer.fill")
                    }
                    Button {
                        Task { await shareReceiptPdf(order: viewModel.order) }
                    } label: {
                        Label("分享 PDF", systemImage: "square.and.arrow.up")
                    }
                } label: {
                    Image(systemName: "printer")
                }
                .disabled(viewModel.order == nil || isReceiptExporting)
            }
        }
        .task {
            await viewModel.load(orderId: orderId, client: env.apiClient)
        }
    }

    @MainActor
    private func printReceipt(order: SalesOrder?) async {
        guard let order, !isReceiptExporting else { return }
        isReceiptExporting = true
        defer { isReceiptExporting = false }
        do {
            let pdf = try await env.apiClient.fetchSaleOrderReceiptPdf(id: order.id)
#if canImport(UIKit)
            receiptMessage = presentSalesReceiptPrintPanel(pdfData: pdf, jobName: "销售单-\(order.orderNo)")
                ? "打印面板已打开。"
                : "系统打印面板未能唤起。"
#else
            receiptMessage = "小票 PDF 已生成。"
#endif
        } catch {
            receiptMessage = receiptExportErrorMessage(error)
        }
    }

    @MainActor
    private func shareReceiptPdf(order: SalesOrder?) async {
        guard let order, !isReceiptExporting else { return }
        isReceiptExporting = true
        defer { isReceiptExporting = false }
        do {
            let pdf = try await env.apiClient.fetchSaleOrderReceiptPdf(id: order.id)
#if canImport(UIKit)
            let url = try SalesReceiptPdfFile.write(pdf, orderNo: order.orderNo)
            receiptMessage = presentSalesReceiptActivitySheet(items: [url])
                ? "小票 PDF 已打开分享面板。"
                : "小票 PDF 已生成。"
#else
            receiptMessage = "小票 PDF 已生成。"
#endif
        } catch {
            receiptMessage = receiptExportErrorMessage(error)
        }
    }

    @ViewBuilder
    private func header(order: SalesOrder) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(order.orderNo)
                        .font(ZhihuijiTheme.Typography.pageTitle)
                    Text(order.customerName ?? "散客")
                        .font(ZhihuijiTheme.Typography.body)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                }
                Spacer()
                StatusChip(title: order.statusLabel, tint: order.statusTint)
            }

            HStack {
                metric("总额", order.totalAmount.currencyText)
                metric("已收", order.paidAmount.currencyText)
                metric("待收", order.outstandingAmount.currencyText)
            }
            if let notes = order.notes, !notes.isEmpty {
                Text(notes)
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
            }
        }
        .padding(16)
        .background(Color.white.opacity(0.58), in: RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: ZhihuijiTheme.Radius.card, style: .continuous)
                .stroke(Color.white.opacity(0.5), lineWidth: ZhihuijiTheme.Stroke.hairline)
        )
    }

    private func metric(_ title: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(ZhihuijiTheme.Typography.caption)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
            Text(value)
                .font(ZhihuijiTheme.Typography.bodyMedium)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private func receiptExportErrorMessage(_ error: Error) -> String {
    let message = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
    return "小票导出失败：\(message)"
}

#if canImport(UIKit)
private enum SalesReceiptPdfFile {
    static func write(_ data: Data, orderNo: String) throws -> URL {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent("sale-receipts", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let fileName = safeFileName(orderNo)
        let url = directory.appendingPathComponent("sale-receipt-\(fileName)-\(UUID().uuidString).pdf")
        try data.write(to: url, options: .atomic)
        return url
    }

    private static func safeFileName(_ value: String) -> String {
        let sanitized = value
            .replacingOccurrences(of: "/", with: "-")
            .replacingOccurrences(of: ":", with: "-")
        return sanitized.isEmpty ? "sale-order" : sanitized
    }
}

@MainActor
private func presentSalesReceiptActivitySheet(items: [Any]) -> Bool {
    guard let presenter = salesReceiptTopViewController() else { return false }
    let controller = UIActivityViewController(activityItems: items, applicationActivities: nil)
    if let popover = controller.popoverPresentationController {
        popover.sourceView = presenter.view
        popover.sourceRect = CGRect(x: presenter.view.bounds.midX, y: presenter.view.bounds.midY, width: 1, height: 1)
        popover.permittedArrowDirections = []
    }
    presenter.present(controller, animated: true)
    return true
}

@MainActor
private func presentSalesReceiptPrintPanel(pdfData: Data, jobName: String) -> Bool {
    guard salesReceiptTopViewController() != nil else { return false }
    let controller = UIPrintInteractionController.shared
    let printInfo = UIPrintInfo.printInfo()
    printInfo.jobName = jobName
    printInfo.outputType = .general
    controller.printInfo = printInfo
    controller.printingItem = pdfData
    return controller.present(animated: true, completionHandler: { _, _, _ in })
}

@MainActor
private func salesReceiptTopViewController(base: UIViewController? = nil) -> UIViewController? {
    let root: UIViewController? = base ?? {
        let activeScene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
        return activeScene?.windows.first(where: { $0.isKeyWindow })?.rootViewController
    }()

    if let navigation = root as? UINavigationController {
        return salesReceiptTopViewController(base: navigation.visibleViewController)
    }
    if let tab = root as? UITabBarController {
        return salesReceiptTopViewController(base: tab.selectedViewController)
    }
    if let presented = root?.presentedViewController {
        return salesReceiptTopViewController(base: presented)
    }
    return root
}
#endif

@MainActor
final class SalesDetailViewModel: ObservableObject {
    @Published var order: SalesOrder?
    @Published var payments: [SalePaymentRecord] = []
    @Published var errorMessage: String?

    func load(orderId: EntityID, client: APIClient) async {
        do {
            async let order = client.fetchSaleOrder(id: orderId)
            async let payments = client.fetchSaleOrderPayments(id: orderId)
            self.order = try await order
            self.payments = try await payments
            errorMessage = nil
        } catch {
            self.order = nil
            self.payments = []
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}
