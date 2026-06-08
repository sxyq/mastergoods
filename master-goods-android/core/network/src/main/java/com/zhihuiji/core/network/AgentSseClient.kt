package com.zhihuiji.core.network

import com.zhihuiji.core.model.v2.agent.AgentStreamEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
    private val streamingOkHttpClient: OkHttpClient = okHttpClient.newBuilder()
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * 发起流式聊天请求，返回事件流。
     *
     * @param requestBodyJson AgentChatRequest 的 JSON 字符串
     */
    fun chatStream(requestBodyJson: String): Flow<AgentStreamEvent> = flow {
        val baseUrl = baseUrlProvider().removeSuffix("/")
        val url = "$baseUrl/v2/agent/chat/stream"

        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .header("Connection", "keep-alive")
            .build()

        val call = callFactory(streamingOkHttpClient, request)
        try {
            call.executeCancellable().use { response ->
                if (!response.isSuccessful) {
                    throw NetworkException(
                        response.code,
                        "SSE 连接失败: ${response.code} ${response.message}"
                    )
                }

                val body = response.body
                    ?: throw NetworkException(-1, "SSE 响应体为空")

                val source = body.source()

                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break

                    // 跳过空行和 SSE 注释行（如 :ping / :ok）
                    if (line.isBlank() || line.startsWith(":")) continue

                    // 支持 SSE 格式 "data:{...}"、"data: {...}" 或纯 ndjson "{...}"
                    val jsonLine = when {
                        line.startsWith("data:") -> line.removePrefix("data:").trimStart()
                        line.startsWith("event:") || line.startsWith("id:") -> continue
                        else -> line
                    }

                    if (jsonLine.isBlank() || jsonLine == "[DONE]") continue

                    val event = parseEvent(jsonLine)
                    if (event != null) {
                        emit(event)
                    }
                }
            }
        } catch (e: IOException) {
            currentCoroutineContext().ensureActive()
            throw e
        }
    }.flowOn(streamDispatcher)

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
