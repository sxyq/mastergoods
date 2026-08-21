package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GeneratePosterPromptToolTest {

    private final GeneratePosterPromptTool tool =
        new GeneratePosterPromptTool(Mockito.mock(ProductRepository.class));

    @Test
    void schemaRequiresARealProductIdForPosterPromptGeneration() {
        JsonNode schema = tool.parameterSchema();

        assertEquals("object", schema.path("type").asText());
        assertTrue(schema.path("required").toString().contains("product_id"));
        assertEquals("integer", schema.path("properties").path("product_id").path("type").asText());
        assertTrue(tool.description().contains("真实 product_id"));
    }
}
