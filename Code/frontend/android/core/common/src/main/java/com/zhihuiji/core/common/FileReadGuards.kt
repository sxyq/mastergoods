package com.zhihuiji.core.common

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import kotlin.coroutines.cancellation.CancellationException

/**
 * 文件/内容读取包装：只把真实读取失败收成 Result，取消必须继续向上传播。
 */
inline fun <T> readOrFailure(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}

/**
 * 可空读取：读取异常返回 null，取消必须继续向上传播。
 */
inline fun <T> readOrNull(block: () -> T?): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    null
}

/**
 * 挂起安全的 Result 包装：真实失败收成 Result.failure，取消必须继续向上传播。
 * 禁止用裸 runCatching 包挂起调用——runCatching 会把 CancellationException 收成失败。
 */
suspend fun <T> runCatchingCancellable(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}

/**
 * 解析 content 流长度；未知或失败返回 -1L。不读取文件内容。
 */
fun resolveContentLength(resolver: ContentResolver, uri: Uri): Long {
    try {
        resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
            if (afd.length >= 0L) return afd.length
        }
    } catch (_: Exception) {
        // 回落到 SIZE 列
    }
    try {
        resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                val size = cursor.getLong(0)
                if (size >= 0L) return size
            }
        }
    } catch (_: Exception) {
        // 未知长度
    }
    return -1L
}
