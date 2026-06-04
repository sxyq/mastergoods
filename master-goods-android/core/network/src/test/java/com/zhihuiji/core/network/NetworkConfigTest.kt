package com.zhihuiji.core.network

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkConfigTest {
    @Test
    fun defaultBaseUrl_targetsHostedApiWithV1Prefix() {
        assertEquals("https://api.zhihuiji.com/v1/", NetworkConfig.defaultBaseUrl)
    }

    @Test
    fun normalizeBaseUrl_trimsAndAppendsTrailingSlash() {
        assertEquals(
            "http://117.72.79.106/zhihuiji/v1/",
            NetworkConfig.normalizeBaseUrl(" http://117.72.79.106/zhihuiji/v1 "),
        )
    }

    @Test
    fun normalizeBaseUrl_mapsLegacyHostsToHostedApi() {
        assertEquals(
            "https://api.zhihuiji.com/v1/",
            NetworkConfig.normalizeBaseUrl("http://124.222.153.108/zhihuiji/v1/"),
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
            originalUrl = "https://api.zhihuiji.com/v1/auth/login".toHttpUrl(),
            newBaseUrl = "http://117.72.79.106/zhihuiji/v1/".toHttpUrl(),
        )

        assertEquals(
            "http://117.72.79.106/zhihuiji/v1/auth/login",
            rewritten.toString(),
        )
    }

    @Test
    fun rewriteUrlForBaseUrl_keepsQueryParameters() {
        val rewritten = NetworkModule.rewriteUrlForBaseUrl(
            originalUrl = "https://api.zhihuiji.com/v1/products?page=2&keyword=abc".toHttpUrl(),
            newBaseUrl = "http://117.72.79.106/zhihuiji/v1/".toHttpUrl(),
        )

        assertEquals(
            "http://117.72.79.106/zhihuiji/v1/products?page=2&keyword=abc",
            rewritten.toString(),
        )
    }
}
