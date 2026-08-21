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
import java.nio.charset.StandardCharsets;
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
import org.springframework.web.client.RestClientResponseException;

@Component
public class LongCatAnthropicClient {
    private static final Logger log = LoggerFactory.getLogger(LongCatAnthropicClient.class);
    private static final String PROVIDER_USER_AGENT = "Java/21";

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
            .requestFactory(requestFactory)
            // The configured provider rejects generic script clients but accepts the Java client policy used by production.
            .defaultHeader("User-Agent", PROVIDER_USER_AGENT);
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

    /**
     * Chat Completions providers do not expose a response id, but they do support
     * the standard assistant tool_calls -> tool messages continuation sequence.
     */
    public boolean supportsToolResultContinuation() {
        return isConfigured()
            && ("responses".equalsIgnoreCase(wireApi)
                || "chat_completions".equalsIgnoreCase(wireApi));
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
        return createJsonMessage(systemPrompt, userPrompt, List.of());
    }

    public Optional<String> createJsonMessage(String systemPrompt, String userPrompt, List<ImageInput> imageInputs) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Optional<String> result = doCreateJsonMessage(systemPrompt, userPrompt, imageInputs);
                if (result.isPresent()) return result;
                lastException = null;
            } catch (Exception ex) {
                lastException = ex;
                log.warn("LongCat agent request attempt {}/{} failed: {}", attempt, MAX_RETRIES, ex.getMessage());
                if (isProviderRateLimited(ex)) {
                    // A provider concurrency quota will not recover within this
                    // request. Retrying here only prolongs the Agent run and can
                    // leave its audit in running state after the client times out.
                    break;
                }
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

    private Optional<String> doCreateJsonMessage(String systemPrompt, String userPrompt, List<ImageInput> imageInputs) {
        if ("responses".equalsIgnoreCase(wireApi)) {
            return doCreateResponsesMessage(systemPrompt, userPrompt, imageInputs);
        }
        if ("chat_completions".equalsIgnoreCase(wireApi)) {
            return doCreateChatCompletionsMessage(systemPrompt, userPrompt, imageInputs);
        }
        AnthropicResponse response = restClient.post()
            .uri(endpointUri("v1/messages"))
            .contentType(MediaType.APPLICATION_JSON)
            .body(new AnthropicRequest(
                properties.getModel(),
                properties.getMaxTokens(),
                properties.getTemperature(),
                properties.isEnableThinking() && properties.getModel().contains("Thinking"),
                properties.getThinkingBudget(),
                systemPrompt,
                List.of(new Message("user", anthropicMessageContent(userPrompt, imageInputs))),
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
        return createMessageWithTools(systemPrompt, userPrompt, tools, "auto");
    }

    /**
     * 调用原生工具选择，并可在 ReAct 后续轮指定模型必须调用的工具。
     *
     * <p>toolChoice 使用 {@code auto}、{@code required} 或工具名。工具名会被转换为
     * OpenAI 兼容接口的命名 tool_choice，避免模型在已经拿到完整事实后停在只读查询轮。
     */
    public Optional<ToolUseResponse> createMessageWithTools(
        String systemPrompt,
        String userPrompt,
        List<ToolDefinition> tools,
        String toolChoice
    ) {
        if (!isConfigured() || tools == null || tools.isEmpty()) {
            return Optional.empty();
        }
        if ("responses".equalsIgnoreCase(wireApi)) {
            return doCreateResponsesMessageWithTools(systemPrompt, userPrompt, tools, toolChoice);
        }
        if ("chat_completions".equalsIgnoreCase(wireApi)) {
            return doCreateChatCompletionsMessageWithTools(systemPrompt, userPrompt, tools, toolChoice);
        }
        // legacy completions 暂未接原生工具调用，仍由调用方降级
        if ("completions".equalsIgnoreCase(wireApi)) {
            return Optional.empty();
        }
        try {
            AnthropicResponse response = restClient.post()
                .uri(endpointUri("v1/messages"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AnthropicRequest(
                    properties.getModel(),
                    properties.getMaxTokens(),
                    properties.getTemperature(),
                    properties.isEnableThinking() && properties.getModel().contains("Thinking"),
                    properties.getThinkingBudget(),
                    systemPrompt,
                    List.of(new Message("user", anthropicMessageContent(userPrompt, List.of()))),
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
        List<ToolDefinition> tools,
        String toolChoice
    ) {
        return doCreateChatCompletionsMessageWithTools(systemPrompt, userPrompt, tools, toolChoice, true);
    }

    private Optional<ToolUseResponse> doCreateChatCompletionsMessageWithTools(
        String systemPrompt,
        String userPrompt,
        List<ToolDefinition> tools,
        String toolChoice,
        boolean allowToolChoiceFallback
    ) {
        try {
            ChatCompletionsResponse response = postJsonForValue(
                "chat/completions",
                new ChatCompletionsToolRequest(
                    properties.getModel(),
                    List.of(
                        new ChatMessage("system", systemPrompt),
                        new ChatMessage("user", chatMessageContent(userPrompt, List.of()))
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
                    providerToolChoice(toolChoice)
                ),
                ChatCompletionsResponse.class
            );
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
            if (allowToolChoiceFallback && isNamedToolChoice(toolChoice)) {
                List<ToolDefinition> targetTools = tools == null
                    ? List.of()
                    : tools.stream()
                        .filter(tool -> tool != null && toolChoice.equals(tool.name()))
                        .toList();
                if (!targetTools.isEmpty()) {
                    log.warn(
                        "LongCat agent provider rejected named tool_choice={}, retrying with auto and only the target tool: {}",
                        toolChoice,
                        ex.getMessage()
                    );
                    return doCreateChatCompletionsMessageWithTools(
                        systemPrompt,
                        userPrompt,
                        targetTools,
                        "auto",
                        false
                    );
                }
            }
            log.warn("LongCat agent chat_completions createMessageWithTools failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private boolean isNamedToolChoice(String toolChoice) {
        return StringUtils.hasText(toolChoice)
            && !"auto".equalsIgnoreCase(toolChoice)
            && !"required".equalsIgnoreCase(toolChoice);
    }

    private Optional<ToolUseResponse> doCreateResponsesMessageWithTools(
        String systemPrompt,
        String userPrompt,
        List<ToolDefinition> tools,
        String toolChoice
    ) {
        try {
            ResponsesResponse response = postJsonForValue(
                "responses",
                new ResponsesToolRequest(
                properties.getModel(),
                systemPrompt,
                List.of(new ResponseMessage("user", responsesContent(userPrompt, List.of()))),
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
                        .toList(),
                    // The planner normally passes auto. It can use required
                    // only after an explicit multi-source request has one
                    // remaining data source, so the provider cannot terminate
                    // before the requested facts are collected.
                    providerToolChoice(toolChoice)
                ),
                ResponsesResponse.class
            );
            if (response == null) {
                return Optional.empty();
            }
            List<ToolUseBlock> toolUses = new ArrayList<>();
            StringBuilder textBuilder = new StringBuilder();
            if (StringUtils.hasText(response.output_text())) {
                textBuilder.append(response.output_text());
            }
            boolean hasOutputText = textBuilder.length() > 0;
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
                    // 当 output_text 已经包含完整文本时，不再从 output[] 中重复追加文本
                    if (!hasOutputText) {
                        appendResponsesText(item, textBuilder);
                    }
                }
            }
            if (toolUses.isEmpty() && textBuilder.length() == 0) {
                return fallbackResponsesToolUseToChatCompletions(systemPrompt, userPrompt, tools, "empty_response");
            }
            String text = textBuilder.length() > 0 ? textBuilder.toString() : null;
            return Optional.of(new ToolUseResponse(toolUses, text, response.id()));
        } catch (Exception ex) {
            log.warn("LongCat agent responses createMessageWithTools failed: {}", ex.getMessage());
            return fallbackResponsesToolUseToChatCompletions(systemPrompt, userPrompt, tools, ex.getClass().getSimpleName());
        }
    }

    private Optional<ToolUseResponse> fallbackResponsesToolUseToChatCompletions(
        String systemPrompt,
        String userPrompt,
        List<ToolDefinition> tools,
        String reason
    ) {
        log.info("LongCat agent falling back from responses tool calling to chat_completions, reason={}", reason);
        return doCreateChatCompletionsMessageWithTools(systemPrompt, userPrompt, tools, "auto");
    }

    private Object providerToolChoice(String toolChoice) {
        if (!StringUtils.hasText(toolChoice) || "auto".equalsIgnoreCase(toolChoice)) {
            return "auto";
        }
        if ("required".equalsIgnoreCase(toolChoice)) {
            return "required";
        }
        return Map.of(
            "type", "function",
            "function", Map.of("name", toolChoice)
        );
    }

    /**
     * 原生 function_call_output 续轮：将服务端执行的真实工具结果以 provider 要求的格式回传给模型，
     * 让模型基于真实 facts 生成最终正式回答。
     *
     * <p>当前仅 Responses API（wire_api=responses）实现该路径。请求格式：
     * <ul>
     *   <li>使用 {@code previous_response_id} 关联上一轮工具调用响应</li>
     *   <li>{@code input} 中携带 {@code function_call} 项（模型上一轮返回的工具调用）
     *       与 {@code function_call_output} 项（服务端执行后的真实结果）</li>
     *   <li>{@code tools} 字段保持与首轮一致，允许模型继续调用或直接生成最终回答</li>
     * </ul>
     *
     * <p>provider 不支持该续轮格式时（如 HTTP 400），方法返回 empty 并记录 warn 日志，
     * 由调用方降级到应用层 facts 注入路径。不得伪造续轮成功。
     *
     * @param previousResponseId 首轮工具调用响应的 ID（来自 {@link ToolUseResponse#responseId()}）
     * @param systemPrompt       系统提示
     * @param userPrompt         原始用户问题（用于无 previous_response_id 时的回退）
     * @param functionCalls      模型上一轮返回的 function_call 列表
     * @param toolOutputs        服务端执行后的 function_call_output 列表（callId 与 functionCalls 对应）
     * @param tools              工具定义列表，保持与首轮一致
     * @return 模型续轮响应 Optional；provider 不支持时为 empty
     */
    public Optional<ToolUseResponse> continueWithToolOutputs(
        String previousResponseId,
        String systemPrompt,
        String userPrompt,
        List<FunctionCallItem> functionCalls,
        List<FunctionCallOutputItem> toolOutputs,
        List<ToolDefinition> tools
    ) {
        return continueWithToolOutputs(
            previousResponseId,
            systemPrompt,
            userPrompt,
            functionCalls,
            toolOutputs,
            tools,
            "auto"
        );
    }

    /**
     * 续轮工具选择。默认仍由模型自主决定；只有调用方已经确认用户明确要求某种结果，
     * 且候选列表只剩一个对应工具时，才可以传入该工具名，避免模型停在没有结构化结果的文字回答。
     */
    public Optional<ToolUseResponse> continueWithToolOutputs(
        String previousResponseId,
        String systemPrompt,
        String userPrompt,
        List<FunctionCallItem> functionCalls,
        List<FunctionCallOutputItem> toolOutputs,
        List<ToolDefinition> tools,
        String toolChoice
    ) {
        if (!isConfigured() || functionCalls == null || functionCalls.isEmpty()
            || toolOutputs == null || toolOutputs.isEmpty()) {
            return Optional.empty();
        }
        if ("chat_completions".equalsIgnoreCase(wireApi)) {
            // Chat Completions uses the provider-neutral assistant/tool message
            // protocol rather than previous_response_id.
            return doContinueChatCompletionsWithToolOutputs(
                systemPrompt, userPrompt, functionCalls, toolOutputs, tools, toolChoice
            );
        }
        if (!"responses".equalsIgnoreCase(wireApi)) {
            log.debug("LongCat agent continueWithToolOutputs skipped: wireApi={} has no supported continuation", wireApi);
            return Optional.empty();
        }
        try {
            List<Object> inputItems = new ArrayList<>();
            boolean hasPreviousResponse = StringUtils.hasText(previousResponseId);
            // With previous_response_id the provider already owns the prior function_call.
            // Replaying it together with function_call_output makes the continuation invalid
            // for providers that enforce the Responses conversation state.
            if (!hasPreviousResponse) {
                for (FunctionCallItem call : functionCalls) {
                    if (call == null || !StringUtils.hasText(call.id()) || !StringUtils.hasText(call.name())) {
                        continue;
                    }
                    inputItems.add(new ResponsesFunctionCallItem(
                        "function_call",
                        call.id(),
                        call.name(),
                        call.arguments() != null ? call.arguments() : "{}"
                    ));
                }
            }
            for (FunctionCallOutputItem output : toolOutputs) {
                if (output == null || !StringUtils.hasText(output.callId())) {
                    continue;
                }
                inputItems.add(new ResponsesFunctionCallOutputItem(
                    "function_call_output",
                    output.callId(),
                    output.output() != null ? output.output() : ""
                ));
            }
            if (inputItems.isEmpty()) {
                return Optional.empty();
            }
            ResponsesToolContinuationRequest request = new ResponsesToolContinuationRequest(
                properties.getModel(),
                systemPrompt,
                hasPreviousResponse ? previousResponseId : null,
                inputItems,
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
                    .toList(),
                providerToolChoice(toolChoice)
            );
            ResponsesResponse response = postJsonForValue("responses", request, ResponsesResponse.class);
            if (response == null) {
                return Optional.empty();
            }
            List<ToolUseBlock> continuedToolUses = new ArrayList<>();
            StringBuilder textBuilder = new StringBuilder();
            if (StringUtils.hasText(response.output_text())) {
                textBuilder.append(response.output_text());
            }
            boolean hasOutputText = textBuilder.length() > 0;
            if (response.output() != null) {
                for (ResponseOutputItem item : response.output()) {
                    if (item == null) {
                        continue;
                    }
                    if ("function_call".equalsIgnoreCase(item.type()) && StringUtils.hasText(item.name())) {
                        continuedToolUses.add(new ToolUseBlock(
                            item.call_id(),
                            item.name(),
                            parseToolArguments(item.arguments())
                        ));
                    }
                    // 当 output_text 已经包含完整文本时，不再从 output[] 中重复追加文本
                    // 仅当 output_text 为空时才从 message.content 中提取文本（兼容某些 provider 不填 output_text）
                    if (!hasOutputText) {
                        appendResponsesText(item, textBuilder);
                    }
                }
            }
            if (continuedToolUses.isEmpty() && textBuilder.length() == 0) {
                return Optional.empty();
            }
            String text = textBuilder.length() > 0 ? textBuilder.toString() : null;
            log.info("LongCat agent function_call_output continuation succeeded, responseId={}, toolUses={}, textLength={}",
                response.id(), continuedToolUses.size(), text == null ? 0 : text.length());
            return Optional.of(new ToolUseResponse(continuedToolUses, text, response.id()));
        } catch (Exception ex) {
            // provider 不支持该续轮格式时（如 HTTP 400），记录 warn 日志并返回 empty
            log.warn("LongCat agent function_call_output continuation failed (provider may not support this format): {}",
                ex.getMessage());
            Optional<ToolUseResponse> chatContinuation = doContinueChatCompletionsWithToolOutputs(
                systemPrompt, userPrompt, functionCalls, toolOutputs, tools, toolChoice
            );
            if (chatContinuation.isPresent()) {
                log.info("LongCat agent recovered tool result continuation through chat_completions");
            }
            return chatContinuation;
        }
    }

    private Optional<ToolUseResponse> doContinueChatCompletionsWithToolOutputs(
        String systemPrompt,
        String userPrompt,
        List<FunctionCallItem> functionCalls,
        List<FunctionCallOutputItem> toolOutputs,
        List<ToolDefinition> tools,
        String toolChoice
    ) {
        try {
            List<ChatCompletionRequestMessage> messages = new ArrayList<>();
            messages.add(new ChatCompletionRequestMessage("system", systemPrompt, null, null));
            messages.add(new ChatCompletionRequestMessage("user", userPrompt, null, null));

            List<ChatCompletionToolCall> assistantToolCalls = functionCalls.stream()
                .filter(call -> call != null && StringUtils.hasText(call.id()) && StringUtils.hasText(call.name()))
                .map(call -> new ChatCompletionToolCall(
                    call.id(),
                    "function",
                    new ChatToolFunction(call.name(), call.arguments() == null ? "{}" : call.arguments())
                ))
                .toList();
            if (assistantToolCalls.isEmpty()) {
                return Optional.empty();
            }
            messages.add(new ChatCompletionRequestMessage("assistant", null, assistantToolCalls, null));
            for (FunctionCallOutputItem output : toolOutputs) {
                if (output == null || !StringUtils.hasText(output.callId())) {
                    continue;
                }
                messages.add(new ChatCompletionRequestMessage(
                    "tool",
                    output.output() == null ? "" : output.output(),
                    null,
                    output.callId()
                ));
            }

            ChatCompletionsResponse response = postJsonForValue(
                "chat/completions",
                new ChatCompletionsContinuationRequest(
                    properties.getModel(),
                    messages,
                    properties.getTemperature(),
                    properties.getMaxTokens(),
                    tools.stream()
                        .map(tool -> new ChatCompletionsTool(
                            "function",
                            new ChatCompletionsFunctionDefinition(
                                tool.name(), tool.description(), tool.input_schema()
                            )
                        ))
                        .toList(),
                    // The planner normally passes auto. It can use required
                    // only after an explicit multi-source request has one
                    // remaining data source, so the provider cannot terminate
                    // before the requested facts are collected.
                    providerToolChoice(toolChoice)
                ),
                ChatCompletionsResponse.class
            );
            return parseChatCompletionsToolResponse(response);
        } catch (Exception ex) {
            log.warn("LongCat agent chat_completions tool continuation failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<ToolUseResponse> parseChatCompletionsToolResponse(ChatCompletionsResponse response) {
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
        return Optional.of(new ToolUseResponse(
            toolUses,
            textBuilder.length() > 0 ? textBuilder.toString() : null,
            null
        ));
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

    public record ImageInput(String mimeType, String dataUrl) {}

    /** createMessageWithTools 的响应：tool_use 列表 + 可选辅助文本 + provider 响应 ID（用于续轮）。 */
    public record ToolUseResponse(List<ToolUseBlock> toolUses, String text, String responseId) {

        public ToolUseResponse(List<ToolUseBlock> toolUses, String text) {
            this(toolUses, text, null);
        }

        /** 便捷判断是否包含工具调用。 */
        public boolean hasToolUses() {
            return toolUses != null && !toolUses.isEmpty();
        }
    }

    /** 续轮中已执行的 function_call 项（模型上一轮返回的工具调用）。 */
    public record FunctionCallItem(String id, String name, String arguments) {}

    /** 续轮中回传给模型的 function_call_output 项（服务端执行后的真实结果）。 */
    public record FunctionCallOutputItem(String callId, String output) {}

    private Optional<String> doCreateResponsesMessage(String systemPrompt, String userPrompt, List<ImageInput> imageInputs) {
        ResponsesResponse response = postJsonForValue(
            "responses",
            new ResponsesRequest(
                properties.getModel(),
                systemPrompt,
                List.of(new ResponseMessage("user", responsesContent(userPrompt, imageInputs))),
                properties.getTemperature(),
                properties.getMaxTokens()
            ),
            ResponsesResponse.class
        );
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

    private Optional<String> doCreateChatCompletionsMessage(String systemPrompt, String userPrompt, List<ImageInput> imageInputs) {
        ChatCompletionsResponse response = postJsonForValue(
            "chat/completions",
            new ChatCompletionsRequest(
                properties.getModel(),
                List.of(
                    new ChatMessage("system", systemPrompt),
                    new ChatMessage("user", chatMessageContent(userPrompt, imageInputs))
                ),
                properties.getTemperature(),
                properties.getMaxTokens()
            ),
            ChatCompletionsResponse.class
        );
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

    private <T> T postJsonForValue(String uri, Object requestBody, Class<T> responseType) {
        try {
            return restClient.post()
                .uri(endpointUri(uri))
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                // Read the raw response stream. Some compatible providers label
                // JSON as application/octet-stream, which can fail before a
                // normal RestClient message converter gets a chance to parse it.
                .exchange((request, response) -> {
                    byte[] responseBytes = response.getBody().readAllBytes();
                    if (response.getStatusCode().isError()) {
                        log.warn(
                            "LongCat provider request failed: uri={}, status={}, body={}",
                            uri,
                            response.getStatusCode().value(),
                            truncateProviderError(new String(responseBytes, StandardCharsets.UTF_8))
                        );
                        throw new RestClientResponseException(
                            "Provider returned HTTP " + response.getStatusCode().value(),
                            response.getStatusCode().value(),
                            response.getStatusText(),
                            response.getHeaders(),
                            responseBytes,
                            StandardCharsets.UTF_8
                        );
                    }
                    String responseBody = new String(responseBytes, StandardCharsets.UTF_8);
                    if (!StringUtils.hasText(responseBody)) {
                        return null;
                    }
                    try {
                        return objectMapper.readValue(responseBody, responseType);
                    } catch (Exception ex) {
                        throw new IllegalStateException(
                            "Failed to parse provider JSON response from " + uri + ": " + ex.getMessage(),
                            ex
                        );
                    }
                });
        } catch (RestClientResponseException ex) {
            log.warn(
                "LongCat provider request failed: uri={}, status={}, body={}",
                uri,
                ex.getStatusCode().value(),
                truncateProviderError(ex.getResponseBodyAsString())
            );
            throw ex;
        }
    }

    private boolean isProviderRateLimited(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof RestClientResponseException responseException
                && responseException.getStatusCode().value() == 429) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && (
                message.contains("429")
                    || message.contains("UPSTREAM_RATE_LIMITED")
                    || message.contains("Concurrency exceeded")
            )) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String truncateProviderError(String body) {
        if (!StringUtils.hasText(body)) {
            return "<empty>";
        }
        return body.length() <= 500 ? body : body.substring(0, 500) + "...";
    }

    private String endpointUri(String uri) {
        if (!StringUtils.hasText(uri)) {
            return normalizedBaseUrl;
        }
        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            return uri;
        }
        String path = uri.startsWith("/") ? uri.substring(1) : uri;
        return normalizedBaseUrl + "/" + path;
    }

    public Optional<String> streamTextMessage(
        String systemPrompt,
        String userPrompt,
        String runId,
        Consumer<String> onDelta
    ) {
        return streamTextMessage(systemPrompt, userPrompt, List.of(), runId, onDelta);
    }

    public Optional<String> streamTextMessage(
        String systemPrompt,
        String userPrompt,
        List<ImageInput> imageInputs,
        String runId,
        Consumer<String> onDelta
    ) {
        if (!supportsStreaming()) {
            return Optional.empty();
        }
        try {
            if ("responses".equalsIgnoreCase(wireApi)) {
                return doStreamResponsesMessage(systemPrompt, userPrompt, imageInputs, runId, onDelta);
            }
            if ("chat_completions".equalsIgnoreCase(wireApi)) {
                return doStreamChatCompletionsMessage(systemPrompt, userPrompt, imageInputs, runId, onDelta);
            }
            if (supportsAnthropicMessagesApi()) {
                return doStreamAnthropicMessage(systemPrompt, userPrompt, imageInputs, runId, onDelta);
            }
            return Optional.empty();
        } catch (Exception ex) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
            }
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
        List<ImageInput> imageInputs,
        String runId,
        Consumer<String> onDelta
    ) throws Exception {
        String requestJson = objectMapper.writeValueAsString(Map.of(
            "model", properties.getModel(),
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", chatMessageContent(userPrompt, imageInputs))
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
            .header("User-Agent", PROVIDER_USER_AGENT)
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
            try (InputStream ignored = response.body()) {
                log.warn("LongCat streaming response failed: status={}", response.statusCode());
            }
            return Optional.empty();
        }
        if (runId != null) {
            activeStreams.put(runId, response);
        }

        StringBuilder answer = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
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
        List<ImageInput> imageInputs,
        String runId,
        Consumer<String> onDelta
    ) throws Exception {
        String requestJson = objectMapper.writeValueAsString(Map.of(
            "model", properties.getModel(),
            "instructions", systemPrompt,
            "input", List.of(Map.of(
                "role", "user",
                "content", responsesContent(userPrompt, imageInputs)
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
        List<ImageInput> imageInputs,
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
            List.of(new Message("user", anthropicMessageContent(userPrompt, imageInputs))),
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
            .header("User-Agent", PROVIDER_USER_AGENT)
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
            try (InputStream ignored = response.body()) {
                log.warn("LongCat streaming response failed: status={}", response.statusCode());
            }
            return Optional.empty();
        }
        if (runId != null) {
            activeStreams.put(runId, response);
        }

        StringBuilder answer = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
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

    private Object chatMessageContent(String userPrompt, List<ImageInput> imageInputs) {
        if (imageInputs == null || imageInputs.isEmpty()) {
            return userPrompt;
        }
        List<ChatContentPart> content = new ArrayList<>();
        content.add(new ChatContentPart("text", userPrompt, null));
        for (ImageInput imageInput : imageInputs) {
            if (imageInput == null || !StringUtils.hasText(imageInput.dataUrl())) {
                continue;
            }
            content.add(new ChatContentPart("image_url", null, new OpenAiImageUrl(imageInput.dataUrl())));
        }
        return content;
    }

    private List<ResponseContent> responsesContent(String userPrompt, List<ImageInput> imageInputs) {
        List<ResponseContent> content = new ArrayList<>();
        content.add(new ResponseContent("input_text", userPrompt, null));
        if (imageInputs == null) {
            return content;
        }
        for (ImageInput imageInput : imageInputs) {
            if (imageInput == null || !StringUtils.hasText(imageInput.dataUrl())) {
                continue;
            }
            content.add(new ResponseContent("input_image", null, imageInput.dataUrl()));
        }
        return content;
    }

    private List<AnthropicContentPart> anthropicMessageContent(String userPrompt, List<ImageInput> imageInputs) {
        List<AnthropicContentPart> content = new ArrayList<>();
        content.add(new AnthropicContentPart("text", userPrompt, null));
        if (imageInputs == null) {
            return content;
        }
        for (ImageInput imageInput : imageInputs) {
            if (imageInput == null
                || !StringUtils.hasText(imageInput.dataUrl())
                || !StringUtils.hasText(imageInput.mimeType())) {
                continue;
            }
            int commaIndex = imageInput.dataUrl().indexOf(',');
            if (commaIndex < 0 || commaIndex >= imageInput.dataUrl().length() - 1) {
                continue;
            }
            content.add(new AnthropicContentPart(
                "image",
                null,
                new AnthropicImageSource("base64", imageInput.mimeType(), imageInput.dataUrl().substring(commaIndex + 1))
            ));
        }
        return content;
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record Message(String role, Object content) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record AnthropicContentPart(String type, String text, AnthropicImageSource source) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record AnthropicImageSource(String type, String media_type, String data) {}

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
        Object tool_choice
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record ChatCompletionRequestMessage(
        String role,
        Object content,
        List<ChatCompletionToolCall> tool_calls,
        String tool_call_id
    ) {}

    private record ChatCompletionToolCall(String id, String type, ChatToolFunction function) {}

    private record ChatCompletionsContinuationRequest(
        String model,
        List<ChatCompletionRequestMessage> messages,
        double temperature,
        int max_tokens,
        List<ChatCompletionsTool> tools,
        Object tool_choice
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record ChatMessage(String role, Object content) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record ChatContentPart(String type, String text, OpenAiImageUrl image_url) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record OpenAiImageUrl(String url) {}

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
        List<ResponsesToolDefinition> tools,
        Object tool_choice
    ) {}

    private record ResponseMessage(String role, List<ResponseContent> content) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record ResponseContent(String type, String text, String image_url) {}

    private record ResponsesToolDefinition(
        String type,
        String name,
        String description,
        Map<String, Object> parameters,
        boolean strict
    ) {}

    /** Responses API 续轮请求中的 function_call 项（模型上一轮返回的工具调用）。 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record ResponsesFunctionCallItem(
        String type,
        String id,
        String name,
        String arguments
    ) {}

    /** Responses API 续轮请求中的 function_call_output 项（服务端执行后的真实结果）。 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record ResponsesFunctionCallOutputItem(
        String type,
        String call_id,
        String output
    ) {}

    /** Responses API 续轮请求体（function_call_output continuation）。 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record ResponsesToolContinuationRequest(
        String model,
        String instructions,
        String previous_response_id,
        List<Object> input,
        double temperature,
        int max_output_tokens,
        List<ResponsesToolDefinition> tools,
        Object tool_choice
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AnthropicResponse(List<ContentBlock> content, Usage usage) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Usage(Integer input_tokens, Integer output_tokens) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ContentBlock(String type, String text, String id, String name, JsonNode input) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ResponsesResponse(String id, String output_text, List<ResponseOutputItem> output) {}

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
