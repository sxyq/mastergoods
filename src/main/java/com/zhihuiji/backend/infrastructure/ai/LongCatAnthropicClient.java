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
        requestFactory.setReadTimeout(120000);
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

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000L;

    public Optional<String> createJsonMessage(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Optional<String> result = doCreateJsonMessage(systemPrompt, userPrompt);
                if (result.isPresent()) return result;
                lastException = null;
            } catch (Exception ex) {
                lastException = ex;
                log.warn("LongCat agent request attempt {}/{} failed: {}", attempt, MAX_RETRIES, ex.getMessage());
                if (attempt < MAX_RETRIES) {
                    long backoff = INITIAL_BACKOFF_MS * (1L << (attempt - 1));
                    try { Thread.sleep(backoff); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return Optional.empty(); }
                }
            }
        }
        if (lastException != null) {
            log.warn("LongCat agent request failed after {} retries: {}", MAX_RETRIES, lastException.getMessage());
        }
        return Optional.empty();
    }

    private Optional<String> doCreateJsonMessage(String systemPrompt, String userPrompt) {
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
        text.ifPresent(ignored -> {
            if (response.usage() != null) {
                log.info("LongCat agent response from model {}, tokens: input={}, output={}",
                    properties.getModel(),
                    response.usage().input_tokens(),
                    response.usage().output_tokens());
            } else {
                log.info("LongCat agent response received from model {}", properties.getModel());
            }
        });
        return text;
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
    private record AnthropicResponse(List<ContentBlock> content, Usage usage) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Usage(Integer input_tokens, Integer output_tokens) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ContentBlock(String type, String text) {}
}
