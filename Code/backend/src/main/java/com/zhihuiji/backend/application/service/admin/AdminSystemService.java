package com.zhihuiji.backend.application.service.admin;

import com.zhihuiji.backend.api.dto.admin.AdminConfigDtos;
import com.zhihuiji.backend.api.dto.admin.AdminSystemDtos;
import com.zhihuiji.backend.infrastructure.ai.LongCatAnthropicClient;
import com.zhihuiji.backend.infrastructure.config.AgentLlmProperties;
import com.zhihuiji.backend.infrastructure.security.admin.AdminAuthorizationService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPermission;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only system and Agent configuration metadata; no provider secret is exposed. */
@Service
public class AdminSystemService {
    private final AdminAuthorizationService authorizationService;
    private final AgentLlmProperties llmProperties;
    private final LongCatAnthropicClient llmClient;

    public AdminSystemService(
        AdminAuthorizationService authorizationService,
        AgentLlmProperties llmProperties,
        LongCatAnthropicClient llmClient
    ) {
        this.authorizationService = authorizationService;
        this.llmProperties = llmProperties;
        this.llmClient = llmClient;
    }

    @Transactional(readOnly = true)
    public AdminConfigDtos.ConfigResponse config(AdminPrincipal principal) {
        authorizationService.requirePermission(principal, AdminPermission.AGENT_CONFIG_READ);
        return new AdminConfigDtos.ConfigResponse(
            llmProperties.getModel(), llmProperties.isEnabled(), 0L, llmClient.configurationStatus()
        );
    }

    @Transactional(readOnly = true)
    public AdminSystemDtos.HealthResponse health(AdminPrincipal principal) {
        authorizationService.requirePermission(principal, AdminPermission.SYSTEM_READ);
        String providerStatus = llmClient.configurationStatus();
        return new AdminSystemDtos.HealthResponse(
            "UP", packageVersion(), Instant.now(), List.of(
                new AdminSystemDtos.Component("backend", "UP", "serving"),
                new AdminSystemDtos.Component("agent_llm", providerStatus, "configuration status only")
            )
        );
    }

    private String packageVersion() {
        return AdminSystemService.class.getPackage().getImplementationVersion();
    }
}
