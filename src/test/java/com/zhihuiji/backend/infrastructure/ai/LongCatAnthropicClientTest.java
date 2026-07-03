package com.zhihuiji.backend.infrastructure.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.zhihuiji.backend.infrastructure.config.AgentLlmProperties;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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
        assertTrue(client.supportsStreaming());
        assertEquals("configured", client.streamingUnavailableStatus());
    }

    @Test
    void createMessageWithToolsParsesChatCompletionsToolCalls() throws Exception {
        AtomicReference<String> capturedBody = new AtomicReference<>("");
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            capturedBody.set(readBody(exchange));
            byte[] body = """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": "已选择创建客户工具",
                        "tool_calls": [
                          {
                            "id": "call_1",
                            "type": "function",
                            "function": {
                              "name": "create_customer",
                              "arguments": "{\\"name\\":\\"李四\\",\\"phone\\":\\"13812345678\\"}"
                            }
                          }
                        ]
                      }
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 11,
                    "completion_tokens": 7,
                    "total_tokens": 18
                  }
                }
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();
        try {
            AgentLlmProperties chatCompletions = properties(
                true,
                "sk-test",
                "deepseek-v4-flash",
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "chat_completions"
            );
            LongCatAnthropicClient client = client(chatCompletions);

            Optional<LongCatAnthropicClient.ToolUseResponse> response = client.createMessageWithTools(
                "你是工具规划器",
                "帮我新建客户李四",
                List.of(new LongCatAnthropicClient.ToolDefinition(
                    "create_customer",
                    "创建客户草稿",
                    java.util.Map.of("type", "object", "properties", java.util.Map.of("name", java.util.Map.of("type", "string")))
                ))
            );

            assertTrue(response.isPresent());
            assertTrue(response.get().hasToolUses());
            assertEquals("已选择创建客户工具", response.get().text());
            assertEquals("create_customer", response.get().toolUses().get(0).name());
            assertNotNull(response.get().toolUses().get(0).input());
            assertEquals("李四", response.get().toolUses().get(0).input().path("name").asText());
            assertEquals("13812345678", response.get().toolUses().get(0).input().path("phone").asText());
            assertTrue(capturedBody.get().contains("\"tools\""), capturedBody.get());
            assertTrue(capturedBody.get().contains("\"tool_choice\":\"auto\""), capturedBody.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void createMessageWithToolsParsesResponsesFunctionCalls() throws Exception {
        AtomicReference<String> capturedBody = new AtomicReference<>("");
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/responses", exchange -> {
            capturedBody.set(readBody(exchange));
            byte[] body = """
                {
                  "output_text": "已选择创建客户工具",
                  "output": [
                    {
                      "type": "function_call",
                      "call_id": "call_resp_1",
                      "name": "create_customer",
                      "arguments": "{\\"name\\":\\"王五\\",\\"phone\\":\\"13900001111\\"}"
                    }
                  ]
                }
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();
        try {
            AgentLlmProperties responses = properties(
                true,
                "sk-test",
                "gpt-5.1",
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "responses"
            );
            LongCatAnthropicClient client = client(responses);

            Optional<LongCatAnthropicClient.ToolUseResponse> response = client.createMessageWithTools(
                "你是工具规划器",
                "帮我新建客户王五",
                List.of(new LongCatAnthropicClient.ToolDefinition(
                    "create_customer",
                    "创建客户草稿",
                    java.util.Map.of("type", "object", "properties", java.util.Map.of("name", java.util.Map.of("type", "string")))
                ))
            );

            assertTrue(response.isPresent());
            assertTrue(response.get().hasToolUses());
            assertEquals("已选择创建客户工具", response.get().text());
            assertEquals("create_customer", response.get().toolUses().get(0).name());
            assertEquals("王五", response.get().toolUses().get(0).input().path("name").asText());
            assertEquals("13900001111", response.get().toolUses().get(0).input().path("phone").asText());
            assertTrue(capturedBody.get().contains("\"tools\""), capturedBody.get());
            assertTrue(capturedBody.get().contains("\"type\":\"function\""), capturedBody.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void createMessageWithToolsFallsBackFromResponsesToChatCompletionsWhenResponsesFails() throws Exception {
        AtomicReference<String> responsesBody = new AtomicReference<>("");
        AtomicReference<String> chatBody = new AtomicReference<>("");
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/responses", exchange -> {
            responsesBody.set(readBody(exchange));
            byte[] body = """
                {"error":{"message":"upstream 502"}}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(502, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.createContext("/chat/completions", exchange -> {
            chatBody.set(readBody(exchange));
            byte[] body = """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "tool_calls": [
                          {
                            "id": "call_1",
                            "type": "function",
                            "function": {
                              "name": "inventory_low_stock_lookup",
                              "arguments": "{\\"keyword\\":\\"最近\\"}"
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();
        try {
            AgentLlmProperties responses = properties(
                true,
                "sk-test",
                "gpt-5.4-mini",
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "responses"
            );
            LongCatAnthropicClient client = client(responses);

            Optional<LongCatAnthropicClient.ToolUseResponse> response = client.createMessageWithTools(
                "你是工具规划器",
                "看下最近库存情况",
                List.of(new LongCatAnthropicClient.ToolDefinition(
                    "inventory_low_stock_lookup",
                    "查询低库存商品",
                    java.util.Map.of("type", "object", "properties", java.util.Map.of("keyword", java.util.Map.of("type", "string")))
                ))
            );

            assertTrue(response.isPresent());
            assertTrue(response.get().hasToolUses());
            assertEquals("inventory_low_stock_lookup", response.get().toolUses().get(0).name());
            assertEquals("最近", response.get().toolUses().get(0).input().path("keyword").asText());
            assertTrue(responsesBody.get().contains("\"tools\""), responsesBody.get());
            assertTrue(chatBody.get().contains("\"tool_choice\":\"auto\""), chatBody.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void createJsonMessageWithImagesEncodesResponsesInputImageBlocks() throws Exception {
        AtomicReference<String> capturedBody = new AtomicReference<>("");
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/responses", exchange -> {
            capturedBody.set(readBody(exchange));
            byte[] body = """
                {
                  "output_text": "图片里是一张手写清单"
                }
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();
        try {
            AgentLlmProperties responses = properties(
                true,
                "sk-test",
                "gpt-5.4-mini",
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "responses"
            );
            LongCatAnthropicClient client = client(responses);

            Optional<String> response = client.createJsonMessage(
                "你是图片助手",
                "帮我看看这张图",
                List.of(new LongCatAnthropicClient.ImageInput("image/png", "data:image/png;base64,ZmFrZQ=="))
            );

            assertTrue(response.isPresent());
            assertEquals("图片里是一张手写清单", response.get());
            assertTrue(capturedBody.get().contains("\"type\":\"input_image\""), capturedBody.get());
            assertTrue(capturedBody.get().contains("\"image_url\":\"data:image/png;base64,ZmFrZQ==\""), capturedBody.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void streamTextMessageWithImagesEncodesChatCompletionImageParts() throws Exception {
        AtomicReference<String> capturedBody = new AtomicReference<>("");
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            capturedBody.set(readBody(exchange));
            byte[] body = """
                data: {"choices":[{"delta":{"content":"这是一张商品照片"}}]}

                data: [DONE]
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();
        try {
            AgentLlmProperties responses = properties(
                true,
                "sk-test",
                "gpt-5.4-mini",
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "chat_completions"
            );
            LongCatAnthropicClient client = client(responses);

            Optional<String> response = client.streamTextMessage(
                "你是图片助手",
                "帮我看看这张图",
                List.of(new LongCatAnthropicClient.ImageInput("image/png", "data:image/png;base64,ZmFrZQ==")),
                "run-image-1",
                delta -> {}
            );

            assertTrue(response.isPresent());
            assertEquals("这是一张商品照片", response.get());
            assertTrue(capturedBody.get().contains("\"type\":\"image_url\""), capturedBody.get());
            assertTrue(capturedBody.get().contains("\"url\":\"data:image/png;base64,ZmFrZQ==\""), capturedBody.get());
        } finally {
            server.stop(0);
        }
    }

    private static LongCatAnthropicClient client(AgentLlmProperties properties) {
        return new LongCatAnthropicClient(properties, RestClient.builder());
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
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
