import Foundation
import XCTest
@testable import ZhihuijiIOS

/// 覆盖 Agent 终态解析、草稿确认状态机、重复确认幂等、拒绝不写入的测试。
final class AgentTerminalStatusTests: XCTestCase {
    override func setUp() {
        super.setUp()
        MockURLProtocol.requestHandler = nil
        MockURLProtocol.requestCount = 0
        MockURLProtocol.didStopLoading = false
        MockURLProtocol.holdOpen = false
    }

    override func tearDown() {
        MockURLProtocol.requestHandler = nil
        MockURLProtocol.requestCount = 0
        MockURLProtocol.didStopLoading = false
        MockURLProtocol.holdOpen = false
        super.tearDown()
    }

    // MARK: - TerminalStatus 解析

    func testTerminalStatusParsesAllStatesCaseInsensitively() {
        XCTAssertEqual(TerminalStatus(ciRawValue: "COMPLETED"), .completed)
        XCTAssertEqual(TerminalStatus(ciRawValue: "completed"), .completed)
        XCTAssertEqual(TerminalStatus(ciRawValue: "Completed"), .completed)

        XCTAssertEqual(TerminalStatus(ciRawValue: "CONFIRMATION_PENDING"), .confirmationPending)
        XCTAssertEqual(TerminalStatus(ciRawValue: "confirmation_pending"), .confirmationPending)

        XCTAssertEqual(TerminalStatus(ciRawValue: "FAILED"), .failed)
        XCTAssertEqual(TerminalStatus(ciRawValue: "BLOCKED"), .blocked)
        XCTAssertEqual(TerminalStatus(ciRawValue: "CANCELLED"), .cancelled)
        XCTAssertEqual(TerminalStatus(ciRawValue: "EXHAUSTED"), .exhausted)

        XCTAssertNil(TerminalStatus(ciRawValue: "unknown"))
        XCTAssertNil(TerminalStatus(ciRawValue: ""))
    }

    func testTerminalStatusSemanticProperties() {
        XCTAssertTrue(TerminalStatus.completed.isSuccessful)
        XCTAssertFalse(TerminalStatus.confirmationPending.isSuccessful)
        XCTAssertFalse(TerminalStatus.failed.isSuccessful)

        XCTAssertTrue(TerminalStatus.confirmationPending.requiresConfirmation)
        XCTAssertFalse(TerminalStatus.completed.requiresConfirmation)
        XCTAssertFalse(TerminalStatus.failed.requiresConfirmation)

        XCTAssertTrue(TerminalStatus.failed.isFailure)
        XCTAssertTrue(TerminalStatus.blocked.isFailure)
        XCTAssertTrue(TerminalStatus.cancelled.isFailure)
        XCTAssertTrue(TerminalStatus.exhausted.isFailure)
        XCTAssertFalse(TerminalStatus.completed.isFailure)
        XCTAssertFalse(TerminalStatus.confirmationPending.isFailure)
    }

    func testTerminalStatusDecodesFromJSON() throws {
        let data = Data("\"CONFIRMATION_PENDING\"".utf8)
        let status = try JSONDecoder().decode(TerminalStatus.self, from: data)
        XCTAssertEqual(status, .confirmationPending)

        // 大小写不敏感。
        let lowerData = Data("\"confirmation_pending\"".utf8)
        let lowerStatus = try JSONDecoder().decode(TerminalStatus.self, from: lowerData)
        XCTAssertEqual(lowerStatus, .confirmationPending)
    }

    func testTerminalStatusUnknownStringFallsBackToFailed() throws {
        let data = Data("\"SOMETHING_NEW\"".utf8)
        let status = try JSONDecoder().decode(TerminalStatus.self, from: data)
        // 未知值降级为 failed，避免前端误判为成功。
        XCTAssertEqual(status, .failed)
    }

    // MARK: - AgentStreamEvent 终态字段

    func testAgentStreamEventDecodesTerminalStatusAndTools() throws {
        let data = Data(
            """
            {
              "event_type": "run_completed",
              "run_id": "run-001",
              "terminal_status": "COMPLETED",
              "safe_message": "运行完成",
              "completed_tools": ["sales_lookup", "inventory_low_stock_lookup"],
              "missing_target_tools": [],
              "final_answer": "summary text",
              "timestamp": 1710000000000
            }
            """.utf8
        )

        let event = try JSONDecoder().decode(AgentStreamEvent.self, from: data)
        XCTAssertEqual(event.eventType, "run_completed")
        XCTAssertEqual(event.terminalStatus, .completed)
        XCTAssertEqual(event.safeMessage, "运行完成")
        XCTAssertEqual(event.completedTools, ["sales_lookup", "inventory_low_stock_lookup"])
        XCTAssertEqual(event.missingTargetTools, [])
        XCTAssertEqual(event.finalAnswer, "summary text")
    }

