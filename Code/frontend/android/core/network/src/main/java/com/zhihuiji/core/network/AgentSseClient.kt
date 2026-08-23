package com.zhihuiji.core.network

import com.zhihuiji.core.model.v2.agent.AgentStreamEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.job
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

/**
 * SSE 连接重连状态。
 */
data class RetryState(
    val isRetrying: Boolean = false,
    val attempt: Int = 0,
    val maxAttempts: Int = 3,
    val nextRetryInMs: Long = 0,
    val lastError: String? = null,
)

/**
 * Agent SSE 流式客户端。
 *
 * 负责：
 * - 通过 OkHttp 发起 SSE 请求（POST /v2/agent/chat/stream）
 * - 逐行读取响应体，按 ndjson 格式解析为 AgentStreamEvent
 * - 通过 Flow 向上层发射事件
 *
 * 输入：AgentChatRequest（JSON 序列化后作为 POST body）
 * 输出：Flow<AgentStreamEvent>
 *
 * 交互模块：AgentV2Repository（消费 Flow）
 * 完成标准：能稳定连接、解析 14 种事件类型、在取消时关闭连接
 */
class AgentSseClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val baseUrlProvider: () -> String,
    private val callFactory: (OkHttpClient, Request) -> Call = { client, request -> client.newCall(request) },
    private val streamDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    // Tool planning plus a provider continuation can legitimately exceed the
    // ordinary API timeout. Keep the client above the backend/provider 120s
    // budget so a slow real model is not turned into a fabricated failure.
    private val streamingOkHttpClient: OkHttpClient = okHttpClient.newBuilder()
        .readTimeout(STREAM_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val _retryState = MutableStateFlow(RetryState())
    val retryState: StateFlow<RetryState> = _retryState.asStateFlow()

    /**
     * 发起流式聊天请求，返回事件流。
     *
     * 连接建立阶段（HTTP 请求）会在 [IOException] 时按指数退避重试；
     * 一旦开始读取响应体并发射事件，则不再重试（避免对话状态不一致）。
     *
     * @param requestBodyJson AgentChatRequest 的 JSON 字符串
     */
    fun chatStream(requestBodyJson: String): Flow<AgentStreamEvent> = flow {
        val url = NetworkConfig.endpointUrl(baseUrlProvider(), "v2/agent/chat/stream")
        val coroutineContext = currentCoroutineContext()
        var lastEventId: String? = null
        val emittedEventKeys = mutableSetOf<String>()
        var terminalEventSeen = false
        var reconnectAttempt = 0

        while (!terminalEventSeen) {
            val requestBuilder = Request.Builder()
                .url(url)
                .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
            if (!lastEventId.isNullOrBlank()) requestBuilder.header("Last-Event-ID", lastEventId!!)

            try {
                val response = retryWithBackoff {
                    val call = callFactory(streamingOkHttpClient, requestBuilder.build())
                    coroutineContext.job.invokeOnCompletion { cause ->
                        if (cause != null && !call.isCanceled()) call.cancel()
                    }
                    call.executeCancellable()
                }

                response.use { resp ->
                    if (!resp.isSuccessful) {
                        throw NetworkException(
                            resp.code,
                            httpErrorMessage(resp.code, "SSE 连接失败: ${resp.code} ${resp.message}")
                        )
                    }
                    val body = resp.body ?: throw NetworkException(-1, "SSE 响应体为空")
                    val source = body.source()
                    val eventData = StringBuilder()
                    var pendingEventId: String? = null

                    suspend fun emitEvent(event: AgentStreamEvent?) {
                        if (event == null) return
                        val eventKeys = event.identityKeys(pendingEventId)
                        if (eventKeys.any { it in emittedEventKeys }) {
                            pendingEventId?.let { lastEventId = it }
                            return
                        }
                        emittedEventKeys += eventKeys
                        pendingEventId?.let { lastEventId = it }
                        if (pendingEventId == null) lastEventId = event.eventCursor() ?: lastEventId
                        emit(event)
                        terminalEventSeen = event.isTerminal()
                        pendingEventId = null
                    }

                    suspend fun flushBufferedEvent() {
                        if (eventData.isEmpty()) return
                        val jsonLine = eventData.toString().trimEnd('\n')
                        eventData.clear()
                        if (jsonLine.isBlank() || jsonLine == "[DONE]") {
                            pendingEventId = null
                            return
                        }
                        emitEvent(parseEvent(jsonLine))
                    }

                    while (!terminalEventSeen && !source.exhausted()) {
                        coroutineContext.ensureActive()
                        val line = source.readUtf8Line() ?: break
                        if (line.isBlank()) {
                            flushBufferedEvent()
                            continue
                        }
                        if (line.startsWith(":")) continue
                        when {
                            line.startsWith("data:") -> {
                                eventData.append(line.removePrefix("data:").trimStart())
                                eventData.append('\n')
                            }
                            line.startsWith("id:") -> pendingEventId = line.removePrefix("id:").trimStart()
                            line.startsWith("event:") || line.startsWith("retry:") -> continue
                            else -> {
                                flushBufferedEvent()
                                eventData.append(line.trim())
                                flushBufferedEvent()
                            }
                        }
                    }
                    if (!terminalEventSeen) flushBufferedEvent()
                }
                _retryState.value = RetryState()
            } catch (e: IOException) {
                currentCoroutineContext().ensureActive()
                reconnectAttempt += 1
                if (reconnectAttempt > MAX_STREAM_RECONNECTS) {
                    throw NetworkException(-1, agentSseNetworkErrorMessage(e))
                }
                val delayMs = minOf(2000L, 250L * reconnectAttempt)
                _retryState.value = RetryState(
                    isRetrying = true,
                    attempt = reconnectAttempt,
                    maxAttempts = MAX_STREAM_RECONNECTS,
                    nextRetryInMs = delayMs,
                    lastError = e.message,
                )
                delay(delayMs)
            }
        }
    }.flowOn(streamDispatcher)

    private suspend fun <T> retryWithBackoff(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000L,
        maxDelayMs: Long = 10000L,
        block: suspend () -> T,
    ): T {
        var lastException: Exception? = null
        for (attempt in 1..maxRetries) {
            try {
                val result = block()
                _retryState.value = RetryState()
                return result
            } catch (e: IOException) {
                lastException = e
                if (attempt == maxRetries) break
                currentCoroutineContext().ensureActive()
                val delayMs = minOf(maxDelayMs, initialDelayMs * (1L shl (attempt - 1))) +
                    Random.nextLong(0, 500)
                _retryState.value = RetryState(
                    isRetrying = true,
                    attempt = attempt,
                    maxAttempts = maxRetries,
                    nextRetryInMs = delayMs,
                    lastError = e.message,
                )
                delay(delayMs)
            }
        }
        throw lastException ?: IOException("Retry exhausted without exception")
    }

    private fun parseEvent(jsonLine: String): AgentStreamEvent? {
        return try {
            json.decodeFromString(AgentStreamEvent.serializer(), jsonLine)
        } catch (e: Exception) {
            AgentStreamEvent.ErrorEvent(
                code = "STREAM_PARSE_ERROR",
                message = "服务端返回了一条无法解析的 Agent 事件，已保留错误状态而不是静默丢弃。片段: ${jsonLine.take(160)}",
            )
        }
    }
}

