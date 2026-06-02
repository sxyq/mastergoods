package com.zhihuiji.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cachedToken: String? = null

    @Volatile
    private var cachedRefreshToken: String? = null

    @Volatile
    private var cachedUserId: Long? = null

    @Volatile
    private var cachedExpiresAt: Long = 0L

    init {
        dataStore.data.onEach { prefs ->
            val storedToken = prefs[KEY_TOKEN]
            val storedRefreshToken = prefs[KEY_REFRESH_TOKEN]
            cachedToken = SecureSessionCipher.decrypt(storedToken)
            cachedRefreshToken = SecureSessionCipher.decrypt(storedRefreshToken)
            cachedUserId = prefs[KEY_USER_ID]
            cachedExpiresAt = prefs[KEY_EXPIRES_AT] ?: 0L
            if (shouldMigrateLegacySession(storedToken, storedRefreshToken)) {
                scope.launch {
                    migrateLegacySession(
                        token = storedToken,
                        refreshToken = storedRefreshToken,
                    )
                }
            }
        }.launchIn(scope)
    }

    val token: Flow<String?> = dataStore.data.map { SecureSessionCipher.decrypt(it[KEY_TOKEN]) }
    val refreshToken: Flow<String?> = dataStore.data.map { SecureSessionCipher.decrypt(it[KEY_REFRESH_TOKEN]) }
    val userId: Flow<Long?> = dataStore.data.map { it[KEY_USER_ID] }

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { prefs ->
        val token = SecureSessionCipher.decrypt(prefs[KEY_TOKEN])
        if (token.isNullOrBlank()) false
        else {
            val expiresAt = prefs[KEY_EXPIRES_AT] ?: 0L
            System.currentTimeMillis() < expiresAt
        }
    }

    suspend fun saveSession(token: String, refreshToken: String, userId: Long, expiresIn: Int) {
        persistSession(token, refreshToken, userId, expiresIn)
    }

    fun saveSessionAsync(token: String, refreshToken: String, userId: Long, expiresIn: Int) {
        scope.launch {
            persistSession(token, refreshToken, userId, expiresIn)
        }
    }

    suspend fun clearSession() {
        cachedToken = null
        cachedRefreshToken = null
        cachedUserId = null
        cachedExpiresAt = 0L
        dataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_EXPIRES_AT)
        }
    }

    suspend fun requireAccessToken(): String {
        val prefs = dataStore.data.first()
        val token = SecureSessionCipher.decrypt(prefs[KEY_TOKEN]) ?: throw IllegalStateException("未登录")
        cachedToken = token
        cachedRefreshToken = SecureSessionCipher.decrypt(prefs[KEY_REFRESH_TOKEN])
        cachedUserId = prefs[KEY_USER_ID]
        cachedExpiresAt = prefs[KEY_EXPIRES_AT] ?: 0L
        return token
    }

    fun peekAccessToken(): String? = cachedToken

    fun peekRefreshToken(): String? = cachedRefreshToken

    fun isTokenExpired(): Boolean = cachedExpiresAt > 0L && System.currentTimeMillis() >= cachedExpiresAt

    private suspend fun persistSession(token: String, refreshToken: String, userId: Long, expiresIn: Int) {
        val expiresAt = System.currentTimeMillis() + expiresIn * 1000L
        cachedToken = token
        cachedRefreshToken = refreshToken
        cachedUserId = userId
        cachedExpiresAt = expiresAt
        dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = SecureSessionCipher.encrypt(token)
            prefs[KEY_REFRESH_TOKEN] = SecureSessionCipher.encrypt(refreshToken)
            prefs[KEY_USER_ID] = userId
            prefs[KEY_EXPIRES_AT] = expiresAt
        }
    }

    private fun shouldMigrateLegacySession(token: String?, refreshToken: String?): Boolean {
        return (!token.isNullOrBlank() && !SecureSessionCipher.isEncrypted(token)) ||
            (!refreshToken.isNullOrBlank() && !SecureSessionCipher.isEncrypted(refreshToken))
    }

    private suspend fun migrateLegacySession(token: String?, refreshToken: String?) {
        if (!shouldMigrateLegacySession(token, refreshToken)) return
        dataStore.edit { prefs ->
            token?.takeIf { !SecureSessionCipher.isEncrypted(it) }?.let {
                prefs[KEY_TOKEN] = SecureSessionCipher.encrypt(it)
            }
            refreshToken?.takeIf { !SecureSessionCipher.isEncrypted(it) }?.let {
                prefs[KEY_REFRESH_TOKEN] = SecureSessionCipher.encrypt(it)
            }
        }
    }
}
