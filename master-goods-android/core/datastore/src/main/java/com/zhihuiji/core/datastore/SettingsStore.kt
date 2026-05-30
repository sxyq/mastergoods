package com.zhihuiji.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
        const val DEFAULT_BASE_URL = "https://api.zhihuiji.com/v1/"
        private const val SERVER_124_HOST = "124.222.153.108"
        private const val DEV_FALLBACK_HOST = "117.72.79.106"

        fun normalizeBaseUrl(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return DEFAULT_BASE_URL
            if (trimmed.contains(SERVER_124_HOST) || trimmed.contains(DEV_FALLBACK_HOST)) return DEFAULT_BASE_URL
            return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        }
    }

    val baseUrl: Flow<String> = dataStore.data.map { prefs ->
        normalizeBaseUrl(prefs[KEY_BASE_URL] ?: DEFAULT_BASE_URL)
    }

    val clientId: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_CLIENT_ID] ?: ""
    }

    suspend fun saveBaseUrl(baseUrl: String) {
        dataStore.edit { it[KEY_BASE_URL] = normalizeBaseUrl(baseUrl) }
    }

    suspend fun saveClientId(clientId: String) {
        dataStore.edit { it[KEY_CLIENT_ID] = clientId }
    }

    suspend fun ensureClientId(): String {
        val prefs = dataStore.data.first()
        val existing = prefs[KEY_CLIENT_ID]
        if (!existing.isNullOrBlank()) return existing
        val newId = java.util.UUID.randomUUID().toString()
        dataStore.edit { it[KEY_CLIENT_ID] = newId }
        return newId
    }
}
