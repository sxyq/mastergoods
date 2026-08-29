package com.zhihuiji.backend.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.admin.AdminAgentDtos;
import com.zhihuiji.backend.api.dto.admin.AdminPageDtos;
import com.zhihuiji.backend.application.service.admin.AdminAgentObservabilityService;
import com.zhihuiji.backend.application.service.admin.AdminAgentDetailService;
import com.zhihuiji.backend.application.service.admin.AdminAgentEventStreamService;
import com.zhihuiji.backend.application.service.admin.AdminAuditService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipalResolver;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API-ADM-05 Agent run list and detail endpoints. */
@RestController
@RequestMapping("/v2/admin/agent")
public class AdminAgentController {
    private final AdminAgentObservabilityService observabilityService;
    private final AdminAgentDetailService detailService;
    private final AdminPrincipalResolver principalResolver;
    private final ObjectMapper objectMapper;
    private final AdminAuditService auditService;
    private final AdminAgentEventStreamService eventStreamService;

    @Autowired
    public AdminAgentController(
        AdminAgentObservabilityService observabilityService,
        AdminAgentDetailService detailService,
        AdminPrincipalResolver principalResolver,
        ObjectMapper objectMapper,
        AdminAuditService auditService,
        AdminAgentEventStreamService eventStreamService
    ) {
        this.observabilityService = observabilityService;
        this.detailService = detailService;
        this.principalResolver = principalResolver;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
        this.eventStreamService = eventStreamService;
    }

    public AdminAgentController(
        AdminAgentObservabilityService observabilityService,
        AdminAgentDetailService detailService,
        AdminPrincipalResolver principalResolver,
        ObjectMapper objectMapper,
        AdminAuditService auditService
    ) {
        this(observabilityService, detailService, principalResolver, objectMapper, auditService, null);
    }

    public AdminAgentController(
        AdminAgentObservabilityService observabilityService,
        AdminAgentDetailService detailService,
        AdminPrincipalResolver principalResolver,
        ObjectMapper objectMapper
    ) {
        this(observabilityService, detailService, principalResolver, objectMapper, null, null);
    }

    /** Compatibility constructor for the existing read-controller slice tests. */
    public AdminAgentController(
        AdminAgentObservabilityService observabilityService,
        AdminPrincipalResolver principalResolver
    ) {
        this(observabilityService, null, principalResolver, new ObjectMapper(), null);
    }

    @GetMapping("/runs")
    public ApiResponse<AdminPageDtos.PageResponse<AdminAgentDtos.RunSummary>> runs(
        @RequestParam(value = "runId", required = false) String runId,
        @RequestParam(value = "conversationId", required = false) Long conversationId,
        @RequestParam(value = "actorUserId", required = false) Long actorUserId,
        @RequestParam(value = "toolName", required = false) String toolName,
        @RequestParam(value = "modelId", required = false) String modelId,
        @RequestParam(value = "terminalStatus", required = false) String terminalStatus,
        @RequestParam(value = "from", required = false) Instant from,
        @RequestParam(value = "to", required = false) Instant to,
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        var principal = principalResolver.requireCurrent();
        var response = observabilityService.listRuns(principal, runId, conversationId, actorUserId, toolName, modelId,
            terminalStatus, from, to, ownerUserId, storeId, page, size);
        recordRead(principal, "admin.agent.runs.read", "AGENT_RUN", runId, ownerUserId, storeId);
        return ApiResponse.success(response);
    }

