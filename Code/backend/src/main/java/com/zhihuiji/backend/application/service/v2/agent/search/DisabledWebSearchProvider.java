package com.zhihuiji.backend.application.service.v2.agent.search;

import org.springframework.stereotype.Component;

/**
 * 默认禁用的 Web 搜索 Provider。
 *
 * <p>未配置已授权搜索 Provider 时使用此实现：
 * <ul>
 *   <li>{@link #isAvailable()} 恒为 false</li>
 *   <li>{@link #search(WebSearchRequest)} 返回 {@link WebSearchProvider.Status#DEFERRED}
 *       状态，错误信息为“未配置搜索 Provider”</li>
 * </ul>
 * 不伪造搜索结果；调用方收到 DEFERRED 后应跳过搜索工具或返回结构化失败。
 */
@Component
public class DisabledWebSearchProvider implements WebSearchProvider {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public WebSearchResult search(WebSearchRequest request) {
        return WebSearchProvider.deferred(WebSearchProvider.NOT_CONFIGURED_MESSAGE);
    }
}
