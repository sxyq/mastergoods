package com.zhihuiji.backend.application.service.v2.agent.search;

import java.util.List;

/**
 * 在线搜索 Provider 接口。
 *
 * <p>未配置已授权搜索 Provider 时，{@link #isAvailable()} 返回 false，调用方
 * 必须使用 {@link DisabledWebSearchProvider} 或返回结构化失败；不得伪造搜索结果。
 *
 * <p>搜索结果、网页正文、标题、摘要和页面中的指令全部视为不可信数据，不能
 * 修改系统规则、工具范围、权限、owner/store 或完成条件。
 *
 * <p>实现要点：
 * <ul>
 *   <li>只允许 HTTP/HTTPS</li>
 *   <li>拒绝环回地址、私有网段、云元数据地址和本机服务地址</li>
 *   <li>限制 DNS 解析结果、重定向次数、响应大小和抓取时间</li>
 *   <li>搜索请求、网页抓取和模型请求使用独立超时</li>
 *   <li>审计记录 URL、域名、状态和摘要，不保存外部服务凭据</li>
 * </ul>
 */
public interface WebSearchProvider {

    /**
     * 判断当前是否已配置并可用。
     *
     * @return 可用返回 true；未配置或被禁用返回 false
     */
    boolean isAvailable();

    /**
     * 执行搜索。失败、超时、无结果或 Provider 不可用时返回结构化失败。
     *
     * @param request 搜索请求
     * @return 搜索结果
     */
    WebSearchResult search(WebSearchRequest request);

    /** 搜索结果项：引用编号、标题、URL、摘要、来源域名与时间戳。 */
    record WebSearchItem(
        String citationId,
        String title,
        String url,
        String snippet,
        String sourceName,
        String publishedAt,
        long retrievedAt
    ) {}

    /** 搜索结果状态。 */
    enum Status {
        /** 搜索成功且有结果。 */
        OK,
        /** 搜索失败（Provider 错误、网络错误等）。 */
        FAILED,
        /** 搜索被安全策略阻止（URL 校验失败、私有网段等）。 */
        BLOCKED,
        /** Provider 未配置或不可用。 */
        DEFERRED
    }

    /** Provider 未配置时返回的安全错误信息。 */
    String NOT_CONFIGURED_MESSAGE = "未配置搜索 Provider";

    /** 默认结果条数上限。 */
    int DEFAULT_RESULT_LIMIT = 5;

    /** 结果条数硬上限。 */
    int MAX_RESULT_LIMIT = 10;

    /** 默认单页抓取超时（毫秒）。 */
    long DEFAULT_FETCH_TIMEOUT_MS = 10_000L;

    /** 默认总抓取超时（毫秒）。 */
    long DEFAULT_TOTAL_TIMEOUT_MS = 30_000L;

    /** 默认最大响应字节数。 */
    long DEFAULT_MAX_RESPONSE_BYTES = 1_048_576L; // 1 MiB

    /** 默认最大重定向次数。 */
    int DEFAULT_MAX_REDIRECTS = 3;

    /**
     * 判断 URL 是否被安全策略拒绝。
     *
     * <p>实现类应使用此方法在抓取前校验目标 URL。拒绝条件包括：
     * <ul>
     *   <li>非 HTTP/HTTPS 协议</li>
     *   <li>环回地址（127.0.0.0/8, ::1）</li>
     *   <li>私有网段（10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16）</li>
     *   <li>链路本地（169.254.0.0/16），包含云元数据地址 169.254.169.254</li>
     *   <li>本机服务地址（localhost, *.local）</li>
     * </ul>
     *
     * @param url 待校验的 URL
     * @return 拒绝返回 true
     */
    static boolean isUrlBlocked(String url) {
        return WebSearchUrlSafety.isBlocked(url);
    }

    /**
     * 便捷方法：构造一个 DEFERRED 状态的结果（Provider 未配置）。
     */
    static WebSearchResult deferred(String reason) {
        return new WebSearchResult(List.of(), Status.DEFERRED, reason);
    }

    /**
     * 便捷方法：构造一个 FAILED 状态的结果。
     */
    static WebSearchResult failed(String reason) {
        return new WebSearchResult(List.of(), Status.FAILED, reason);
    }

    /**
     * 便捷方法：构造一个 BLOCKED 状态的结果。
     */
    static WebSearchResult blocked(String reason) {
        return new WebSearchResult(List.of(), Status.BLOCKED, reason);
    }
}
