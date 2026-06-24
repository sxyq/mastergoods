import XCTest
@testable import ZhihuijiIOS

final class PlanningOverviewViewModelTests: XCTestCase {
    func testPlanningOverviewViewModelSummarizesModulesAndMilestones() {
        let viewModel = PlanningOverviewViewModel()

        XCTAssertEqual(viewModel.modules.count, 4)
        XCTAssertEqual(viewModel.milestones.count, 3)
        XCTAssertEqual(viewModel.nextSteps.count, 5)
        XCTAssertEqual(viewModel.overallProgress, 68)

        XCTAssertEqual(viewModel.modules.first?.title, "后端 V2 API")
        XCTAssertEqual(viewModel.modules[2].title, "iOS App")
        XCTAssertEqual(viewModel.milestones.first?.status, "已完成")
        XCTAssertEqual(viewModel.milestones[2].status, "规划中")
    }
}