    @GetMapping("/runs/{runId}")
    public ApiResponse<AdminAgentDtos.RunSummary> run(
        @PathVariable String runId,
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId
    ) {
        var principal = principalResolver.requireCurrent();
        var response = observabilityService.getRun(principal, runId, ownerUserId, storeId);
        recordRead(principal, "admin.agent.run.read", "AGENT_RUN", runId, ownerUserId, storeId);
        return ApiResponse.success(response);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<AdminPageDtos.PageResponse<AdminAgentDtos.Message>> messages(
        @PathVariable String conversationId,
        @RequestParam(value = "includeContent", defaultValue = "false") boolean includeContent,
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        var principal = principalResolver.requireCurrent();
        var response = detailService.messages(principal, conversationId, includeContent, page, size, ownerUserId, storeId);
        recordRead(principal, "admin.agent.messages.read", "CONVERSATION", conversationId, ownerUserId, storeId);
        return ApiResponse.success(response);
    }

    @GetMapping("/runs/{runId}/events")
    public ApiResponse<AdminAgentDtos.EventPage> events(
        @PathVariable String runId,
        @RequestParam(value = "afterSequence", required = false) Integer afterSequence,
        @RequestParam(value = "includeContent", defaultValue = "false") boolean includeContent,
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId
    ) {
        var principal = principalResolver.requireCurrent();
        var response = detailService.events(principal, runId, afterSequence, includeContent, ownerUserId, storeId);
        recordRead(principal, "admin.agent.events.read", "AGENT_RUN", runId, ownerUserId, storeId);
        return ApiResponse.success(response);
    }

    @GetMapping(value = "/runs/{runId}/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter eventsStream(
        @PathVariable String runId,
        @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId,
        @RequestParam(value = "afterSequence", required = false) Integer afterSequence,
        @RequestParam(value = "includeContent", defaultValue = "false") boolean includeContent,
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId
    ) {
        Integer replayAfter = afterSequence != null ? afterSequence : parseSequence(lastEventId);
        var principal = principalResolver.requireCurrent();
        AdminAgentDtos.EventPage page = detailService.events(principal, runId,
            replayAfter, includeContent, ownerUserId, storeId);
        recordRead(principal, "admin.agent.events.stream", "AGENT_RUN", runId, ownerUserId, storeId);
        if (eventStreamService != null) {
            return eventStreamService.open(principal, runId, replayAfter, includeContent, ownerUserId, storeId, page);
        }
        SseEmitter emitter = new SseEmitter(30_000L);
        try {
            emitter.send(SseEmitter.event().name("stream_integrity")
                .data(objectMapper.writeValueAsString(Map.of(
                    "event_integrity", page.eventIntegrity(),
                    "after_sequence", replayAfter == null ? 0 : replayAfter
                ))));
            for (AdminAgentDtos.Event event : page.items()) {
                emitter.send(SseEmitter.event().id(Long.toString(event.sequence()))
                    .name(event.eventType()).data(objectMapper.writeValueAsString(event)));
                if (AdminAgentEventStreamService.isTerminalEvent(event.eventType())) {
                    emitter.complete();
                    return emitter;
                }
            }
            emitter.complete();
        } catch (Exception ex) {
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    @GetMapping("/usage")
    public ApiResponse<AdminAgentDtos.UsagePage> usage(
        @RequestParam(value = "from", required = false) Instant from,
        @RequestParam(value = "to", required = false) Instant to,
        @RequestParam(value = "modelId", required = false) String modelId,
        @RequestParam(value = "granularity", required = false) String granularity,
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        var principal = principalResolver.requireCurrent();
        var response = detailService.usage(principal, from, to, modelId, granularity, ownerUserId, storeId, page, size);
        recordRead(principal, "admin.agent.usage.read", "AGENT_USAGE", null, ownerUserId, storeId);
        return ApiResponse.success(response);
    }

    @GetMapping("/runs/{runId}/context")
    public ApiResponse<AdminAgentDtos.ContextResponse> context(
        @PathVariable String runId,
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId
    ) {
        var principal = principalResolver.requireCurrent();
        var response = detailService.context(principal, runId, ownerUserId, storeId);
        recordRead(principal, "admin.agent.context.read", "AGENT_RUN", runId, ownerUserId, storeId);
        return ApiResponse.success(response);
    }

    @GetMapping("/runs/{runId}/drafts")
    public ApiResponse<List<AdminAgentDtos.Draft>> drafts(
        @PathVariable String runId,
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId
    ) {
        var principal = principalResolver.requireCurrent();
        var response = detailService.drafts(principal, runId, ownerUserId, storeId);
        recordRead(principal, "admin.agent.drafts.read", "AGENT_RUN", runId, ownerUserId, storeId);
        return ApiResponse.success(response);
    }

    private void recordRead(com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal principal,
                            String action, String resourceType, String resourceId, Long ownerUserId, Long storeId) {
        if (auditService != null) auditService.recordRead(principal, action, resourceType, resourceId, ownerUserId, storeId, "read");
    }

    private Integer parseSequence(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Math.max(0, Integer.parseInt(value)); }
        catch (NumberFormatException ignored) { return null; }
    }

}