    func testAgentStreamEventDecodesFailedTerminalStatus() throws {
        let data = Data(
            """
            {
              "event_type": "run_failed",
              "run_id": "run-failed-001",
              "terminal_status": "FAILED",
              "safe_message": "token budget exhausted",
              "error_code": "E_LLM_TIMEOUT",
              "timestamp": 1710000000001
            }
            """.utf8
        )

        let event = try JSONDecoder().decode(AgentStreamEvent.self, from: data)
        XCTAssertEqual(event.eventType, "run_failed")
        XCTAssertEqual(event.terminalStatus, .failed)
        XCTAssertEqual(event.errorCode, "E_LLM_TIMEOUT")
        XCTAssertEqual(event.safeMessage, "token budget exhausted")
    }

    func testAgentStreamEventDecodesBlockedAndCancelled() throws {
        let blockedData = Data(
            """
            {
              "event_type": "run_blocked",
              "run_id": "run-blocked",
              "terminal_status": "BLOCKED",
              "safe_message": "safety policy triggered",
              "timestamp": 1710000000002
            }
            """.utf8
        )
        let cancelledData = Data(
            """
            {
              "event_type": "run_cancelled",
              "run_id": "run-cancelled",
              "terminal_status": "CANCELLED",
              "safe_message": "user cancelled",
              "timestamp": 1710000000003
            }
            """.utf8
        )

        let blocked = try JSONDecoder().decode(AgentStreamEvent.self, from: blockedData)
        XCTAssertEqual(blocked.eventType, "run_blocked")
        XCTAssertEqual(blocked.terminalStatus, .blocked)

        let cancelled = try JSONDecoder().decode(AgentStreamEvent.self, from: cancelledData)
        XCTAssertEqual(cancelled.eventType, "run_cancelled")
        XCTAssertEqual(cancelled.terminalStatus, .cancelled)
    }

    func testAgentStreamEventDecodesExhaustedWithMissingTools() throws {
        let data = Data(
            """
            {
              "event_type": "run_exhausted",
              "run_id": "run-exhausted-001",
              "terminal_status": "EXHAUSTED",
              "safe_message": "round limit reached",
              "missing_target_tools": ["draft_create"],
              "timestamp": 1710000000004
            }
            """.utf8
        )

        let event = try JSONDecoder().decode(AgentStreamEvent.self, from: data)
        XCTAssertEqual(event.eventType, "run_exhausted")
        XCTAssertEqual(event.terminalStatus, .exhausted)
        XCTAssertEqual(event.missingTargetTools, ["draft_create"])
    }

    func testAgentStreamEventDecodesDraftCreatedWithStatus() throws {
        let data = Data(
            """
            {
              "event_type": "draft_created",
              "run_id": "run-draft",
              "draft_id": "99001",
              "draft_type": "purchase_plan",
              "title": "补货草稿",
              "status": "pending_confirmation",
              "timestamp": 1710000000005
            }
            """.utf8
        )

        let event = try JSONDecoder().decode(AgentStreamEvent.self, from: data)
        XCTAssertEqual(event.draftId?.rawValue, "99001")
        XCTAssertEqual(event.draftType, "purchase_plan")
        XCTAssertEqual(event.title, "补货草稿")
        XCTAssertEqual(event.status, "pending_confirmation")
    }

    func testAgentStreamEventDecodesContextCompacted() throws {
        let data = Data(
            """
            {
              "event_type": "context_compacted",
              "run_id": "run-context",
              "compacted_count": 18,
              "summary": "已整理早期追问",
              "timestamp": 1710000000006
            }
            """.utf8
        )

        let event = try JSONDecoder().decode(AgentStreamEvent.self, from: data)
        XCTAssertEqual(event.eventType, "context_compacted")
        XCTAssertEqual(event.compactedCount, 18)
        XCTAssertEqual(event.summary, "已整理早期追问")
    }

