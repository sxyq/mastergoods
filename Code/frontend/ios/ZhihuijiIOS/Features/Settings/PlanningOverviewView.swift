import SwiftUI

struct PlanningOverviewView: View {
    @StateObject private var viewModel = PlanningOverviewViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                headerSection
                overallProgressSection
                moduleGridSection
                milestoneSection
                nextStepSection
            }
            .padding(20)
        }
        .navigationTitle("经营规划")
    }

    private var headerSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("经营规划概览")
                .font(ZhihuijiTheme.Typography.pageTitle)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

            Text("智慧记 / Master-Goods 产品规划总览：覆盖后端 V2 API、Android、iOS、Web 管理端四个模块的进度、里程碑与下一步建议。")
                .font(ZhihuijiTheme.Typography.body)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(16)
        .glassCard()
    }

    private var overallProgressSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("整体进度")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            HStack(spacing: 20) {
                overallProgressRing

                VStack(alignment: .leading, spacing: 8) {
                    Text("\(viewModel.overallProgress)%")
                        .font(ZhihuijiTheme.Typography.amount)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.primary)
                    Text("综合完成度")
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Text("四个模块加权平均")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                }
                Spacer()
            }
        }
        .padding(16)
        .glassCard()
    }

    private var overallProgressRing: some View {
        let progress = viewModel.overallProgress
        let clamped = max(0, min(1, Double(progress) / 100))
        return ZStack {
            Circle()
                .stroke(ZhihuijiTheme.ColorToken.surfaceGray, lineWidth: 14)
            Circle()
                .trim(from: 0, to: clamped)
                .stroke(
                    LinearGradient(
                        colors: [ZhihuijiTheme.ColorToken.primaryBright, ZhihuijiTheme.ColorToken.primary],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    style: StrokeStyle(lineWidth: 14, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))
            VStack(spacing: 2) {
                Text("\(progress)%")
                    .font(ZhihuijiTheme.Typography.bodyMedium)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
            }
        }
        .frame(width: 108, height: 108)
    }

    private var moduleGridSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("模块进度")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(viewModel.modules) { module in
                    moduleCard(module)
                }
            }
        }
    }

    private func moduleCard(_ module: PlanModule) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                Circle()
                    .fill(statusTint(module.status).opacity(0.14))
                    .frame(width: 32, height: 32)
                    .overlay(
                        Image(systemName: moduleIcon(module.title))
                            .font(ZhihuijiTheme.Typography.caption)
                            .foregroundStyle(statusTint(module.status))
                    )
                Spacer()
                StatusChip(title: module.status, tint: statusTint(module.status))
            }

            Text(module.title)
                .font(ZhihuijiTheme.Typography.cardTitle)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)

            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text("\(module.progress)%")
                        .font(ZhihuijiTheme.Typography.captionSemibold)
                        .foregroundStyle(statusTint(module.status))
                    Spacer()
                    Text(progressLabel(module.progress))
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                }
                GeometryReader { proxy in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 4, style: .continuous)
                            .fill(ZhihuijiTheme.ColorToken.surfaceGray)
                            .frame(height: 6)
                        RoundedRectangle(cornerRadius: 4, style: .continuous)
                            .fill(statusTint(module.status))
                            .frame(width: proxy.size.width * CGFloat(module.progress) / 100, height: 6)
                    }
                }
                .frame(height: 6)
            }

            Text(module.description)
                .font(ZhihuijiTheme.Typography.caption)
                .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(14)
        .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
    }

    private var milestoneSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("里程碑")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            ForEach(viewModel.milestones) { milestone in
                milestoneRow(milestone)
            }
        }
    }

    private func milestoneRow(_ milestone: PlanMilestone) -> some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(spacing: 0) {
                Circle()
                    .fill(statusTint(milestone.status))
                    .frame(width: 14, height: 14)
                Rectangle()
                    .fill(ZhihuijiTheme.ColorToken.divider.opacity(0.5))
                    .frame(width: 1)
                    .frame(maxHeight: .infinity)
            }
            .frame(width: 14)

            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(milestone.title)
                        .font(ZhihuijiTheme.Typography.bodyMedium)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                    Spacer()
                    StatusChip(title: milestone.status, tint: statusTint(milestone.status))
                }
                Text(milestone.description)
                    .font(ZhihuijiTheme.Typography.caption)
                    .foregroundStyle(ZhihuijiTheme.ColorToken.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
                HStack(spacing: 10) {
                    Text("进度 \(milestone.progress)%")
                        .font(ZhihuijiTheme.Typography.caption)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textTertiary)
                }
            }
            .padding(.bottom, 8)
        }
    }

    private var nextStepSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("下一步建议")
                .font(ZhihuijiTheme.Typography.sectionTitle)

            ForEach(Array(viewModel.nextSteps.enumerated()), id: \.offset) { index, step in
                HStack(alignment: .top, spacing: 12) {
                    Text("\(index + 1)")
                        .font(ZhihuijiTheme.Typography.captionSemibold)
                        .foregroundStyle(.white)
                        .frame(width: 24, height: 24)
                        .background(
                            LinearGradient(
                                colors: [ZhihuijiTheme.ColorToken.primaryBright, ZhihuijiTheme.ColorToken.primary],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            in: Circle()
                        )
                    Text(step)
                        .font(ZhihuijiTheme.Typography.body)
                        .foregroundStyle(ZhihuijiTheme.ColorToken.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                    Spacer(minLength: 0)
                }
                .padding(12)
                .glassCard(cornerRadius: ZhihuijiTheme.Radius.cardSmall)
            }
        }
    }

    private func statusTint(_ status: String) -> Color {
        switch status.lowercased() {
        case "completed", "done", "已交付", "已完成":
            return ZhihuijiTheme.ColorToken.success
        case "in_progress", "进行中", "开发中":
            return ZhihuijiTheme.ColorToken.primary
        case "planned", "规划中", "待启动":
            return ZhihuijiTheme.ColorToken.warning
        case "blocked", "阻塞":
            return ZhihuijiTheme.ColorToken.danger
        default:
            return ZhihuijiTheme.ColorToken.primary
        }
    }

    private func moduleIcon(_ title: String) -> String {
        switch title {
        case let t where t.contains("后端") || t.contains("API"):
            return "server.rack"
        case let t where t.contains("Android"):
            return "robot"
        case let t where t.contains("iOS"):
            return "iphone"
        case let t where t.contains("Web") || t.contains("管理"):
            return "macwindow"
        default:
            return "square.dashed"
        }
    }

    private func progressLabel(_ progress: Int) -> String {
        switch progress {
        case 100: return "已完成"
        case 80...: return "收尾中"
        case 50...: return "过半"
        case 20...: return "推进中"
        default: return "起步"
        }
    }
}

