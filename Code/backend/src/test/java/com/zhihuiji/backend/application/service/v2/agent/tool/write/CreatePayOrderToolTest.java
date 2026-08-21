package com.zhihuiji.backend.application.service.v2.agent.tool.write;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.application.service.v2.agent.tool.AgentEntityReferenceValidator;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentDraftRepository;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CreatePayOrderToolTest {

    @Mock private AgentDraftRepository agentDraftRepository;
    @Mock private AgentEntityReferenceValidator referenceValidator;

    private ObjectMapper objectMapper;
    private CreatePayOrderTool tool;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        tool = new CreatePayOrderTool(agentDraftRepository, referenceValidator);
        when(agentDraftRepository.save(any(AgentDraftEntity.class))).thenAnswer(invocation -> {
            AgentDraftEntity entity = invocation.getArgument(0);
            setId(entity, 901L);
            return entity;
        });
    }

    @Test
    void schemaRequiresRealSupplierReferenceAndPositiveAmount() {
        JsonNode schema = tool.parameterSchema();
        JsonNode properties = schema.path("properties");

        assertEquals(1, properties.path("supplier_id").path("minimum").asInt());
        assertEquals(1, properties.path("supplier_name").path("minLength").asInt());
        assertTrue(properties.path("amount").path("minimum").asDouble() > 0D);
        assertEquals(
            java.util.List.of("supplier_id", "supplier_name", "amount"),
            java.util.stream.StreamSupport.stream(schema.path("required").spliterator(), false)
                .map(JsonNode::asText)
                .toList()
        );
        assertTrue(tool.description().contains("supplier_directory_lookup"));
        assertTrue(tool.description().contains("不会实际付款"));
    }

    @Test
    void missingReferenceOrInvalidAmountNeverCreatesDraft() {
        ObjectNode missingSupplierId = validParams();
        missingSupplierId.remove("supplier_id");
        assertRejected(missingSupplierId, "缺少必填参数 supplier_id");

        ObjectNode zeroSupplierId = validParams();
        zeroSupplierId.put("supplier_id", 0L);
        assertRejected(zeroSupplierId, "supplier_id 必须是正整数");

        ObjectNode zeroAmount = validParams();
        zeroAmount.put("amount", 0D);
        assertRejected(zeroAmount, "付款金额必须是大于 0 的有限数字");

        ObjectNode infiniteAmount = validParams();
        infiniteAmount.put("amount", Double.POSITIVE_INFINITY);
        assertRejected(infiniteAmount, "付款金额必须是大于 0 的有限数字");

        verify(agentDraftRepository, never()).save(any(AgentDraftEntity.class));
    }

    @Test
    void supplierFromAnotherOwnerNeverCreatesDraft() {
        when(referenceValidator.supplierMatches(7L, 21L, "真实供应商")).thenReturn(false);

        var result = tool.execute(context(), validParams());

        assertFalse(result.success());
        assertEquals("供应商不属于当前账号，无法生成付款草稿", result.errorMessage());
        verify(agentDraftRepository, never()).save(any(AgentDraftEntity.class));
    }

    @Test
    void validInputCreatesConfirmableOwnerScopedDraft() throws Exception {
        when(referenceValidator.supplierMatches(7L, 21L, "真实供应商")).thenReturn(true);

        var result = tool.execute(context(), validParams());

        assertTrue(result.success(), result.errorMessage());
        assertEquals(901L, result.toolFacts().path("draft_id").asLong());
        assertEquals(21L, result.toolFacts().path("supplier_id").asLong());

        var draftCaptor = org.mockito.ArgumentCaptor.forClass(AgentDraftEntity.class);
        verify(agentDraftRepository).save(draftCaptor.capture());
        AgentDraftEntity draft = draftCaptor.getValue();
        assertEquals(7L, draft.getOwnerUserId());
        assertEquals("active", draft.getStatus());
        JsonNode content = objectMapper.readTree(draft.getContentJson());
        assertEquals(21L, content.path("supplier_id").asLong());
        assertEquals("真实供应商", content.path("supplier_name").asText());
        assertEquals(1.23D, content.path("amount").asDouble(), 0.0001D);
        assertEquals("全量工具测试", content.path("notes").asText());
        assertTrue(content.has("method"));
        assertTrue(content.has("reference_no"));
        assertTrue(content.has("account_id"));
        assertTrue(content.has("status"));
    }

    private ObjectNode validParams() {
        return objectMapper.createObjectNode()
            .put("supplier_id", 21L)
            .put("supplier_name", "真实供应商")
            .put("amount", 1.23D)
            .put("remark", "全量工具测试");
    }

    private void assertRejected(ObjectNode params, String message) {
        var result = tool.execute(context(), params);
        assertFalse(result.success());
        assertEquals(message, result.errorMessage());
    }

    private ToolContext context() {
        return new ToolContext(7L, null, 300L, "run-create-pay-order", null, objectMapper);
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
