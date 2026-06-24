import XCTest
@testable import ZhihuijiIOS

final class ReportsViewModelTests: XCTestCase {
    func testMakeCSVAndPrintableHTMLReflectSeededReportData() throws {
        let viewModel = ReportsViewModel()
        viewModel.range = .month
        viewModel.salesSummary = SalesSummaryReport(
            startAt: 1710000000000,
            endAt: 1710086399000,
            totalSalesAmount: 1280,
            totalPaidAmount: 980,
            totalRefundAmount: 20,
            totalUnpaidAmount: 300,
            totalOrderCount: 3
        )
        viewModel.profitSummary = ProfitSummaryReport(
            startAt: 1710000000000,
            endAt: 1710086399000,
            estimatedCostAmount: 880,
            estimatedProfitAmount: 400,
            estimatedProfitRate: 0.3125
        )
        viewModel.cashflowSummary = CashflowSummaryReport(
            startAt: 1710000000000,
            endAt: 1710086399000,
            totalIncomeAmount: 1200,
            totalExpenseAmount: 430,
            netCashFlow: 770,
            totalRecordCount: 6
        )
        viewModel.reconciliation = ReconciliationSummaryReport(
            startAt: 1710000000000,
            endAt: 1710086399000,
            totalReceivableAmount: 300,
            totalPayableAmount: 120,
            totalReceivableCustomerCount: 2,
            totalPayableSupplierCount: 1,
            totalReceivedAmount: 980,
            totalPaidAmount: 430,
            netCashFlow: 550
        )
        viewModel.salesTrend = [
            SalesTrendPoint(startAt: 1710000000000, endAt: 1710003600000, totalSalesAmount: 88, totalOrderCount: 2)
        ]
        viewModel.topProducts = [
            TopSellingProductReport(productId: "1001", productCode: "P-001", productName: "测试商品", totalQuantity: 5, totalAmount: 300)
        ]
        viewModel.productProfits = [
            ProfitByProductReport(productId: "1001", productCode: "P-001", productName: "测试商品", totalSalesAmount: 300, totalCostAmount: 180, totalProfitAmount: 120, profitRate: 0.4)
        ]
        viewModel.customerSales = [
            CustomerSalesReport(customerId: "2001", customerName: "测试客户", totalOrders: 4, totalAmount: 500)
        ]
        viewModel.receivableCustomers = [
            CustomerReceivableReport(customerId: "2001", customerName: "测试客户", phone: "13800000001", balance: 300)
        ]
        viewModel.refunds = [
            RefundRecordReport(paymentId: "3001", orderId: "4001", orderNo: "S-001", customerName: "测试客户", refundAmount: 20, method: 1, referenceNo: "RF-01", createdAt: 1710000000000)
        ]
        viewModel.stockOutRecords = [
            StockOutRecordReport(orderId: "4001", orderNo: "S-001", customerId: "2001", customerName: "测试客户", productId: "1001", productCode: "P-001", productName: "测试商品", quantity: 2, unitPrice: 60, amount: 120, itemCreatedAt: 1710000000000, orderCreatedAt: 1710000000000)
        ]
        viewModel.lowStockProducts = [
            LowStockProductReport(productId: "1001", productCode: "P-001", productName: "测试商品", stock: 3, safeStock: 8)
        ]

        let csv = viewModel.makeCSV()
        XCTAssertTrue(csv.contains("\"overview\",\"销售额\",\"¥1280.00\",\"本月\""))
        XCTAssertTrue(csv.contains("\"overview\",\"净现金\",\"¥770.00\",\"现金流\""))
        XCTAssertTrue(csv.contains("\"trend\""))
        XCTAssertTrue(csv.contains("\"top_product\",\"测试商品\",\"¥300.00\",\"销量 5\""))
        XCTAssertTrue(csv.contains("\"product_profit\",\"测试商品\",\"¥120.00\",\"利润率 40.0%\""))
        XCTAssertTrue(csv.contains("\"customer_sales\",\"测试客户\",\"¥500.00\",\"订单 4\""))
        XCTAssertTrue(csv.contains("\"receivable\",\"测试客户\",\"¥300.00\",\"13800000001\""))
        XCTAssertTrue(csv.contains("\"refund\",\"S-001\",\"¥20.00\",\"测试客户\""))
        XCTAssertTrue(csv.contains("\"stock_out\",\"测试商品\",\"2.00\",\"¥120.00\""))
        XCTAssertTrue(csv.contains("\"low_stock\",\"测试商品\",\"3.00\",\"安全库存 8.00\""))

        let html = viewModel.makePrintableHTML()
        XCTAssertTrue(html.contains("<h1>经营报表</h1>"))
        XCTAssertTrue(html.contains("时间范围：本月"))
        XCTAssertTrue(html.contains("测试商品"))
        XCTAssertTrue(html.contains("¥1280.00"))

        let fileURL = try XCTUnwrap(viewModel.writeCSVToTemporaryFile())
        let writtenCSV = try String(contentsOf: fileURL, encoding: .utf8)
        XCTAssertEqual(writtenCSV, csv)
        try? FileManager.default.removeItem(at: fileURL)
    }

    func testReportRangeBucketMatchesTimeWindow() {
        XCTAssertEqual(ReportRange.today.bucket, "hour6")
        XCTAssertEqual(ReportRange.week.bucket, "day")
        XCTAssertEqual(ReportRange.month.bucket, "day")
    }
}
