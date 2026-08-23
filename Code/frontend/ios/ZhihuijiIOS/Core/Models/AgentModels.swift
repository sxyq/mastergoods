import Foundation

enum AgentContractStatus {
    static let active = "active"
    static let closed = "closed"
    static let archived = "archived"
}

/// 草稿确认相关状态字符串，与后端 `drafts.status` 字段保持一致。
/// 客户端不会绕过草稿确认直接调用正式业务创建接口，所以这些状态是终态与二次授权的依据。
enum AgentDraftStatus {
    static let pendingConfirmation = "pending_confirmation"
    static let confirmed = "confirmed"
    static let rejected = "rejected"
    static let cancelled = "cancelled"
    static let expired = "expired"
    static let active = "active"
    static let archived = "archived"

    /// 是否需要展示二次确认弹窗。
    static func requiresConfirmation(_ status: String?) -> Bool {
        guard let status = status?.lowercased() else { return false }
        return status == pendingConfirmation || status == active
    }

    /// 是否已经进入终态（不能再确认）。
    static func isTerminal(_ status: String?) -> Bool {
        guard let status = status?.lowercased() else { return false }
        return [
            confirmed,
            rejected,
            cancelled,
            expired,
            archived,
        ].contains(status)
    }

    /// 是否已经确认写入。
    static func isConfirmed(_ status: String?) -> Bool {
        status?.lowercased() == confirmed
    }
}

enum AgentAccessibilityPolicy {
    static func animationEnabled(reduceMotion: Bool) -> Bool { !reduceMotion }
    static func contrastStrokeWidth(increasedContrast: Bool) -> Double { increasedContrast ? 2 : 1 }
    static func dynamicTypeAllowsMultiline() -> Bool { true }
}

/// Agent 运行的终态枚举。后端通过 `terminal_status` 字段下发，大小写不敏感。
/// - 注意：`exhausted` 表示轮次耗尽，前端不应展示为成功样式。
enum TerminalStatus: String, Codable, Equatable {
    case completed = "COMPLETED"
    case confirmationPending = "CONFIRMATION_PENDING"
    case failed = "FAILED"
    case blocked = "BLOCKED"
    case cancelled = "CANCELLED"
    case exhausted = "EXHAUSTED"

    /// 大小写不敏感的解析，避免后端字段大小写变化导致解析失败。
    init?(ciRawValue raw: String) {
        switch raw.uppercased() {
        case "COMPLETED": self = .completed
        case "CONFIRMATION_PENDING": self = .confirmationPending
        case "FAILED": self = .failed
        case "BLOCKED": self = .blocked
        case "CANCELLED": self = .cancelled
        case "EXHAUSTED": self = .exhausted
        default: return nil
        }
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let raw = try? container.decode(String.self),
           let value = TerminalStatus(ciRawValue: raw) {
            self = value
            return
        }
        self = .failed
    }

    /// 是否属于"成功"终态。`confirmationPending` 不计入成功，需要二次确认才算写入。
    var isSuccessful: Bool { self == .completed }

    /// 是否需要触发二次确认弹窗。
    var requiresConfirmation: Bool { self == .confirmationPending }

    /// 是否属于"非成功"终态，不应展示成功样式。
    var isFailure: Bool {
        switch self {
        case .failed, .blocked, .cancelled, .exhausted:
            return true
        case .completed, .confirmationPending:
            return false
        }
    }
}

struct AgentConversationSummary: Identifiable, Codable, Equatable {
    let id: EntityID
    let title: String
    let status: String?
    let latestSummary: String?
    let createdAt: Int64?
    let updatedAt: Int64
    let lastMessageAt: Int64?
}

struct AgentMessage: Identifiable, Codable, Equatable {
    let id: EntityID
    let conversationId: EntityID?
    let role: String
    let messageType: String?
    let content: String
    let structuredDataJson: String?
    let createdAt: Int64
}

struct AgentWorkbench: Codable, Equatable {
    let greeting: String
    let kpiCards: [AgentKpiCard]
    let quickQuestions: [String]
    let recentConversations: [AgentRecentConversation]
    let pendingDrafts: [AgentPendingDraft]
    let riskAlerts: [AgentRiskAlert]
    let todaySummary: String?
    let status: String?
    let dataPolicy: String?
    let capabilities: [AgentCapability]
    let warnings: [String]
}

struct AgentCapability: Identifiable, Codable, Equatable {
    let id: String
    let title: String
    let description: String
}