    // MARK: - AgentDraftCardBlockData 状态字段

    func testAgentDraftCardBlockDataDecodesStatusAndActionLabel() throws {
        let data = Data(
            """
            {
              "draft_id": "90001",
              "draft_type": "purchase_plan",
              "title": "补货草稿",
              "summary": "等待二次确认",
              "item_count": 3,
              "total_amount": "1280.00",
              "partner_name": "华东供货中心",
              "warnings": [],
              "status": "pending_confirmation",
              "action_label": "二次确认"
            }
            """.utf8
        )

        let block = try JSONDecoder().decode(AgentDraftCardBlockData.self, from: data)
        XCTAssertEqual(block.draftId.rawValue, "90001")
        XCTAssertEqual(block.draftType, "purchase_plan")
        XCTAssertEqual(block.status, "pending_confirmation")
        XCTAssertEqual(block.actionLabel, "二次确认")
        XCTAssertEqual(block.itemCount, 3)
        XCTAssertEqual(block.totalAmount, "1280.00")
    }

    func testAgentDraftCardBlockDataToleratesMissingStatus() throws {
        // 旧版本后端可能不下发 status / action_label。
        let data = Data(
            """
            {
              "draft_id": "90001",
              "draft_type": "purchase_plan",
              "title": "补货草稿",
              "summary": "等待二次确认"
            }
            """.utf8
        )

        let block = try JSONDecoder().decode(AgentDraftCardBlockData.self, from: data)
        XCTAssertEqual(block.draftId.rawValue, "90001")
        XCTAssertNil(block.status)
        XCTAssertNil(block.actionLabel)
        XCTAssertNil(block.itemCount)
    }

    // MARK: - AgentDraftStatus 辅助函数

    func testAgentDraftStatusHelpers() {
        XCTAssertTrue(AgentDraftStatus.requiresConfirmation(AgentDraftStatus.pendingConfirmation))
        XCTAssertTrue(AgentDraftStatus.requiresConfirmation(AgentDraftStatus.active))
        XCTAssertFalse(AgentDraftStatus.requiresConfirmation(AgentDraftStatus.confirmed))
        XCTAssertFalse(AgentDraftStatus.requiresConfirmation(AgentDraftStatus.rejected))
        XCTAssertFalse(AgentDraftStatus.requiresConfirmation(AgentDraftStatus.cancelled))
        XCTAssertFalse(AgentDraftStatus.requiresConfirmation(nil))

        XCTAssertTrue(AgentDraftStatus.isConfirmed(AgentDraftStatus.confirmed))
        XCTAssertFalse(AgentDraftStatus.isConfirmed(AgentDraftStatus.pendingConfirmation))
        XCTAssertFalse(AgentDraftStatus.isConfirmed(nil))

        XCTAssertTrue(AgentDraftStatus.isTerminal(AgentDraftStatus.confirmed))
        XCTAssertTrue(AgentDraftStatus.isTerminal(AgentDraftStatus.rejected))
        XCTAssertTrue(AgentDraftStatus.isTerminal(AgentDraftStatus.cancelled))
        XCTAssertTrue(AgentDraftStatus.isTerminal(AgentDraftStatus.expired))
        XCTAssertTrue(AgentDraftStatus.isTerminal(AgentDraftStatus.archived))
        XCTAssertFalse(AgentDraftStatus.isTerminal(AgentDraftStatus.pendingConfirmation))
        XCTAssertFalse(AgentDraftStatus.isTerminal(AgentDraftStatus.active))
        XCTAssertFalse(AgentDraftStatus.isTerminal(nil))
    }

    // MARK: - AgentViewModel 终态状态机

    @MainActor
    func testAgentViewModelResetsTerminalStatusOnRunStarted() {
        let viewModel = AgentViewModel()
        viewModel.terminalStatus = .failed
        viewModel.terminalMessage = "previous failure"
        viewModel.missingTargetTools = ["some_tool"]

        let event = makeEvent(eventType: "run_started", runId: "run-2")
        viewModel.consume(event)

        XCTAssertNil(viewModel.terminalStatus)
        XCTAssertNil(viewModel.terminalMessage)
        XCTAssertTrue(viewModel.missingTargetTools.isEmpty)
        XCTAssertEqual(viewModel.liveRun?.runId, "run-2")
    }

