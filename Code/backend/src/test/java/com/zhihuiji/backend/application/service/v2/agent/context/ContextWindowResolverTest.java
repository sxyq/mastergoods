package com.zhihuiji.backend.application.service.v2.agent.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zhihuiji.backend.infrastructure.config.AgentLlmProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * ContextWindowResolver 单元测试（plan 6.3）。
 *
 * <p>覆盖：配置按模型覆盖、unknown model 保守窗口、configuredMaximum 截断、
 * 保守回退判定。
 */
class ContextWindowResolverTest {

    private AgentLlmProperties properties(String model, String wireApi) {
        AgentLlmProperties props = new AgentLlmProperties();
        props.setModel(model);
        props.setWireApi(wireApi);
        return props;
    }

    @Test
    void unknownModelFallsBackToConservativeWindow() {
        ContextWindowResolver resolver = new ContextWindowResolver(properties("future-model", "anthropic"));
        int resolved = resolver.resolve("provider-x", "future-model", "anthropic");
        assertEquals(ContextWindowResolver.CONSERVATIVE_FALLBACK_WINDOW, resolved);
        assertTrue(resolver.isConservativeFallback(resolved));
    }

    @Test
    void knownModelWindowComesFromOverrideAndIsNotGlobal() {
        // provider-a:model-a 的窗口只影响该组合，不影响其他模型。
        Map<String, Integer> overrides = Map.of("provider-a:model-a:anthropic", 64_000);
        ContextWindowResolver resolver = new ContextWindowResolver(
            properties("model-a", "anthropic"), 131_072, overrides
        );
        assertEquals(64_000, resolver.resolve("provider-a", "model-a", "anthropic"));
        // 相同模型但不同 provider 不能继承 provider-a 的覆盖。
        assertEquals(
            ContextWindowResolver.CONSERVATIVE_FALLBACK_WINDOW,
            resolver.resolve("provider-b", "model-a", "anthropic")
        );
        // 相同 provider 相同模型但不同 wire API 不能继承。
        assertEquals(
            ContextWindowResolver.CONSERVATIVE_FALLBACK_WINDOW,
            resolver.resolve("provider-a", "model-a", "chat_completions")
        );
    }

    @Test
    void overrideIsClampedToConfiguredMaximum() {
        Map<String, Integer> overrides = Map.of("default:model-b:default", 512_000);
        ContextWindowResolver resolver = new ContextWindowResolver(
            properties("model-b", null), 32_768, overrides
        );
        int resolved = resolver.resolve(null, "model-b", null);
        assertEquals(32_768, resolved);
        assertFalse(resolver.isConservativeFallback(resolved));
    }

    @Test
    void configuredMaximumIsFloorClampedToMinimum() {
        ContextWindowResolver resolver = new ContextWindowResolver(
            properties("model-c", "responses"), 128, Map.of("default:model-c:default", 2000)
        );
        // configuredMaximum 至少 1024；override 2000 被截断到 1024。
        assertEquals(1024, resolver.resolve(null, "model-c", null));
    }

    @Test
    void resolveForCurrentUsesConfiguredModel() {
        ContextWindowResolver resolver = new ContextWindowResolver(
            properties("current-model", "chat_completions"),
            32_768,
            Map.of("default:current-model:chat_completions", 16_000)
        );
        assertEquals(16_000, resolver.resolveForCurrent());
    }

    @Test
    void conservativeFallbackRaisedForUnknownWireApi() {
        ContextWindowResolver resolver = new ContextWindowResolver(
            properties("model-d", "unknown-wire-api")
        );
        int resolved = resolver.resolve(null, "model-d", "unknown-wire-api");
        assertEquals(ContextWindowResolver.CONSERVATIVE_FALLBACK_WINDOW, resolved);
        assertTrue(resolver.isConservativeFallback(resolved));
    }
}
