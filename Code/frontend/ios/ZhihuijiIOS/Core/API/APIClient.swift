import Foundation

final class APIClient {
    private let baseURL: URL
    private let tokenStore: AuthTokenStore
    private let session: URLSession
    private let decoder: JSONDecoder
    private let encoder: JSONEncoder
    private let refreshCoordinator = TokenRefreshCoordinator()

    init(baseURL: URL, tokenStore: AuthTokenStore, session: URLSession = .shared) {
        self.baseURL = baseURL
        self.tokenStore = tokenStore
        self.session = session
        self.decoder = JSONDecoder()
        self.encoder = JSONEncoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        encoder.keyEncodingStrategy = .convertToSnakeCase
    }

    func send<Response: Codable>(
        _ endpoint: APIEndpoint,
        method: String = "GET",
        body: Encodable? = nil,
        authorized: Bool = true,
        queryItems: [URLQueryItem] = [],
        headers: [String: String] = [:]
    ) async throws -> Response {
        try await send(
            path: endpoint.path,
            method: method,
            body: body,
            authorized: authorized,
            queryItems: queryItems,
            headers: headers
        )
    }

    func send<Response: Codable>(
        path: String,
        method: String = "GET",
        body: Encodable? = nil,
        authorized: Bool = true,
        queryItems: [URLQueryItem] = [],
        headers: [String: String] = [:]
    ) async throws -> Response {
        let (data, response) = try await performRequest(
            path: path,
            method: method,
            body: body,
            authorized: authorized,
            queryItems: queryItems,
            retryOnAuthFailure: authorized,
            headers: headers
        )
        return try decodeEnvelope(response: response, data: data)
    }

    func login(phone: String, password: String) async throws -> AuthPayload {
        try await send(
            .login,
            method: "POST",
            body: LoginRequest(phone: phone, password: password),
            authorized: false
        )
    }

    func register(phone: String, password: String, verifyCode: String) async throws -> AuthPayload {
        try await send(
            path: "/v2/auth/register",
            method: "POST",
            body: RegisterRequest(phone: phone, password: password, verifyCode: verifyCode),
            authorized: false
        )
    }

    func issueVerifyCode(phone: String, type: String) async throws -> VerifyCodeResponse {
        try await send(
            path: "/v2/auth/verify-code",
            method: "POST",
            body: VerifyCodeRequest(phone: phone, type: type),
            authorized: false
        )
    }

    func refresh(refreshToken: String) async throws -> AuthPayload {
        try await send(
            .refresh,
            method: "POST",
            body: RefreshRequest(refreshToken: refreshToken),
            authorized: false
        )
    }

    func logout() async throws {
        let _: EmptyPayload = try await send(.logout, method: "POST")
    }

    func fetchCurrentUser() async throws -> UserProfile {
        try await send(.currentUser)
    }

    func fetchCurrentStore() async throws -> CurrentStoreProfile {
        try await send(.currentStore)
    }

    func fetchStoreMembers() async throws -> [StoreStaffMember] {
        try await send(.storeMembers)
    }

    func createStoreMember(_ payload: StoreMemberCreatePayload) async throws -> StoreStaffMember {
        try await send(.storeMembers, method: "POST", body: payload)
    }

    func updateStoreMember(userId: EntityID, payload: StoreMemberUpdatePayload) async throws -> StoreStaffMember {
        try await send(path: "/v2/stores/current/members/\(userId.rawValue)", method: "PUT", body: payload)
    }

    func fetchMediaAssets() async throws -> [MediaAssetRecord] {
        try await send(.mediaAssets)
    }

    func fetchMediaAsset(id: EntityID) async throws -> MediaAssetRecord {
        try await send(path: "/v2/media/assets/\(id.rawValue)")
    }

    func createMediaAsset(payload: MediaAssetCreatePayload) async throws -> MediaAssetRecord {
        try await send(path: "/v2/media/assets", method: "POST", body: payload)
    }

