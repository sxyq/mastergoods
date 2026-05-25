package com.zhihuiji.core.network

object NetworkConfig {
    const val SERVER_117_BASE_URL = "http://117.72.79.106/zhihuiji/v1/"
    private const val SERVER_124_HOST = "124.222.153.108"

    var baseUrl: String = SERVER_117_BASE_URL
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L

    fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return SERVER_117_BASE_URL
        if (trimmed.contains(SERVER_124_HOST)) return SERVER_117_BASE_URL
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }
}
