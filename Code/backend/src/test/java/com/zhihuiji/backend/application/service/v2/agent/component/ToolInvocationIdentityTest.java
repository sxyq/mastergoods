package com.zhihuiji.backend.application.service.v2.agent.component;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ToolInvocationIdentityTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void canonicalizesObjectFieldOrder() throws Exception {
        String first = ToolInvocationIdentity.key(
            "product_catalog_lookup",
            "call-1",
            objectMapper.readTree("{\"keyword\":\"茶\",\"limit\":10}"),
            objectMapper
        );
        String second = ToolInvocationIdentity.key(
            "product_catalog_lookup",
            "call-1",
            objectMapper.readTree("{\"limit\":10,\"keyword\":\"茶\"}"),
            objectMapper
        );

        assertTrue(first.equals(second), first + " != " + second);
    }

    @Test
    void differentiatesCallArgumentsAndCallIds() throws Exception {
        String first = ToolInvocationIdentity.key(
            "product_catalog_lookup",
            "call-1",
            objectMapper.readTree("{\"keyword\":\"茶\"}"),
            objectMapper
        );
        String differentArguments = ToolInvocationIdentity.key(
            "product_catalog_lookup",
            "call-1",
            objectMapper.readTree("{\"keyword\":\"咖啡\"}"),
            objectMapper
        );
        String differentCall = ToolInvocationIdentity.key(
            "product_catalog_lookup",
            "call-2",
            objectMapper.readTree("{\"keyword\":\"茶\"}"),
            objectMapper
        );

        assertNotEquals(first, differentArguments);
        assertNotEquals(first, differentCall);
    }
}