    func uploadMediaAsset(fileData: Data, fileName: String, mimeType: String, assetType: String = "product_image") async throws -> MediaAssetRecord {
        let boundary = "zhihuiji-boundary-\(UUID().uuidString)"
        let body = makeMultipartBody(
            boundary: boundary,
            fileData: fileData,
            fileName: fileName,
            mimeType: mimeType,
            assetType: assetType
        )

        let (data, response) = try await performUploadRequest(
            path: "/v2/media/assets/upload",
            method: "POST",
            body: body,
            contentType: "multipart/form-data; boundary=\(boundary)"
        )
        return try decodeEnvelope(response: response, data: data)
    }

    func deleteMediaAsset(id: EntityID) async throws {
        let _: EmptyPayload = try await send(path: "/v2/media/assets/\(id.rawValue)", method: "DELETE")
    }

    func fetchMediaBindings(targetType: String, targetId: EntityID) async throws -> [MediaBindingRecord] {
        try await send(
            .mediaBindings,
            queryItems: [
                URLQueryItem(name: "target_type", value: targetType),
                URLQueryItem(name: "target_id", value: targetId.rawValue),
            ]
        )
    }

    func createMediaBinding(payload: MediaBindingCreatePayload) async throws -> MediaBindingRecord {
        try await send(path: "/v2/media/bindings", method: "POST", body: payload)
    }

    func deleteMediaBinding(id: EntityID) async throws {
        let _: EmptyPayload = try await send(path: "/v2/media/bindings/\(id.rawValue)", method: "DELETE")
    }

    func fetchSyncHealth() async throws -> SyncHealthRecord {
        try await send(.syncHealth)
    }

    func fetchSyncCursor(clientId: String) async throws -> SyncCursorRecord {
        try await send(path: "/v2/sync/cursor/\(clientId)")
    }

    func acknowledgeSyncCursor(payload: SyncCursorAckPayload) async throws -> SyncCursorRecord {
        try await send(path: "/v2/sync/cursor/ack", method: "POST", body: payload)
    }

    func uploadSyncChanges(payload: SyncUploadPayload) async throws -> SyncUploadResponse {
        try await send(path: "/v2/sync/upload", method: "POST", body: payload)
    }

    func pullSyncChanges(payload: SyncPullPayload) async throws -> SyncPullResponse {
        try await send(path: "/v2/sync/pull", method: "POST", body: payload)
    }

    func fetchImportJobs(status: String? = nil) async throws -> [ImportJobRecord] {
        var queryItems: [URLQueryItem] = []
        if let status, !status.isEmpty {
            queryItems.append(URLQueryItem(name: "status", value: status))
        }
        return try await send(.importJobs, queryItems: queryItems)
    }

    func fetchImportJob(id: EntityID) async throws -> ImportJobRecord {
        try await send(path: "/v2/import-jobs/\(id.rawValue)")
    }

    func createImportJob(payload: ImportJobCreatePayload) async throws -> ImportJobRecord {
        try await send(path: "/v2/import-jobs", method: "POST", body: payload)
    }

    func retryImportJob(id: EntityID, payload: ImportJobRetryPayload? = nil) async throws -> ImportJobRecord {
        if let payload {
            return try await send(path: "/v2/import-jobs/\(id.rawValue)/retry", method: "POST", body: payload)
        }
        return try await send(path: "/v2/import-jobs/\(id.rawValue)/retry", method: "POST")
    }

    func cancelImportJob(id: EntityID) async throws -> ImportJobRecord {
        try await send(path: "/v2/import-jobs/\(id.rawValue)/cancel", method: "POST")
    }

    func importLegacySQLite(payload: LegacySQLiteImportPayload) async throws -> LegacySQLiteImportResult {
        try await send(path: "/v2/import-jobs/legacy-sqlite", method: "POST", body: payload)
    }

    func fetchAgentWorkbench() async throws -> AgentWorkbench {
        try await send(.agentWorkbench)
    }

    func fetchAgentConversations(page: Int? = nil, limit: Int? = nil) async throws -> [AgentConversationSummary] {
        var queryItems: [URLQueryItem] = []
        if let page {
            queryItems.append(URLQueryItem(name: "page", value: String(page)))
        }
        if let limit {
            queryItems.append(URLQueryItem(name: "limit", value: String(limit)))
        }
        return try await send(path: "/v2/agent/conversations", queryItems: queryItems)
    }

