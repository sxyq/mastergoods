package com.zhihuiji.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkConfigTest {
    @Test
    fun defaultBaseUrl_targetsServer117WithV1Prefix() {
        assertEquals("http://117.72.79.106/zhihuiji/v1/", NetworkConfig.baseUrl)
    }

    @Test
    fun normalizeBaseUrl_trimsAndAppendsTrailingSlash() {
        assertEquals(
            "http://117.72.79.106/zhihuiji/v1/",
            NetworkConfig.normalizeBaseUrl(" http://117.72.79.106/zhihuiji/v1 "),
        )
    }

    @Test
    fun normalizeBaseUrl_mapsServer124ToServer117() {
        assertEquals(
            "http://117.72.79.106/zhihuiji/v1/",
            NetworkConfig.normalizeBaseUrl("http://124.222.153.108/zhihuiji/v1/"),
        )
    }

    @Test
    fun timeoutBudget_isNotTooAggressiveForRemoteServer() {
        assertTrue(NetworkConfig.CONNECT_TIMEOUT >= 10L)
        assertTrue(NetworkConfig.READ_TIMEOUT >= 10L)
        assertTrue(NetworkConfig.WRITE_TIMEOUT >= 10L)
    }
}
