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
import java.util.concurrent.atomic.AtomicInteger;
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
            // tokenrhythm may return JSON with an octet-stream content type.
            exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
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
    void createJsonMessageStopsRetryingWhenProviderReportsConcurrencyLimit() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            calls.incrementAndGet();
            byte[] body = "{\"code\":\"UPSTREAM_RATE_LIMITED\",\"message\":\"Concurrency exceeded.\"}"
                .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
            exchange.sendResponseHeaders(429, body.length);
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

            assertFalse(client.createJsonMessage("你是助手", "查询商品").isPresent());
            assertEquals(1, calls.get(), "429 限流不应在同一个请求内继续重试");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void retriesNamedToolChoiceWithAutoAndOnlyTargetToolWhenProviderRejectsIt() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<String> firstBody = new AtomicReference<>("");
        AtomicReference<String> secondBody = new AtomicReference<>("");
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            int attempt = calls.incrementAndGet();
            String requestBody = readBody(exchange);
            if (attempt == 1) {
                firstBody.set(requestBody);
                byte[] body = "{\"error\":{\"code\":\"MODEL_TOOL_CHOICE_NOT_SUPPORTED\"}}"
                    .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(400, body.length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(body);
                }
                return;
            }
            secondBody.set(requestBody);
            byte[] body = """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "tool_calls": [
                          {
                            "id": "call_target",
                            "type": "function",
                            "function": {
                              "name": "create_sales_return",
                              "arguments": "{\\"sale_order_id\\":42}"
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
            AgentLlmProperties chatCompletions = properties(
                true,
                "sk-test",
                "deepseek-v4-flash",
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "chat_completions"
            );
            LongCatAnthropicClient client = client(chatCompletions);

            Optional<LongCatAnthropicClient.ToolUseResponse> response = client.createMessageWithTools(
                "必须生成销售退货草稿",
                "把这张销售单退一件",
                List.of(
                    new LongCatAnthropicClient.ToolDefinition(
                        "create_sales_return",
                        "创建销售退货草稿",
                        java.util.Map.of("type", "object", "properties", java.util.Map.of())
                    ),
                    new LongCatAnthropicClient.ToolDefinition(
                        "sale_order_lookup",
                        "查询销售单",
                        java.util.Map.of("type", "object", "properties", java.util.Map.of())
                    )
                ),
                "create_sales_return"
            );

            assertTrue(response.isPresent());
            assertTrue(response.get().hasToolUses());
            assertEquals("create_sales_return", response.get().toolUses().get(0).name());
            assertEquals(2, calls.get());
            assertTrue(firstBody.get().contains("create_sales_return"), firstBody.get());
            assertTrue(firstBody.get().contains("\"tool_choice\":{"), firstBody.get());
            assertTrue(firstBody.get().contains("\"function\":{\"name\":\"create_sales_return\""), firstBody.get());
            assertTrue(secondBody.get().contains("\"tool_choice\":\"auto\""), secondBody.get());
            assertTrue(secondBody.get().contains("create_sales_return"), secondBody.get());
            assertFalse(secondBody.get().contains("sale_order_lookup"), secondBody.get());
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
                  "id": "resp_abc123",
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
            assertEquals("resp_abc123", response.get().responseId());
            assertTrue(capturedBody.get().contains("\"tools\""), capturedBody.get());
            assertTrue(capturedBody.get().contains("\"type\":\"function\""), capturedBody.get());
        } finally {
            server.stop(0);
        }
    }

    /**
     * 契约测试：provider 支持 function_call_output 续轮时，方法返回模型生成的最终回答。
     */
    @Test
    void continueWithToolOutputsSucceedsWhenProviderSupportsContinuation() throws Exception {
        AtomicReference<String> capturedBody = new AtomicReference<>("");
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/responses", exchange -> {
            capturedBody.set(readBody(exchange));
            byte[] body = """
                {
                  "id": "resp_final_456",
                  "output_text": "当前账号共有 3 个商品，库存充足。",
                  "output": [
                    {
                      "type": "message",
                      "content": [{"type": "output_text", "text": "当前账号共有 3 个商品，库存充足。"}]
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
                "gpt-5.6-luna",
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "responses"
            );
            LongCatAnthropicClient client = client(responses);

            Optional<LongCatAnthropicClient.ToolUseResponse> response = client.continueWithToolOutputs(
                "resp_abc123",
                "你是智慧记助手，根据工具查询结果回答用户问题。",
                "当前账号有多少商品？",
                List.of(new LongCatAnthropicClient.FunctionCallItem(
                    "call_resp_1",
                    "product_catalog_lookup",
                    "{\"keyword\":\"\"}"
                )),
                List.of(new LongCatAnthropicClient.FunctionCallOutputItem(
                    "call_resp_1",
                    "{\"product_count\":3,\"stock_total\":35}"
                )),
                List.of(new LongCatAnthropicClient.ToolDefinition(
                    "product_catalog_lookup",
                    "查询商品目录",
                    java.util.Map.of("type", "object", "properties", java.util.Map.of())
                ))
            );

            assertTrue(response.isPresent());
            assertEquals("当前账号共有 3 个商品，库存充足。", response.get().text());
            assertEquals("resp_final_456", response.get().responseId());
            assertFalse(response.get().hasToolUses());
            // previous_response_id 已经携带上一轮 function_call，续轮只发送 output。
            assertTrue(capturedBody.get().contains("\"previous_response_id\":\"resp_abc123\""), capturedBody.get());
            assertTrue(capturedBody.get().contains("\"type\":\"function_call_output\""), capturedBody.get());
            assertFalse(capturedBody.get().contains("\"type\":\"function_call\""), capturedBody.get());
            assertTrue(capturedBody.get().contains("\"call_id\":\"call_resp_1\""), capturedBody.get());
            assertTrue(capturedBody.get().contains("\"output\":\"{\\\"product_count\\\":3,\\\"stock_total\\\":35}\""), capturedBody.get());
        } finally {
            server.stop(0);
        }
    }

    /**
     * 契约测试：provider 返回 HTTP 400 且没有可用的 Chat Completions 兜底时，
     * 方法返回 empty 并记 warn 日志，不得伪造续轮成功。
     */
    @Test
    void continueWithToolOutputsReturnsEmptyWhenProviderRejectsWithHttp400() throws Exception {
        AtomicReference<String> capturedBody = new AtomicReference<>("");
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/responses", exchange -> {
            capturedBody.set(readBody(exchange));
            byte[] body = """
                {"error":{"message":"function_call_output continuation not supported","type":"invalid_request_error","code":"unsupported_function_call_output"}}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();
        try {
            AgentLlmProperties responses = properties(
                true,
                "sk-test",
                "gpt-5.6-luna",
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "responses"
            );
            LongCatAnthropicClient client = client(responses);

            Optional<LongCatAnthropicClient.ToolUseResponse> response = client.continueWithToolOutputs(
                "resp_abc123",
                "你是智慧记助手。",
                "当前账号有多少商品？",
                List.of(new LongCatAnthropicClient.FunctionCallItem(
                    "call_resp_1",
                    "product_catalog_lookup",
                    "{}"
                )),
                List.of(new LongCatAnthropicClient.FunctionCallOutputItem(
                    "call_resp_1",
                    "{\"product_count\":3}"
                )),
                List.of(new LongCatAnthropicClient.ToolDefinition(
                    "product_catalog_lookup",
                    "查询商品目录",
                    java.util.Map.of("type", "object", "properties", java.util.Map.of())
                ))
            );

            // provider 不支持续轮时返回 empty，由调用方降级
            assertFalse(response.isPresent());
            // 验证请求确实发出了 function_call_output 格式
            assertTrue(capturedBody.get().contains("\"previous_response_id\""), capturedBody.get());
            assertTrue(capturedBody.get().contains("\"type\":\"function_call_output\""), capturedBody.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void continueWithToolOutputsUsesNativeChatCompletionsFallbackAfterResponses400() throws Exception {
        AtomicReference<String> chatBody = new AtomicReference<>("");
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/responses", exchange -> {
            byte[] body = "{\"error\":{\"message\":\"unsupported continuation\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, body.length);
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
                        "content": "当前账号共有 3 个商品。"
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
                "gpt-5.6-luna",
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "responses"
            );
            LongCatAnthropicClient client = client(responses);

            Optional<LongCatAnthropicClient.ToolUseResponse> response = client.continueWithToolOutputs(
                "resp_abc123",
                "你是智慧记助手。",
                "当前账号有多少商品？",
                List.of(new LongCatAnthropicClient.FunctionCallItem(
                    "call_resp_1", "product_catalog_lookup", "{\"limit\":10}"
                )),
                List.of(new LongCatAnthropicClient.FunctionCallOutputItem(
                    "call_resp_1", "{\"product_count\":3}"
                )),
                List.of(new LongCatAnthropicClient.ToolDefinition(
                    "product_catalog_lookup",
                    "查询商品目录",
                    java.util.Map.of("type", "object", "properties", java.util.Map.of())
                ))
            );

            assertTrue(response.isPresent());
            assertEquals("当前账号共有 3 个商品。", response.get().text());
            assertTrue(chatBody.get().contains("\"role\":\"assistant\""), chatBody.get());
            assertTrue(chatBody.get().contains("\"tool_calls\""), chatBody.get());
            assertTrue(chatBody.get().contains("\"role\":\"tool\""), chatBody.get());
            assertTrue(chatBody.get().contains("\"tool_call_id\":\"call_resp_1\""), chatBody.get());
        } finally {
            server.stop(0);
        }
    }

    /**
     * 契约测试：续轮响应中模型可能继续返回 function_call（多轮工具调用）。
     */
    @Test
    void continueWithToolOutputsParsesAdditionalFunctionCalls() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/responses", exchange -> {
            byte[] body = """
                {
                  "id": "resp_continue_789",
                  "output_text": "",
                  "output": [
                    {
                      "type": "function_call",
                      "call_id": "call_2",
                      "name": "inventory_low_stock_lookup",
                      "arguments": "{\\"keyword\\":\\"最近\\"}"
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
                "gpt-5.6-luna",
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "responses"
            );
            LongCatAnthropicClient client = client(responses);

            Optional<LongCatAnthropicClient.ToolUseResponse> response = client.continueWithToolOutputs(
                "resp_first_round",
                "你是智慧记助手。",
                "查看库存和商品",
                List.of(new LongCatAnthropicClient.FunctionCallItem(
                    "call_1",
                    "product_catalog_lookup",
                    "{}"
                )),
                List.of(new LongCatAnthropicClient.FunctionCallOutputItem(
                    "call_1",
                    "{\"product_count\":3}"
                )),
                List.of(new LongCatAnthropicClient.ToolDefinition(
                    "product_catalog_lookup",
                    "查询商品目录",
                    java.util.Map.of("type", "object", "properties", java.util.Map.of())
                ))
            );

            assertTrue(response.isPresent());
            assertTrue(response.get().hasToolUses());
            assertEquals("inventory_low_stock_lookup", response.get().toolUses().get(0).name());
            assertEquals("call_2", response.get().toolUses().get(0).id());
            assertEquals("resp_continue_789", response.get().responseId());
        } finally {
            server.stop(0);
        }
    }

    /**
     * 契约测试：Chat Completions 使用 assistant tool_calls 与 tool messages
     * 续轮，不依赖 Responses API 的 previous_response_id。
     */
    @Test
    void continueWithToolOutputsUsesChatCompletionsToolMessages() throws Exception {
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
                        "content": "当前账号共有 3 个商品。"
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
            AgentLlmProperties chatCompletions = properties(
                true,
                "sk-test",
                "deepseek-v4-flash",
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "chat_completions"
            );
            LongCatAnthropicClient client = client(chatCompletions);

            Optional<LongCatAnthropicClient.ToolUseResponse> response = client.continueWithToolOutputs(
                null,
                "你是智慧记助手。",
                "当前账号有多少商品？",
                List.of(new LongCatAnthropicClient.FunctionCallItem(
                    "call_1",
                    "product_catalog_lookup",
                    "{}"
                )),
                List.of(new LongCatAnthropicClient.FunctionCallOutputItem(
                    "call_1",
                    "{\"product_count\":3}"
                )),
                List.of(new LongCatAnthropicClient.ToolDefinition(
                    "product_catalog_lookup",
                    "查询商品目录",
                    java.util.Map.of("type", "object", "properties", java.util.Map.of())
                ))
            );

            assertTrue(response.isPresent());
            assertEquals("当前账号共有 3 个商品。", response.get().text());
            assertFalse(response.get().hasToolUses());
            assertTrue(capturedBody.get().contains("\"role\":\"assistant\""), capturedBody.get());
            assertTrue(capturedBody.get().contains("\"tool_calls\""), capturedBody.get());
            assertTrue(capturedBody.get().contains("\"role\":\"tool\""), capturedBody.get());
            assertTrue(capturedBody.get().contains("\"tool_call_id\":\"call_1\""), capturedBody.get());
            assertTrue(capturedBody.get().contains("\"tool_choice\":\"auto\""), capturedBody.get());
            assertFalse(capturedBody.get().contains("result_visualization"), capturedBody.get());
            assertFalse(capturedBody.get().contains("previous_response_id"), capturedBody.get());
        } finally {
            server.stop(0);
        }
    }

    /**
     * 契约测试：空 functionCalls 或 toolOutputs 时返回 empty（不发送请求）。
     */
    @Test
    void continueWithToolOutputsReturnsEmptyForEmptyInputs() {
        AgentLlmProperties responses = properties(
            true,
            "sk-test",
            "gpt-5.6-luna",
            "http://127.0.0.1:0",
            "responses"
        );
        LongCatAnthropicClient client = client(responses);

        // 空 functionCalls
        assertFalse(client.continueWithToolOutputs(
            "resp_1", "sys", "user",
            List.of(),
            List.of(new LongCatAnthropicClient.FunctionCallOutputItem("call_1", "{}")),
            List.of()
        ).isPresent());

        // 空 toolOutputs
        assertFalse(client.continueWithToolOutputs(
            "resp_1", "sys", "user",
            List.of(new LongCatAnthropicClient.FunctionCallItem("call_1", "tool", "{}")),
            List.of(),
            List.of()
        ).isPresent());

        // null 输入
        assertFalse(client.continueWithToolOutputs(
            "resp_1", "sys", "user",
            null,
            null,
            List.of()
        ).isPresent());
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

    @Test
    void streamTextMessageUsesUtf8AndClosesNon2xxResponsePath() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            int call = calls.incrementAndGet();
            byte[] body = call == 1
                ? "data: {\"choices\":[{\"delta\":{\"content\":\"你好，世界\"}}]}\n\ndata: [DONE]\n".getBytes(StandardCharsets.UTF_8)
                : "上游拒绝".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(call == 1 ? 200 : 503, body.length);
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

            assertEquals("你好，世界", client.streamTextMessage(
                "你是助手",
                "打招呼",
                "run-utf8",
                delta -> {}
            ).orElseThrow());
            assertFalse(client.streamTextMessage(
                "你是助手",
                "再次请求",
                "run-error",
                delta -> {}
            ).isPresent());
            assertEquals(2, calls.get());
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