    @MainActor
    func testAgentViewModelAppliesCompletedTerminalStatus() {
        let viewModel = AgentViewModel()

        let event = makeEvent(
            eventType: "run_completed",
            runId: "run-1",
            terminalStatus: .completed,
            safeMessage: "本轮分析完成",
            finalAnswer: "summary",
            completedTools: ["sales_lookup"]
        )
        viewModel.consume(event)

        XCTAssertEqual(viewModel.terminalStatus, .completed)
        XCTAssertEqual(viewModel.terminalMessage, "本轮分析完成")
        XCTAssertEqual(viewModel.completedTools, ["sales_lookup"])
    }

    @MainActor
    func testAgentViewModelAppliesFailedTerminalStatus() {
        let viewModel = AgentViewModel()

        let event = makeEvent(
            eventType: "run_failed",
            runId: "run-failed",
            terminalStatus: .failed,
            safeMessage: "LLM 超时",
            errorCode: "E_LLM_TIMEOUT"
        )
        viewModel.consume(event)

        XCTAssertEqual(viewModel.terminalStatus, .failed)
        XCTAssertEqual(viewModel.terminalMessage, "LLM 超时")
        XCTAssertNotNil(viewModel.errorMessage)
        XCTAssertTrue(viewModel.terminalStatus?.isFailure == true)
    }

    @MainActor
    func testAgentViewModelAppliesBlockedTerminalStatus() {
        let viewModel = AgentViewModel()

        let event = makeEvent(
            eventType: "run_blocked",
            runId: "run-blocked",
            terminalStatus: .blocked,
            safeMessage: "安全策略阻止"
        )
        viewModel.consume(event)

        XCTAssertEqual(viewModel.terminalStatus, .blocked)
        XCTAssertEqual(viewModel.terminalMessage, "安全策略阻止")
    }

    @MainActor
    func testAgentViewModelAppliesCancelledTerminalStatus() {
        let viewModel = AgentViewModel()

        let event = makeEvent(
            eventType: "run_cancelled",
            runId: "run-cancelled",
            terminalStatus: .cancelled
        )
        viewModel.consume(event)

        XCTAssertEqual(viewModel.terminalStatus, .cancelled)
    }

    @MainActor
    func testAgentViewModelAppliesExhaustedTerminalStatusAndMissingTools() {
        let viewModel = AgentViewModel()

        let event = makeEvent(
            eventType: "run_exhausted",
            runId: "run-exhausted",
            terminalStatus: .exhausted,
            safeMessage: "轮次耗尽",
            missingTargetTools: ["draft_create"]
        )
        viewModel.consume(event)

        XCTAssertEqual(viewModel.terminalStatus, .exhausted)
        XCTAssertEqual(viewModel.terminalMessage, "轮次耗尽")
        XCTAssertEqual(viewModel.missingTargetTools, ["draft_create"])
        // EXHAUSTED 不应展示成功样式。
        XCTAssertTrue(viewModel.terminalStatus?.isFailure == true)
        XCTAssertFalse(viewModel.terminalStatus?.isSuccessful == true)
    }

    @MainActor
    func testAgentViewModelInfersTerminalStatusWhenBackendOmitsField() {
        // 老版本后端可能不下发 terminal_status，只下发 event_type。
        let viewModel = AgentViewModel()

        viewModel.consume(makeEvent(eventType: "run_completed", runId: "r1"))
        XCTAssertEqual(viewModel.terminalStatus, .completed)

        viewModel.consume(makeEvent(eventType: "run_failed", runId: "r2", safeMessage: "fail"))
        XCTAssertEqual(viewModel.terminalStatus, .failed)
        XCTAssertEqual(viewModel.terminalMessage, "fail")

        viewModel.consume(makeEvent(eventType: "run_blocked", runId: "r3"))
        XCTAssertEqual(viewModel.terminalStatus, .blocked)

        viewModel.consume(makeEvent(eventType: "run_cancelled", runId: "r4"))
        XCTAssertEqual(viewModel.terminalStatus, .cancelled)

        viewModel.consume(makeEvent(eventType: "run_exhausted", runId: "r5"))
        XCTAssertEqual(viewModel.terminalStatus, .exhausted)
    }

