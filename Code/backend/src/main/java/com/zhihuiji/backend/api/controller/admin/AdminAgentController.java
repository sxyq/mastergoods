package com.zhihuiji.backend.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.admin.AdminAgentDtos;
import com.zhihuiji.backend.api.dto.admin.AdminPageDtos;
import com.zhihuiji.backend.application.service.admin.AdminAgentObservabilityService;
import com.zhihuiji.backend.application.service.admin.AdminAgentDetailService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipalResolver;
import java.time.Instant;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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

    public AdminAgentController(
        AdminAgentObservabilityService observabilityService,
        AdminAgentDetailService detailService,
        AdminPrincipalResolver principalResolver,
        ObjectMapper objectMapper
    ) {
        this.observabilityService = observabilityService;
        this.detailService = detailService;
        this.principalResolver = principalResolver;
        this.objectMapper = objectMapper;
    }

    /** Compatibility constructor for the existing read-controller slice tests. */
    public AdminAgentController(
        AdminAgentObservabilityService observabilityService,
        AdminPrincipalResolver principalResolver
    ) {
        this(observabilityService, null, principalResolver, new ObjectMapper());
    }

    @GetMapping("/runs")
    public ApiResponse<AdminPageDtos.PageResponse<AdminAgentDtos.RunSummary>> runs(
        @RequestParam(value = "runId", required = false) String runId,
        @RequestParam(value = "conversationId", required = false) Long conversationId,
        @RequestParam(value = "terminalStatus", required = false) String terminalStatus,
        @RequestParam(value = "from", required = false) Instant from,
        @RequestParam(value = "to", required = false) Instant to,
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        return ApiResponse.success(
            observabilityService.listRuns(
                principalResolver.requireCurrent(), runId, conversationId, terminalStatus,
                from, to, ownerUserId, storeId, page, size
            )
        );
    }

    @GetMapping("/runs/{runId}")
    public ApiResponse<AdminAgentDtos.RunSummary> run(
        @PathVariable String runId,
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId
    ) {
        return ApiResponse.success(
            observabilityService.getRun(
                principalResolver.requireCurrent(), runId, ownerUserId, storeId
            )
        );
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
        return ApiResponse.success(detailService.messages(principalResolver.requireCurrent(), conversationId,
            includeContent, page, size, ownerUserId, storeId));
    }

    @GetMapping("/runs/{runId}/events")
    public ApiResponse<AdminAgentDtos.EventPage> events(
        @PathVariable String runId,
        @RequestParam(value = "afterSequence", required = false) Integer afterSequence,
        @RequestParam(value = "includeContent", defaultValue = "false") boolean includeContent,
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId
    ) {
        return ApiResponse.success(detailService.events(principalResolver.requireCurrent(), runId, afterSequence,
            includeContent, ownerUserId, storeId));
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
        AdminAgentDtos.EventPage page = detailService.events(principalResolver.requireCurrent(), runId,
            replayAfter, includeContent, ownerUserId, storeId);
        SseEmitter emitter = new SseEmitter(30_000L);
        try {
            for (AdminAgentDtos.Event event : page.items()) {
                emitter.send(SseEmitter.event().id(Long.toString(event.sequence()))
                    .name(event.eventType()).data(objectMapper.writeValueAsString(event)));
                if (isTerminal(event.eventType())) {
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
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        return ApiResponse.success(detailService.usage(principalResolver.requireCurrent(), from, to,
            ownerUserId, storeId, page, size));
    }

    @GetMapping("/runs/{runId}/context")
    public ApiResponse<AdminAgentDtos.ContextResponse> context(
        @PathVariable String runId,
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId
    ) {
        return ApiResponse.success(detailService.context(principalResolver.requireCurrent(), runId, ownerUserId, storeId));
    }

    @GetMapping("/runs/{runId}/drafts")
    public ApiResponse<List<AdminAgentDtos.Draft>> drafts(
        @PathVariable String runId,
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId
    ) {
        return ApiResponse.success(detailService.drafts(principalResolver.requireCurrent(), runId, ownerUserId, storeId));
    }

    private Integer parseSequence(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Math.max(0, Integer.parseInt(value)); }
        catch (NumberFormatException ignored) { return null; }
    }

    private boolean isTerminal(String eventType) {
        if (eventType == null) return false;
        String value = eventType.toLowerCase(java.util.Locale.ROOT);
        return value.equals("run_completed") || value.equals("run_failed")
            || value.equals("run_cancelled") || value.equals("run_exhausted")
            || value.endsWith(".completed") || value.endsWith(".failed")
            || value.endsWith(".cancelled") || value.endsWith(".exhausted");
    }
}
