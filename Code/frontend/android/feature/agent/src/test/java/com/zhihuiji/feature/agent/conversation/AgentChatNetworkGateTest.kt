// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
package com.zhihuiji.feature.agent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentChatNetworkGateTest {

    @Test
    fun queuesWhenNoInternetCapabilityExists() {
        assertFalse(isUsableAgentNetwork(hasInternetCapability = false, isValidated = false))
    }

    @Test
    fun queuesWhenInternetIsNotValidated() {
        assertFalse(isUsableAgentNetwork(hasInternetCapability = true, isValidated = false))
    }

    @Test
    fun streamsOnlyOnValidatedInternet() {
        assertTrue(isUsableAgentNetwork(hasInternetCapability = true, isValidated = true))
    }
}
