package com.zhihuiji.core.network

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkConfigTest {
    @Test
    fun defaultBaseUrl_targetsCurrentApiHost() {
        assertEquals("https://zhj-api.sxyq27.online/", NetworkConfig.defaultBaseUrl)
    }

    @Test
    fun normalizeBaseUrl_trimsAndAppendsTrailingSlash() {
        assertEquals(
            "https://zhj-api.sxyq27.online/",
            NetworkConfig.normalizeBaseUrl(" https://zhj-api.sxyq27.online/v1 "),
        )
    }

    @Test
    fun normalizeBaseUrl_stripsPersistedEndpointVersionSuffixes() {
        assertEquals(
            "https://zhj-api.sxyq27.online/",
            NetworkConfig.normalizeBaseUrl("https://zhj-api.sxyq27.online/v2/"),
        )
        assertEquals(
            "https://sxyq27.online/",
            NetworkConfig.normalizeBaseUrl("https://sxyq27.online/v1"),
        )
    }

    @Test
    fun endpointUrl_doesNotDuplicateApiVersionWhenLegacyBaseUrlWasPersisted() {
        assertEquals(
            "https://zhj-api.sxyq27.online/v1/auth/refresh",
            NetworkConfig.endpointUrl("https://zhj-api.sxyq27.online/v1/", "v1/auth/refresh"),
        )
        assertEquals(
            "https://zhj-api.sxyq27.online/v2/agent/chat/stream",
            NetworkConfig.endpointUrl("https://zhj-api.sxyq27.online/v1/", "/v2/agent/chat/stream"),
        )
    }

    @Test
    fun normalizeBaseUrl_mapsLegacyHostsToDefaultBaseUrl() {
        assertEquals(
            NetworkConfig.defaultBaseUrl,
            NetworkConfig.normalizeBaseUrl("http://124.222.153.108/zhihuiji/v1/"),
        )
        assertEquals(
            NetworkConfig.defaultBaseUrl,
            NetworkConfig.normalizeBaseUrl("http://117.72.79.106/zhihuiji/v1/"),
        )
    }

    @Test
    fun timeoutBudget_isNotTooAggressiveForRemoteServer() {
        assertTrue(NetworkConfig.CONNECT_TIMEOUT >= 10L)
        assertTrue(NetworkConfig.READ_TIMEOUT >= 10L)
        assertTrue(NetworkConfig.WRITE_TIMEOUT >= 10L)
    }

    @Test
    fun rewriteUrlForBaseUrl_preservesConfiguredPathPrefix() {
        val rewritten = NetworkModule.rewriteUrlForBaseUrl(
            originalUrl = "https://zhj-api.sxyq27.online/v1/auth/login".toHttpUrl(),
            newBaseUrl = "https://zhj-api.sxyq27.online/".toHttpUrl(),
        )

        assertEquals(
            "https://zhj-api.sxyq27.online/v1/auth/login",
            rewritten.toString(),
        )
    }

    @Test
    fun rewriteUrlForBaseUrl_keepsQueryParameters() {
        val rewritten = NetworkModule.rewriteUrlForBaseUrl(
            originalUrl = "https://zhj-api.sxyq27.online/v1/products?page=2&keyword=abc".toHttpUrl(),
            newBaseUrl = "https://zhj-api.sxyq27.online/".toHttpUrl(),
        )

        assertEquals(
            "https://zhj-api.sxyq27.online/v1/products?page=2&keyword=abc",
            rewritten.toString(),
        )
    }
}
