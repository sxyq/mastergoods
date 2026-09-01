package com.zhihuiji.backend.application.service.v2.agent.tool;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.application.service.admin.AdminAgentRuntimeConfigService;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentRunState;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:agent-tool-transaction-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.open-in-view=false",
    "spring.flyway.enabled=false",
    "agent.llm.enabled=false"
})
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.datasource.password=")
class AgentReadOnlyToolTransactionIntegrationTest {

    private static final Long OWNER_ID = 42L;
    private static final Long USER_ID = 100L;
    private static final Long STORE_ID = 7L;

    @Autowired
    private ToolExecutor toolExecutor;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CurrentOwnerService currentOwnerService;

    @MockBean
    private AdminAgentRuntimeConfigService runtimeConfigService;

    @ParameterizedTest(name = "{0} executes within the tool transaction boundary")
    @MethodSource("readOnlyTools")
    void executesReadOnlyToolsWithoutAnAmbientTestTransaction(String toolName, String paramsJson) throws Exception {
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(OWNER_ID);
        when(currentOwnerService.requireCurrentUserId()).thenReturn(USER_ID);
        when(currentOwnerService.findCurrentStoreId()).thenReturn(Optional.of(STORE_ID));
        when(runtimeConfigService.isToolEnabled(any(), any(), anyString())).thenReturn(true);

        JsonNode params = objectMapper.readTree(paramsJson);
        ToolExecutor.ExecutionOutcome outcome = toolExecutor.execute(
            new AgentRunState("run-" + toolName, 10L, OWNER_ID, STORE_ID, 1),
            toolName,
            params,
            null,
            10L,
            "run-" + toolName,
            null,
            objectMapper
        );

        assertTrue(outcome.executed(), () -> toolName + " was rejected: " + outcome.decision());
        assertNotNull(outcome.result());
        assertTrue(outcome.result().success(), () -> toolName + " failed: " + outcome.result().errorMessage());
    }

    private static Stream<Arguments> readOnlyTools() {
        return Stream.of(
            Arguments.of("product_catalog_lookup", "{}"),
            Arguments.of("cashflow_summary_lookup", "{}"),
            Arguments.of("store_info_lookup", "{}")
        );
    }
}
