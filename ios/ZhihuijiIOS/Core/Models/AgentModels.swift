import Foundation

enum AgentContractStatus {
    static let active = "active"
    static let closed = "closed"
    static let archived = "archived"
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
    let content: String?
    let toolCallId: String?
    let toolName: String?
    let inputSummary: String?
    let resultSummary: String?
    let errorCode: String?
    let errorSummary: String?
    let safeMessage: String?
    let finalAnswer: String?
    let mode: String?
    let llmStatus: String?
    let planSource: String?
    let reason: String?
    let block: AgentResultBlock?
    let timestamp: Int64?
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

    enum CodingKeys: String, CodingKey {
        case draftId = "draft_id"
        case draftType = "draft_type"
        case title
        case summary
        case itemCount = "item_count"
        case totalAmount = "total_amount"
        case partnerName = "partner_name"
        case warnings
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
