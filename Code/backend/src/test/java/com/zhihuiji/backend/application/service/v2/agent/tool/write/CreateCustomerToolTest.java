package com.zhihuiji.backend.application.service.v2.agent.tool.write;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentDraftRepository;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CreateCustomerToolTest {

    @Mock private AgentDraftRepository agentDraftRepository;

    private ObjectMapper objectMapper;
    private CreateCustomerTool tool;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        tool = new CreateCustomerTool(agentDraftRepository);
        when(agentDraftRepository.save(any(AgentDraftEntity.class))).thenAnswer(invocation -> {
            AgentDraftEntity entity = invocation.getArgument(0);
            setId(entity, 101L);
            return entity;
        });
    }

    @Test
    void schemaRequiresNonEmptyCustomerName() {
        var schema = tool.parameterSchema();

        assertEquals("name", schema.path("required").get(0).asText());
        assertEquals(1, schema.path("properties").path("name").path("minLength").asInt());
    }

    @Test
    void blankCustomerNameFailsWithoutCreatingDraft() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", "   ");
        params.put("phone", "13900000001");

        var result = tool.execute(context(), params);

        assertFalse(result.success());
        assertEquals("客户名称不能为空", result.errorMessage());
        verify(agentDraftRepository, never()).save(any(AgentDraftEntity.class));
    }

    @Test
    void validCustomerInputCreatesDraftWithRealInput() throws Exception {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", "全量工具测试客户");
        params.put("phone", "13900000001");
        params.put("remark", "先确认");

        var result = tool.execute(context(), params);

        assertTrue(result.success(), result.errorMessage());
        assertEquals(101L, result.toolFacts().path("draft_id").asLong());
        var content = objectMapper.readTree(result.toolFacts().path("content_json").asText());
        assertEquals("全量工具测试客户", content.path("name").asText());
        assertEquals("13900000001", content.path("phone").asText());
        verify(agentDraftRepository).save(any(AgentDraftEntity.class));
    }

    private ToolContext context() {
        return new ToolContext(7L, null, 200L, "run-create-customer", null, objectMapper);
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
