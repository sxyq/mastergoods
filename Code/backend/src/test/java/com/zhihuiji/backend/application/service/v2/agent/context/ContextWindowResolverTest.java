// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
package com.zhihuiji.backend.application.service.v2.agent.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zhihuiji.backend.infrastructure.config.AgentLlmProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

/**
 * ContextWindowResolver 单元测试（plan 6.3）。
 *
 * <p>覆盖：配置按模型覆盖、unknown model 保守窗口、configuredMaximum 截断、
 * 保守回退判定。
 */
class ContextWindowResolverTest {

    private static final int DEFAULT_WINDOW = 272_000;

    private AgentLlmProperties properties(String model, String wireApi) {
        AgentLlmProperties props = new AgentLlmProperties();
        props.setModel(model);
        props.setWireApi(wireApi);
        return props;
    }

    @Test
    void glmFlashUses272kDefaultWindow() {
        ContextWindowResolver resolver = new ContextWindowResolver(
            properties("glm-5.3-flash", "chat_completions"), DEFAULT_WINDOW, Map.of()
        );

        ContextWindowResolver.Resolution resolution = resolver.resolveForCurrentWithSource();

        assertEquals(DEFAULT_WINDOW, resolution.tokens());
        assertEquals(ContextWindowResolver.Source.KNOWN_MODEL, resolution.source());
        assertFalse(resolver.isConservativeFallback(resolution));
    }

    @Test
    void validEnvironmentValueOverridesDefaultParser() {
        assertEquals(131_072, ContextWindowResolver.configuredMaximumFromEnvironment(" 131072 "));
        assertEquals(DEFAULT_WINDOW, ContextWindowResolver.configuredMaximumFromEnvironment(null));
    }

    @Test
    void springConstructorUsesValidConfiguredMaximum() {
        ContextWindowResolver resolver = new ContextWindowResolver(
            properties("glm-5.3-flash", "chat_completions"), "131072"
        );

        assertEquals(131_072, resolver.configuredMaximum());
        assertEquals(131_072, resolver.resolveForCurrent());
    }

    @Test
    void springPropertyTakesPrecedenceOverEnvironmentProperty() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                "test-window-properties",
                Map.of(
                    "agent.context.maximum-window", "131072",
                    "AGENT_CONTEXT_MAXIMUM_WINDOW", "65536"
                )
            ));
            context.registerBean(AgentLlmProperties.class);
            context.registerBean(ContextWindowResolver.class);
            context.refresh();

            assertEquals(131_072, context.getBean(ContextWindowResolver.class).configuredMaximum());
        }
    }

    @Test
    void invalidEnvironmentAndSpringValuesUseDefault() {
        assertEquals(DEFAULT_WINDOW, ContextWindowResolver.configuredMaximumFromEnvironment(""));
        assertEquals(DEFAULT_WINDOW, ContextWindowResolver.configuredMaximumFromEnvironment("not-a-number"));
        assertEquals(DEFAULT_WINDOW, ContextWindowResolver.configuredMaximumFromEnvironment("0"));
        assertEquals(DEFAULT_WINDOW, ContextWindowResolver.configuredMaximumFromEnvironment("-1"));
        assertEquals(DEFAULT_WINDOW, ContextWindowResolver.parseConfiguredMaximum("2147483648"));

        ContextWindowResolver resolver = new ContextWindowResolver(
            properties("glm-5.3-flash", "chat_completions"), "not-a-number"
        );
        assertEquals(DEFAULT_WINDOW, resolver.configuredMaximum());

        assertEquals(
            DEFAULT_WINDOW,
            new ContextWindowResolver(properties("glm-5.3-flash", "chat_completions"), 0).configuredMaximum()
        );
    }

    @Test
    void configuredSpringMaximumCapsKnownModelWithoutDegradingSource() {
        ContextWindowResolver resolver = new ContextWindowResolver(
            properties("glm-5.3-flash", "chat_completions"), 131_072, Map.of()
        );

        ContextWindowResolver.Resolution resolution = resolver.resolveForCurrentWithSource();

        assertEquals(131_072, resolution.tokens());
        assertEquals(ContextWindowResolver.Source.KNOWN_MODEL, resolution.source());
        assertFalse(resolver.isConservativeFallback(resolution));
    }

    @Test
    void unknownModelFallsBackToConservativeWindow() {
        ContextWindowResolver resolver = new ContextWindowResolver(
            properties("future-model", "anthropic"), DEFAULT_WINDOW, Map.of()
        );
        ContextWindowResolver.Resolution resolved = resolver.resolveWithSource(
            "provider-x", "future-model", "anthropic"
        );
        assertEquals(ContextWindowResolver.CONSERVATIVE_FALLBACK_WINDOW, resolved.tokens());
        assertEquals(ContextWindowResolver.Source.CONSERVATIVE_FALLBACK, resolved.source());
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
    void explicitSmallOverrideIsNotConservativeFallback() {
        ContextWindowResolver resolver = new ContextWindowResolver(
            properties("model-small", "chat_completions"), DEFAULT_WINDOW,
            Map.of("default:model-small:chat_completions", 8_192)
        );

        ContextWindowResolver.Resolution resolution = resolver.resolveWithSource(
            null, "model-small", "chat_completions"
        );

        assertEquals(8_192, resolution.tokens());
        assertEquals(ContextWindowResolver.Source.CONFIGURED_OVERRIDE, resolution.source());
        assertFalse(resolver.isConservativeFallback(resolution));
    }

    @Test
    void configuredMaximumIsFloorClampedToMinimum() {
        ContextWindowResolver resolver = new ContextWindowResolver(
            properties("model-c", "responses"), 128, Map.of("default:model-c:responses", 2000)
        );
        // configuredMaximum 至少 1024；override 2000 被截断到 1024。
        ContextWindowResolver.Resolution resolution = resolver.resolveWithSource(null, "model-c", null);
        assertEquals(1024, resolution.tokens());
        assertEquals(ContextWindowResolver.Source.CONFIGURED_OVERRIDE, resolution.source());
        assertFalse(resolver.isConservativeFallback(resolution));
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
            properties("model-d", "unknown-wire-api"), DEFAULT_WINDOW, Map.of()
        );
        ContextWindowResolver.Resolution resolved = resolver.resolveWithSource(
            null, "model-d", "unknown-wire-api"
        );
        assertEquals(ContextWindowResolver.CONSERVATIVE_FALLBACK_WINDOW, resolved.tokens());
        assertEquals(ContextWindowResolver.Source.CONSERVATIVE_FALLBACK, resolved.source());
        assertTrue(resolver.isConservativeFallback(resolved));
    }
}
