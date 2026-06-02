package com.zhihuiji.core.network

import com.zhihuiji.core.datastore.SettingsStore

object NetworkConfig {
    val defaultBaseUrl: String
        get() = SettingsStore.DEFAULT_BASE_URL
    const val DEFAULT_FALLBACK_URL = "https://api.zhihuiji.com/v1/"
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L

    fun normalizeBaseUrl(raw: String): String = SettingsStore.normalizeBaseUrl(raw)
}
