package com.zhihuiji.core.datastore

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsStoreTest {
    @Test
    fun normalizeBaseUrl_mapsLegacy117HostToCurrentEdgeApi() {
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
    fun normalizeBaseUrl_keepsCurrentEdgeApiPath() {
        assertEquals(
            SettingsStore.DEFAULT_BASE_URL,
            SettingsStore.normalizeBaseUrl(" https://sxyq27.online/zhj-api/v1 ", allowDebug117Host = true),
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
