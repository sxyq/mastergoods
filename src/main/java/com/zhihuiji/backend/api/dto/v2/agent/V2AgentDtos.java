package com.zhihuiji.backend.api.dto.v2.agent;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public final class V2AgentDtos {
    private V2AgentDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentConversationResponse(
        Long id,
        String title,
        String status,
        String latestSummary,
        Long createdAt,
        Long updatedAt,
        Long lastMessageAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentConversationCreateRequest(
        @NotBlank String title,
        String status
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentConversationUpdateRequest(
        String title,
        String status
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentMessageResponse(
        Long id,
        Long conversationId,
        String role,
        String messageType,
        String content,
        String structuredDataJson,
        Long createdAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentMessageCreateRequest(
        @NotBlank String role,
        @NotBlank String messageType,
        @NotBlank String content,
        String structuredDataJson
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentDraftResponse(
        Long id,
        Long conversationId,
        String draftType,
        String title,
        String contentJson,
        String status,
        Long createdAt,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentDraftCreateRequest(
        Long conversationId,
        @NotBlank String draftType,
        @NotBlank String title,
        @NotBlank String contentJson,
        String status
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentDraftUpdateRequest(
        Long conversationId,
        @NotBlank String draftType,
        @NotBlank String title,
        @NotBlank String contentJson,
        String status
    ) {}

}
