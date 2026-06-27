package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 海报提示词生成工具（只读）。
 *
 * <p>根据商品信息与用户意图，输出优化后的完整海报生成提示词，包含风格、构图、文案建议。
 * 本工具不直接生成图片，仅产出供海报生成服务使用的 prompt 文本。
 */
@Component
public class GeneratePosterPromptTool extends ToolSupport {

    private final ProductRepository productRepository;

    public GeneratePosterPromptTool(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public String name() {
        return "generate_poster_prompt";
    }

    @Override
    public String displayName() {
        return "海报提示词生成";
    }

    @Override
    public String description() {
        return "根据商品信息与用户意图，生成优化的海报提示词（含风格/构图/文案建议），不直接生成图片";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        Long productId = paramLong(params, "product_id", null);
        String intent = paramString(params, "intent");
        Map<String, Object> input = mapOf(
            "product_id", productId == null ? "" : productId,
            "intent", intent == null ? "" : intent
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        if (productId == null) {
            emitToolFailed(ctx, name(), "缺少商品 ID");
            return ToolResult.failure(name(), "缺少商品 ID，无法生成海报提示词");
        }

        ProductEntity product = productRepository.findByIdAndOwnerUserId(productId, ownerUserId).orElse(null);
        if (product == null) {
            emitToolFailed(ctx, name(), "商品不存在");
            return ToolResult.failure(name(), "商品不存在，无法生成海报提示词");
        }

        String productName = StringUtils.hasText(product.getName()) ? product.getName() : "商品";
        String category = StringUtils.hasText(product.getCategory()) ? product.getCategory() : "通用";
        String salePrice = money(safeDouble(product.getSalePrice()));
        String userIntent = StringUtils.hasText(intent) ? intent : "提升商品吸引力，促进销售转化";

        String styleSuggestion = "现代简约风格，高对比配色，柔和光影，突出商品质感";
        String compositionSuggestion = "商品居中放大，四周留白，顶部主标题，底部价格与行动按钮";
        String copySuggestion = "主标题突出「" + productName + "」，副标题点明卖点，底部标注价格 " + salePrice;

        String optimizedPrompt = buildOptimizedPrompt(productName, category, salePrice, userIntent,
            styleSuggestion, compositionSuggestion, copySuggestion);

        audit.markReturned(1);
        emitToolCompleted(ctx, name(), "已为商品「" + productName + "」生成海报提示词", audit);

        V2AgentDtos.ResultBlockDto promptBlock = new V2AgentDtos.ResultBlockDto(
            "text",
            "海报提示词",
            toJsonNode(ctx, mapOf(
                "prompt", optimizedPrompt,
                "style", styleSuggestion,
                "composition", compositionSuggestion,
                "copy", copySuggestion
            ))
        );

        String answer = "已为商品「" + productName + "」生成海报提示词。用户意图：" + userIntent
            + "。可直接调用海报生成接口（posters:write）使用该提示词生成图片。";
        String toolSummary = "为商品 " + productName + " 生成海报提示词";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "product_id", productId,
            "product_name", productName,
            "category", category,
            "sale_price", salePrice,
            "user_intent", userIntent,
            "optimized_prompt", optimizedPrompt,
            "query_audit", audit.facts()
        ));
        return ToolResult.success(answer, List.of(promptBlock), toolFacts, toolSummary);
    }

    private String buildOptimizedPrompt(
        String productName,
        String category,
        String salePrice,
        String userIntent,
        String styleSuggestion,
        String compositionSuggestion,
        String copySuggestion
    ) {
        return "为商品「" + productName + "」（分类：" + category + "，售价 " + salePrice
            + "）生成一张营销海报。用户意图：" + userIntent + "。\n"
            + "风格：" + styleSuggestion + "。\n"
            + "构图：" + compositionSuggestion + "。\n"
            + "文案：" + copySuggestion + "。\n"
            + "整体氛围需契合「" + category + "」品类调性，画面清晰、信息层级分明，适合移动端展示。";
    }
}
