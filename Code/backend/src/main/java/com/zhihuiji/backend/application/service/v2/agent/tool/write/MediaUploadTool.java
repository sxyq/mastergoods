package com.zhihuiji.backend.application.service.v2.agent.tool.write;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentDraftRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 媒体上传意图草稿工具（CREATE_ONLY）。
 *
 * <p>不实际上传文件，而是生成上传意图草稿保存到 agent_drafts 表（status=active），
 * 返回上传参数（文件名、大小、MIME、绑定类型）供前端执行实际上传与绑定。
 */
@Component
public class MediaUploadTool extends ToolSupport {

    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private final AgentDraftRepository agentDraftRepository;
    private final ObjectMapper objectMapper;

    public MediaUploadTool(AgentDraftRepository agentDraftRepository, ObjectMapper objectMapper) {
        this.agentDraftRepository = agentDraftRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "media_upload_tool";
    }

    @Override
    public String displayName() {
        return "媒体上传草稿";
    }

    @Override
    public String description() {
        return "生成媒体上传意图草稿，返回上传参数供前端执行上传";
    }

    @Override
    public ToolType type() {
        return ToolType.CREATE_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ObjectNode fileName = properties.putObject("file_name");
        fileName.put("type", "string");
        fileName.put("description", "文件名（必填）");
        ObjectNode fileSize = properties.putObject("file_size");
        fileSize.put("type", "integer");
        fileSize.put("description", "文件大小（字节，必填）");
        ObjectNode mimeType = properties.putObject("mime_type");
        mimeType.put("type", "string");
        mimeType.put("description", "MIME 类型，默认 application/octet-stream");
        ObjectNode bindingType = properties.putObject("binding_type");
        bindingType.put("type", "string");
        bindingType.put("description", "绑定类型：product/customer/supplier（可选）");
        schema.putArray("required")
            .add("file_name")
            .add("file_size");
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        String fileName = paramString(params, "file_name");
        if (fileName == null) {
            String err = "文件名不能为空";
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }
        Long fileSize = paramLong(params, "file_size", null);
        if (fileSize == null) {
            String err = "文件大小不能为空";
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }
        String mimeType = paramString(params, "mime_type");
        String mime = mimeType == null ? DEFAULT_MIME_TYPE : mimeType;
        String bindingType = paramString(params, "binding_type");

        Map<String, Object> input = mapOf(
            "file_name", fileName,
            "file_size", fileSize,
            "mime_type", mime,
            "binding_type", bindingType
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        String uploadUrlHint = "请前端调用媒体上传接口上传文件后，将返回的资源 ID 与 binding_type 对应实体绑定。";
        Map<String, Object> content = mapOf(
            "file_name", fileName,
            "file_size", fileSize,
            "mime_type", mime,
            "binding_type", bindingType,
            "upload_url_hint", uploadUrlHint
        );
        String contentJson;
        try {
            contentJson = ctx.objectMapper().writeValueAsString(content);
        } catch (JsonProcessingException ex) {
            String err = "草稿序列化失败";
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }

        String title = "媒体上传：" + fileName;
        long now = System.currentTimeMillis();
        AgentDraftEntity draft = new AgentDraftEntity();
        draft.setOwnerUserId(ownerUserId);
        draft.setConversationId(ctx.conversationId());
        draft.setDraftType("media_upload");
        draft.setTitle(title);
        draft.setContentJson(contentJson);
        draft.setStatus("active");
        draft.setCreatedAt(now);
        draft.setUpdatedAt(now);
        AgentDraftEntity saved = agentDraftRepository.save(draft);
        Long draftId = saved.getId();

        emitToolCompleted(ctx, name(), "生成上传草稿 #" + draftId, audit);

        V2AgentDtos.ResultBlockDto block = new V2AgentDtos.ResultBlockDto(
            "draft_card",
            "上传草稿待确认",
            toJsonNode(ctx, mapOf(
                "draft_id", draftId,
                "draft_type", "media_upload",
                "title", title,
                "fields", List.of(
                    mapOf("label", "文件名", "value", fileName),
                    mapOf("label", "文件大小", "value", formatReadableSize(fileSize)),
                    mapOf("label", "MIME 类型", "value", mime),
                    mapOf("label", "绑定类型", "value", bindingType == null ? "-" : bindingType),
                    mapOf("label", "上传提示", "value", uploadUrlHint)
                )
            ))
        );

        String toolSummary = "生成媒体上传草稿 " + title + "（#" + draftId + "）";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "draft_id", draftId,
            "draft_type", "media_upload",
            "title", title,
            "status", "active",
            "file_name", fileName,
            "file_size", fileSize,
            "mime_type", mime,
            "binding_type", bindingType,
            "upload_url_hint", uploadUrlHint,
            "content_json", contentJson,
            "query_audit", audit.facts()
        ));
        return ToolResult.success(List.of(block), toolFacts, toolSummary);
    }

    private String formatReadableSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024) {
            return formatNumber(bytes / 1024.0) + " KB";
        }
        if (bytes < 1024L * 1024 * 1024) {
            return formatNumber(bytes / (1024.0 * 1024)) + " MB";
        }
        return formatNumber(bytes / (1024.0 * 1024 * 1024)) + " GB";
    }
}
