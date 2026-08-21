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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.application.service.v2.agent.tool.AgentEntityReferenceValidator;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentDraftRepository;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CreateSaleOrderToolTest {

    @Mock private AgentDraftRepository agentDraftRepository;
    @Mock private AgentEntityReferenceValidator referenceValidator;

    private ObjectMapper objectMapper;
    private CreateSaleOrderTool tool;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        tool = new CreateSaleOrderTool(agentDraftRepository, referenceValidator);
        when(agentDraftRepository.save(any(AgentDraftEntity.class))).thenAnswer(invocation -> {
            AgentDraftEntity entity = invocation.getArgument(0);
            setId(entity, 202L);
            return entity;
        });
    }

    @Test
    void schemaRequiresCustomerAndAtLeastOnePositiveSaleItem() {
        JsonNode schema = tool.parameterSchema();
        JsonNode properties = schema.path("properties");
        JsonNode itemSchema = properties.path("items").path("items");

        assertEquals(1, properties.path("customer_id").path("minimum").asInt());
        assertEquals(1, properties.path("items").path("minItems").asInt());
        assertEquals(1, itemSchema.path("properties").path("product_id").path("minimum").asInt());
        assertTrue(itemSchema.path("properties").path("quantity").path("minimum").asDouble() > 0D);
        assertTrue(itemSchema.path("properties").path("price").path("minimum").asDouble() > 0D);
    }

    @Test
    void missingCustomerIdAndEmptyItemsNeverCreateDraft() {
        ObjectNode missingCustomerId = validParams();
        missingCustomerId.remove("customer_id");
        assertRejected(missingCustomerId, "缺少必填参数 customer_id");

        ObjectNode emptyItems = validParams();
        emptyItems.set("items", objectMapper.createArrayNode());
        assertRejected(emptyItems, "至少需要一条商品明细");

        verify(agentDraftRepository, never()).save(any(AgentDraftEntity.class));
    }

    @Test
    void invalidIdsQuantityAndPriceNeverCreateDraft() {
        ObjectNode invalidProductId = validParams();
        item(invalidProductId).put("product_id", 0);
        assertRejected(invalidProductId, "商品明细 product_id 必须是正整数");

        ObjectNode fractionalCustomerId = validParams();
        fractionalCustomerId.put("customer_id", 11.5D);
        assertRejected(fractionalCustomerId, "customer_id 必须是正整数");

        ObjectNode zeroQuantity = validParams();
        item(zeroQuantity).put("quantity", 0D);
        assertRejected(zeroQuantity, "商品数量必须是大于 0 的有限数字");

        ObjectNode negativePrice = validParams();
        item(negativePrice).put("price", -1D);
        assertRejected(negativePrice, "商品单价必须是大于 0 的有限数字");

        ObjectNode zeroPrice = validParams();
        item(zeroPrice).put("price", 0D);
        assertRejected(zeroPrice, "商品单价必须是大于 0 的有限数字");

        verify(agentDraftRepository, never()).save(any(AgentDraftEntity.class));
    }

    @Test
    void ownerMismatchNeverCreatesDraft() {
        when(referenceValidator.customerMatches(7L, 11L, "测试客户A")).thenReturn(false);

        var result = tool.execute(context(), validParams());

        assertFalse(result.success());
        assertEquals("客户或商品不属于当前账号，无法生成销售草稿", result.errorMessage());
        verify(agentDraftRepository, never()).save(any(AgentDraftEntity.class));
    }

    @Test
    void validInputCreatesOneRealSaleItemDraft() throws Exception {
        when(referenceValidator.customerMatches(7L, 11L, "测试客户A")).thenReturn(true);
        when(referenceValidator.productBelongsToOwner(7L, 21L)).thenReturn(true);

        var result = tool.execute(context(), validParams());

        assertTrue(result.success(), result.errorMessage());
        assertEquals(202L, result.toolFacts().path("draft_id").asLong());
        assertEquals(1, result.toolFacts().path("items_count").asInt());
        ArgumentCaptor<AgentDraftEntity> draftCaptor = ArgumentCaptor.forClass(AgentDraftEntity.class);
        verify(agentDraftRepository).save(draftCaptor.capture());
        JsonNode content = objectMapper.readTree(draftCaptor.getValue().getContentJson());
        assertEquals(11L, content.path("customer_id").asLong());
        assertEquals(1, content.path("items").size());
        assertEquals(21L, content.path("items").get(0).path("product_id").asLong());
        assertEquals(1D, content.path("items").get(0).path("quantity").asDouble(), 0.0001D);
        assertEquals(1.23D, content.path("items").get(0).path("unit_price").asDouble(), 0.0001D);
    }

    private ObjectNode validParams() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("customer_id", 11L);
        params.put("customer_name", "测试客户A");
        ArrayNode items = params.putArray("items");
        ObjectNode item = items.addObject();
        item.put("product_id", 21L);
        item.put("product_name", "测试商品A");
        item.put("quantity", 1D);
        item.put("price", 1.23D);
        return params;
    }

    private ObjectNode item(ObjectNode params) {
        return (ObjectNode) params.path("items").get(0);
    }

    private void assertRejected(ObjectNode params, String message) {
        var result = tool.execute(context(), params);
        assertFalse(result.success());
        assertEquals(message, result.errorMessage());
    }

    private ToolContext context() {
        return new ToolContext(7L, null, 300L, "run-create-sale-order", null, objectMapper);
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
