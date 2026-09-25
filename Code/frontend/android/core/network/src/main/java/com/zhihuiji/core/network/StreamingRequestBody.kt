package com.zhihuiji.core.network

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException
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
 *
 * 源的打开与读取失败归为 [MediaSourceReadException]（固定「读取图片失败」）；
 * 向 sink 的网络写入失败保持 [IOException] 原样上抛，不得改报成读图失败。
 */
class StreamingRequestBody(
    private val contentType: MediaType?,
    private val contentLength: Long,
    private val openStream: () -> InputStream,
) : RequestBody() {
    override fun contentType(): MediaType? = contentType

    override fun contentLength(): Long = contentLength

    override fun writeTo(sink: BufferedSink) {
        val input = openSource()
        var sinkWriteFailed = false
        try {
            input.use { stream ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    // 读源失败属于文件侧，归为读图失败。
                    val read = try {
                        stream.read(buffer)
                    } catch (e: IOException) {
                        throw MediaSourceReadException(cause = e)
                    }
                    if (read < 0) break
                    // 写 sink 失败属于网络侧，保持 IOException 上抛。
                    try {
                        sink.write(buffer, 0, read)
                    } catch (e: IOException) {
                        sinkWriteFailed = true
                        throw e
                    }
                }
            }
        } catch (e: MediaSourceReadException) {
            throw e
        } catch (e: IOException) {
            // sink 已写失败时上层拿到的就是那次网络错误；否则是源关闭失败，仍按读图失败上报。
            if (sinkWriteFailed) throw e else throw MediaSourceReadException(cause = e)
        }
    }

    private fun openSource(): InputStream = try {
        openStream()
    } catch (e: CancellationException) {
        throw e
    } catch (e: MediaSourceReadException) {
        throw e
    } catch (e: Exception) {
        throw MediaSourceReadException(cause = e)
    }

    private companion object {
        const val BUFFER_SIZE = 8 * 1024
    }
}