private fun AgentStreamEvent.identityKeys(sseId: String?): List<String> = buildList {
    sseId?.takeIf { it.isNotBlank() }?.let { add("sse:$it") }
    eventId()?.let { add("event:$it") }
    seq()?.let { add("seq:${runId()}:$it") }
    toolCallId()?.let { add("call:${eventType()}:${runId()}:$it") }
    if (isTerminal()) runId()?.let { add("terminal:$it") }
}

private fun AgentStreamEvent.eventCursor(): String? = eventId() ?: seq()?.toString()

private fun AgentStreamEvent.eventId(): String? = when (this) {
    is AgentStreamEvent.ToolStarted -> eventId
    is AgentStreamEvent.ToolCompleted -> eventId
    is AgentStreamEvent.ToolFailed -> eventId
    is AgentStreamEvent.AnswerDelta -> eventId
    else -> null
}

private fun AgentStreamEvent.seq(): Int? = when (this) {
    is AgentStreamEvent.ToolStarted -> seq
    is AgentStreamEvent.ToolCompleted -> seq
    is AgentStreamEvent.ToolFailed -> seq
    is AgentStreamEvent.AnswerDelta -> seq
    else -> null
}

private fun AgentStreamEvent.toolCallId(): String? = when (this) {
    is AgentStreamEvent.ToolStarted -> toolCallId
    is AgentStreamEvent.ToolCompleted -> toolCallId
    is AgentStreamEvent.ToolFailed -> toolCallId
    else -> null
}

