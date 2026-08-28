package com.zhihuiji.backend.application.service.v2.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.api.common.BusinessException;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.domain.entity.AgentDraftEntity;

/**
 * 编解码生图草稿中保存的正式结果。
 *
 * <p>结果和原始生图请求共存于 content_json，确认路由反序列化请求前会移除
 * image_result；这样既保留现有草稿结构，也能在重复确认和列表读取时恢复结果。
 */
public final class AgentDraftImageResultCodec {
    private static final String IMAGE_RESULT_FIELD = "image_result";

    private AgentDraftImageResultCodec() {}

    public static V2AgentDtos.AgentImageGenerateRequest readRequest(
        ObjectMapper objectMapper,
        String contentJson
    ) {
        ObjectNode content = readObject(objectMapper, contentJson, "生图草稿参数无法读取");
        content.remove(IMAGE_RESULT_FIELD);
        try {
            return objectMapper.treeToValue(content, V2AgentDtos.AgentImageGenerateRequest.class);
        } catch (Exception ex) {
            throw new BusinessException("生图草稿参数无法读取");
        }
    }

    public static String withImageResult(
        ObjectMapper objectMapper,
        String contentJson,
        V2AgentDtos.AgentImageGenerateResponse imageResult
    ) {
        if (imageResult == null || imageResult.imageUrl() == null || imageResult.imageUrl().isBlank()) {
            throw new BusinessException("生图服务未返回可保存的图片结果");
        }
        ObjectNode content = readObject(objectMapper, contentJson, "生图草稿结果无法保存");
        content.set(IMAGE_RESULT_FIELD, objectMapper.valueToTree(imageResult));
        try {
            return objectMapper.writeValueAsString(content);
        } catch (Exception ex) {
            throw new BusinessException("生图草稿结果无法保存");
        }
    }

    public static V2AgentDtos.AgentImageGenerateResponse readPersistedResult(
        ObjectMapper objectMapper,
        AgentDraftEntity entity
    ) {
        if (entity == null || !"image_generate".equalsIgnoreCase(entity.getDraftType())
            || !"confirmed".equalsIgnoreCase(entity.getStatus())) {
            return null;
        }
        ObjectNode content = readObject(objectMapper, entity.getContentJson(), "已确认的生图草稿结果无法恢复");
        JsonNode resultNode = content.get(IMAGE_RESULT_FIELD);
        if (resultNode == null || !resultNode.isObject()) {
            throw new BusinessException("已确认的生图草稿缺少可恢复的图片结果");
        }
        try {
            V2AgentDtos.AgentImageGenerateResponse result = objectMapper.treeToValue(
                resultNode,
                V2AgentDtos.AgentImageGenerateResponse.class
            );
            if (result == null || result.imageUrl() == null || result.imageUrl().isBlank()) {
                throw new BusinessException("已确认的生图草稿缺少可恢复的图片结果");
            }
            return result;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("已确认的生图草稿结果无法恢复");
        }
    }

    private static ObjectNode readObject(ObjectMapper objectMapper, String contentJson, String message) {
        try {
            JsonNode content = objectMapper.readTree(contentJson);
            if (content == null || !content.isObject()) {
                throw new BusinessException(message);
            }
            return (ObjectNode) content;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(message);
        }
    }
}
