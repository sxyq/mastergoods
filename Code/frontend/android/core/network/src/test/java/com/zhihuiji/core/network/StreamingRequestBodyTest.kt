package com.zhihuiji.core.network

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.cancellation.CancellationException
import okhttp3.MediaType.Companion.toMediaType
import okio.Buffer
import okio.Sink
import okio.Timeout
import okio.buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class StreamingRequestBodyTest {

    private val pdfType = "application/pdf".toMediaType()

    /** 模拟网络 sink：Okio 缓冲攒满完整段后向下层写入时抛 IOException。 */
    private class FailingSink : Sink {
        override fun write(source: Buffer, byteCount: Long): Unit = throw IOException("socket closed")

        override fun flush() = Unit

        override fun timeout(): Timeout = Timeout.NONE

        override fun close() = Unit
    }

    @Test
    fun writeTo_streamsAllBytesFromOpenStream() {
        val payload = "%PDF-1.7 body".toByteArray()
        val body = StreamingRequestBody(
            contentType = pdfType,
            contentLength = payload.size.toLong(),
            openStream = { ByteArrayInputStream(payload) },
        )
        val sink = Buffer()

        body.writeTo(sink)

        assertArrayEquals(payload, sink.readByteArray())
        assertEquals(payload.size.toLong(), body.contentLength())
    }

    @Test
    fun openStream_canBeInvokedAgainForRetry() {
        val payload = "retry-me".toByteArray()
        var openCount = 0
        val body = StreamingRequestBody(
            contentType = pdfType,
            contentLength = payload.size.toLong(),
            openStream = {
                openCount += 1
                ByteArrayInputStream(payload)
            },
        )

        body.writeTo(Buffer())
        body.writeTo(Buffer())

        assertEquals(2, openCount)
    }

    @Test
    fun writeTo_wrapsReadFailureAsMediaSourceReadException() {
        val body = StreamingRequestBody(
            contentType = pdfType,
            contentLength = 1L,
            openStream = {
                object : InputStream() {
                    override fun read(): Int = throw IOException("disk")
                }
            },
        )

        try {
            body.writeTo(Buffer())
            fail("writeTo should throw MediaSourceReadException")
        } catch (e: MediaSourceReadException) {
            assertEquals(MediaSourceReadException.READ_FAILURE, e.message)
        }
    }

    @Test
    fun writeTo_rethrowsCancellationException() {
        val cancellation = CancellationException("cancelled")
        val body = StreamingRequestBody(
            contentType = pdfType,
            contentLength = 0L,
            openStream = { throw cancellation },
        )

        try {
            body.writeTo(Buffer())
            fail("writeTo should rethrow CancellationException")
        } catch (e: CancellationException) {
            assertSame(cancellation, e)
        }
    }

    @Test
    fun writeTo_wrapsOpenFailureAsMediaSourceReadException() {
        val body = StreamingRequestBody(
            contentType = pdfType,
            contentLength = 1L,
            openStream = { throw IOException("missing file") },
        )

        try {
            body.writeTo(Buffer())
            fail("writeTo should throw MediaSourceReadException")
        } catch (e: MediaSourceReadException) {
            assertEquals(MediaSourceReadException.READ_FAILURE, e.message)
            assertEquals("missing file", (e.cause as IOException).message)
        }
    }

    @Test
    fun writeTo_keepsSinkWriteFailureAsIoexception() {
        // Okio 只在攒满完整段后才向下层 sink 写入，故用超过两个段的负载。
        val payload = ByteArray(20 * 1024) { 'a'.code.toByte() }
        val body = StreamingRequestBody(
            contentType = pdfType,
            contentLength = payload.size.toLong(),
            openStream = { ByteArrayInputStream(payload) },
        )
        val sink = FailingSink().buffer()

        try {
            body.writeTo(sink)
            fail("writeTo should rethrow the sink IOException")
        } catch (e: MediaSourceReadException) {
            fail("sink write failure must not be reported as a media read failure")
        } catch (e: IOException) {
            assertEquals("socket closed", e.message)
        }
    }
}