@MainActor
final class PlanningOverviewViewModel: ObservableObject {
    let modules: [PlanModule]
    let milestones: [PlanMilestone]
    let nextSteps: [String]

    var overallProgress: Int {
        guard !modules.isEmpty else { return 0 }
        let total = modules.reduce(0) { $0 + $1.progress }
        return total / modules.count
    }

    init() {
        modules = PlanModule.defaultModules
        milestones = PlanMilestone.defaultMilestones
        nextSteps = PlanningOverviewViewModel.defaultNextSteps
    }

    private static let defaultNextSteps: [String] = [
        "完成后端 V2 API 剩余接口的回归测试与文档补齐，确保前端契约稳定。",
        "推进 Android 端库存与报表模块的功能补齐，对齐 iOS 端能力。",
        "iOS 端继续完善 Agent 工作台/草稿/任务专页与库存台账的交互细节。",
        "Web 管理端补齐经营规划看板与权限矩阵的可视化呈现。",
        "启动 Phase 3 的多端联调与性能压测，准备灰度发布。",
    ]
}

struct PlanModule: Identifiable {
    let id: String
    let title: String
    let status: String
    let progress: Int
    let description: String

    init(id: String, title: String, status: String, progress: Int, description: String) {
        self.id = id
        self.title = title
        self.status = status
        self.progress = progress
        self.description = description
    }

    static let defaultModules: [PlanModule] = [
        PlanModule(
            id: "backend-v2",
            title: "后端 V2 API",
            status: "进行中",
            progress: 82,
            description: "Spring Boot V2 接口已覆盖销售、采购、库存、资金、报表、Agent 等域，剩余同步与导入相关接口待补齐测试。"
        ),
        PlanModule(
            id: "android-app",
            title: "Android App",
            status: "进行中",
            progress: 70,
            description: "Compose + MVVM 架构，已覆盖主要业务域，库存盘点与 Agent 工作台正在补齐。"
        ),
        PlanModule(
            id: "ios-app",
            title: "iOS App",
            status: "进行中",
            progress: 65,
            description: "SwiftUI + MVVM，已补齐库存台账、Agent 工作台/草稿/任务专页与经营规划页面。"
        ),
        PlanModule(
            id: "web-admin",
            title: "Web 管理端",
            status: "进行中",
            progress: 58,
            description: "Vue/Vite 管理端已覆盖基础单据与档案，经营规划看板与权限矩阵待完善。"
        ),
    ]
}

struct PlanMilestone: Identifiable {
    let id: String
    let title: String
    let status: String
    let progress: Int
    let description: String

    init(id: String, title: String, status: String, progress: Int, description: String) {
        self.id = id
        self.title = title
        self.status = status
        self.progress = progress
        self.description = description
    }

    static let defaultMilestones: [PlanMilestone] = [
        PlanMilestone(
            id: "phase-1",
            title: "Phase 1 · 基础业务域",
            status: "已完成",
            progress: 100,
            description: "销售、采购、库存、资金、档案等核心业务域的 V2 API 与多端基础页面落地。"
        ),
        PlanMilestone(
            id: "phase-2",
            title: "Phase 2 · 智能与报表",
            status: "进行中",
            progress: 68,
            description: "Agent 工作台、草稿、任务通知、报表看板与库存台账专页在多端逐步对齐。"
        ),
        PlanMilestone(
            id: "phase-3",
            title: "Phase 3 · 联调与发布",
            status: "规划中",
            progress: 15,
            description: "多端联调、性能压测、灰度发布与正式上线，覆盖同步与导入的端到端验证。"
        ),
    ]
}