    func createAgentConversation(title: String, status: String? = nil) async throws -> AgentConversationSummary {
        struct Payload: Codable {
            let title: String
            let status: String?
        }
        return try await send(path: "/v2/agent/conversations", method: "POST", body: Payload(title: title, status: status))
    }

    func updateAgentConversation(id: EntityID, title: String? = nil, status: String? = nil) async throws -> AgentConversationSummary {
        struct Payload: Codable {
            let title: String?
            let status: String?
        }
        return try await send(path: "/v2/agent/conversations/\(id.rawValue)", method: "PUT", body: Payload(title: title, status: status))
    }

    func deleteAgentConversation(id: EntityID) async throws {
        let _: EmptyPayload = try await send(path: "/v2/agent/conversations/\(id.rawValue)", method: "DELETE")
    }

    func fetchAgentMessages(conversationId: EntityID, page: Int? = nil, limit: Int? = nil) async throws -> [AgentMessage] {
        var queryItems: [URLQueryItem] = []
        if let page {
            queryItems.append(URLQueryItem(name: "page", value: String(page)))
        }
        if let limit {
            queryItems.append(URLQueryItem(name: "limit", value: String(limit)))
        }
        return try await send(path: "/v2/agent/conversations/\(conversationId.rawValue)/messages", queryItems: queryItems)
    }

    func fetchAgentDrafts(conversationId: EntityID? = nil, page: Int? = nil, limit: Int? = nil) async throws -> [AgentDraft] {
        var queryItems: [URLQueryItem] = []
        if let conversationId {
            queryItems.append(URLQueryItem(name: "conversation_id", value: conversationId.rawValue))
        }
        if let page {
            queryItems.append(URLQueryItem(name: "page", value: String(page)))
        }
        if let limit {
            queryItems.append(URLQueryItem(name: "limit", value: String(limit)))
        }
        return try await send(path: "/v2/agent/drafts", queryItems: queryItems)
    }

    func createAgentDraft(payload: AgentDraftCreatePayload) async throws -> AgentDraft {
        try await send(path: "/v2/agent/drafts", method: "POST", body: payload)
    }

    func updateAgentDraft(id: EntityID, payload: AgentDraftUpdatePayload) async throws -> AgentDraft {
        try await send(path: "/v2/agent/drafts/\(id.rawValue)", method: "PUT", body: payload)
    }

    func deleteAgentDraft(id: EntityID) async throws {
        let _: EmptyPayload = try await send(path: "/v2/agent/drafts/\(id.rawValue)", method: "DELETE")
    }

    /// 草稿二次确认接口。仅后端在确认后才会触发正式业务写入。
    /// - 重要：客户端不得直接调用正式业务创建接口，所有写入都必须经过此接口。
    func confirmAgentDraft(id: EntityID, idempotencyKey: String = UUID().uuidString) async throws -> AgentDraft {
        try await send(
            path: "/v2/agent/drafts/\(id.rawValue)/confirm",
            method: "POST",
            headers: ["Idempotency-Key": idempotencyKey]
        )
    }

    func fetchAgentTasks() async throws -> [AgentTask] {
        try await send(path: "/v2/agent/tasks")
    }

    func fetchAgentNotifications(unreadOnly: Bool? = nil) async throws -> [AgentNotification] {
        var queryItems: [URLQueryItem] = []
        if let unreadOnly {
            queryItems.append(URLQueryItem(name: "unread_only", value: unreadOnly ? "true" : "false"))
        }
        return try await send(path: "/v2/agent/notifications", queryItems: queryItems)
    }

    func markAgentNotificationRead(id: EntityID) async throws -> AgentNotificationReadResponse {
        try await send(path: "/v2/agent/notifications/\(id.rawValue)/read", method: "POST")
    }

