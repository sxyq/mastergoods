package com.zhihuiji.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.net.URI
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SettingsStore @Inject constructor(
    @Named("settings") private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
        private val KEY_CLIENT_ID = stringPreferencesKey("client_id")
        val DEFAULT_BASE_URL = if (BuildConfig.BASE_URL_EDITABLE) {
            "http://117.72.79.106/zhihuiji/"
        } else {
            "https://api.zhihuiji.com/"
        }
        private const val PRODUCTION_HOST = "api.zhihuiji.com"
        private const val SERVER_124_HOST = "124.222.153.108"
        private const val DEBUG_SERVER_117_HOST = "117.72.79.106"
        private val RELEASE_ALLOWED_HOSTS = setOf(PRODUCTION_HOST)

        fun normalizeBaseUrl(raw: String): String = normalizeBaseUrl(raw, allowDebug117Host = BuildConfig.BASE_URL_EDITABLE)

        internal fun normalizeBaseUrl(raw: String, allowDebug117Host: Boolean): String {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return DEFAULT_BASE_URL
            if (trimmed.contains(SERVER_124_HOST)) return DEFAULT_BASE_URL
            if (!allowDebug117Host && trimmed.contains(DEBUG_SERVER_117_HOST)) return DEFAULT_BASE_URL
            val withTrailingSlash = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
            return stripEndpointVersionSuffix(withTrailingSlash)
        }

        private fun stripEndpointVersionSuffix(baseUrl: String): String {
            val uri = runCatching { URI(baseUrl) }.getOrNull() ?: return baseUrl
            val path = uri.path.orEmpty().removeSuffix("/")
            val canonicalPath = when {
                path.endsWith("/v1") -> path.removeSuffix("/v1")
                path.endsWith("/v2") -> path.removeSuffix("/v2")
                path == "/v1" || path == "/v2" -> ""
                else -> return baseUrl
            }
            val rebuilt = URI(
                uri.scheme,
                uri.userInfo,
                uri.host,
                uri.port,
                canonicalPath.ifBlank { "/" },
                uri.query,
                uri.fragment,
            ).toString()
            return if (rebuilt.endsWith("/")) rebuilt else "$rebuilt/"
        }

        fun isTrustedReleaseBaseUrl(baseUrl: String): Boolean {
            val normalized = normalizeBaseUrl(baseUrl)
            val uri = runCatching { URI(normalized) }.getOrNull() ?: return false
            return uri.scheme.equals("https", ignoreCase = true) && uri.host in RELEASE_ALLOWED_HOSTS
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cachedBaseUrl: String = DEFAULT_BASE_URL

    @Volatile
    private var cachedClientId: String = ""

    init {
        dataStore.data.onEach { prefs ->
            cachedBaseUrl = normalizeBaseUrl(prefs[KEY_BASE_URL] ?: DEFAULT_BASE_URL)
            cachedClientId = prefs[KEY_CLIENT_ID] ?: ""
        }.launchIn(scope)
    }

    val baseUrl: Flow<String> = dataStore.data.map { prefs ->
        normalizeBaseUrl(prefs[KEY_BASE_URL] ?: DEFAULT_BASE_URL)
    }

    val clientId: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_CLIENT_ID] ?: ""
    }

    suspend fun saveBaseUrl(baseUrl: String) {
        val normalized = sanitizeBaseUrlForCurrentBuild(baseUrl)
        cachedBaseUrl = normalized
        dataStore.edit { it[KEY_BASE_URL] = normalized }
    }

    suspend fun saveClientId(clientId: String) {
        cachedClientId = clientId
        dataStore.edit { it[KEY_CLIENT_ID] = clientId }
    }

    suspend fun ensureClientId(): String {
        val prefs = dataStore.data.first()
        val existing = prefs[KEY_CLIENT_ID]
        if (!existing.isNullOrBlank()) return existing
        val newId = java.util.UUID.randomUUID().toString()
        cachedClientId = newId
        dataStore.edit { it[KEY_CLIENT_ID] = newId }
        return newId
    }

    fun peekBaseUrl(): String = sanitizeBaseUrlForCurrentBuild(cachedBaseUrl)

    fun peekClientId(): String = cachedClientId

    fun isBaseUrlEditable(): Boolean = BuildConfig.BASE_URL_EDITABLE

    private fun sanitizeBaseUrlForCurrentBuild(raw: String): String {
        val normalized = normalizeBaseUrl(raw)
        if (BuildConfig.BASE_URL_EDITABLE) return normalized
        return if (isTrustedReleaseBaseUrl(normalized)) normalized else DEFAULT_BASE_URL
    }
}
