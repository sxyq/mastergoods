// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.infrastructure.repository.InventoryMonthlyStatsRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;

class InventoryPanoramaLookupToolTest {
    @Mock private ProductRepository productRepository;
    @Mock private InventoryMonthlyStatsRepository inventoryMonthlyStatsRepository;
    @Mock private SaleOrderItemRepository saleOrderItemRepository;

    private InventoryPanoramaLookupTool tool;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        tool = new InventoryPanoramaLookupTool(
            productRepository,
            inventoryMonthlyStatsRepository,
            saleOrderItemRepository,
            objectMapper
        );
    }

    @Test
    void emptyOwnerScopeReturnsEmptyResultWithoutSortingImmutableList() {
        when(productRepository.findAllByOwnerUserIdOrderByNameAsc(4L, PageRequest.of(0, 10)))
            .thenReturn(java.util.List.of());

        var result = tool.execute(
            new ToolContext(4L, 4L, 2L, 1784L, "run-empty-owner", null, objectMapper),
            objectMapper.createObjectNode().put("limit", 10)
        );

        assertTrue(result.success());
        assertTrue(result.toolFacts().isNull());
        verify(inventoryMonthlyStatsRepository, never())
            .findByOwnerUserIdAndProductIdAndYearAndMonth(any(), any(), any(), any());
        verify(saleOrderItemRepository, never()).recentStockOutRows(
            eq(4L), any(), any(), any(), any()
        );
    }
}
