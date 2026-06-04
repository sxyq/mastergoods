package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.V2AgentConversationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/agent")
public class V2AgentController {
    private final V2AgentConversationService v2AgentConversationService;

    public V2AgentController(V2AgentConversationService v2AgentConversationService) {
        this.v2AgentConversationService = v2AgentConversationService;
    }

    @GetMapping("/conversations")
    public ApiResponse<List<V2AgentDtos.AgentConversationResponse>> listConversations() {
        return ApiResponse.success(v2AgentConversationService.listConversations());
    }

    @GetMapping("/conversations/{id}")
    public ApiResponse<V2AgentDtos.AgentConversationResponse> getConversation(@PathVariable Long id) {
        return ApiResponse.success(v2AgentConversationService.getConversation(id));
    }

    @PostMapping("/conversations")
    public ApiResponse<V2AgentDtos.AgentConversationResponse> createConversation(
        @Valid @RequestBody V2AgentDtos.AgentConversationCreateRequest request
    ) {
        return ApiResponse.success(v2AgentConversationService.createConversation(request));
    }

    @PutMapping("/conversations/{id}")
    public ApiResponse<V2AgentDtos.AgentConversationResponse> updateConversation(
        @PathVariable Long id,
        @Valid @RequestBody V2AgentDtos.AgentConversationUpdateRequest request
    ) {
        return ApiResponse.success(v2AgentConversationService.updateConversation(id, request));
    }

    @DeleteMapping("/conversations/{id}")
    public ApiResponse<Void> deleteConversation(@PathVariable Long id) {
        v2AgentConversationService.deleteConversation(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<List<V2AgentDtos.AgentMessageResponse>> listMessages(@PathVariable Long conversationId) {
        return ApiResponse.success(v2AgentConversationService.listMessages(conversationId));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ApiResponse<V2AgentDtos.AgentMessageResponse> createMessage(
        @PathVariable Long conversationId,
        @Valid @RequestBody V2AgentDtos.AgentMessageCreateRequest request
    ) {
        return ApiResponse.success(v2AgentConversationService.createMessage(conversationId, request));
    }

    @GetMapping("/drafts")
    public ApiResponse<List<V2AgentDtos.AgentDraftResponse>> listDrafts(
        @RequestParam(value = "conversation_id", required = false) Long conversationId
    ) {
        return ApiResponse.success(v2AgentConversationService.listDrafts(conversationId));
    }

    @PostMapping("/drafts")
    public ApiResponse<V2AgentDtos.AgentDraftResponse> createDraft(@Valid @RequestBody V2AgentDtos.AgentDraftCreateRequest request) {
        return ApiResponse.success(v2AgentConversationService.createDraft(request));
    }

    @PutMapping("/drafts/{id}")
    public ApiResponse<V2AgentDtos.AgentDraftResponse> updateDraft(
        @PathVariable Long id,
        @Valid @RequestBody V2AgentDtos.AgentDraftUpdateRequest request
    ) {
        return ApiResponse.success(v2AgentConversationService.updateDraft(id, request));
    }

    @DeleteMapping("/drafts/{id}")
    public ApiResponse<Void> deleteDraft(@PathVariable Long id) {
        v2AgentConversationService.deleteDraft(id);
        return ApiResponse.success(null);
    }
}
