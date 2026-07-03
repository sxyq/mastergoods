package com.zhihuiji.backend.application.service.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.common.BusinessException;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.MediaAssetEntity;
import com.zhihuiji.backend.infrastructure.config.AgentImageProperties;
import com.zhihuiji.backend.infrastructure.repository.MediaAssetRepository;
import com.zhihuiji.backend.infrastructure.storage.MediaStorageService;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class AgentImageService {

    private final CurrentOwnerService currentOwnerService;
    private final MediaAssetRepository mediaAssetRepository;
    private final MediaStorageService mediaStorageService;
    private final AgentImageProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String normalizedBaseUrl;

    public AgentImageService(
        CurrentOwnerService currentOwnerService,
        MediaAssetRepository mediaAssetRepository,
        MediaStorageService mediaStorageService,
        AgentImageProperties properties,
        RestClient.Builder restClientBuilder
    ) {
        this.currentOwnerService = currentOwnerService;
        this.mediaAssetRepository = mediaAssetRepository;
        this.mediaStorageService = mediaStorageService;
        this.properties = properties;
        this.normalizedBaseUrl = normalizeBaseUrl(properties.getBaseUrl());
        RestClient.Builder builder = restClientBuilder;
        if (StringUtils.hasText(normalizedBaseUrl)) {
            builder = builder.baseUrl(normalizedBaseUrl);
        }
        if (StringUtils.hasText(properties.getApiKey())) {
            builder = builder.defaultHeader("Authorization", "Bearer " + properties.getApiKey());
        }
        this.restClient = builder.build();
    }

    public V2AgentDtos.AgentImageGenerateResponse generate(V2AgentDtos.AgentImageGenerateRequest request) {
        String prompt = normalizeRequired(request.prompt(), "prompt 不能为空");
        if (!isConfigured()) {
            throw new BusinessException("生图服务未配置，请先补充 URL、Key 与模型");
        }
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<OwnedImageAsset> referenceImages = loadReferenceImages(ownerUserId, request.referenceAssetIds());
        String responseBody = referenceImages.isEmpty()
            ? postTextToImage(prompt)
            : postImageToImage(prompt, referenceImages.get(0));
        return parseGenerateResponse(responseBody);
    }

    private boolean isConfigured() {
        return StringUtils.hasText(properties.getBaseUrl())
            && StringUtils.hasText(properties.getApiKey())
            && StringUtils.hasText(properties.getModel());
    }

    private String postTextToImage(String prompt) {
        return restClient.post()
            .uri(endpointUri("images/generations"))
            .contentType(MediaType.APPLICATION_JSON)
            .body(new TextToImageRequest(properties.getModel(), prompt, "1024x1024"))
            .retrieve()
            .body(String.class);
    }

    private String postImageToImage(String prompt, OwnedImageAsset referenceImage) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("model", properties.getModel());
        body.add("prompt", prompt);
        body.add("size", "1024x1024");
        body.add("image", asMultipartImage(referenceImage));
        return restClient.post()
            .uri(endpointUri("images/edits"))
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .body(String.class);
    }

    private HttpEntity<ByteArrayResource> asMultipartImage(OwnedImageAsset referenceImage) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(referenceImage.mimeType()));
        ByteArrayResource resource = new ByteArrayResource(referenceImage.bytes()) {
            @Override
            public String getFilename() {
                return referenceImage.fileName();
            }
        };
        return new HttpEntity<>(resource, headers);
    }

    private V2AgentDtos.AgentImageGenerateResponse parseGenerateResponse(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            throw new BusinessException("生图服务返回为空");
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode first = root.path("data").isArray() && !root.path("data").isEmpty()
                ? root.path("data").get(0)
                : null;
            if (first == null || first.isMissingNode()) {
                throw new BusinessException("生图服务未返回图片结果");
            }
            String imageUrl = first.path("url").asText(null);
            if (!StringUtils.hasText(imageUrl)) {
                String base64Image = first.path("b64_json").asText(null);
                if (StringUtils.hasText(base64Image)) {
                    imageUrl = "data:image/png;base64," + base64Image;
                }
            }
            if (!StringUtils.hasText(imageUrl)) {
                throw new BusinessException("生图服务未返回可展示的图片地址");
            }
            String revisedPrompt = first.path("revised_prompt").asText(null);
            return new V2AgentDtos.AgentImageGenerateResponse(imageUrl, revisedPrompt);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("解析生图服务响应失败");
        }
    }

    private List<OwnedImageAsset> loadReferenceImages(Long ownerUserId, List<Long> referenceAssetIds) {
        if (referenceAssetIds == null || referenceAssetIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> normalizedIds = new LinkedHashSet<>();
        for (Long referenceAssetId : referenceAssetIds) {
            if (referenceAssetId != null && referenceAssetId > 0L) {
                normalizedIds.add(referenceAssetId);
            }
        }
        if (normalizedIds.isEmpty()) {
            return List.of();
        }
        return normalizedIds.stream().map(assetId -> loadOwnedImage(ownerUserId, assetId)).toList();
    }

    private OwnedImageAsset loadOwnedImage(Long ownerUserId, Long assetId) {
        MediaAssetEntity asset = mediaAssetRepository.findByIdAndOwnerUserId(assetId, ownerUserId)
            .orElseThrow(() -> new BusinessException("参考图片不存在: " + assetId));
        if (!StringUtils.hasText(asset.getMimeType()) || !asset.getMimeType().startsWith("image/")) {
            throw new BusinessException("仅支持图片作为参考图: " + assetId);
        }
        try {
            byte[] bytes = mediaStorageService.load(asset.getObjectKey());
            return new OwnedImageAsset(
                asset.getOriginalFileName() != null ? asset.getOriginalFileName() : ("reference-" + assetId + ".png"),
                asset.getMimeType(),
                bytes
            );
        } catch (Exception ex) {
            throw new BusinessException("读取参考图片失败: " + assetId);
        }
    }

    private String normalizeRequired(String value, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(errorMessage);
        }
        return value.trim();
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

    private String endpointUri(String uri) {
        if (!StringUtils.hasText(uri)) {
            return normalizedBaseUrl;
        }
        String path = uri.startsWith("/") ? uri.substring(1) : uri;
        return normalizedBaseUrl + "/" + path;
    }

    private record TextToImageRequest(String model, String prompt, String size) {}

    private record OwnedImageAsset(String fileName, String mimeType, byte[] bytes) {}
}