    @MainActor
    func testAgentViewModelDraftCreatedTriggersConfirmationForPendingStatus() {
        let viewModel = AgentViewModel()

        let event = makeEvent(
            eventType: "draft_created",
            runId: "run-draft",
            draftId: EntityID(rawValue: "99001"),
            draftType: "purchase_plan",
            title: "补货草稿",
            status: AgentDraftStatus.pendingConfirmation,
            timestamp: 1710000000000
        )
        viewModel.consume(event)

        XCTAssertEqual(viewModel.pendingConfirmationDraft?.id.rawValue, "99001")
        XCTAssertTrue(viewModel.isConfirmationPresented)
        XCTAssertEqual(viewModel.confirmationState, .idle)
    }

    @MainActor
    func testAgentViewModelDraftCreatedSkipsConfirmationForConfirmedStatus() {
        let viewModel = AgentViewModel()

        let event = makeEvent(
            eventType: "draft_created",
            runId: "run-draft-confirmed",
            draftId: EntityID(rawValue: "99002"),
            draftType: "purchase_plan",
            title: "已确认草稿",
            status: AgentDraftStatus.confirmed,
            timestamp: 1710000000001
        )
        viewModel.consume(event)

        // 已确认的草稿不应再触发二次确认弹窗。
        XCTAssertNil(viewModel.pendingConfirmationDraft)
        XCTAssertFalse(viewModel.isConfirmationPresented)
    }

    @MainActor
    func testAgentViewModelRejectDoesNotCallConfirmEndpoint() {
        let viewModel = AgentViewModel()
        let draft = makeDraft(id: "99003", status: AgentDraftStatus.pendingConfirmation)
        viewModel.requestConfirmation(for: draft)
        XCTAssertTrue(viewModel.isConfirmationPresented)

        // 拒绝：仅本地状态变更，不应触发任何 API 调用。
        viewModel.rejectPendingDraft()

        XCTAssertEqual(viewModel.confirmationState, .rejected)
        XCTAssertFalse(viewModel.isConfirmationPresented)
        XCTAssertNil(viewModel.pendingConfirmationDraft)
    }

    @MainActor
    func testAgentViewModelDismissConfirmationDoesNotConfirm() {
        let viewModel = AgentViewModel()
        let draft = makeDraft(id: "99004", status: AgentDraftStatus.pendingConfirmation)
        viewModel.requestConfirmation(for: draft)
        XCTAssertTrue(viewModel.isConfirmationPresented)

        // 关闭弹窗：等同于放弃确认，不应触发写入。
        viewModel.dismissConfirmation()

        XCTAssertFalse(viewModel.isConfirmationPresented)
        XCTAssertNotEqual(viewModel.confirmationState, .confirmed)
        XCTAssertNil(viewModel.pendingConfirmationDraft)
    }

    // MARK: - 草稿确认接口（使用 MockURLProtocol）

    @MainActor
    func testConfirmAgentDraftPostsToConfirmEndpoint() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        var confirmCallCount = 0
        MockURLProtocol.requestHandler = { request in
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer access-token")
            let path = try XCTUnwrap(request.url?.path)
            XCTAssertEqual(request.httpMethod, "POST")
            XCTAssertEqual(path, "/v2/agent/drafts/99005/confirm")
            confirmCallCount += 1
            return Self.draftConfirmEnvelope(
                draftId: "99005",
                status: AgentDraftStatus.confirmed
            )
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let updated = try await client.confirmAgentDraft(id: EntityID(rawValue: "99005"))

        XCTAssertEqual(confirmCallCount, 1)
        XCTAssertEqual(updated.id.rawValue, "99005")
        XCTAssertEqual(updated.status, AgentDraftStatus.confirmed)
    }

