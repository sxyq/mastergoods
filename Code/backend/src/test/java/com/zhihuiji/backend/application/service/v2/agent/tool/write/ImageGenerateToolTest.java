package com.zhihuiji.backend.application.service.v2.agent.tool.write;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.api.common.BusinessException;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentRunState;
import com.zhihuiji.backend.application.service.v2.agent.tool.AgentTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolExecutor;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolRegistry;
import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentDraftRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;

class ImageGenerateToolTest {

    @Mock private AgentDraftRepository agentDraftRepository;

    private ObjectMapper objectMapper;
    private ImageGenerateTool tool;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        tool = new ImageGenerateTool(agentDraftRepository);
        when(agentDraftRepository.save(any(AgentDraftEntity.class))).thenAnswer(invocation -> {
            AgentDraftEntity entity = invocation.getArgument(0);
            setId(entity, 501L);
            return entity;
        });
    }

    @Test
    void registersAsConfirmedCreateOnlyToolWithStrictSchema() {
        JsonNode schema = tool.parameterSchema();

        assertEquals("image_generate", tool.name());
        assertEquals(AgentTool.ToolType.CREATE_ONLY, tool.type());
        assertEquals("agent:write", tool.requiredPermission());
        assertEquals(List.of("prompt"),
            java.util.stream.StreamSupport.stream(schema.path("required").spliterator(), false)
                .map(JsonNode::asText).toList());
        assertEquals(1, schema.path("properties").path("prompt").path("minLength").asInt());
        assertEquals(2000, schema.path("properties").path("prompt").path("maxLength").asInt());
        assertEquals("array", schema.path("properties").path("reference_asset_ids").path("type").asText());
        assertEquals("integer", schema.path("properties").path("reference_asset_ids").path("items").path("type").asText());
        assertEquals(1, schema.path("properties").path("reference_asset_ids").path("maxItems").asInt());
        assertFalse(schema.path("additionalProperties").asBoolean());
    }

    @Test
    void registryRejectsInvalidPromptReferencesAndExtraFieldsBeforeDraftSave() throws Exception {
        ToolRegistry registry = new ToolRegistry(List.of(tool));
        ObjectNode params = objectMapper.createObjectNode()
            .put("prompt", " ")
            .put("unexpected", true);
        params.putArray("reference_asset_ids").add(0).add(2);

        var result = registry.executeTool(
            tool.name(),
            context(),
            params
        ).orElseThrow();

        assertFalse(result.success());
        assertEquals(0, result.toolFacts().size());
        verify(agentDraftRepository, never()).save(any(AgentDraftEntity.class));
    }

    @Test
    void validInputCreatesActiveOwnerScopedDraftWithAuditedStructuredResult() throws Exception {
        ObjectNode params = objectMapper.createObjectNode()
            .put("prompt", "生成一张蓝白色商品主图");
        params.putArray("reference_asset_ids").add(9L);

        var result = tool.execute(context(), params);

        assertTrue(result.success(), result.errorMessage());
        assertEquals(501L, result.toolFacts().path("draft_id").asLong());
        assertEquals("image_generate", result.toolFacts().path("draft_type").asText());
        assertTrue(result.toolFacts().path("query_audit").path("tool_input").path("prompt").asText()
            .contains("蓝白色商品主图"));
        assertEquals("draft_card", result.blocks().get(0).blockType());

        var captor = org.mockito.ArgumentCaptor.forClass(AgentDraftEntity.class);
        verify(agentDraftRepository).save(captor.capture());
        AgentDraftEntity draft = captor.getValue();
        assertEquals(7L, draft.getOwnerUserId());
        assertEquals("image_generate", draft.getDraftType());
        assertEquals("active", draft.getStatus());
        JsonNode content = objectMapper.readTree(draft.getContentJson());
        assertEquals("生成一张蓝白色商品主图", content.path("prompt").asText());
        assertEquals(9L, content.path("reference_asset_ids").get(0).asLong());
    }

    @Test
    void executorUsesRealCallerPermissionAndStoreContextBeforeCreatingDraft() throws Exception {
        CurrentOwnerService currentOwnerService = mock(CurrentOwnerService.class);
        when(currentOwnerService.findCurrentStoreId()).thenReturn(Optional.of(200L));
        when(currentOwnerService.requireCurrentUserId()).thenReturn(8L);
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(7L);
        ToolExecutor executor = new ToolExecutor(new ToolRegistry(List.of(tool)), currentOwnerService);
        AgentRunState runState = new AgentRunState("run-image", 300L, 7L, 200L, 1);

        var outcome = executor.execute(
            runState,
            tool.name(),
            objectMapper.createObjectNode().put("prompt", "生成图片"),
            null,
            300L,
            "run-image",
            null,
            objectMapper
        );

        assertTrue(outcome.executed());
        assertTrue(outcome.result().success());
        verify(currentOwnerService).requirePermissions("agent:write");
        verify(currentOwnerService).findCurrentStoreId();
        verify(agentDraftRepository).save(any(AgentDraftEntity.class));
    }

    @Test
    void executorRejectsRunStateOwnerDifferentFromCurrentOwnerBeforeToolOrRepository() throws Exception {
        CurrentOwnerService currentOwnerService = mock(CurrentOwnerService.class);
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(7L);
        ToolExecutor executor = new ToolExecutor(new ToolRegistry(List.of(tool)), currentOwnerService);
        AgentRunState runState = new AgentRunState("run-image-cross-owner", 300L, 8L, 200L, 1);

        var outcome = executor.execute(
            runState,
            tool.name(),
            objectMapper.createObjectNode().put("prompt", "生成图片"),
            null,
            300L,
            "run-image-cross-owner",
            null,
            objectMapper
        );

        assertFalse(outcome.executed());
        assertEquals(ToolExecutor.TOOL_CONTEXT_INVALID, outcome.decision().reasonCode());
        verify(currentOwnerService).requireCurrentOwnerUserId();
        verify(agentDraftRepository, never()).save(any(AgentDraftEntity.class));
    }

    @Test
    void executorRejectsCallerWithoutAgentWriteBeforeDraftCreation() throws Exception {
        CurrentOwnerService currentOwnerService = mock(CurrentOwnerService.class);
        doThrow(new AccessDeniedException("denied"))
            .when(currentOwnerService).requirePermissions("agent:write");
        ToolExecutor executor = new ToolExecutor(new ToolRegistry(List.of(tool)), currentOwnerService);
        AgentRunState runState = new AgentRunState("run-image-denied", 300L, 7L, 200L, 1);

        var outcome = executor.execute(
            runState,
            tool.name(),
            objectMapper.createObjectNode().put("prompt", "生成图片"),
            null,
            300L,
            "run-image-denied",
            null,
            objectMapper
        );

        assertFalse(outcome.executed());
        assertEquals(ToolExecutor.TOOL_PERMISSION_DENIED, outcome.decision().reasonCode());
        verify(agentDraftRepository, never()).save(any(AgentDraftEntity.class));
    }

    private ToolContext context() {
        return new ToolContext(7L, 8L, 200L, "run-image-generate", null, objectMapper);
    }

    private void setId(AgentDraftEntity entity, Long id) {
        try {
            Field field = AgentDraftEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
