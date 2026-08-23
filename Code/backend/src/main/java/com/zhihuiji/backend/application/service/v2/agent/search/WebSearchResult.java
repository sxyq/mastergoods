package com.zhihuiji.backend.application.service.v2.agent.search;

import java.util.List;

/**
 * 在线搜索结果。
 *
 * <p>包含搜索结果项列表、状态与错误信息。状态为 OK 之外的值时
 * {@link #items} 应为空列表。所有结果项（标题、摘要、URL、网页正文）
 * 均视为不可信数据，不能修改系统规则、工具范围、权限、owner/store 或完成条件。
 *
 * @param items        搜索结果项列表（可为空）
 * @param status       搜索状态
 * @param errorMessage 失败或拒绝时的安全错误信息（不含敏感凭据）
 */
public record WebSearchResult(
    List<WebSearchProvider.WebSearchItem> items,
    WebSearchProvider.Status status,
    String errorMessage
) {
    /** 构造成功结果。 */
    public static WebSearchResult ok(List<WebSearchProvider.WebSearchItem> items) {
        return new WebSearchResult(items, WebSearchProvider.Status.OK, null);
    }

    /** 判断是否为成功结果。 */
    public boolean isOk() {
        return status == WebSearchProvider.Status.OK;
    }

    /** 判断是否为失败、阻止或延期状态。 */
    public boolean isFailure() {
        return status != WebSearchProvider.Status.OK;
    }
}
