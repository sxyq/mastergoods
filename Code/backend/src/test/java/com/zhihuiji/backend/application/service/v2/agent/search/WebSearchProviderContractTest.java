package com.zhihuiji.backend.application.service.v2.agent.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zhihuiji.backend.application.service.v2.agent.search.WebSearchProvider.Status;
import com.zhihuiji.backend.application.service.v2.agent.search.WebSearchProvider.WebSearchItem;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 在线搜索契约测试（计划 9 节）。
 *
 * <p>覆盖：未配置 Provider 返回 DEFERRED 不伪造结果、结果状态机、
 * 引用编号与结果一一对应、URL 安全委托、工厂方法。
 */
class WebSearchProviderContractTest {

    @Test
    void disabledProviderReturnsDeferredWithoutFabricatingResults() {
        DisabledWebSearchProvider provider = new DisabledWebSearchProvider();

        assertFalse(provider.isAvailable());
        WebSearchResult result = provider.search(new WebSearchRequest("苹果手机", 5, null, null, null));

        assertEquals(Status.DEFERRED, result.status());
        assertTrue(result.items().isEmpty(), "未配置 Provider 不得伪造搜索结果");
        assertTrue(result.errorMessage().contains("未配置"));
    }

    @Test
    void resultFactoriesMapToStatuses() {
        assertEquals(Status.DEFERRED, WebSearchProvider.deferred("未配置").status());
        assertEquals(Status.FAILED, WebSearchProvider.failed("网络错误").status());
        assertEquals(Status.BLOCKED, WebSearchProvider.blocked("URL 被安全策略阻止").status());
    }

    @Test
    void citationIdsCorrespondOneToOneToResults() {
        WebSearchResult result = new WebSearchResult(List.of(
            new WebSearchItem("[1]", "标题一", "https://example.com/a", "摘要一", "example.com", null, 1L),
            new WebSearchItem("[2]", "标题二", "https://example.com/b", "摘要二", "example.com", null, 2L)
        ), Status.OK, "ok");

        assertEquals(2, result.items().size());
        assertEquals("[1]", result.items().get(0).citationId());
        assertEquals("[2]", result.items().get(1).citationId());
        assertTrue(result.items().stream().allMatch(item -> item.url().startsWith("https://")));
        assertTrue(result.items().stream().allMatch(item -> item.sourceName() != null && !item.sourceName().isBlank()));
        assertTrue(result.items().stream().allMatch(item -> item.retrievedAt() > 0L));
    }

    @Test
    void urlSafetyDelegatesToSharedGuard() {
        assertTrue(WebSearchProvider.isUrlBlocked("http://127.0.0.1/x"));
        assertTrue(WebSearchProvider.isUrlBlocked("http://169.254.169.254/latest/meta-data/"));
        assertTrue(WebSearchProvider.isUrlBlocked("http://localhost/"));
        assertFalse(WebSearchProvider.isUrlBlocked("https://example.com/"));
    }

    @Test
    void requestCarriesQueryLimitRecencyAndDomains() {
        WebSearchRequest request = new WebSearchRequest(
            "进货价格", 8, "d7", List.of("jd.com", "1688.com"), "zh"
        );
        assertEquals("进货价格", request.query());
        assertEquals(8, request.resultLimit());
        assertEquals("d7", request.recency());
        assertEquals(List.of("jd.com", "1688.com"), request.domains());
        assertEquals("zh", request.language());
        assertNotNull(request);
    }

    @Test
    void requestDefaultsToSensibleLimits() {
        WebSearchRequest request = new WebSearchRequest("默认参数", null, null, null, null);
        assertEquals(WebSearchProvider.DEFAULT_RESULT_LIMIT,
            request.effectiveResultLimit(WebSearchProvider.DEFAULT_RESULT_LIMIT, WebSearchProvider.MAX_RESULT_LIMIT));
        // 超限时被截断到硬上限。
        WebSearchRequest large = new WebSearchRequest("大 limit", 999, null, null, null);
        assertEquals(WebSearchProvider.MAX_RESULT_LIMIT,
            large.effectiveResultLimit(WebSearchProvider.DEFAULT_RESULT_LIMIT, WebSearchProvider.MAX_RESULT_LIMIT));
    }
}
