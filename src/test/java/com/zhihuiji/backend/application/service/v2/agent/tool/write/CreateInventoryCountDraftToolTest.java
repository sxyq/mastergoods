package com.zhihuiji.backend.application.service.v2.agent.tool.write;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentDraftRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CreateInventoryCountDraftToolTest {

    @Mock private AgentDraftRepository agentDraftRepository;
    @Mock private ProductRepository productRepository;

    private ObjectMapper objectMapper;
    private CreateInventoryCountDraftTool tool;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        tool = new CreateInventoryCountDraftTool(agentDraftRepository, productRepository, objectMapper);
        when(agentDraftRepository.save(any(AgentDraftEntity.class))).thenAnswer(invocation -> {
            AgentDraftEntity entity = invocation.getArgument(0);
            setId(entity, 88L);
            return entity;
        });
    }

    @Test
    void executeBuildsConfirmableInventoryAdjustmentDraft() throws Exception {
        ProductEntity product = new ProductEntity();
        product.setId(7L);
        product.setOwnerUserId(1L);
        product.setCode("P-7");
        product.setName("盘点商品");
        product.setCategory("默认");
        product.setUnit("件");
        product.setSalePrice(10.0);
        product.setPurchasePrice(6.0);
        product.setStock(12.0);
        product.setSafeStock(4.0);
        product.setStatus(1);
        product.setSyncStatus(1);
        product.setSyncVersion(1L);
        product.setCreatedAt(1L);
        product.setUpdatedAt(1L);
        when(productRepository.findByIdAndOwnerUserId(7L, 1L)).thenReturn(Optional.of(product));

        ObjectNode params = objectMapper.createObjectNode();
        params.put("product_id", 7L);
        params.put("counted_quantity", 9.5);
        params.put("note", "夜班复核");

        var result = tool.execute(new ToolContext(1L, null, 100L, "run-1", null, objectMapper), params);

        assertTrue(
            result.success(),
            "success=" + result.success() + ", error=" + result.errorMessage() + ", summary=" + result.toolSummary()
        );
        assertEquals("create_inventory_adjustment", result.toolFacts().path("draft_type").asText());
        assertEquals(-2.5, result.toolFacts().path("difference").asDouble(), 0.0001);

        var content = objectMapper.readTree(result.toolFacts().path("content_json").asText());
        assertEquals(7L, content.path("product_id").asLong());
        assertEquals("inventory_count", content.path("source_type").asText());
        assertEquals(-2.5, content.path("quantity_change").asDouble(), 0.0001);
        assertTrue(content.path("notes").asText().contains("盘点记录"));
        assertTrue(content.path("notes").asText().contains("夜班复核"));
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
