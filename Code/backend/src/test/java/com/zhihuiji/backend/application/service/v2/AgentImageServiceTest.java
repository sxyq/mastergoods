package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.zhihuiji.backend.api.common.BusinessException;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.MediaAssetEntity;
import com.zhihuiji.backend.infrastructure.config.AgentImageProperties;
import com.zhihuiji.backend.infrastructure.repository.MediaAssetRepository;
import com.zhihuiji.backend.infrastructure.storage.MediaStorageService;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

class AgentImageServiceTest {

    @Test
    void generatePostsTextToImageJsonRequest() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://mock-provider.test/images/generations"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(containsString("\"prompt\":\"生成一张促销海报\"")))
            .andExpect(content().string(containsString("\"model\":\"gpt-image-1\"")))
            .andRespond(withSuccess("""
                {
                  "data": [
                    {
                      "url": "https://mock-provider.test/generated.png",
                      "revised_prompt": "更干净的商品海报"
                    }
                  ]
                }
                """.getBytes(StandardCharsets.UTF_8), MediaType.APPLICATION_JSON));

        AgentImageService service = service(
            "http://mock-provider.test",
            mock(MediaAssetRepository.class),
            mock(MediaStorageService.class),
            builder
        );

        V2AgentDtos.AgentImageGenerateResponse response = service.generate(
            new V2AgentDtos.AgentImageGenerateRequest("生成一张促销海报", List.of())
        );

        assertEquals("https://mock-provider.test/generated.png", response.imageUrl());
        assertEquals("更干净的商品海报", response.revisedPrompt());
        server.verify();
    }

    @Test
    void generatePostsImageEditMultipartRequestWhenReferenceImageExists() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://mock-provider.test/images/edits"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", containsString("multipart/form-data")))
            .andExpect(content().string(containsString("name=\"prompt\"")))
            .andExpect(content().string(containsString("filename=\"reference.png\"")))
            .andRespond(withSuccess("""
                {
                  "data": [
                    {
                      "b64_json": "ZmFrZS1wbmc="
                    }
                  ]
                }
                """.getBytes(StandardCharsets.UTF_8), MediaType.APPLICATION_JSON));

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
            "http://mock-provider.test",
            mediaAssetRepository,
            mediaStorageService,
            builder
        );

        V2AgentDtos.AgentImageGenerateResponse response = service.generate(
            new V2AgentDtos.AgentImageGenerateRequest("把这张商品图做成海报", List.of(9L))
        );

        assertEquals("data:image/png;base64,ZmFrZS1wbmc=", response.imageUrl());
        server.verify();
    }

    @Test
    void providerHttpFailureIsMappedToStableBusinessError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://mock-provider.test/images/generations"))
            .andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        AgentImageService service = service(
            "http://mock-provider.test", mock(MediaAssetRepository.class), mock(MediaStorageService.class), builder
        );

        BusinessException error = assertThrows(BusinessException.class, () -> service.generate(
            new V2AgentDtos.AgentImageGenerateRequest("生成图片", List.of())
        ));

        assertEquals("生图服务请求失败", error.getMessage());
        server.verify();
    }

    @Test
    void providerTimeoutIsMappedWithoutExposingProviderDetails() {
        RestClient.Builder builder = RestClient.builder()
            .requestInterceptor((request, body, execution) -> {
                throw new ResourceAccessException("provider detail", new SocketTimeoutException("timeout detail"));
            });
        AgentImageService service = service(
            "http://mock-provider.test", mock(MediaAssetRepository.class), mock(MediaStorageService.class), builder
        );

        BusinessException error = assertThrows(BusinessException.class, () -> service.generate(
            new V2AgentDtos.AgentImageGenerateRequest("生成图片", List.of())
        ));

        assertEquals("生图服务请求超时", error.getMessage());
    }

    @Test
    void cancelledRequestIsRejectedBeforeProviderCall() {
        AgentImageService service = service(
            "http://mock-provider.test", mock(MediaAssetRepository.class), mock(MediaStorageService.class),
            RestClient.builder()
        );
        Thread.currentThread().interrupt();
        try {
            BusinessException error = assertThrows(BusinessException.class, () -> service.generate(
                new V2AgentDtos.AgentImageGenerateRequest("生成图片", List.of())
            ));
            assertEquals("生图服务请求已取消", error.getMessage());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void invalidReferenceAssetIdIsRejectedBeforeProviderCall() {
        AgentImageService service = service(
            "http://mock-provider.test", mock(MediaAssetRepository.class), mock(MediaStorageService.class),
            RestClient.builder()
        );

        BusinessException error = assertThrows(BusinessException.class, () -> service.generate(
            new V2AgentDtos.AgentImageGenerateRequest("生成图片", List.of(0L))
        ));

        assertEquals("参考图片资源 ID 必须是正整数", error.getMessage());
    }

    private static AgentImageService service(
        String baseUrl,
        MediaAssetRepository mediaAssetRepository,
        MediaStorageService mediaStorageService,
        RestClient.Builder builder
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
            builder
        );
    }
}