private fun AgentStreamEvent.runId(): String? = when (this) {
    is AgentStreamEvent.RunStarted -> runId
    is AgentStreamEvent.SafetyCheckStarted -> runId
    is AgentStreamEvent.SafetyCheckPassed -> runId
    is AgentStreamEvent.SafetyCheckBlocked -> runId
    is AgentStreamEvent.PlanDelta -> runId
    is AgentStreamEvent.ToolStarted -> runId
    is AgentStreamEvent.ToolProgress -> runId
    is AgentStreamEvent.ToolCompleted -> runId
    is AgentStreamEvent.ToolFailed -> runId
    is AgentStreamEvent.AnswerDelta -> runId
    is AgentStreamEvent.AnswerCompleted -> runId
    is AgentStreamEvent.ResultBlockEvent -> runId
    is AgentStreamEvent.DraftCreated -> runId
    is AgentStreamEvent.ContextCompacted -> runId
    is AgentStreamEvent.RunCompleted -> runId
    is AgentStreamEvent.RunFailed -> runId
    is AgentStreamEvent.RunBlocked -> runId
    is AgentStreamEvent.RunExhausted -> runId
    is AgentStreamEvent.RunCancelled -> runId
    is AgentStreamEvent.ErrorEvent -> runId
}

private fun AgentStreamEvent.eventType(): String = when (this) {
    is AgentStreamEvent.RunStarted -> "run_started"
    is AgentStreamEvent.SafetyCheckStarted -> "safety_check_started"
    is AgentStreamEvent.SafetyCheckPassed -> "safety_check_passed"
    is AgentStreamEvent.SafetyCheckBlocked -> "safety_check_blocked"
    is AgentStreamEvent.PlanDelta -> "plan_delta"
    is AgentStreamEvent.ToolStarted -> "tool_started"
    is AgentStreamEvent.ToolProgress -> "tool_progress"
    is AgentStreamEvent.ToolCompleted -> "tool_completed"
    is AgentStreamEvent.ToolFailed -> "tool_failed"
    is AgentStreamEvent.AnswerDelta -> "answer_delta"
    is AgentStreamEvent.AnswerCompleted -> "answer_completed"
    is AgentStreamEvent.ResultBlockEvent -> "result_block"
    is AgentStreamEvent.DraftCreated -> "draft_created"
    is AgentStreamEvent.ContextCompacted -> "context_compacted"
    is AgentStreamEvent.RunCompleted -> "run_completed"
    is AgentStreamEvent.RunFailed -> "run_failed"
    is AgentStreamEvent.RunBlocked -> "run_blocked"
    is AgentStreamEvent.RunExhausted -> "run_exhausted"
    is AgentStreamEvent.RunCancelled -> "run_cancelled"
    is AgentStreamEvent.ErrorEvent -> "error"
}

private fun AgentStreamEvent.isTerminal(): Boolean = when (this) {
    is AgentStreamEvent.RunCompleted,
    is AgentStreamEvent.RunFailed,
    is AgentStreamEvent.RunBlocked,
    is AgentStreamEvent.RunExhausted,
    is AgentStreamEvent.RunCancelled -> true
    else -> false
}

internal const val STREAM_READ_TIMEOUT_SECONDS = 180L
private const val MAX_STREAM_RECONNECTS = 3

private fun agentSseNetworkErrorMessage(error: IOException): String {
    val detail = error.message?.takeIf { it.isNotBlank() }
    return if (detail == null) {
        "AI 流式连接中断，请检查网络后重试"
    } else {
        "AI 流式连接中断，请检查网络后重试：$detail"
    }
}

private suspend fun Call.executeCancellable(): Response =
    suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            if (!isCanceled()) {
                cancel()
            }
        }
        try {
            val response = execute()
            if (continuation.isActive) {
                continuation.resume(response)
            } else {
                response.close()
            }
        } catch (e: IOException) {
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    }
