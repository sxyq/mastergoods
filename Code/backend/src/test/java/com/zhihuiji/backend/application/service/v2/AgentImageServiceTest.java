package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.MediaAssetEntity;
import com.zhihuiji.backend.infrastructure.config.AgentImageProperties;
import com.zhihuiji.backend.infrastructure.repository.MediaAssetRepository;
import com.zhihuiji.backend.infrastructure.storage.MediaStorageService;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class AgentImageServiceTest {

    @Test
    void generatePostsTextToImageJsonRequest() throws Exception {
        AtomicReference<String> capturedBody = new AtomicReference<>("");
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/images/generations", exchange -> {
            capturedBody.set(readBody(exchange));
            byte[] body = """
                {
                  "data": [
                    {
                      "url": "https://example.com/generated.png",
                      "revised_prompt": "更干净的商品海报"
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
            AgentImageService service = service(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                mock(MediaAssetRepository.class),
                mock(MediaStorageService.class)
            );

            V2AgentDtos.AgentImageGenerateResponse response = service.generate(
                new V2AgentDtos.AgentImageGenerateRequest("生成一张促销海报", List.of())
            );

            assertEquals("https://example.com/generated.png", response.imageUrl());
            assertEquals("更干净的商品海报", response.revisedPrompt());
            assertTrue(capturedBody.get().contains("\"prompt\":\"生成一张促销海报\""), capturedBody.get());
            assertTrue(capturedBody.get().contains("\"model\":\"gpt-image-1\""), capturedBody.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void generatePostsImageEditMultipartRequestWhenReferenceImageExists() throws Exception {
        AtomicReference<String> capturedBody = new AtomicReference<>("");
        AtomicReference<String> capturedContentType = new AtomicReference<>("");
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/images/edits", exchange -> {
            capturedContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            capturedBody.set(readBody(exchange));
            byte[] body = """
                {
                  "data": [
                    {
                      "b64_json": "ZmFrZS1wbmc="
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
            MediaAssetRepository mediaAssetRepository = mock(MediaAssetRepository.class);
            MediaStorageService mediaStorageService = mock(MediaStorageService.class);
            MediaAssetEntity entity = new MediaAssetEntity();
            entity.setOwnerUserId(1L);
            entity.setOriginalFileName("reference.png");
            entity.setMimeType("image/png");
            entity.setObjectKey("media/reference.png");
            when(mediaAssetRepository.findByIdAndOwnerUserId(9L, 1L)).thenReturn(Optional.of(entity));
            when(mediaStorageService.load("media/reference.png")).thenReturn("fake-image".getBytes(StandardCharsets.UTF_8));
            AgentImageService service = service(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                mediaAssetRepository,
                mediaStorageService
            );

            V2AgentDtos.AgentImageGenerateResponse response = service.generate(
                new V2AgentDtos.AgentImageGenerateRequest("把这张商品图做成海报", List.of(9L))
            );

            assertEquals("data:image/png;base64,ZmFrZS1wbmc=", response.imageUrl());
            assertTrue(capturedContentType.get().contains("multipart/form-data"), capturedContentType.get());
            assertTrue(capturedBody.get().contains("name=\"prompt\""), capturedBody.get());
            assertTrue(capturedBody.get().contains("filename=\"reference.png\""), capturedBody.get());
        } finally {
            server.stop(0);
        }
    }

    private static AgentImageService service(
        String baseUrl,
        MediaAssetRepository mediaAssetRepository,
        MediaStorageService mediaStorageService
    ) {
        CurrentOwnerService currentOwnerService = mock(CurrentOwnerService.class);
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
        AgentImageProperties properties = new AgentImageProperties();
        properties.setBaseUrl(baseUrl);
        properties.setApiKey("sk-test");
        properties.setModel("gpt-image-1");
        return new AgentImageService(
            currentOwnerService,
            mediaAssetRepository,
            mediaStorageService,
            properties,
            RestClient.builder()
        );
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }
}