    func chatWithAgent(conversationId: EntityID?, message: String, stream: Bool = false) async throws -> AgentChatResponse {
        try await send(
            path: "/v2/agent/chat",
            method: "POST",
            body: AgentChatPayload(conversationId: conversationId, message: message, stream: stream)
        )
    }

    func cancelAgentRun(runId: String) async throws -> AgentRunCancelResponse {
        try await send(path: "/v2/agent/runs/\(runId)/cancel", method: "POST")
    }

    func fetchAgentRunAudit(runId: String) async throws -> AgentRunAudit {
        try await send(path: "/v2/agent/runs/\(runId)/audit")
    }

    func streamAgentChat(conversationId: EntityID?, message: String) throws -> AsyncThrowingStream<AgentStreamEvent, Error> {
        let request = try makeRequest(
            path: "/v2/agent/chat/stream",
            method: "POST",
            body: AgentChatPayload(conversationId: conversationId, message: message, stream: true),
            authorized: true,
            queryItems: []
        )

        return AsyncThrowingStream { continuation in
            let task = Task {
                var lastEventId: String?
                var seenKeys = Set<String>()
                var terminalSeen = false
                var reconnectAttempt = 0
                do {
                    var request = request
                    request.setValue("text/event-stream", forHTTPHeaderField: "Accept")
                    if let lastEventId { request.setValue(lastEventId, forHTTPHeaderField: "Last-Event-ID") }
                    do {
                        let (bytes, response) = try await session.bytes(for: request)
                        if let http = response as? HTTPURLResponse,
                           !(200 ..< 300).contains(http.statusCode) {
                            let errorData = try await self.collectStreamErrorData(from: bytes)
                            try self.validate(response: response, data: errorData)
                        }
                        try self.validate(response: response, data: Data())

                        var dataLines: [String] = []
                        var pendingEventId: String?
                        func flushEvent() throws {
                            guard !dataLines.isEmpty else { return }
                            let payloadText = dataLines.joined(separator: "\n")
                            dataLines.removeAll(keepingCapacity: true)
                            guard let data = payloadText.data(using: .utf8) else { return }
                            let event = try self.decoder.decode(AgentStreamEvent.self, from: data)
                            let keys = agentStreamIdentityKeys(event, sseId: pendingEventId)
                            let duplicate = keys.contains { seenKeys.contains($0) }
                            if !duplicate {
                                seenKeys.formUnion(keys)
                                continuation.yield(event)
                                terminalSeen = agentStreamIsTerminal(event)
                            }
                            if let pendingEventId { lastEventId = pendingEventId }
                            else if let eventId = event.eventId { lastEventId = eventId }
                            else if let seq = event.seq { lastEventId = String(seq) }
                            pendingEventId = nil
                        }

                        for try await rawLine in bytes.lines {
                            if Task.isCancelled { break }
                            if rawLine.isEmpty { try flushEvent(); continue }
                            if rawLine.hasPrefix(":") { continue }
                            if rawLine.hasPrefix("id:") {
                                pendingEventId = String(rawLine.dropFirst(3)).trimmingCharacters(in: .whitespaces)
                            } else if rawLine.hasPrefix("data:") {
                                dataLines.append(String(rawLine.dropFirst(5)).trimmingCharacters(in: .whitespaces))
                            }
                            if terminalSeen { break }
                        }
                        try flushEvent()
                        if terminalSeen || Task.isCancelled { break }
                        continuation.finish()
                        return
                    } catch {
                        if Task.isCancelled { throw error }
                        reconnectAttempt += 1
                        if reconnectAttempt > 3 { throw error }
                        try await Task.sleep(nanoseconds: UInt64(min(2000, reconnectAttempt * 250)) * 1_000_000)
                    }
                } catch {
                    continuation.finish(throwing: error)
                    return
                }
                continuation.finish()
            }

            continuation.onTermination = { @Sendable _ in
                task.cancel()
            }
        }
    }

