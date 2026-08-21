package com.zhihuiji.core.datastore

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsStoreTest {
    @Test
    fun normalizeBaseUrl_mapsLegacy117HostToCurrentApi() {
        assertEquals(
            SettingsStore.DEFAULT_BASE_URL,
            SettingsStore.normalizeBaseUrl(" http://117.72.79.106/zhihuiji/v1 ", allowDebug117Host = true),
        )
    }

    @Test
    fun normalizeBaseUrl_releaseBuildFallsBackFrom117Host() {
        assertEquals(
            SettingsStore.DEFAULT_BASE_URL,
            SettingsStore.normalizeBaseUrl("http://117.72.79.106/zhihuiji/v1/", allowDebug117Host = false),
        )
    }

    @Test
    fun normalizeBaseUrl_alwaysFallsBackFrom124Host() {
        assertEquals(
            SettingsStore.DEFAULT_BASE_URL,
            SettingsStore.normalizeBaseUrl("http://124.222.153.108/zhihuiji/v1/", allowDebug117Host = true),
        )
    }

    @Test
    fun normalizeBaseUrl_keepsCurrentApiHost() {
        assertEquals(
            SettingsStore.DEFAULT_BASE_URL,
            SettingsStore.normalizeBaseUrl(" https://zhj-api.sxyq27.online/v1 ", allowDebug117Host = true),
        )
    }

    @Test
    fun normalizeBaseUrl_migratesLegacyEdgePathAndRetired154Host() {
        assertEquals(
            SettingsStore.DEFAULT_BASE_URL,
            SettingsStore.normalizeBaseUrl("https://sxyq27.online/zhj-api/v1/", allowDebug117Host = true),
        )
        assertEquals(
            SettingsStore.DEFAULT_BASE_URL,
            SettingsStore.normalizeBaseUrl("http://154.217.241.207:18080/", allowDebug117Host = true),
        )
    }

    @Test
    fun normalizeBaseUrl_forcesHttpsForPersistedHttpUrls() {
        assertEquals(
            "https://example.com/zhihuiji/",
            SettingsStore.normalizeBaseUrl("http://example.com/zhihuiji/v2/", allowDebug117Host = true),
        )
    }
}
