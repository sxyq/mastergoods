package com.zhihuiji.backend.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
import java.util.ArrayList;
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
        if (!isConfigured()) {
            return false;
        }
        return "responses".equalsIgnoreCase(wireApi)
            || "chat_completions".equalsIgnoreCase(wireApi)
            || supportsAnthropicMessagesApi();
    }

    public String streamingUnavailableStatus() {
        if (!isConfigured()) {
            return configurationStatus();
        }
        return supportsStreaming() ? "configured" : "stream_not_supported";
    }

    private boolean usesOpenAiAuth(String wireApi) {
        return properties.isRequiresOpenaiAuth()
            || "responses".equalsIgnoreCase(wireApi)
            || "chat_completions".equalsIgnoreCase(wireApi)
            || "completions".equalsIgnoreCase(wireApi);
    }

    private boolean supportsAnthropicMessagesApi() {
        return !"responses".equalsIgnoreCase(wireApi)
            && !"chat_completions".equalsIgnoreCase(wireApi)
            && !"completions".equalsIgnoreCase(wireApi);
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
                List.of(new Message("user", userPrompt)),
                null
            ))
            .retrieve()
            .body(AnthropicResponse.class);
        return extractTextFromAnthropicResponse(response);
    }

    private Optional<String> extractTextFromAnthropicResponse(AnthropicResponse response) {
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

    /**
     * 原生 Function Calling：调用 Anthropic Messages API 并附带 tools 定义。
     *
     * <p>模型返回原生工具调用时，提取工具名与参数；同时收集文本说明（rationale）。
     * 当前支持：
     * <ul>
     *   <li>Anthropic Messages API：{@code tool_use} content block</li>
     *   <li>OpenAI Responses API：{@code response.output[*].type=function_call}</li>
     *   <li>Chat Completions API：{@code message.tool_calls}</li>
     * </ul>
     * 其他不支持原生工具调用的路径返回 empty，由调用方降级到 JSON 字符串解析路径。
     *
     * @param systemPrompt 系统提示
     * @param userPrompt   用户提示
     * @param tools        工具定义列表，null 或空列表返回 empty
     * @return 工具调用响应 Optional；模型未返回 tool_use 时 text 字段仍可能非空
     */
    public Optional<ToolUseResponse> createMessageWithTools(
        String systemPrompt,
        String userPrompt,
        List<ToolDefinition> tools
    ) {
        if (!isConfigured() || tools == null || tools.isEmpty()) {
            return Optional.empty();
        }
        if ("responses".equalsIgnoreCase(wireApi)) {
            return doCreateResponsesMessageWithTools(systemPrompt, userPrompt, tools);
        }
        if ("chat_completions".equalsIgnoreCase(wireApi)) {
            return doCreateChatCompletionsMessageWithTools(systemPrompt, userPrompt, tools);
        }
        // legacy completions 暂未接原生工具调用，仍由调用方降级
        if ("completions".equalsIgnoreCase(wireApi)) {
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
                    List.of(new Message("user", userPrompt)),
                    tools
                ))
                .retrieve()
                .body(AnthropicResponse.class);
            if (response == null || response.content() == null) {
                return Optional.empty();
            }
            List<ToolUseBlock> toolUses = new ArrayList<>();
            StringBuilder textBuilder = new StringBuilder();
            for (ContentBlock block : response.content()) {
                if ("tool_use".equalsIgnoreCase(block.type())) {
                    if (StringUtils.hasText(block.name())) {
                        toolUses.add(new ToolUseBlock(block.id(), block.name(), block.input()));
                    }
                } else if ("text".equalsIgnoreCase(block.type()) && StringUtils.hasText(block.text())) {
                    if (textBuilder.length() > 0) {
                        textBuilder.append('\n');
                    }
                    textBuilder.append(block.text());
                }
            }
            if (toolUses.isEmpty() && textBuilder.length() == 0) {
                return Optional.empty();
            }
            String text = textBuilder.length() > 0 ? textBuilder.toString() : null;
            if (response.usage() != null) {
                log.info("LongCat agent tool_use response from model {}, tools={}, tokens: input={}, output={}",
                    properties.getModel(),
                    toolUses.size(),
                    response.usage().input_tokens(),
                    response.usage().output_tokens());
            } else {
                log.info("LongCat agent tool_use response received from model {}, tools={}",
                    properties.getModel(), toolUses.size());
            }
            return Optional.of(new ToolUseResponse(toolUses, text));
        } catch (Exception ex) {
            log.warn("LongCat agent createMessageWithTools failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<ToolUseResponse> doCreateChatCompletionsMessageWithTools(
        String systemPrompt,
        String userPrompt,
        List<ToolDefinition> tools
    ) {
        try {
            ChatCompletionsResponse response = restClient.post()
                .uri("chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChatCompletionsToolRequest(
                    properties.getModel(),
                    List.of(
                        new ChatMessage("system", systemPrompt),
                        new ChatMessage("user", userPrompt)
                    ),
                    properties.getTemperature(),
                    properties.getMaxTokens(),
                    tools.stream()
                        .map(tool -> new ChatCompletionsTool(
                            "function",
                            new ChatCompletionsFunctionDefinition(
                                tool.name(),
                                tool.description(),
                                tool.input_schema()
                            )
                        ))
                        .toList(),
                    "auto"
                ))
                .retrieve()
                .body(ChatCompletionsResponse.class);
            if (response == null || response.choices() == null) {
                return Optional.empty();
            }
            List<ToolUseBlock> toolUses = new ArrayList<>();
            StringBuilder textBuilder = new StringBuilder();
            for (ChatChoice choice : response.choices()) {
                ChatCompletionResponseMessage message = choice.message();
                if (message == null) {
                    continue;
                }
                if (StringUtils.hasText(message.content())) {
                    if (textBuilder.length() > 0) {
                        textBuilder.append('\n');
                    }
                    textBuilder.append(message.content());
                }
                if (message.tool_calls() == null) {
                    continue;
                }
                for (ChatToolCall toolCall : message.tool_calls()) {
                    if (toolCall == null || toolCall.function() == null || !StringUtils.hasText(toolCall.function().name())) {
                        continue;
                    }
                    toolUses.add(new ToolUseBlock(
                        toolCall.id(),
                        toolCall.function().name(),
                        parseToolArguments(toolCall.function().arguments())
                    ));
                }
            }
            if (toolUses.isEmpty() && textBuilder.length() == 0) {
                return Optional.empty();
            }
            String text = textBuilder.length() > 0 ? textBuilder.toString() : null;
            if (response.usage() != null) {
                log.info("LongCat agent chat_completions tool response from model {}, tools={}, tokens: prompt={}, completion={}",
                    properties.getModel(),
                    toolUses.size(),
                    response.usage().prompt_tokens(),
                    response.usage().completion_tokens());
            } else {
                log.info("LongCat agent chat_completions tool response received from model {}, tools={}",
                    properties.getModel(), toolUses.size());
            }
            return Optional.of(new ToolUseResponse(toolUses, text));
        } catch (Exception ex) {
            log.warn("LongCat agent chat_completions createMessageWithTools failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<ToolUseResponse> doCreateResponsesMessageWithTools(
        String systemPrompt,
        String userPrompt,
        List<ToolDefinition> tools
    ) {
        try {
            ResponsesResponse response = restClient.post()
                .uri("responses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ResponsesToolRequest(
                    properties.getModel(),
                    systemPrompt,
                    List.of(new ResponseMessage("user", List.of(new ResponseContent("input_text", userPrompt)))),
                    properties.getTemperature(),
                    properties.getMaxTokens(),
                    tools.stream()
                        .map(tool -> new ResponsesToolDefinition(
                            "function",
                            tool.name(),
                            tool.description(),
                            tool.input_schema(),
                            true
                        ))
                        .toList()
                ))
                .retrieve()
                .body(ResponsesResponse.class);
            if (response == null) {
                return Optional.empty();
            }
            List<ToolUseBlock> toolUses = new ArrayList<>();
            StringBuilder textBuilder = new StringBuilder();
            if (StringUtils.hasText(response.output_text())) {
                textBuilder.append(response.output_text());
            }
            if (response.output() != null) {
                for (ResponseOutputItem item : response.output()) {
                    if (item == null) {
                        continue;
                    }
                    if ("function_call".equalsIgnoreCase(item.type()) && StringUtils.hasText(item.name())) {
                        toolUses.add(new ToolUseBlock(
                            item.call_id(),
                            item.name(),
                            parseToolArguments(item.arguments())
                        ));
                    }
                    appendResponsesText(item, textBuilder);
                }
            }
            if (toolUses.isEmpty() && textBuilder.length() == 0) {
                return Optional.empty();
            }
            String text = textBuilder.length() > 0 ? textBuilder.toString() : null;
            return Optional.of(new ToolUseResponse(toolUses, text));
        } catch (Exception ex) {
            log.warn("LongCat agent responses createMessageWithTools failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private void appendResponsesText(ResponseOutputItem item, StringBuilder textBuilder) {
        if (item.content() == null) {
            return;
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

    private JsonNode parseToolArguments(String arguments) {
        if (!StringUtils.hasText(arguments)) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode parsed = objectMapper.readTree(arguments);
            return parsed != null ? parsed : objectMapper.createObjectNode();
        } catch (Exception ex) {
            log.debug("Failed to parse tool arguments JSON: {}", ex.getMessage());
            return objectMapper.createObjectNode();
        }
    }

    /** 原生 Function Calling 工具定义，映射 Anthropic tools 字段。 */
    public record ToolDefinition(String name, String description, Map<String, Object> input_schema) {}

    /** 模型返回的单个 tool_use block。 */
    public record ToolUseBlock(String id, String name, JsonNode input) {}

    /** createMessageWithTools 的响应：tool_use 列表 + 可选辅助文本。 */
    public record ToolUseResponse(List<ToolUseBlock> toolUses, String text) {

        /** 便捷判断是否包含工具调用。 */
        public boolean hasToolUses() {
            return toolUses != null && !toolUses.isEmpty();
        }
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
            .map(ChatCompletionResponseMessage::content)
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
            if ("responses".equalsIgnoreCase(wireApi)) {
                return doStreamResponsesMessage(systemPrompt, userPrompt, runId, onDelta);
            }
            if ("chat_completions".equalsIgnoreCase(wireApi)) {
                return doStreamChatCompletionsMessage(systemPrompt, userPrompt, runId, onDelta);
            }
            if (supportsAnthropicMessagesApi()) {
                return doStreamAnthropicMessage(systemPrompt, userPrompt, runId, onDelta);
            }
            return Optional.empty();
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

    private Optional<String> doStreamResponsesMessage(
        String systemPrompt,
        String userPrompt,
        String runId,
        Consumer<String> onDelta
    ) throws Exception {
        String requestJson = objectMapper.writeValueAsString(Map.of(
            "model", properties.getModel(),
            "instructions", systemPrompt,
            "input", List.of(Map.of(
                "role", "user",
                "content", List.of(Map.of("type", "input_text", "text", userPrompt))
            )),
            "temperature", properties.getTemperature(),
            "max_output_tokens", properties.getMaxTokens(),
            "stream", true
        ));
        return doStreamRequest(
            normalizedBaseUrl + "/responses",
            requestJson,
            runId,
            onDelta,
            this::parseResponsesDelta
        );
    }

    private Optional<String> doStreamAnthropicMessage(
        String systemPrompt,
        String userPrompt,
        String runId,
        Consumer<String> onDelta
    ) throws Exception {
        String requestJson = objectMapper.writeValueAsString(new StreamingAnthropicRequest(
            properties.getModel(),
            properties.getMaxTokens(),
            properties.getTemperature(),
            properties.isEnableThinking() && properties.getModel().contains("Thinking"),
            properties.getThinkingBudget(),
            systemPrompt,
            List.of(new Message("user", userPrompt)),
            true
        ));
        return doStreamRequest(
            normalizedBaseUrl + "/v1/messages",
            requestJson,
            runId,
            onDelta,
            this::parseAnthropicMessageDelta
        );
    }

    private Optional<String> doStreamRequest(
        String url,
        String requestJson,
        String runId,
        Consumer<String> onDelta,
        StreamingDeltaParser deltaParser
    ) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
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
        if (supportsAnthropicMessagesApi() && StringUtils.hasText(properties.getAnthropicVersion())) {
            requestBuilder.header("anthropic-version", properties.getAnthropicVersion());
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
                String delta = deltaParser.parse(payload);
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

    private String parseResponsesDelta(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            if (!"response.output_text.delta".equals(root.path("type").asText())) {
                return "";
            }
            return root.path("delta").asText("");
        } catch (Exception ex) {
            log.debug("Failed to parse responses streaming chunk: {}", ex.getMessage());
            return "";
        }
    }

    private String parseAnthropicMessageDelta(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            if (!"content_block_delta".equals(root.path("type").asText())) {
                return "";
            }
            JsonNode delta = root.path("delta");
            if (!"text_delta".equals(delta.path("type").asText())) {
                return "";
            }
            return delta.path("text").asText("");
        } catch (Exception ex) {
            log.debug("Failed to parse anthropic streaming chunk: {}", ex.getMessage());
            return "";
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record AnthropicRequest(
        String model,
        int max_tokens,
        double temperature,
        boolean enable_thinking,
        int thinking_budget,
        String system,
        List<Message> messages,
        List<ToolDefinition> tools
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record StreamingAnthropicRequest(
        String model,
        int max_tokens,
        double temperature,
        boolean enable_thinking,
        int thinking_budget,
        String system,
        List<Message> messages,
        boolean stream
    ) {}

    private record Message(String role, String content) {}

    private record ChatCompletionsRequest(
        String model,
        List<ChatMessage> messages,
        double temperature,
        int max_tokens
    ) {}

    private record ChatCompletionsToolRequest(
        String model,
        List<ChatMessage> messages,
        double temperature,
        int max_tokens,
        List<ChatCompletionsTool> tools,
        String tool_choice
    ) {}

    private record ChatMessage(String role, String content) {}

    private record ChatCompletionsTool(String type, ChatCompletionsFunctionDefinition function) {}

    private record ChatCompletionsFunctionDefinition(
        String name,
        String description,
        Map<String, Object> parameters
    ) {}

    private record ResponsesRequest(
        String model,
        String instructions,
        List<ResponseMessage> input,
        double temperature,
        int max_output_tokens
    ) {}

    private record ResponsesToolRequest(
        String model,
        String instructions,
        List<ResponseMessage> input,
        double temperature,
        int max_output_tokens,
        List<ResponsesToolDefinition> tools
    ) {}

    private record ResponseMessage(String role, List<ResponseContent> content) {}

    private record ResponseContent(String type, String text) {}

    private record ResponsesToolDefinition(
        String type,
        String name,
        String description,
        Map<String, Object> parameters,
        boolean strict
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AnthropicResponse(List<ContentBlock> content, Usage usage) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Usage(Integer input_tokens, Integer output_tokens) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ContentBlock(String type, String text, String id, String name, JsonNode input) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ResponsesResponse(String output_text, List<ResponseOutputItem> output) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ResponseOutputItem(
        String type,
        List<ResponseTextContent> content,
        String call_id,
        String name,
        String arguments
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ResponseTextContent(String type, String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatCompletionsResponse(List<ChatChoice> choices, ChatUsage usage) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatChoice(ChatCompletionResponseMessage message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatCompletionResponseMessage(String role, String content, List<ChatToolCall> tool_calls) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatToolCall(String id, String type, ChatToolFunction function) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatToolFunction(String name, String arguments) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatUsage(Integer prompt_tokens, Integer completion_tokens, Integer total_tokens) {}

    @FunctionalInterface
    private interface StreamingDeltaParser {
        String parse(String payload);
    }
}
