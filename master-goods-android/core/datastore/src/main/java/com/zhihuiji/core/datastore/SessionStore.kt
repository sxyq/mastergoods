package com.zhihuiji.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SessionStore @Inject constructor(
    @Named("session") private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val KEY_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID = longPreferencesKey("user_id")
        private val KEY_EXPIRES_AT = longPreferencesKey("expires_at")
    }

    val token: Flow<String?> = dataStore.data.map { it[KEY_TOKEN] }
    val refreshToken: Flow<String?> = dataStore.data.map { it[KEY_REFRESH_TOKEN] }
    val userId: Flow<Long?> = dataStore.data.map { it[KEY_USER_ID] }

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { prefs ->
        !prefs[KEY_TOKEN].isNullOrBlank()
    }

    suspend fun saveSession(token: String, refreshToken: String, userId: Long, expiresIn: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
            prefs[KEY_REFRESH_TOKEN] = refreshToken
            prefs[KEY_USER_ID] = userId
            prefs[KEY_EXPIRES_AT] = System.currentTimeMillis() + expiresIn * 1000L
        }
    }

    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_EXPIRES_AT)
        }
    }

    suspend fun requireAccessToken(): String {
        val prefs = dataStore.data.first()
        return prefs[KEY_TOKEN] ?: throw IllegalStateException("未登录")
    }
}
