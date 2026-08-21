package com.zhihuiji.backend.application.service.v2.agent.tool;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToolRegistryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void toolListsAreStableAndRequiredParametersAreCheckedBeforeExecution() throws Exception {
        ObjectNode transferSchema = objectMapper.createObjectNode();
        transferSchema.put("type", "object");
        transferSchema.putArray("required").add("from_account_id").add("to_account_id").add("amount");
        AgentTool transfer = new TestTool(
            "create_account_transfer",
            "创建账户转账",
            "生成账户转账草稿",
            AgentTool.ToolType.CREATE_ONLY,
            transferSchema
        );
        AgentTool balance = new TestTool(
            "account_balance_lookup",
            "账户余额查询",
            "查询账户余额",
            AgentTool.ToolType.READ_ONLY,
            objectMapper.createObjectNode()
        );
        ToolRegistry registry = new ToolRegistry(List.of(transfer, balance));

        assertTrue(registry.listTools().get(0).name().equals("account_balance_lookup"));
        assertFalse(registry.hasAllRequiredParameters("create_account_transfer", objectMapper.createObjectNode()));

        JsonNode complete = objectMapper.readTree(
            "{\"from_account_id\":1,\"to_account_id\":2,\"amount\":1.23}"
        );
        assertTrue(registry.hasAllRequiredParameters("create_account_transfer", complete));
    }

    @Test
    void nestedArrayRequiredParametersAreCheckedBeforeDraftExecution() throws Exception {
        ObjectNode itemSchema = objectMapper.createObjectNode();
        itemSchema.put("type", "object");
        itemSchema.set("properties", objectMapper.createObjectNode()
            .putObject("product_id").put("type", "integer"));
        itemSchema.putArray("required").add("product_id");
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = objectMapper.createObjectNode();
        ObjectNode itemsProperty = properties.putObject("items");
        itemsProperty.put("type", "array");
        itemsProperty.set("items", itemSchema);
        schema.set("properties", properties);
        schema.putArray("required").add("items");
        ToolRegistry registry = new ToolRegistry(List.of(new TestTool(
            "create_sale_order", "创建销售单", "生成销售草稿", AgentTool.ToolType.CREATE_ONLY, schema
        )));

        assertFalse(registry.hasAllRequiredParameters(
            "create_sale_order", objectMapper.readTree("{\"items\":[{\"product_name\":\"商品\"}]}")
        ));
        assertTrue(registry.hasAllRequiredParameters(
            "create_sale_order", objectMapper.readTree("{\"items\":[{\"product_id\":7}]}")
        ));
    }

    @Test
    void numericAndCollectionSchemaConstraintsRejectInvalidWriteArguments() throws Exception {
        ObjectNode itemSchema = objectMapper.createObjectNode();
        itemSchema.put("type", "object");
        ObjectNode itemProperties = itemSchema.putObject("properties");
        itemProperties.putObject("product_id").put("type", "integer").put("minimum", 1);
        itemProperties.putObject("quantity").put("type", "number").put("minimum", 0.000001D);
        itemProperties.putObject("price").put("type", "number").put("minimum", 0.000001D);
        itemSchema.putArray("required").add("product_id").add("quantity").add("price");

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("customer_id").put("type", "integer").put("minimum", 1);
        ObjectNode itemsProperty = properties.putObject("items");
        itemsProperty.put("type", "array").put("minItems", 1);
        itemsProperty.set("items", itemSchema);
        schema.putArray("required").add("customer_id").add("items");

        ToolRegistry registry = new ToolRegistry(List.of(new TestTool(
            "create_sale_order", "创建销售单", "生成销售草稿", AgentTool.ToolType.CREATE_ONLY, schema
        )));

        assertFalse(registry.hasAllRequiredParameters(
            "create_sale_order", objectMapper.readTree("{\"customer_id\":null,\"items\":[]}")));
        assertFalse(registry.hasAllRequiredParameters(
            "create_sale_order", objectMapper.readTree("{\"customer_id\":0,\"items\":[{\"product_id\":1,\"quantity\":1,\"price\":1}]}")));
        assertFalse(registry.hasAllRequiredParameters(
            "create_sale_order", objectMapper.readTree("{\"customer_id\":1,\"items\":[]}")));
        assertFalse(registry.hasAllRequiredParameters(
            "create_sale_order", objectMapper.readTree("{\"customer_id\":1,\"items\":[{\"product_id\":0,\"quantity\":1,\"price\":1}]}")));
        assertFalse(registry.hasAllRequiredParameters(
            "create_sale_order", objectMapper.readTree("{\"customer_id\":1,\"items\":[{\"product_id\":1,\"quantity\":0,\"price\":1}]}")));
        assertFalse(registry.hasAllRequiredParameters(
            "create_sale_order", objectMapper.readTree("{\"customer_id\":1,\"items\":[{\"product_id\":1,\"quantity\":1,\"price\":0}]}")));
        assertTrue(registry.hasAllRequiredParameters(
            "create_sale_order", objectMapper.readTree("{\"customer_id\":1,\"items\":[{\"product_id\":1,\"quantity\":1,\"price\":1.23}]}")));
    }

    @Test
    void positiveMinimumRejectsPlaceholderEntityIds() throws Exception {
        ObjectNode itemSchema = objectMapper.createObjectNode();
        itemSchema.put("type", "object");
        itemSchema.putObject("properties")
            .putObject("product_id").put("type", "integer").put("minimum", 1);
        itemSchema.putArray("required").add("product_id");

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("supplier_id").put("type", "integer").put("minimum", 1);
        properties.putObject("items")
            .put("type", "array")
            .put("minItems", 1)
            .set("items", itemSchema);
        schema.putArray("required").add("supplier_id").add("items");

        ToolRegistry registry = new ToolRegistry(List.of(new TestTool(
            "create_purchase_order", "创建采购单", "生成采购草稿", AgentTool.ToolType.CREATE_ONLY, schema
        )));

        assertFalse(registry.hasAllRequiredParameters(
            "create_purchase_order",
            objectMapper.readTree("{\"supplier_id\":0,\"items\":[{\"product_id\":0}]}")
        ));
        assertTrue(registry.hasAllRequiredParameters(
            "create_purchase_order",
            objectMapper.readTree("{\"supplier_id\":1,\"items\":[{\"product_id\":2}]}")
        ));
    }

    @Test
    void executeToolRejectsUnknownAndInvalidSchemaFieldsBeforeCallingTool() throws Exception {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties").putObject("amount").put("type", "number").put("minimum", 0.01D);
        schema.putArray("required").add("amount");
        schema.put("additionalProperties", false);
        TestTool tool = new TestTool(
            "create_finance_record", "创建流水", "生成流水草稿", AgentTool.ToolType.CREATE_ONLY, schema
        );
        ToolRegistry registry = new ToolRegistry(List.of(tool));
        ToolContext context = new ToolContext(1L, 1L, null, "run", null, objectMapper);

        ToolResult invalid = registry.executeTool(
            tool.name(), context, objectMapper.readTree("{\"amount\":0,\"unexpected\":true}")
        ).orElseThrow();
        assertFalse(invalid.success());
        assertEquals(0, tool.executionCount);

        ToolResult valid = registry.executeTool(
            tool.name(), context, objectMapper.readTree("{\"amount\":1.2}")
        ).orElseThrow();
        assertTrue(valid.success());
        assertEquals(1, tool.executionCount);
    }

    private record TestTool(
        String name,
        String displayName,
        String description,
        ToolType type,
        JsonNode schema
    ) implements AgentTool {
        private static int executionCount;
        @Override
        public JsonNode parameterSchema() {
            return schema;
        }

        @Override
        public ToolResult execute(ToolContext ctx, JsonNode params) {
            executionCount++;
            return ToolResult.empty("test");
        }
    }
}
