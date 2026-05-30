package com.zhihuiji.backend.api.controller;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.agent.AgentDto;
import com.zhihuiji.backend.application.service.AgentTaskService;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/v1/agent")
public class AgentTaskController {
    private final AgentTaskService agentTaskService;

    public AgentTaskController(AgentTaskService agentTaskService) {
        this.agentTaskService = agentTaskService;
    }

    @PostMapping("/tasks")
    public ApiResponse<AgentDto.AgentTaskSummaryDto> createTask(@Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.success(agentTaskService.submitTask(request.taskType(), request.title(), request.input()));
    }

    @GetMapping("/tasks")
    public ApiResponse<List<AgentDto.AgentTaskSummaryDto>> listTasks() {
        return ApiResponse.success(agentTaskService.listTasks());
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<AgentDto.AgentTaskDetailDto> getTask(@PathVariable Long taskId) {
        return ApiResponse.success(agentTaskService.getTask(taskId));
    }

    @GetMapping("/notifications")
    public ApiResponse<List<AgentDto.AgentNotificationDto>> notifications(
        @RequestParam(value = "unread_only", defaultValue = "false") boolean unreadOnly,
        @RequestParam(value = "undelivered_only", defaultValue = "false") boolean undeliveredOnly
    ) {
        return ApiResponse.success(agentTaskService.listNotifications(unreadOnly, undeliveredOnly));
    }

    @PostMapping("/notifications/{notificationId}/read")
    public ApiResponse<AgentDto.AgentNotificationDto> markRead(@PathVariable Long notificationId) {
        return ApiResponse.success(agentTaskService.markNotificationRead(notificationId));
    }

    @PostMapping("/notifications/{notificationId}/delivered")
    public ApiResponse<AgentDto.AgentNotificationDto> markDelivered(@PathVariable Long notificationId) {
        return ApiResponse.success(agentTaskService.markNotificationDelivered(notificationId));
    }

    @GetMapping(path = "/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter notificationStream() {
        return agentTaskService.subscribeNotifications();
    }

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record CreateTaskRequest(String taskType, String title, String input) {}
}