struct AgentKpiCard: Identifiable, Codable, Equatable {
    var id: String { label + route.orEmpty }
    let label: String
    let value: String
    let trendDirection: String?
    let trendValue: String?
    let route: String?
}

struct AgentRecentConversation: Identifiable, Codable, Equatable {
    let id: EntityID
    let title: String
    let lastMessageAt: Int64?
    let messageCount: Int?
}

struct AgentPendingDraft: Identifiable, Codable, Equatable {
    let id: EntityID
    let draftType: String
    let title: String
    let createdAt: Int64
}

struct AgentDraft: Identifiable, Codable, Equatable {
    let id: EntityID
    let conversationId: EntityID?
    let draftType: String
    let title: String
    let contentJson: String
    let status: String?
    let createdAt: Int64
    let updatedAt: Int64
}

struct AgentRiskAlert: Identifiable, Codable, Equatable {
    var id: String { level + title }
    let level: String
    let title: String
    let description: String
}

struct AgentTask: Identifiable, Codable, Equatable {
    let id: EntityID
    let taskType: String
    let title: String
    let triggerSource: String?
    let status: String?
    let statusLabel: String?
    let progress: Int?
    let inputText: String?
    let resultJson: String?
    let createdAt: Int64?
    let updatedAt: Int64?
    let completedAt: Int64?
}

struct AgentNotification: Identifiable, Codable, Equatable {
    let id: EntityID
    let taskId: EntityID?
    let title: String
    let body: String
    let level: String?
    let isRead: Bool?
    let isDelivered: Bool?
    let createdAt: Int64
}

struct AgentChatPayload: Codable {
    let conversationId: EntityID?
    let message: String
    let stream: Bool
}

struct AgentDraftCreatePayload: Codable {
    let conversationId: EntityID?
    let draftType: String
    let title: String
    let contentJson: String
    let status: String?
}

struct AgentDraftUpdatePayload: Codable {
    let conversationId: EntityID?
    let draftType: String
    let title: String
    let contentJson: String
    let status: String?
}

struct AgentChatResponse: Codable, Equatable {
    let runId: String
    let conversationId: EntityID
    let answer: String
    let blocks: [AgentResultBlock]
    let draftId: EntityID?
    let safetyPassed: Bool?
    let safetyReason: String?
    let mode: String?
    let llmStatus: String?
    let planSource: String?
    let planSummary: String?
    let toolCalls: [AgentToolCall]
    let evidenceRefs: [AgentEvidenceRef]
    let resultBlocks: [AgentResultBlock]
    let performanceSummary: AgentPerformanceSummary?
    let auditId: String?
    let traceId: String?
    let observability: AgentObservability?
    let terminalStatus: TerminalStatus? = nil
    let errorCode: String? = nil
    let safeMessage: String? = nil
    let completedTools: [String] = []
    let missingTargetTools: [String] = []
}

struct AgentResultBlock: Identifiable, Codable, Equatable {
    var id: String { [blockType, title.orEmpty].joined(separator: ":") }
    let blockType: String
    let title: String?
    let data: JSONValue?
}

struct AgentToolCall: Identifiable, Codable, Equatable {
    let toolCallId: String
    let toolName: String
    let status: String?
    let summaryPreview: String? = nil
    let inputSummary: String?
    let returnedCount: Int?
    let totalCount: Int?
    let limit: Int?
    let isTruncated: Bool?
    let durationMs: Int64?
    let resultSummary: String?
    let errorCode: String?
    let errorMessage: String?

    var id: String { toolCallId }
}

struct AgentEvidenceRef: Identifiable, Codable, Equatable {
    let evidenceId: String
    let toolCallId: String?
    let toolName: String?
    let label: String?
    let value: String?
    let isTruncated: Bool?

    var id: String { evidenceId }
}

struct AgentPerformanceSummary: Codable, Equatable {
    let startedAt: Int64?
    let completedAt: Int64?
    let durationMs: Int64?
    let toolDurationMs: Int64?
    let modelDurationMs: Int64?
}

struct AgentObservability: Codable, Equatable {
    let requestId: String?
    let correlationId: String?
    let traceId: String?
    let auditId: String?
    let logRef: String?
}

struct AgentNotificationReadResponse: Codable, Equatable {
    let id: EntityID
    let taskId: EntityID?
    let title: String
    let body: String
    let level: String?
    let isRead: Bool?
    let isDelivered: Bool?
    let createdAt: Int64
}

