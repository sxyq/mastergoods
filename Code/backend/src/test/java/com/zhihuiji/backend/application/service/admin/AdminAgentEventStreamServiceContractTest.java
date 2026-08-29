package com.zhihuiji.backend.application.service.admin;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.dto.admin.AdminAgentDtos;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class AdminAgentEventStreamServiceContractTest {
    @Test
    void blockedTerminalInInitialReplayCompletesImmediatelyWithoutPolling() throws Exception {
        AdminAgentDetailService detailService = mock(AdminAgentDetailService.class);
        AdminAgentEventStreamService service = new AdminAgentEventStreamService(detailService, new ObjectMapper());

        try {
            SseEmitter emitter = service.open(
                AdminPrincipal.forRole(
                    901L,
                    AdminPrincipal.AdminRole.SUPER_ADMIN,
                    com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope.allOwners(
                        false,
                        com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope.ContentMode.AUTHORIZED
                    )
                ),
                "run-1",
                8,
                false,
                null,
                null,
                new AdminAgentDtos.EventPage(List.of(event(9, "run_blocked")), 1, true)
            );

            assertThrows(
                IllegalStateException.class,
                () -> emitter.send(SseEmitter.event().data("after-terminal"))
            );
            verify(detailService, after(900).never()).events(any(), any(), any(), anyBoolean(), any(), any());
        } finally {
            service.shutdown();
        }
    }

    private static AdminAgentDtos.Event event(long sequence, String type) {
        return new AdminAgentDtos.Event(
            "event-" + sequence,
            "run-1",
            sequence,
            type,
            null,
            null,
            Instant.parse("2026-08-29T00:00:00Z"),
            "BLOCKED",
            null,
            null,
            null,
            AdminAgentDtos.RedactionState.REDACTED
        );
    }
}
