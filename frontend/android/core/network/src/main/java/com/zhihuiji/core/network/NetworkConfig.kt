package com.zhihuiji.core.network

import com.zhihuiji.core.datastore.SettingsStore

object NetworkConfig {
    val defaultBaseUrl: String
        get() = SettingsStore.DEFAULT_BASE_URL
    val DEFAULT_FALLBACK_URL: String
        get() = SettingsStore.DEFAULT_BASE_URL
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L

    fun normalizeBaseUrl(raw: String): String = SettingsStore.normalizeBaseUrl(raw)

    fun endpointUrl(baseUrl: String, relativePath: String): String {
        val normalizedBaseUrl = normalizeBaseUrl(baseUrl)
        val cleanRelativePath = relativePath.trimStart('/')
        return normalizedBaseUrl + cleanRelativePath
    }
}