struct AgentRunAudit: Identifiable, Codable, Equatable {
    var id: String { runId }
    let runId: String
    let ownerUserId: EntityID?
    let conversationId: EntityID?
    let status: String?
    let mode: String?
    let llmStatus: String?
    let planSource: String?
    let toolCount: Int?
    let eventCount: Int?
    let auditWriteDroppedCount: Int?
    let auditWriteFailedCount: Int?
    let auditLossy: Bool?
    let emittedEventCount: Int?
    let warnings: [String]
    let auditId: String?
    let traceId: String?
    let errorCode: String?
    let errorMessage: String?
    let startedAt: Int64?
    let completedAt: Int64?
    let updatedAt: Int64?
    let events: [AgentRunAuditEvent]
}

struct AgentRunAuditEvent: Identifiable, Codable, Equatable {
    let eventId: String
    let seq: Int
    let eventType: String
    let payload: JSONValue?
    let createdAt: Int64

    var id: String { eventId }
}

struct AgentRunCancelResponse: Codable, Equatable {
    let runId: String
    let status: String?
    let cancelled: Bool?
}

struct AgentStreamEvent: Codable, Equatable {
    let eventType: String
    let runId: String?
    let conversationId: EntityID?
    let seq: Int?
    let eventId: String?
    let delta: String?
    let deltaSource: String?
    let content: String?
    let answer: String?
    let toolCallId: String?
    let toolName: String?
    let inputSummary: String?
    let message: String?
    let resultSummary: String?
    let errorCode: String?
    let errorSummary: String?
    let safeMessage: String?
    let finalAnswer: String?
    let draftId: EntityID?
    let draftType: String?
    let title: String?
    let suggestedAction: String?
    let compactedCount: Int?
    let summary: String?
    let mode: String?
    let llmStatus: String?
    let planSource: String?
    let reason: String?
    let block: AgentResultBlock?
    let timestamp: Int64?
    let terminalStatus: TerminalStatus?
    let completedTools: [String]?
    let missingTargetTools: [String]?
    let status: String?

    enum CodingKeys: String, CodingKey {
        case eventType = "event_type"
        case runId = "run_id"
        case conversationId = "conversation_id"
        case seq
        case eventId = "event_id"
        case delta
        case deltaSource = "delta_source"
        case content
        case answer
        case toolCallId = "tool_call_id"
        case toolName = "tool_name"
        case inputSummary = "input_summary"
        case message
        case resultSummary = "result_summary"
        case errorCode = "error_code"
        case errorSummary = "error_summary"
        case safeMessage = "safe_message"
        case finalAnswer = "final_answer"
        case draftId = "draft_id"
        case draftType = "draft_type"
        case title
        case suggestedAction = "suggested_action"
        case compactedCount = "compacted_count"
        case summary
        case summaryPreview = "summary_preview"
        case mode
        case llmStatus = "llm_status"
        case planSource = "plan_source"
        case reason
        case block
        case timestamp
        case terminalStatus = "terminal_status"
        case completedTools = "completed_tools"
        case missingTargetTools = "missing_target_tools"
        case status
    }
}

struct AgentLiveRunPreview: Equatable {
    let runId: String
    let conversationId: EntityID?
    var answer: String
    var planSummary: String?
    var toolCalls: [AgentToolCall]
    var resultBlocks: [AgentResultBlock]
    var mode: String?
    var llmStatus: String?
    var planSource: String?
}

struct AgentTextBlockData: Codable, Equatable {
    let text: String?
    let markdown: String?
}

struct AgentLineChartBlockData: Codable, Equatable {
    let title: String?
    let labels: [String]
    let series: [AgentChartSeries]
}

struct AgentBarChartBlockData: Codable, Equatable {
    let title: String?
    let labels: [String]
    let series: [AgentChartSeries]
}

struct AgentDonutChartBlockData: Codable, Equatable {
    let title: String?
    let segments: [AgentChartSegment]
}

struct AgentChartSeries: Codable, Equatable, Identifiable {
    var id: String { [name, color ?? ""].joined(separator: ":") }

    let name: String
    let data: [Double]
    let color: String?
}

struct AgentChartSegment: Codable, Equatable, Identifiable {
    var id: String { [name, color ?? ""].joined(separator: ":") }

    let name: String
    let value: Double
    let color: String?
}

struct AgentEvidenceCardBlockData: Codable, Equatable {
    let title: String?
    let items: [EvidenceItem]

    struct EvidenceItem: Codable, Equatable, Identifiable {
        var id: String { [label, toolCallId ?? "", source ?? ""].joined(separator: ":") }