    private func agentStreamIdentityKeys(_ event: AgentStreamEvent, sseId: String?) -> [String] {
        var keys: [String] = []
        if let sseId, !sseId.isEmpty { keys.append("sse:\(sseId)") }
        if let eventId = event.eventId { keys.append("event:\(eventId)") }
        if let seq = event.seq { keys.append("seq:\(event.runId ?? ""):\(seq)") }
        if let toolCallId = event.toolCallId { keys.append("call:\(event.eventType):\(event.runId ?? ""):\(toolCallId)") }
        if agentStreamIsTerminal(event), let runId = event.runId { keys.append("terminal:\(runId)") }
        return keys
    }

    private func agentStreamIsTerminal(_ event: AgentStreamEvent) -> Bool {
        ["run_completed", "run_failed", "run_blocked", "run_exhausted", "run_cancelled"].contains(event.eventType)
    }

    private func decodeEnvelope<Response: Codable>(response: URLResponse, data: Data) throws -> Response {
        try validate(response: response, data: data)
        let payload: APIEnvelope<Response>
        do {
            payload = try decoder.decode(APIEnvelope<Response>.self, from: data)
        } catch {
            throw APIError.decoding(error.localizedDescription)
        }
        if payload.code != 0 {
            throw APIError.server(
                status: (response as? HTTPURLResponse)?.statusCode ?? -1,
                message: payload.message ?? "服务端返回错误"
            )
        }
        return payload.data
    }

    private func performRequest(
        path: String,
        method: String,
        body: Encodable?,
        authorized: Bool,
        queryItems: [URLQueryItem],
        retryOnAuthFailure: Bool,
        headers: [String: String]
    ) async throws -> (Data, URLResponse) {
        let request = try makeRequest(
            path: path,
            method: method,
            body: body,
            authorized: authorized,
            queryItems: queryItems,
            headers: headers
        )
        let (data, response) = try await session.data(for: request)

        if retryOnAuthFailure,
           authorized,
           let http = response as? HTTPURLResponse,
           http.statusCode == 401 {
            try await refreshAccessTokenIfNeeded()
            let retriedRequest = try makeRequest(
                path: path,
                method: method,
                body: body,
                authorized: authorized,
                queryItems: queryItems,
                headers: headers
            )
            return try await session.data(for: retriedRequest)
        }

        return (data, response)
    }

    private func performUploadRequest(
        path: String,
        method: String,
        body: Data,
        contentType: String
    ) async throws -> (Data, URLResponse) {
        var request = try makeBaseRequest(path: path, method: method, queryItems: [])
        request.setValue(contentType, forHTTPHeaderField: "Content-Type")
        request.httpBody = body

        let (data, response) = try await session.data(for: request)

        if let http = response as? HTTPURLResponse,
           http.statusCode == 401 {
            try await refreshAccessTokenIfNeeded()
            var retriedRequest = try makeBaseRequest(path: path, method: method, queryItems: [])
            retriedRequest.setValue(contentType, forHTTPHeaderField: "Content-Type")
            retriedRequest.httpBody = body
            return try await session.data(for: retriedRequest)
        }

        return (data, response)
    }

    private func makeMultipartBody(
        boundary: String,
        fileData: Data,
        fileName: String,
        mimeType: String,
        assetType: String
    ) -> Data {
        var body = Data()
        let crlf = "\r\n"

        body.append("--\(boundary)\(crlf)".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"asset_type\"\(crlf)\(crlf)".data(using: .utf8)!)
        body.append("\(assetType)\(crlf)".data(using: .utf8)!)

        body.append("--\(boundary)\(crlf)".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"\(fileName)\"\(crlf)".data(using: .utf8)!)
        body.append("Content-Type: \(mimeType)\(crlf)\(crlf)".data(using: .utf8)!)
        body.append(fileData)
        body.append(crlf.data(using: .utf8)!)

        body.append("--\(boundary)--\(crlf)".data(using: .utf8)!)
        return body
    }

