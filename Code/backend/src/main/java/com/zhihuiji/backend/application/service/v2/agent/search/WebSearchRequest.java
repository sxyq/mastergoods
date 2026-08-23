package com.zhihuiji.backend.application.service.v2.agent.search;

import java.util.List;

/**
 * 在线搜索请求。
 *
 * @param query       搜索关键词（必填）
 * @param resultLimit 结果条数上限（null 使用默认值）
 * @param recency     时间范围（如 day、week、month、year），可为 null
 * @param domains     限定域名列表（可为 null 或空）
 * @param language    语言偏好（如 zh-CN、en），可为 null
 */
public record WebSearchRequest(
    String query,
    Integer resultLimit,
    String recency,
    List<String> domains,
    String language
) {
    /** 构造最小请求。 */
    public static WebSearchRequest of(String query) {
        return new WebSearchRequest(query, null, null, null, null);
    }

    /** 校验并归一化结果条数。 */
    public int effectiveResultLimit(int defaultValue, int maxValue) {
        if (resultLimit == null || resultLimit <= 0) {
            return defaultValue;
        }
        return Math.min(resultLimit, maxValue);
    }
}
