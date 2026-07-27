package com.zhihuiji.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SyncPreferenceStore @Inject constructor(
    @Named("sync") private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private fun cursorKey(entityType: String) = stringPreferencesKey("sync_cursor_$entityType")
        private fun timestampKey(entityType: String) = longPreferencesKey("sync_timestamp_$entityType")
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

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
