package com.zhihuiji.backend.infrastructure.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zhihuiji.backend.infrastructure.config.AgentLlmProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class LongCatAnthropicClientTest {
    @Test
    void configurationStatusDistinguishesDisabledMissingAndConfiguredStates() {
        AgentLlmProperties disabled = properties(false, "", "deepseek-v4-flash", "https://token.sensenova.cn/v1/", "chat_completions");
        assertEquals("disabled", client(disabled).configurationStatus());
        assertFalse(client(disabled).isConfigured());

        AgentLlmProperties missingApiKey = properties(true, "", "deepseek-v4-flash", "https://token.sensenova.cn/v1/", "chat_completions");
        assertEquals("not_configured", client(missingApiKey).configurationStatus());
        assertFalse(client(missingApiKey).isConfigured());

        AgentLlmProperties configured = properties(true, "sk-test", "deepseek-v4-flash", "https://token.sensenova.cn/v1/", "chat_completions");
        assertEquals("configured", client(configured).configurationStatus());
        assertTrue(client(configured).isConfigured());
    }

    @Test
    void streamingUnavailableStatusReportsUnsupportedWireApiHonestly() {
        AgentLlmProperties responses = properties(true, "sk-test", "gpt-5.1", "https://api.openai.com/v1/", "responses");
        LongCatAnthropicClient client = client(responses);

        assertTrue(client.isConfigured());
        assertFalse(client.supportsStreaming());
        assertEquals("stream_not_supported", client.streamingUnavailableStatus());
    }

    private static LongCatAnthropicClient client(AgentLlmProperties properties) {
        return new LongCatAnthropicClient(properties, RestClient.builder());
    }

    private static AgentLlmProperties properties(
        boolean enabled,
        String apiKey,
        String model,
        String baseUrl,
        String wireApi
    ) {
        AgentLlmProperties properties = new AgentLlmProperties();
        properties.setEnabled(enabled);
        properties.setApiKey(apiKey);
        properties.setModel(model);
        properties.setBaseUrl(baseUrl);
        properties.setWireApi(wireApi);
        return properties;
    }
}
