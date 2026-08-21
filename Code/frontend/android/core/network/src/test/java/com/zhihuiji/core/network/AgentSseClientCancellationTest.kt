package com.zhihuiji.core.network

import com.zhihuiji.core.model.v2.agent.AgentStreamEvent
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentSseClientCancellationTest {

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    @Test
    fun chatStream_cancelsUnderlyingOkHttpCallWhenCollectorIsCancelled() = runBlocking {
        val call = BlockingCall()
        val streamDispatcher = newSingleThreadContext("agent-sse-test")
        try {
            val client = AgentSseClient(
                okHttpClient = OkHttpClient(),
                json = Json { ignoreUnknownKeys = true },
                baseUrlProvider = { "http://localhost" },
                callFactory = { _, _ -> call },
                streamDispatcher = streamDispatcher,
            )

            val collectJob = launch {
                client.chatStream("""{"message":"库存风险","stream":true}""").collect {}
            }
            yield()

            assertTrue("SSE request did not start", call.awaitExecute())

            collectJob.cancelAndJoin()

            assertTrue("Cancelling the Flow must cancel the underlying OkHttp Call", call.awaitCancel())
        } finally {
            streamDispatcher.close()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    @Test
    fun chatStream_cancelsUnderlyingOkHttpCallWhileReadingResponseBody() = runBlocking {
        val bodyReadStarted = CountDownLatch(1)
        val call = BlockingBodyCall(bodyReadStarted)
        val streamDispatcher = newSingleThreadContext("agent-sse-body-read-test")
        try {
            val client = AgentSseClient(
                okHttpClient = OkHttpClient(),
                json = Json { ignoreUnknownKeys = true },
                baseUrlProvider = { "http://localhost" },
                callFactory = { _, _ -> call },
                streamDispatcher = streamDispatcher,
            )

            val collectJob = launch {
                client.chatStream("""{"message":"销售趋势","stream":true}""").collect {}
            }
            yield()

            assertTrue("SSE body read did not start", bodyReadStarted.await(3, TimeUnit.SECONDS))

            collectJob.cancelAndJoin()

            assertTrue("Cancelling during response-body read must cancel the underlying OkHttp Call", call.awaitCancel())
        } finally {
            streamDispatcher.close()
        }
    }

    @Test
    fun chatStream_buffersStandardMultiLineSseDataUntilBlankLine() = runBlocking {
        val client = clientForBody(
            """
            event: answer_delta
            data: {"event_type":"answer_delta",
            data: "run_id":"run-1",
            data: "delta":"真实模型流",
            data: "delta_source":"model_stream"}

            """.trimIndent()
        )

        val events = client.chatStream("""{"message":"销售趋势","stream":true}""").toList()

        assertEquals(1, events.size)
        val delta = events.single() as AgentStreamEvent.AnswerDelta
        assertEquals("run-1", delta.runId)
        assertEquals("真实模型流", delta.delta)
        assertEquals("model_stream", delta.deltaSource)
    }

    @Test
    fun chatStream_flushesLastBufferedSseEventWhenStreamEndsWithoutBlankLine() = runBlocking {
        val client = clientForBody(
            """data: {"event_type":"run_completed","run_id":"run-1","final_answer":"完成"}"""
        )

        val events = client.chatStream("""{"message":"库存","stream":true}""").toList()

        assertEquals(1, events.size)
        val completed = events.single() as AgentStreamEvent.RunCompleted
        assertEquals("完成", completed.finalAnswer)
    }

    @Test
    fun chatStream_stopsReadingAfterRunCompletedWithoutWaitingForEof() = runBlocking {
        val client = clientForBody(
            """
            data: {"event_type":"run_completed","run_id":"run-1","final_answer":"完成"}

            data: {"event_type":"answer_delta","run_id":"run-1","delta":"不应继续读取"}

            """.trimIndent()
        )

        val events = client.chatStream("""{"message":"销售趋势","stream":true}""").toList()

        assertEquals(1, events.size)
        assertTrue(events.single() is AgentStreamEvent.RunCompleted)
    }

    @Test
    fun chatStream_emitsParseErrorAndContinuesAfterMalformedSseData() = runBlocking {
        val client = clientForBody(
            """
            data: {"event_type":"answer_delta","delta":

            data: {"event_type":"run_completed","run_id":"run-1","final_answer":"完成"}

            """.trimIndent()
        )

        val events = client.chatStream("""{"message":"库存","stream":true}""").toList()

        assertEquals(2, events.size)
        val error = events[0] as AgentStreamEvent.ErrorEvent
        assertEquals("STREAM_PARSE_ERROR", error.code)
        assertTrue(error.message.contains("无法解析"))
        val completed = events[1] as AgentStreamEvent.RunCompleted
        assertEquals("完成", completed.finalAnswer)
    }

    @Test
    fun chatStream_mapsTransportIoFailureToUserReadableNetworkException() = runBlocking {
        val client = AgentSseClient(
            okHttpClient = OkHttpClient(),
            json = Json {
                ignoreUnknownKeys = true
                classDiscriminator = "event_type"
            },
            baseUrlProvider = { "http://localhost" },
            callFactory = { _, request -> FailingCall(request, IOException("socket closed")) },
        )

        val failures = mutableListOf<Throwable>()
        client.chatStream("""{"message":"库存","stream":true}""")
            .catch { failures += it }
            .toList()

        val error = failures.single() as NetworkException
        assertEquals(-1, error.code)
        assertTrue(error.message.orEmpty().contains("AI 流式连接中断"))
        assertTrue(error.message.orEmpty().contains("socket closed"))
    }

    @Test
    fun chatStream_mapsLegacyBaseUrlToCurrentApiBeforeOpeningSseEndpoint() = runBlocking {
        var capturedUrl = ""
        val client = AgentSseClient(
            okHttpClient = OkHttpClient(),
            json = Json {
                ignoreUnknownKeys = true
                classDiscriminator = "event_type"
            },
            baseUrlProvider = { "http://117.72.79.106/zhihuiji/v1/" },
            callFactory = { _, request ->
                capturedUrl = request.url.toString()
                StaticBodyCall(request, """data: {"event_type":"run_completed","run_id":"run-1","final_answer":"完成"}""")
            },
        )

        client.chatStream("""{"message":"库存","stream":true}""").toList()

        assertEquals(
            "https://zhj-api.sxyq27.online/v2/agent/chat/stream",
            capturedUrl,
        )
    }

    private fun clientForBody(body: String): AgentSseClient =
        AgentSseClient(
            okHttpClient = OkHttpClient(),
            json = Json {
                ignoreUnknownKeys = true
                classDiscriminator = "event_type"
            },
            baseUrlProvider = { "http://localhost" },
            callFactory = { _, request -> StaticBodyCall(request, body) },
        )

    private class BlockingCall : Call {
        private val executeStarted = CountDownLatch(1)
        private val cancelCalled = CountDownLatch(1)

        override fun request(): Request =
            Request.Builder().url("http://localhost/v2/agent/chat/stream").build()

        override fun execute(): Response {
            executeStarted.countDown()
            cancelCalled.await(3, TimeUnit.SECONDS)
            throw IOException("cancelled")
        }

        override fun enqueue(responseCallback: okhttp3.Callback) = error("Not used")

        override fun cancel() {
            cancelCalled.countDown()
        }

        override fun isExecuted(): Boolean = executeStarted.count == 0L

        override fun isCanceled(): Boolean = cancelCalled.count == 0L

        override fun timeout(): okio.Timeout = okio.Timeout.NONE

        override fun clone(): Call = this

        fun awaitExecute(): Boolean = executeStarted.await(3, TimeUnit.SECONDS)

        fun awaitCancel(): Boolean = cancelCalled.await(3, TimeUnit.SECONDS)
    }

    private class StaticBodyCall(
        private val request: Request,
        private val body: String,
    ) : Call {
        override fun request(): Request = request

        override fun execute(): Response =
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body.toResponseBody("text/event-stream".toMediaType()))
                .build()

        override fun enqueue(responseCallback: okhttp3.Callback) = error("Not used")

        override fun cancel() = Unit

        override fun isExecuted(): Boolean = false

        override fun isCanceled(): Boolean = false

        override fun timeout(): okio.Timeout = okio.Timeout.NONE

        override fun clone(): Call = StaticBodyCall(request, body)
    }

    private class FailingCall(
        private val request: Request,
        private val failure: IOException,
    ) : Call {
        override fun request(): Request = request

        override fun execute(): Response {
            throw failure
        }

        override fun enqueue(responseCallback: okhttp3.Callback) = error("Not used")

        override fun cancel() = Unit

        override fun isExecuted(): Boolean = false

        override fun isCanceled(): Boolean = false

        override fun timeout(): okio.Timeout = okio.Timeout.NONE

        override fun clone(): Call = FailingCall(request, failure)
    }

    private class BlockingBodyCall(
        private val bodyReadStarted: CountDownLatch,
    ) : Call {
        private val cancelCalled = CountDownLatch(1)
        private val request = Request.Builder().url("http://localhost/v2/agent/chat/stream").build()

        override fun request(): Request = request

        override fun execute(): Response =
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(object : ResponseBody() {
                    override fun contentType() = "text/event-stream".toMediaType()

                    override fun contentLength(): Long = -1L

                    @Suppress("DEPRECATION_ERROR")
                    override fun source(): okio.BufferedSource {
                        val source = object : okio.Source {
                            private var emittedFirstEvent = false

                            override fun read(sink: okio.Buffer, byteCount: Long): Long {
                                if (!emittedFirstEvent) {
                                    emittedFirstEvent = true
                                    return okio.Buffer()
                                        .writeUtf8("""data: {"event_type":"answer_delta","run_id":"run-1","delta":"真实","delta_source":"model_stream"}""" + "\n\n")
                                        .read(sink, byteCount)
                                }
                                bodyReadStarted.countDown()
                                cancelCalled.await(3, TimeUnit.SECONDS)
                                throw IOException("cancelled while reading body")
                            }

                            override fun timeout(): okio.Timeout = okio.Timeout.NONE

                            override fun close() = Unit
                        }
                        return okio.Okio.buffer(source)
                    }
                })
                .build()

        override fun enqueue(responseCallback: okhttp3.Callback) = error("Not used")

        override fun cancel() {
            cancelCalled.countDown()
        }

        override fun isExecuted(): Boolean = true

        override fun isCanceled(): Boolean = cancelCalled.count == 0L

        override fun timeout(): okio.Timeout = okio.Timeout.NONE

        override fun clone(): Call = this

        fun awaitCancel(): Boolean = cancelCalled.await(3, TimeUnit.SECONDS)
    }
}
