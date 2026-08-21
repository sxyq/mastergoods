package com.zhihuiji.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SyncPreferenceStore @Inject constructor(
    @Named("sync") private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val CLIENT_ID = stringPreferencesKey("sync_client_id")
        private val STORE_PERMISSIONS = stringPreferencesKey("store_permissions")
        private fun cursorKey(entityType: String) = stringPreferencesKey("sync_cursor_$entityType")
        private fun timestampKey(entityType: String) = longPreferencesKey("sync_timestamp_$entityType")
    }

    @Volatile
    private var cachedPermissions: Set<String> = emptySet()

    private val cacheScope = CoroutineScope(Dispatchers.IO)

    init {
        dataStore.data.onEach { prefs ->
            cachedPermissions = prefs[STORE_PERMISSIONS]
                ?.split('|')
                ?.map(String::trim)
                ?.filter(String::isNotBlank)
                ?.toSet()
                ?: emptySet()
        }.launchIn(cacheScope)
    }

    fun observeCursor(entityType: String): Flow<String> = dataStore.data.map { prefs ->
        prefs[cursorKey(entityType)] ?: ""
    }

    fun observeLastSyncAt(entityType: String): Flow<Long> = dataStore.data.map { prefs ->
        prefs[timestampKey(entityType)] ?: 0L
    }

    suspend fun saveCursor(entityType: String, cursor: String) {
        dataStore.edit { prefs ->
            prefs[cursorKey(entityType)] = cursor
            prefs[timestampKey(entityType)] = System.currentTimeMillis()
        }
    }

    suspend fun requireClientId(): String {
        val current = dataStore.data.first()[CLIENT_ID]
        if (!current.isNullOrBlank()) return current
        val generated = "android-${UUID.randomUUID()}"
        dataStore.edit { prefs -> prefs[CLIENT_ID] = generated }
        return generated
    }

    fun peekPermissions(): Set<String> = cachedPermissions

    suspend fun savePermissions(permissions: Set<String>) {
        cachedPermissions = permissions
        dataStore.edit { prefs ->
            if (permissions.isEmpty()) {
                prefs.remove(STORE_PERMISSIONS)
            } else {
                prefs[STORE_PERMISSIONS] = permissions.sorted().joinToString("|")
            }
        }
    }

    suspend fun clearAll() {
        cachedPermissions = emptySet()
        dataStore.edit { it.clear() }
    }
}