        let label: String
        let value: String
        let source: String?
        let toolCallId: String?
        let queryWindow: JSONValue?
        let isTruncated: Bool?

        enum CodingKeys: String, CodingKey {
            case label
            case value
            case source
            case toolCallId = "tool_call_id"
            case queryWindow = "query_window"
            case isTruncated = "is_truncated"
        }
    }
}

struct AgentDraftCardBlockData: Codable, Equatable {
    let draftId: EntityID
    let draftType: String
    let title: String
    let summary: String
    let itemCount: Int?
    let totalAmount: String?
    let partnerName: String?
    let warnings: [String]?
    let status: String?
    let actionLabel: String?

    enum CodingKeys: String, CodingKey {
        case draftId = "draft_id"
        case draftType = "draft_type"
        case title
        case summary
        case itemCount = "item_count"
        case totalAmount = "total_amount"
        case partnerName = "partner_name"
        case warnings
        case status
        case actionLabel = "action_label"
    }
}

enum JSONValue: Codable, Equatable {
    case string(String)
    case number(Double)
    case bool(Bool)
    case object([String: JSONValue])
    case array([JSONValue])
    case null

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if container.decodeNil() {
            self = .null
        } else if let value = try? container.decode(Bool.self) {
            self = .bool(value)
        } else if let value = try? container.decode(Double.self) {
            self = .number(value)
        } else if let value = try? container.decode(String.self) {
            self = .string(value)
        } else if let value = try? container.decode([String: JSONValue].self) {
            self = .object(value)
        } else if let value = try? container.decode([JSONValue].self) {
            self = .array(value)
        } else {
            throw DecodingError.dataCorruptedError(in: container, debugDescription: "Unsupported JSON value")
        }
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        switch self {
        case let .string(value):
            try container.encode(value)
        case let .number(value):
            try container.encode(value)
        case let .bool(value):
            try container.encode(value)
        case let .object(value):
            try container.encode(value)
        case let .array(value):
            try container.encode(value)
        case .null:
            try container.encodeNil()
        }
    }
}

extension JSONValue {
    func decode<T: Decodable>(as type: T.Type, decoder: JSONDecoder = JSONDecoder()) -> T? {
        guard let rawJSONObject else { return nil }
        guard JSONSerialization.isValidJSONObject(rawJSONObject) else { return nil }
        guard let data = try? JSONSerialization.data(withJSONObject: rawJSONObject) else { return nil }
        return try? decoder.decode(T.self, from: data)
    }

    var rawJSONObject: Any? {
        switch self {
        case let .string(value):
            return value
        case let .number(value):
            return value
        case let .bool(value):
            return value
        case let .object(value):
            return value.mapValues { $0.rawJSONObject ?? NSNull() }
        case let .array(value):
            return value.map { $0.rawJSONObject ?? NSNull() }
        case .null:
            return NSNull()
        }
    }
}

extension AgentMessage {
    var isAssistant: Bool { role == "assistant" }

    var resultBlocks: [AgentResultBlock] {
        AgentResultBlock.decodeList(from: structuredDataJson)
    }
}

extension AgentResultBlock {
    static func decodeList(from structuredDataJson: String?) -> [AgentResultBlock] {
        guard let structuredDataJson = structuredDataJson?.trimmingCharacters(in: .whitespacesAndNewlines),
              !structuredDataJson.isEmpty,
              let data = structuredDataJson.data(using: .utf8) else {
            return []
        }

        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase

        if let blocks = try? decoder.decode([AgentResultBlock].self, from: data) {
            return blocks
        }

        if let envelope = try? decoder.decode(AgentResultBlockEnvelope.self, from: data) {
            if !envelope.resultBlocks.isEmpty {
                return envelope.resultBlocks
            }
            return envelope.blocks
        }

        return []
    }
}

private struct AgentResultBlockEnvelope: Codable {
    let blocks: [AgentResultBlock]
    let resultBlocks: [AgentResultBlock]

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        blocks = try container.decodeIfPresent([AgentResultBlock].self, forKey: .blocks) ?? []
        resultBlocks = try container.decodeIfPresent([AgentResultBlock].self, forKey: .resultBlocks) ?? []
    }
}

extension AgentNotification {
    var levelTint: String {
        switch level?.lowercased() {
        case "high", "danger": return "danger"
        case "medium", "warning": return "warning"
        default: return "primary"
        }
    }
}

private extension Optional where Wrapped == String {
    var orEmpty: String { self ?? "" }
}
