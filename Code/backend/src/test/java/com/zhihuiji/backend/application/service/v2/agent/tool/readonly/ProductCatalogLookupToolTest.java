package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.domain.entity.product.ProductEntity;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;

class ProductCatalogLookupToolTest {
    @Mock private ProductRepository productRepository;

    private ProductCatalogLookupTool tool;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        tool = new ProductCatalogLookupTool(productRepository);
    }

    @Test
    void factsExposeOwnerScopedProductIdForFollowUpDraftTools() {
        ProductEntity product = new ProductEntity();
        product.setId(7L);
        product.setOwnerUserId(1L);
        product.setName("盘点商品");
        product.setCode("P-7");
        product.setCategory("默认");
        product.setStock(12.0);
        product.setSalePrice(10.0);
        product.setStatus(1);
        when(productRepository.findAllByOwnerUserIdOrderByNameAsc(eq(1L), eq(PageRequest.of(0, 10))))
            .thenReturn(List.of(product));
        when(productRepository.countByOwnerUserId(1L)).thenReturn(1L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(12.0);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(0L);

        var result = tool.execute(new ToolContext(1L, null, 100L, "run-1", null, objectMapper), objectMapper.createObjectNode());

        assertEquals(7L, result.toolFacts().path("top_products").get(0).path("product_id").asLong());
    }

    @Test
    void ignoresModelNullSentinelsAndZeroOptionalIds() {
        ProductEntity product = new ProductEntity();
        product.setId(8L);
        product.setOwnerUserId(1L);
        product.setName("真实商品");
        product.setCode("P-8");
        product.setCategory("默认");
        product.setStock(3.0);
        product.setSalePrice(5.0);
        product.setStatus(1);
        when(productRepository.findAllByOwnerUserIdOrderByNameAsc(eq(1L), eq(PageRequest.of(0, 10))))
            .thenReturn(List.of(product));
        when(productRepository.countByOwnerUserId(1L)).thenReturn(1L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(3.0);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(0L);
        var params = objectMapper.createObjectNode()
            .put("keyword", "null")
            .put("status", 1)
            .put("category_id", 0)
            .put("unit_id", 0);

        var result = tool.execute(new ToolContext(1L, null, 100L, "run-2", null, objectMapper), params);

        assertEquals(1, result.toolFacts().path("returned_product_count").asInt());
        assertEquals(8L, result.toolFacts().path("top_products").get(0).path("product_id").asLong());
    }
}
