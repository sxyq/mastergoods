package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.V2AgentAiService;
import com.zhihuiji.backend.application.service.v2.V2AgentConversationService;
import com.zhihuiji.backend.application.service.v2.agent.AgentDraftConfirmService;
import com.zhihuiji.backend.infrastructure.security.RequireStorePermission;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/v2/agent")
public class V2AgentController {
    private final V2AgentConversationService v2AgentConversationService;
    private final V2AgentAiService v2AgentAiService;
    private final AgentDraftConfirmService agentDraftConfirmService;

    public V2AgentController(
        V2AgentConversationService v2AgentConversationService,
        V2AgentAiService v2AgentAiService,
        AgentDraftConfirmService agentDraftConfirmService
    ) {
        this.v2AgentConversationService = v2AgentConversationService;
        this.v2AgentAiService = v2AgentAiService;
        this.agentDraftConfirmService = agentDraftConfirmService;
    }

    @GetMapping("/conversations")
    @RequireStorePermission("agent:view")
    public ApiResponse<List<V2AgentDtos.AgentConversationResponse>> listConversations(
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return ApiResponse.success(v2AgentConversationService.listConversations(page, limit));
    }

    @GetMapping("/conversations/{id}")
    @RequireStorePermission("agent:view")
    public ApiResponse<V2AgentDtos.AgentConversationResponse> getConversation(@PathVariable Long id) {
        return ApiResponse.success(v2AgentConversationService.getConversation(id));
    }

    @PostMapping("/conversations")
    @RequireStorePermission("agent:write")
    public ApiResponse<V2AgentDtos.AgentConversationResponse> createConversation(
        @Valid @RequestBody V2AgentDtos.AgentConversationCreateRequest request
    ) {
        return ApiResponse.success(v2AgentConversationService.createConversation(request));
    }

    @PutMapping("/conversations/{id}")
    @RequireStorePermission("agent:write")
    public ApiResponse<V2AgentDtos.AgentConversationResponse> updateConversation(
        @PathVariable Long id,
        @Valid @RequestBody V2AgentDtos.AgentConversationUpdateRequest request
    ) {
        return ApiResponse.success(v2AgentConversationService.updateConversation(id, request));
    }

    @DeleteMapping("/conversations/{id}")
    @RequireStorePermission("agent:write")
    public ApiResponse<Void> deleteConversation(@PathVariable Long id) {
        v2AgentConversationService.deleteConversation(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @RequireStorePermission("agent:view")
    public ApiResponse<List<V2AgentDtos.AgentMessageResponse>> listMessages(
        @PathVariable Long conversationId,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return ApiResponse.success(v2AgentConversationService.listMessages(conversationId, page, limit));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    @RequireStorePermission("agent:write")
    public ApiResponse<V2AgentDtos.AgentMessageResponse> createMessage(
        @PathVariable Long conversationId,
        @Valid @RequestBody V2AgentDtos.AgentMessageCreateRequest request
    ) {
        return ApiResponse.success(v2AgentConversationService.createMessage(conversationId, request));
    }

    @GetMapping("/drafts")
    @RequireStorePermission("agent:view")
    public ApiResponse<List<V2AgentDtos.AgentDraftResponse>> listDrafts(
        @RequestParam(value = "conversation_id", required = false) Long conversationId,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return ApiResponse.success(v2AgentConversationService.listDrafts(conversationId, page, limit));
    }

    @PostMapping("/drafts")
    @RequireStorePermission("agent:write")
    public ApiResponse<V2AgentDtos.AgentDraftResponse> createDraft(@Valid @RequestBody V2AgentDtos.AgentDraftCreateRequest request) {
        return ApiResponse.success(v2AgentConversationService.createDraft(request));
    }

    @GetMapping("/drafts/pending")
    @RequireStorePermission("agent:view")
    public ApiResponse<List<V2AgentDtos.AgentDraftResponse>> listPendingDrafts() {
        return ApiResponse.success(agentDraftConfirmService.listPendingDrafts());
    }

    @PostMapping("/drafts/{id}/confirm")
    @RequireStorePermission("agent:write")
    public ApiResponse<V2AgentDtos.AgentDraftResponse> confirmDraft(@PathVariable Long id) {
        return ApiResponse.success(agentDraftConfirmService.confirmDraft(id));
    }

    @PostMapping("/drafts/{id}/cancel")
    @RequireStorePermission("agent:write")
    public ApiResponse<V2AgentDtos.AgentDraftResponse> cancelDraft(@PathVariable Long id) {
        return ApiResponse.success(agentDraftConfirmService.cancelDraft(id));
    }

    @PutMapping("/drafts/{id}")
    @RequireStorePermission("agent:write")
    public ApiResponse<V2AgentDtos.AgentDraftResponse> updateDraft(
        @PathVariable Long id,
        @Valid @RequestBody V2AgentDtos.AgentDraftUpdateRequest request
    ) {
        return ApiResponse.success(v2AgentConversationService.updateDraft(id, request));
    }

    @DeleteMapping("/drafts/{id}")
    @RequireStorePermission("agent:write")
    public ApiResponse<Void> deleteDraft(@PathVariable Long id) {
        v2AgentConversationService.deleteDraft(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/workbench")
    @RequireStorePermission("agent:view")
    public ApiResponse<V2AgentDtos.AgentWorkbenchResponse> getWorkbench() {
        return ApiResponse.success(v2AgentAiService.getWorkbench());
    }

    @GetMapping("/tasks")
    @RequireStorePermission("agent:view")
    public ApiResponse<List<V2AgentDtos.AgentTaskResponse>> listTasks() {
        return ApiResponse.success(v2AgentAiService.listTasks());
    }

    @GetMapping("/notifications")
    @RequireStorePermission("agent:view")
    public ApiResponse<List<V2AgentDtos.AgentNotificationResponse>> listNotifications(
        @RequestParam(value = "unread_only", required = false) Boolean unreadOnly
    ) {
        return ApiResponse.success(v2AgentAiService.listNotifications(Boolean.TRUE.equals(unreadOnly)));
    }

    @PostMapping("/notifications/{id}/read")
    @RequireStorePermission("agent:write")
    public ApiResponse<V2AgentDtos.AgentNotificationResponse> markNotificationRead(@PathVariable Long id) {
        return ApiResponse.success(v2AgentAiService.markNotificationRead(id));
    }

    @PostMapping("/chat")
    @RequireStorePermission("agent:write")
    public ApiResponse<V2AgentDtos.AgentChatResponse> chat(@Valid @RequestBody V2AgentDtos.AgentChatRequest request) {
        return ApiResponse.success(v2AgentAiService.chat(request));
    }

    @PostMapping(value = "/chat/stream", produces = "text/event-stream")
    @RequireStorePermission("agent:write")
    public SseEmitter chatStream(@Valid @RequestBody V2AgentDtos.AgentChatRequest request) {
        return v2AgentAiService.chatStream(request);
    }

    @PostMapping("/runs/{runId}/cancel")
    @RequireStorePermission("agent:write")
    public ApiResponse<V2AgentDtos.AgentRunCancelResponse> cancelRun(@PathVariable String runId) {
        return ApiResponse.success(v2AgentAiService.cancelRun(runId));
    }

    @GetMapping("/runs/{runId}/audit")
    @RequireStorePermission("agent:view")
    public ApiResponse<V2AgentDtos.AgentRunAuditResponse> getRunAudit(@PathVariable String runId) {
        return ApiResponse.success(v2AgentAiService.getRunAudit(runId));
    }
}