    private func refreshAccessTokenIfNeeded() async throws {
        guard let refreshToken = tokenStore.readRefreshToken()?.nilIfBlank else {
            NotificationCenter.default.post(name: .zhihuijiUnauthorized, object: nil)
            throw APIError.unauthorized
        }

        let payload = try await refreshCoordinator.refresh { [self] in
            let request = try makeRequest(
                path: APIEndpoint.refresh.path,
                method: "POST",
                body: RefreshRequest(refreshToken: refreshToken),
                authorized: false,
                queryItems: []
            )
            let (data, response) = try await session.data(for: request)
            let payload: AuthPayload = try decodeEnvelope(response: response, data: data)
            tokenStore.save(accessToken: payload.token, refreshToken: payload.refreshToken ?? refreshToken)
            return payload
        }

        tokenStore.save(accessToken: payload.token, refreshToken: payload.refreshToken ?? refreshToken)
    }

    private func makeRequest(
        path: String,
        method: String,
        body: Encodable?,
        authorized: Bool,
        queryItems: [URLQueryItem],
        headers: [String: String] = [:]
    ) throws -> URLRequest {
        var request = try makeBaseRequest(path: path, method: method, queryItems: queryItems, authorized: authorized)
        if let body {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try AnyEncodable(body).encode(using: encoder)
        }
        headers.forEach { request.setValue($1, forHTTPHeaderField: $0) }
        return request
    }

    private func makeBaseRequest(
        path: String,
        method: String,
        queryItems: [URLQueryItem],
        authorized: Bool = true
    ) throws -> URLRequest {
        guard var components = URLComponents(url: baseURL, resolvingAgainstBaseURL: false) else {
            throw APIError.invalidURL
        }
        components.path = normalizedPath(base: components.path, append: path)
        if !queryItems.isEmpty {
            components.queryItems = queryItems
        }
        guard let url = components.url else {
            throw APIError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = method
        request.timeoutInterval = 20
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if authorized, let token = tokenStore.readAccessToken(), !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        return request
    }

    private func normalizedPath(base: String, append: String) -> String {
        let basePart = base == "/" ? "" : base.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        let appendPart = append.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        let combined = [basePart, appendPart].filter { !$0.isEmpty }.joined(separator: "/")
        return "/" + combined
    }

    private func validate(response: URLResponse, data: Data) throws {
        guard let http = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }
        switch http.statusCode {
        case 200 ..< 300:
            return
        case 401:
            NotificationCenter.default.post(name: .zhihuijiUnauthorized, object: nil)
            throw APIError.unauthorized
        case 403:
            NotificationCenter.default.post(
                name: .zhihuijiForbidden,
                object: nil,
                userInfo: ["message": errorMessage(from: data) ?? APIError.forbidden.errorDescription ?? "当前账号没有权限访问该数据"]
            )
            throw APIError.forbidden
        default:
            let message = errorMessage(from: data) ?? String(data: data, encoding: .utf8) ?? "服务端返回错误"
            throw APIError.server(status: http.statusCode, message: message)
        }
    }

    private func errorMessage(from data: Data) -> String? {
        struct ErrorEnvelope: Decodable {
            let message: String?
        }

        return try? decoder.decode(ErrorEnvelope.self, from: data).message?.nilIfBlank
    }

    private func collectStreamErrorData(from bytes: URLSession.AsyncBytes, maxBytes: Int = 16 * 1024) async throws -> Data {
        var text = ""
        for try await line in bytes.lines {
            if !text.isEmpty {
                text.append("\n")
            }
            text.append(line)
            if text.utf8.count >= maxBytes {
                break
            }
        }
        return Data(text.utf8)
    }
}

private struct AnyEncodable: Encodable {
    private let encodeBlock: (Encoder) throws -> Void

    init(_ value: Encodable) {
        encodeBlock = value.encode
    }

    func encode(to encoder: Encoder) throws {
        try encodeBlock(encoder)
    }

    func encode(using encoder: JSONEncoder) throws -> Data {
        try encoder.encode(self)
    }
}

private struct EmptyPayload: Codable {}

private actor TokenRefreshCoordinator {
    private var currentTask: Task<AuthPayload, Error>?

    func refresh(using operation: @escaping @Sendable () async throws -> AuthPayload) async throws -> AuthPayload {
        if let currentTask {
            return try await currentTask.value
        }

        let task = Task { try await operation() }
        currentTask = task
        defer { currentTask = nil }
        return try await task.value
    }
}
