package com.zhihuiji.backend.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zhihuiji.backend.infrastructure.config.AgentLlmProperties;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Component
public class LongCatAnthropicClient {
    private static final Logger log = LoggerFactory.getLogger(LongCatAnthropicClient.class);

    private final AgentLlmProperties properties;
    private final RestClient restClient;

    public LongCatAnthropicClient(AgentLlmProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10000);
        requestFactory.setReadTimeout(25000);
        this.restClient = restClientBuilder
            .baseUrl(properties.getBaseUrl())
            .requestFactory(requestFactory)
            .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
            .defaultHeader("anthropic-version", properties.getAnthropicVersion())
            .build();
    }

    public boolean isConfigured() {
        return properties.isEnabled()
            && StringUtils.hasText(properties.getApiKey())
            && StringUtils.hasText(properties.getModel())
            && StringUtils.hasText(properties.getBaseUrl());
    }

    public Optional<String> createJsonMessage(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            AnthropicResponse response = restClient.post()
                .uri("v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AnthropicRequest(
                    properties.getModel(),
                    properties.getMaxTokens(),
                    properties.getTemperature(),
                    properties.isEnableThinking() && properties.getModel().contains("Thinking"),
                    properties.getThinkingBudget(),
                    systemPrompt,
                    List.of(new Message("user", userPrompt))
                ))
                .retrieve()
                .body(AnthropicResponse.class);
            if (response == null || response.content() == null) {
                return Optional.empty();
            }
            Optional<String> text = response.content().stream()
                .filter(block -> "text".equalsIgnoreCase(block.type()) && StringUtils.hasText(block.text()))
                .map(ContentBlock::text)
                .reduce((left, right) -> left + "\n" + right);
            text.ifPresent(ignored -> log.info("LongCat agent response received from model {}", properties.getModel()));
            return text;
        } catch (Exception ex) {
            log.warn("LongCat agent request failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private record AnthropicRequest(
        String model,
        int max_tokens,
        double temperature,
        boolean enable_thinking,
        int thinking_budget,
        String system,
        List<Message> messages
    ) {}

    private record Message(String role, String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AnthropicResponse(List<ContentBlock> content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ContentBlock(String type, String text) {}
}
