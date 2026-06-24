package com.zhihuiji.backend.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.infrastructure.config.AgentLlmProperties;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
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
    private final String normalizedBaseUrl;
    private final String wireApi;
    private final boolean hasApiKey;
    private final boolean openAiAuth;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient streamingHttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    private final Map<String, HttpResponse<InputStream>> activeStreams = new ConcurrentHashMap<>();

    public LongCatAnthropicClient(AgentLlmProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.normalizedBaseUrl = normalizeBaseUrl(properties.getBaseUrl());
        this.wireApi = properties.getWireApi() == null ? "" : properties.getWireApi();
        this.hasApiKey = StringUtils.hasText(properties.getApiKey());
        this.openAiAuth = hasApiKey && usesOpenAiAuth(wireApi);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10000);
        requestFactory.setReadTimeout(120000);
        RestClient.Builder builder = restClientBuilder
            .baseUrl(normalizedBaseUrl)
            .requestFactory(requestFactory);
        if (hasApiKey && openAiAuth) {
            builder = builder.defaultHeader("Authorization", "Bearer " + properties.getApiKey());
        } else if (hasApiKey) {
            builder = builder.defaultHeader("x-api-key", properties.getApiKey());
        }
        if (!"responses".equalsIgnoreCase(wireApi) && StringUtils.hasText(properties.getAnthropicVersion())) {
            builder = builder.defaultHeader("anthropic-version", properties.getAnthropicVersion());
        }
        this.restClient = builder.build();
    }

    public boolean isConfigured() {
        return "configured".equals(configurationStatus());
    }

    public String configurationStatus() {
        if (!properties.isEnabled()) {
            return "disabled";
        }
        if (!hasApiKey
            || !StringUtils.hasText(properties.getModel())
            || !StringUtils.hasText(properties.getBaseUrl())) {
            return "not_configured";
        }
        return "configured";
    }

    public boolean supportsStreaming() {
        return isConfigured() && "chat_completions".equalsIgnoreCase(wireApi);
    }

    public String streamingUnavailableStatus() {
        if (!isConfigured()) {
            return configurationStatus();
        }
        return supportsStreaming() ? "configured" : "stream_not_supported";
    }

    private boolean usesOpenAiAuth() {
        return usesOpenAiAuth(wireApi);
    }

    private boolean usesOpenAiAuth(String wireApi) {
        return properties.isRequiresOpenaiAuth()
            || "responses".equalsIgnoreCase(wireApi)
            || "chat_completions".equalsIgnoreCase(wireApi)
            || "completions".equalsIgnoreCase(wireApi);
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
        if ("responses".equalsIgnoreCase(wireApi)) {
            return doCreateResponsesMessage(systemPrompt, userPrompt);
        }
        if ("chat_completions".equalsIgnoreCase(wireApi)) {
            return doCreateChatCompletionsMessage(systemPrompt, userPrompt);
        }
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
        StringBuilder textBuilder = new StringBuilder();
        for (ContentBlock block : response.content()) {
            if (!"text".equalsIgnoreCase(block.type()) || !StringUtils.hasText(block.text())) {
                continue;
            }
            if (textBuilder.length() > 0) {
                textBuilder.append('\n');
            }
            textBuilder.append(block.text());
        }
        Optional<String> text = textBuilder.length() > 0 ? Optional.of(textBuilder.toString()) : Optional.empty();
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

    private Optional<String> doCreateResponsesMessage(String systemPrompt, String userPrompt) {
        ResponsesResponse response = restClient.post()
            .uri("responses")
            .contentType(MediaType.APPLICATION_JSON)
            .body(new ResponsesRequest(
                properties.getModel(),
                systemPrompt,
                List.of(new ResponseMessage("user", List.of(new ResponseContent("input_text", userPrompt)))),
                properties.getTemperature(),
                properties.getMaxTokens()
            ))
            .retrieve()
            .body(ResponsesResponse.class);
        if (response == null) {
            return Optional.empty();
        }
        if (StringUtils.hasText(response.output_text())) {
            return Optional.of(response.output_text());
        }
        if (response.output() == null) {
            return Optional.empty();
        }
        StringBuilder textBuilder = new StringBuilder();
        for (ResponseOutputItem item : response.output()) {
            if (item.content() == null) {
                continue;
            }
            for (ResponseTextContent block : item.content()) {
                if (!StringUtils.hasText(block.text())) {
                    continue;
                }
                if (textBuilder.length() > 0) {
                    textBuilder.append('\n');
                }
                textBuilder.append(block.text());
            }
        }
        return textBuilder.length() > 0 ? Optional.of(textBuilder.toString()) : Optional.empty();
    }

    private Optional<String> doCreateChatCompletionsMessage(String systemPrompt, String userPrompt) {
        ChatCompletionsResponse response = restClient.post()
            .uri("chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(new ChatCompletionsRequest(
                properties.getModel(),
                List.of(
                    new ChatMessage("system", systemPrompt),
                    new ChatMessage("user", userPrompt)
                ),
                properties.getTemperature(),
                properties.getMaxTokens()
            ))
            .retrieve()
            .body(ChatCompletionsResponse.class);
        if (response == null || response.choices() == null) {
            return Optional.empty();
        }
        Optional<String> text = response.choices().stream()
            .map(ChatChoice::message)
            .filter(message -> message != null && StringUtils.hasText(message.content()))
            .map(ChatMessage::content)
            .reduce((left, right) -> left + "\n" + right);
        text.ifPresent(ignored -> {
            if (response.usage() != null) {
                log.info("LongCat agent chat completion from model {}, tokens: prompt={}, completion={}",
                    properties.getModel(),
                    response.usage().prompt_tokens(),
                    response.usage().completion_tokens());
            } else {
                log.info("LongCat agent chat completion received from model {}", properties.getModel());
            }
        });
        return text;
    }

    public Optional<String> streamTextMessage(
        String systemPrompt,
        String userPrompt,
        String runId,
        Consumer<String> onDelta
    ) {
        if (!supportsStreaming()) {
            return Optional.empty();
        }
        try {
            return doStreamChatCompletionsMessage(systemPrompt, userPrompt, runId, onDelta);
        } catch (Exception ex) {
            log.warn("LongCat agent streaming request failed: {}", ex.getMessage());
            return Optional.empty();
        } finally {
            if (runId != null) {
                activeStreams.remove(runId);
            }
        }
    }

    /**
     * 关闭指定 runId 对应的活跃 HTTP 流,用于取消时中断 provider 的阻塞读取。
     */
    public void cancelStream(String runId) {
        if (runId == null) {
            return;
        }
        HttpResponse<InputStream> response = activeStreams.remove(runId);
        if (response != null) {
            try {
                response.body().close();
                log.info("LongCat agent stream cancelled and closed for runId {}", runId);
            } catch (Exception ex) {
                log.debug("LongCat agent stream close on cancel ignored: {}", ex.getMessage());
            }
        }
    }

    private Optional<String> doStreamChatCompletionsMessage(
        String systemPrompt,
        String userPrompt,
        String runId,
        Consumer<String> onDelta
    ) throws Exception {
        String requestJson = objectMapper.writeValueAsString(Map.of(
            "model", properties.getModel(),
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ),
            "temperature", properties.getTemperature(),
            "max_tokens", properties.getMaxTokens(),
            "stream", true
        ));

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(normalizedBaseUrl + "/chat/completions"))
            .timeout(Duration.ofSeconds(120))
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .POST(HttpRequest.BodyPublishers.ofString(requestJson));
        if (StringUtils.hasText(properties.getApiKey())) {
            if (openAiAuth) {
                requestBuilder.header("Authorization", "Bearer " + properties.getApiKey());
            } else {
                requestBuilder.header("x-api-key", properties.getApiKey());
            }
        }

        HttpResponse<InputStream> response = streamingHttpClient.send(
            requestBuilder.build(),
            HttpResponse.BodyHandlers.ofInputStream()
        );
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("LongCat streaming response failed: status={}", response.statusCode());
            return Optional.empty();
        }
        if (runId != null) {
            activeStreams.put(runId, response);
        }

        StringBuilder answer = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (Thread.currentThread().isInterrupted()) {
                    log.info("LongCat agent stream interrupted, aborting read loop for runId {}", runId);
                    break;
                }
                String payload = normalizeSsePayload(line);
                if (!StringUtils.hasText(payload) || "[DONE]".equals(payload)) {
                    continue;
                }
                String delta = parseChatCompletionDelta(payload);
                if (StringUtils.hasText(delta)) {
                    answer.append(delta);
                    onDelta.accept(delta);
                }
            }
        }
        return answer.length() > 0 ? Optional.of(answer.toString()) : Optional.empty();
    }

    private String normalizeBaseUrl(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }

    private String normalizeSsePayload(String line) {
        if (line == null) {
            return "";
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith(":") || trimmed.startsWith("event:") || trimmed.startsWith("id:")) {
            return "";
        }
        return trimmed.startsWith("data:")
            ? trimmed.substring("data:".length()).trim()
            : trimmed;
    }

    private String parseChatCompletionDelta(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return "";
            }
            JsonNode choice = choices.get(0);
            return choice.path("delta").path("content").asText("");
        } catch (Exception ex) {
            log.debug("Failed to parse streaming chunk: {}", ex.getMessage());
            return "";
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

    private record ChatCompletionsRequest(
        String model,
        List<ChatMessage> messages,
        double temperature,
        int max_tokens
    ) {}

    private record ChatMessage(String role, String content) {}

    private record ResponsesRequest(
        String model,
        String instructions,
        List<ResponseMessage> input,
        double temperature,
        int max_output_tokens
    ) {}

    private record ResponseMessage(String role, List<ResponseContent> content) {}

    private record ResponseContent(String type, String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AnthropicResponse(List<ContentBlock> content, Usage usage) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Usage(Integer input_tokens, Integer output_tokens) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ContentBlock(String type, String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ResponsesResponse(String output_text, List<ResponseOutputItem> output) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ResponseOutputItem(String type, List<ResponseTextContent> content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ResponseTextContent(String type, String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatCompletionsResponse(List<ChatChoice> choices, ChatUsage usage) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatChoice(ChatMessage message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatUsage(Integer prompt_tokens, Integer completion_tokens, Integer total_tokens) {}
}