    @MainActor
    func testAgentViewModelConfirmPendingDraftUpdatesState() async {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        var confirmCallCount = 0
        MockURLProtocol.requestHandler = { request in
            let path = try XCTUnwrap(request.url?.path)
            switch (request.httpMethod, path) {
            case ("POST", "/v2/agent/drafts/99006/confirm"):
                confirmCallCount += 1
                return Self.draftConfirmEnvelope(
                    draftId: "99006",
                    status: AgentDraftStatus.confirmed
                )
            // ViewModel 在确认成功后会重新拉取工作台/草稿/任务/通知等，需要兜底返回空数据。
            case ("GET", "/v2/agent/workbench"):
                return Self.emptyObjectEnvelope()
            case ("GET", "/v2/agent/drafts"):
                return Self.emptyArrayEnvelope()
            case ("GET", "/v2/agent/tasks"):
                return Self.emptyArrayEnvelope()
            case ("GET", "/v2/agent/notifications"):
                return Self.emptyArrayEnvelope()
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = AgentViewModel()
        let draft = makeDraft(id: "99006", status: AgentDraftStatus.pendingConfirmation)
        viewModel.requestConfirmation(for: draft)

        await viewModel.confirmPendingDraft(using: client)

        XCTAssertEqual(confirmCallCount, 1)
        XCTAssertEqual(viewModel.confirmationState, .confirmed)
        XCTAssertFalse(viewModel.isConfirmationPresented)
        XCTAssertNotNil(viewModel.pendingConfirmationDraft)
        XCTAssertEqual(viewModel.pendingConfirmationDraft?.status, AgentDraftStatus.confirmed)
    }

    @MainActor
    func testAgentViewModelConfirmIsIdempotentDuringInflightCall() async {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        let confirmCallCount = ManagedAtomicBox<Int>(0)
        MockURLProtocol.requestHandler = { request in
            let path = try XCTUnwrap(request.url?.path)
            switch (request.httpMethod, path) {
            case ("POST", "/v2/agent/drafts/99007/confirm"):
                _ = confirmCallCount.increment()
                return Self.draftConfirmEnvelope(
                    draftId: "99007",
                    status: AgentDraftStatus.confirmed
                )
            case ("GET", "/v2/agent/workbench"):
                return Self.emptyObjectEnvelope()
            case ("GET", "/v2/agent/drafts"):
                return Self.emptyArrayEnvelope()
            case ("GET", "/v2/agent/tasks"):
                return Self.emptyArrayEnvelope()
            case ("GET", "/v2/agent/notifications"):
                return Self.emptyArrayEnvelope()
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = AgentViewModel()
        let draft = makeDraft(id: "99007", status: AgentDraftStatus.pendingConfirmation)
        viewModel.requestConfirmation(for: draft)

        // 同时发起两次确认，确保只触发一次后端调用。
        async let first: Void = viewModel.confirmPendingDraft(using: client)
        async let second: Void = viewModel.confirmPendingDraft(using: client)
        _ = await (first, second)

        XCTAssertEqual(confirmCallCount.value, 1)
        XCTAssertEqual(viewModel.confirmationState, .confirmed)
    }

    @MainActor
    func testAgentViewModelConfirmFailureMarksFailedState() async {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        var confirmCallCount = 0
        MockURLProtocol.requestHandler = { request in
            let path = try XCTUnwrap(request.url?.path)
            switch (request.httpMethod, path) {
            case ("POST", "/v2/agent/drafts/99008/confirm"):
                confirmCallCount += 1
                return Self.response(
                    statusCode: 410,
                    body: Data(#"{"code":410,"message":"draft expired"}"#.utf8)
                )
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = AgentViewModel()
        let draft = makeDraft(id: "99008", status: AgentDraftStatus.pendingConfirmation)
        viewModel.requestConfirmation(for: draft)

        await viewModel.confirmPendingDraft(using: client)

        XCTAssertEqual(confirmCallCount, 1)
        if case .failed = viewModel.confirmationState {
            // expected
        } else {
            XCTFail("Expected .failed state, got \(viewModel.confirmationState)")
        }
        XCTAssertNotNil(viewModel.errorMessage)
        // 失败时不应自动关闭弹窗，让用户看到错误。
        // （此处保留弹窗打开的状态由 view 决定，ViewModel 仅记录失败状态。）
    }

    @MainActor
    func testAgentViewModelRejectDoesNotTriggerAnyHTTPCall() async {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        // 任何请求都不应被触发。若被触发，抛错。
        MockURLProtocol.requestHandler = { _ in
            XCTFail("Reject flow must not trigger any HTTP request")
            throw URLError(.badURL)
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = AgentViewModel()
        let draft = makeDraft(id: "99009", status: AgentDraftStatus.pendingConfirmation)
        viewModel.requestConfirmation(for: draft)
        viewModel.rejectPendingDraft()

        XCTAssertEqual(viewModel.confirmationState, .rejected)
        XCTAssertFalse(viewModel.isConfirmationPresented)
    }

    // MARK: - AgentDraftsViewModel 二次确认流程

    @MainActor
    func testAgentDraftsViewModelConfirmCallsEndpointOnly() async {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        var confirmCallCount = 0
        var unauthorizedWriteCount = 0
        MockURLProtocol.requestHandler = { request in
            let path = try XCTUnwrap(request.url?.path)
            let method = request.httpMethod
            switch (method, path) {
            case ("POST", "/v2/agent/drafts/99010/confirm"):
                confirmCallCount += 1
                return Self.draftConfirmEnvelope(
                    draftId: "99010",
                    status: AgentDraftStatus.confirmed
                )
            // 正式业务创建接口不应该被调用。
            case ("POST", "/v2/sale-orders"),
                 ("POST", "/v2/purchase-orders"),
                 ("POST", "/v2/purchase-receipts"),
                 ("POST", "/v2/sales-returns"),
                 ("POST", "/v2/purchase-returns"),
                 ("POST", "/v2/finance-records"),
                 ("POST", "/v2/pay-orders"):
                unauthorizedWriteCount += 1
                return Self.response(statusCode: 418, body: Data())
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = AgentDraftsViewModel()
        let draft = makeDraft(id: "99010", status: AgentDraftStatus.pendingConfirmation)
        viewModel.drafts = [draft]
        viewModel.beginConfirm(draft)
        XCTAssertTrue(viewModel.isConfirmPresented)

        await viewModel.confirm(using: client)

        XCTAssertEqual(confirmCallCount, 1)
        XCTAssertEqual(unauthorizedWriteCount, 0)
        XCTAssertEqual(viewModel.confirmationErrors[draft.id], nil)
        XCTAssertFalse(viewModel.isConfirmPresented)
        XCTAssertEqual(viewModel.drafts.first?.status, AgentDraftStatus.confirmed)
    }

    @MainActor
    func testAgentDraftsViewModelRejectDoesNotCallEndpoint() async {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { _ in
            XCTFail("Reject flow must not trigger any HTTP request")
            throw URLError(.badURL)
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = AgentDraftsViewModel()
        let draft = makeDraft(id: "99011", status: AgentDraftStatus.pendingConfirmation)
        viewModel.drafts = [draft]
        viewModel.beginConfirm(draft)
        viewModel.reject()

        XCTAssertFalse(viewModel.isConfirmPresented)
        XCTAssertNil(viewModel.pendingConfirmDraft)
        XCTAssertEqual(viewModel.drafts.first?.status, AgentDraftStatus.pendingConfirmation)
    }

    @MainActor
    func testAgentDraftsViewModelSkipConfirmForAlreadyConfirmedDraft() async {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        MockURLProtocol.requestHandler = { _ in
            XCTFail("Already confirmed draft must not trigger any HTTP request")
            throw URLError(.badURL)
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = AgentDraftsViewModel()
        let draft = makeDraft(id: "99012", status: AgentDraftStatus.confirmed)
        viewModel.drafts = [draft]
        viewModel.beginConfirm(draft)

        // 已确认的草稿不应打开确认弹窗。
        XCTAssertFalse(viewModel.isConfirmPresented)
        // 即便调 confirm，也不应触发 HTTP。
        await viewModel.confirm(using: client)
    }

    @MainActor
    func testAgentDraftsViewModelConfirmFailureKeepsErrorForRetry() async {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let tokenStore = AuthTokenStore()
        tokenStore.clear()
        tokenStore.save(accessToken: "access-token", refreshToken: "refresh-token")

        var confirmCallCount = 0
        MockURLProtocol.requestHandler = { request in
            let path = try XCTUnwrap(request.url?.path)
            switch (request.httpMethod, path) {
            case ("POST", "/v2/agent/drafts/99013/confirm"):
                confirmCallCount += 1
                return Self.response(
                    statusCode: 403,
                    body: Data(#"{"code":403,"message":"agent:write required"}"#.utf8)
                )
            default:
                throw URLError(.fileDoesNotExist)
            }
        }

        let client = APIClient(baseURL: URL(string: "https://example.com")!, tokenStore: tokenStore, session: session)
        let viewModel = AgentDraftsViewModel()
        let draft = makeDraft(id: "99013", status: AgentDraftStatus.pendingConfirmation)
        viewModel.drafts = [draft]
        viewModel.beginConfirm(draft)

        await viewModel.confirm(using: client)

        XCTAssertEqual(confirmCallCount, 1)
        XCTAssertEqual(viewModel.drafts.first?.status, AgentDraftStatus.pendingConfirmation)
        XCTAssertNotNil(viewModel.confirmationErrors[draft.id])
    }

    // MARK: - Helpers

    private func makeEvent(
        eventType: String,
        runId: String,
        conversationId: EntityID? = nil,
        delta: String? = nil,
        content: String? = nil,
        answer: String? = nil,
        toolCallId: String? = nil,
        toolName: String? = nil,
        inputSummary: String? = nil,
        message: String? = nil,
        resultSummary: String? = nil,
        errorCode: String? = nil,
        errorSummary: String? = nil,
        safeMessage: String? = nil,
        finalAnswer: String? = nil,
        draftId: EntityID? = nil,
        draftType: String? = nil,
        title: String? = nil,
        suggestedAction: String? = nil,
        compactedCount: Int? = nil,
        summary: String? = nil,
        mode: String? = nil,
        llmStatus: String? = nil,
        planSource: String? = nil,
        reason: String? = nil,
        block: AgentResultBlock? = nil,
        timestamp: Int64? = nil,
        terminalStatus: TerminalStatus? = nil,
        completedTools: [String]? = nil,
        missingTargetTools: [String]? = nil,
        status: String? = nil
    ) -> AgentStreamEvent {
        AgentStreamEvent(
            eventType: eventType,
            runId: runId,
            conversationId: conversationId,
            seq: nil,
            eventId: nil,
            delta: delta,
            deltaSource: nil,
            content: content,
            answer: answer,
            toolCallId: toolCallId,
            toolName: toolName,
            inputSummary: inputSummary,
            message: message,
            resultSummary: resultSummary,
            errorCode: errorCode,
            errorSummary: errorSummary,
            safeMessage: safeMessage,
            finalAnswer: finalAnswer,
            draftId: draftId,
            draftType: draftType,
            title: title,
            suggestedAction: suggestedAction,
            compactedCount: compactedCount,
            summary: summary,
            mode: mode,
            llmStatus: llmStatus,
            planSource: planSource,
            reason: reason,
            block: block,
            timestamp: timestamp,
            terminalStatus: terminalStatus,
            completedTools: completedTools,
            missingTargetTools: missingTargetTools,
            status: status
        )
    }

    private func makeDraft(id: String, status: String) -> AgentDraft {
        AgentDraft(
            id: EntityID(rawValue: id),
            conversationId: EntityID(rawValue: "conv-1"),
            draftType: "purchase_plan",
            title: "测试草稿 \(id)",
            contentJson: #"{"content":"hello"}"#,
            status: status,
            createdAt: 1710000000000,
            updatedAt: 1710000000000
        )
    }

    private static func response(statusCode: Int, body: Data) -> (HTTPURLResponse, Data) {
        let url = URL(string: "https://example.com")!
        let response = HTTPURLResponse(
            url: url,
            statusCode: statusCode,
            httpVersion: "HTTP/1.1",
            headerFields: ["Content-Type": "application/json"]
        )!
        return (response, body)
    }

    private static func draftConfirmEnvelope(draftId: String, status: String) -> (HTTPURLResponse, Data) {
        let body = """
        {
          "code": 0,
          "message": "ok",
          "data": {
            "id": "\(draftId)",
            "conversation_id": "conv-1",
            "draft_type": "purchase_plan",
            "title": "已确认草稿",
            "content_json": "{}",
            "status": "\(status)",
            "created_at": 1710000000000,
            "updated_at": 1710000000001
          }
        }
        """
        return response(statusCode: 200, body: Data(body.utf8))
    }

    private static func emptyObjectEnvelope() -> (HTTPURLResponse, Data) {
        response(statusCode: 200, body: Data(#"{"code":0,"message":"ok","data":{}}"#.utf8))
    }

    private static func emptyArrayEnvelope() -> (HTTPURLResponse, Data) {
        response(statusCode: 200, body: Data(#"{"code":0,"message":"ok","data":[]}"#.utf8))
    }
}

/// 简单的线程安全计数器，用于并发确认幂等测试。
private final class ManagedAtomicBox<Value: Numeric & Equatable> {
    private var storage: Value
    private let lock = NSLock()
    init(_ initial: Value) { storage = initial }
    @discardableResult
    func increment() -> Value {
        lock.lock()
        defer { lock.unlock() }
        storage += 1
        return storage
    }
    var value: Value {
        lock.lock()
        defer { lock.unlock() }
        return storage
    }
}
