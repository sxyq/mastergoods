package com.zhihuiji.backend.infrastructure.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zhihuiji.backend.application.service.SessionAccessService;
import com.zhihuiji.backend.application.service.admin.AdminAuditService;
import com.zhihuiji.backend.domain.entity.SessionEntity;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipalResolver;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:admin-security-filter-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "agent.llm.enabled=false"
})
class AdminSecurityFilterChainTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SessionAccessService sessionAccessService;

    @MockBean
    private AdminAuditService adminAuditService;

    @MockBean
    private AdminPrincipalResolver adminPrincipalResolver;

    @Test
    void anonymousAdminRequestReturns401AndPersistsSecurityDenial() throws Exception {
        mockMvc.perform(get("/v2/admin/session")
                .header("X-Request-ID", "request-anonymous")
                .header("User-Agent", "security-test"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("unauthorized"))
            .andExpect(jsonPath("$.data").doesNotExist());

        verify(adminAuditService).recordSecurityDenial(
            any(), isNull(Long.class), eq("ANONYMOUS"), eq("authentication_required")
        );
        verifyNoInteractions(adminPrincipalResolver);
    }

    @Test
    void anonymousLegacyAdminRequestUsesTheSameUnauthorizedContract() throws Exception {
        mockMvc.perform(get("/v1/admin/summary"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));

        verify(adminAuditService).recordSecurityDenial(
            any(), isNull(Long.class), eq("ANONYMOUS"), eq("authentication_required")
        );
    }

    @Test
    void authenticatedNonAdminRequestReturns403AndAuditsSubject() throws Exception {
        when(sessionAccessService.findActiveSessionByToken("ordinary-token"))
            .thenReturn(Optional.of(activeSession(42L, "ordinary-token")));
        when(adminPrincipalResolver.requireCurrent())
            .thenThrow(new AccessDeniedException("administrator authentication required"));

        mockMvc.perform(get("/v2/admin/session")
                .header("Authorization", "Bearer ordinary-token")
                .header("X-Request-ID", "request-ordinary"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403));

        verify(adminAuditService).recordSecurityDenial(
            any(), eq(42L), eq("NON_ADMIN"), eq("permission_denied")
        );
    }

    private static SessionEntity activeSession(Long userId, String token) {
        SessionEntity session = new SessionEntity();
        session.setUserId(userId);
        session.setToken(token);
        session.setRefreshToken("refresh-token");
        session.setExpiresAt(System.currentTimeMillis() + 60_000L);
        session.setIsActive(true);
        session.setCreatedAt(System.currentTimeMillis());
        return session;
    }
}
