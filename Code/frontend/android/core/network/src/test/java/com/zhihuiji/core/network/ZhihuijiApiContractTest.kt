package com.zhihuiji.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import retrofit2.http.GET
import retrofit2.http.POST

class ZhihuijiApiContractTest {
    @Test
    fun apiContract_keepsCriticalRemoteEndpointPaths() {
        assertEquals("v1/auth/login", postPath("login"))
        assertEquals("v1/auth/register", postPath("register"))
        assertEquals("v1/auth/users/me", getPath("me"))
        assertEquals("v1/sync/health", getPath("syncHealth"))
        assertEquals("v2/agent/workbench", getPath("agentWorkbenchV2", ZhihuijiV2Api::class.java))
    }

    @Test
    fun apiContract_hasMutationEndpointsForCoreInfrastructureFlows() {
        assertEquals("v2/sync/pull", postPath("pullSyncChangesV2", ZhihuijiV2Api::class.java))
        assertEquals("v2/agent/chat", postPath("agentChatV2", ZhihuijiV2Api::class.java))
    }

    private fun getPath(methodName: String, apiClass: Class<*> = ZhihuijiApi::class.java): String {
        val method = apiClass.methods.first { it.name == methodName }
        return requireNotNull(method.getAnnotation(GET::class.java)).value
    }

    private fun postPath(methodName: String, apiClass: Class<*> = ZhihuijiApi::class.java): String {
        val method = apiClass.methods.first { it.name == methodName }
        val annotation = method.getAnnotation(POST::class.java)
        assertNotNull("Missing @POST on $methodName", annotation)
        return requireNotNull(annotation).value
    }
}
