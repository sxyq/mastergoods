package com.zhihuiji.backend.application.service.v2.agent.context;

import com.zhihuiji.backend.infrastructure.config.AgentLlmProperties;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 上下文窗口解析器（plan 6.3）。
 *
 * <p>根据 provider、model、wire API 返回模型的上下文窗口大小。Provider、model
 * 或 wire API 的窗口无法确认时，使用配置中的较小安全上限，并提高安全余量；在
 * 确认真实窗口前不得按最大可能窗口发送完整历史。
 *
 * <p>窗口大小可按 {@code agent.context.window-overrides.<provider>:<model>}=<tokens>
 * 形式在配置文件中覆盖。配置必须可按模型覆盖，不能把某个模型的窗口写成全局固定值。
 */
@Component
public class ContextWindowResolver {
    public enum Source {
        CONFIGURED_OVERRIDE,
        KNOWN_MODEL,
        CONSERVATIVE_FALLBACK
    }

    public record Resolution(int tokens, Source source) {}

    /**
     * 保守预算：在 Provider 真实窗口未知时使用。该值小于主流模型典型窗口，
     * 确保未确认窗口前不会盲目发送完整历史。
     */
    public static final int CONSERVATIVE_FALLBACK_WINDOW = 8192;

    /**
     * 配置中已知的较保守上限。即使配置文件声明了更大的窗口，也通过
     * {@code agent.context.maximum-window} 限制全局最大值。
     */
    public static final int CONFIGURED_MAXIMUM_DEFAULT = 32_768;

    /**
     * 按模型已知的窗口大小。生产部署应通过配置覆盖，这里只保留少量稳定的
     * 公开值，避免在 Provider 升级时静默使用过时窗口。
     */
    private static final Map<String, Integer> KNOWN_MODEL_WINDOWS = Map.of(
        // 显式留空：真实窗口由配置或 Provider 文档提供。这里不写死任何值，
        // 防止 Provider 升级后仍按旧窗口发送历史导致超限。
    );

    private final AgentLlmProperties properties;
    private final int configuredMaximum;
    private final Map<String, Integer> overrides;

    /**
     * 生产构造器：从配置读取模型与窗口；窗口覆盖与全局上限使用默认值。
     */
    @Autowired
    public ContextWindowResolver(AgentLlmProperties properties) {
        this(properties, CONFIGURED_MAXIMUM_DEFAULT, Map.of());
    }

    public ContextWindowResolver(AgentLlmProperties properties, int configuredMaximum, Map<String, Integer> overrides) {
        this.properties = properties;
        this.configuredMaximum = Math.max(1024, configuredMaximum);
        this.overrides = overrides == null ? Map.of() : Map.copyOf(overrides);
    }

    /**
     * 解析模型上下文窗口大小。
     *
     * @param provider provider 标识（可为空，回退到配置）
     * @param model    模型名（可为空，回退到配置）
     * @param wireApi  wire API（anthropic / chat_completions / responses）
     * @return 模型上下文窗口 token 数；未知时返回 {@link #CONSERVATIVE_FALLBACK_WINDOW}
     */
    public int resolve(String provider, String model, String wireApi) {
        return resolveWithSource(provider, model, wireApi).tokens();
    }

    /**
     * Resolves a window and keeps the provenance for observability callers.
     * The source is intentionally explicit so an administrator can distinguish
     * a configured value from the conservative fallback used for unknown models.
     */
    public Resolution resolveWithSource(String provider, String model, String wireApi) {
        String key = buildOverrideKey(provider, model, wireApi);
        if (key != null) {
            Integer overridden = overrides.get(key);
            if (overridden != null && overridden > 0) {
                return new Resolution(Math.min(overridden, configuredMaximum), Source.CONFIGURED_OVERRIDE);
            }
        }
        if (StringUtils.hasText(model)) {
            Integer known = KNOWN_MODEL_WINDOWS.get(model);
            if (known != null && known > 0) {
                return new Resolution(Math.min(known, configuredMaximum), Source.KNOWN_MODEL);
            }
        }
        // Provider、model 或 wire API 的窗口无法确认时使用保守预算，并交由
        // TokenEstimator / ContextBuilder 提高安全余量。
        return new Resolution(Math.min(CONSERVATIVE_FALLBACK_WINDOW, configuredMaximum), Source.CONSERVATIVE_FALLBACK);
    }

    /**
     * 解析当前配置 provider 的窗口大小（便捷方法）。
     */
    public int resolveForCurrent() {
        return resolveForCurrentWithSource().tokens();
    }

    public Resolution resolveForCurrentWithSource() {
        return resolveWithSource(
            properties == null ? null : properties.getProvider(),
            properties == null ? null : properties.getModel(),
            properties == null ? null : properties.getWireApi()
        );
    }

    /**
     * 是否使用的是降级保守窗口（即真实窗口未知）。
     *
     * <p>调用方应在降级时提高安全余量，并优先压缩已完成历史。
     */
    public boolean isConservativeFallback(int resolvedWindow) {
        return resolvedWindow <= CONSERVATIVE_FALLBACK_WINDOW;
    }

    private String buildOverrideKey(String provider, String model, String wireApi) {
        String providerText = StringUtils.hasText(provider)
            ? provider
            : (properties == null ? null : properties.getProvider());
        String modelText = StringUtils.hasText(model) ? model : (properties == null ? null : properties.getModel());
        if (!StringUtils.hasText(modelText)) {
            return null;
        }
        String wireApiText = StringUtils.hasText(wireApi) ? wireApi
            : (properties == null ? "" : properties.getWireApi());
        return (StringUtils.hasText(providerText) ? providerText : "default")
            + ":" + modelText
            + ":" + (StringUtils.hasText(wireApiText) ? wireApiText : "default");
    }
}
