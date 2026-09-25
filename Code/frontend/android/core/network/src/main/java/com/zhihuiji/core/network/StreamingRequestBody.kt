package com.zhihuiji.core.network

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.InputStream
import kotlin.coroutines.cancellation.CancellationException

/**
 * 图片/文件源读取失败。message 固定为「读取图片失败」，供 UI 展示。
 * 继承 [java.io.IOException]，走 OkHttp 正常 IO 失败路径。
 */
class MediaSourceReadException(
    message: String = READ_FAILURE,
    cause: Throwable? = null,
) : java.io.IOException(message, cause) {
    companion object {
        const val READ_FAILURE: String = "读取图片失败"
    }
}

/**
 * 流式 RequestBody：writeTo 时从 [openStream] 分块写入 sink，避免整文件 ByteArray。
 * [openStream] 可被多次调用（OkHttp 重试时会再次 writeTo）。
 */
class StreamingRequestBody(
    private val contentType: MediaType?,
    private val contentLength: Long,
    private val openStream: () -> InputStream,
) : RequestBody() {
    override fun contentType(): MediaType? = contentType

    override fun contentLength(): Long = contentLength

    override fun writeTo(sink: BufferedSink) {
        try {
            openStream().use { input -> sink.writeAll(input.source()) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: MediaSourceReadException) {
            throw e
        } catch (e: Exception) {
            throw MediaSourceReadException(cause = e)
        }
    }
}
