package com.zhihuiji.backend.application.service.v2.agent.search;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 在线搜索 URL 安全策略。
 *
 * <p>在抓取前校验目标 URL，拒绝环回地址、私有网段、云元数据地址、本机服务地址
 * 和 DNS 重绑定。每次重定向都应重新调用 {@link #isBlocked(String)} 校验。
 *
 * <p>只允许 HTTP/HTTPS 协议。
 */
final class WebSearchUrlSafety {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private static final Set<String> BLOCKED_HOSTNAMES = Set.of(
        "localhost",
        "ip6-localhost",
        "ip6-loopback",
        "metadata.google.internal",
        "metadata",
        "169.254.169.254",
        "metadata.azure.com",
        "169.254.169.253"
    );

    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$"
    );

    private static final Pattern IPV6_LOOPBACK = Pattern.compile(
        "^(?:0*:)*:?(?:0*1|0+)(?:%[a-zA-Z0-9]+)?$",
        Pattern.CASE_INSENSITIVE
    );

    private static final String LOCAL_SUFFIX = ".local";

    private WebSearchUrlSafety() {
    }

    /**
     * 判断 URL 是否被安全策略拒绝。
     *
     * <p>拒绝条件：
     * <ul>
     *   <li>非 HTTP/HTTPS 协议</li>
     *   <li>无法解析的 URL 或缺失主机</li>
     *   <li>主机名为 localhost、metadata 等本机或云元数据地址</li>
     *   <li>主机名为 .local 后缀</li>
     *   <li>主机名解析为环回地址、私有网段、链路本地地址</li>
     * </ul>
     *
     * @param rawUrl 待校验的 URL
     * @return 拒绝返回 true；URL 为 null 也返回 true
     */
    static boolean isBlocked(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return true;
        }
        URI uri;
        try {
            uri = new URI(rawUrl);
        } catch (URISyntaxException ex) {
            return true;
        }
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            return true;
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return true;
        }
        String normalizedHost = host.toLowerCase();
        if (BLOCKED_HOSTNAMES.contains(normalizedHost)) {
            return true;
        }
        if (normalizedHost.endsWith(LOCAL_SUFFIX)) {
            return true;
        }
        if (isBlockedIpv4(normalizedHost) || isBlockedIpv6(normalizedHost)) {
            return true;
        }
        // Resolve hostname and check each returned address. This guards against
        // DNS rebinding where the hostname is public but resolves to a private IP.
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isBlockedAddress(address)) {
                    return true;
                }
            }
        } catch (UnknownHostException ex) {
            // Unknown host is treated as blocked to avoid leaking reachability
            // probes through the search surface.
            return true;
        }
        return false;
    }

    private static boolean isBlockedIpv4(String host) {
        var matcher = IPV4_PATTERN.matcher(host);
        if (!matcher.matches()) {
            return false;
        }
        int[] octets = new int[4];
        for (int i = 0; i < 4; i++) {
            int value = Integer.parseInt(matcher.group(i + 1));
            if (value < 0 || value > 255) {
                return true; // malformed, treat as blocked
            }
            octets[i] = value;
        }
        // Loopback 127.0.0.0/8
        if (octets[0] == 127) {
            return true;
        }
        // Private 10.0.0.0/8
        if (octets[0] == 10) {
            return true;
        }
        // Private 172.16.0.0/12
        if (octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31) {
            return true;
        }
        // Private 192.168.0.0/16
        if (octets[0] == 192 && octets[1] == 168) {
            return true;
        }
        // Link-local 169.254.0.0/16 (includes cloud metadata 169.254.169.254)
        if (octets[0] == 169 && octets[1] == 254) {
            return true;
        }
        // Carrier-grade NAT 100.64.0.0/10
        if (octets[0] == 100 && octets[1] >= 64 && octets[1] <= 127) {
            return true;
        }
        // IETF reserved 0.0.0.0/8
        if (octets[0] == 0) {
            return true;
        }
        return false;
    }

    private static boolean isBlockedIpv6(String host) {
        if (!host.contains(":")) {
            return false;
        }
        // Strip zone id if present.
        String trimmed = host;
        int percentIndex = trimmed.indexOf('%');
        if (percentIndex >= 0) {
            trimmed = trimmed.substring(0, percentIndex);
        }
        if (IPV6_LOOPBACK.matcher(trimmed).matches()) {
            return true;
        }
        // fc00::/7 unique local, fe80::/10 link-local
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("fc") || lower.startsWith("fd")
            || lower.startsWith("fe8") || lower.startsWith("fe9")
            || lower.startsWith("fea") || lower.startsWith("feb")) {
            return true;
        }
        return false;
    }

    private static boolean isBlockedAddress(InetAddress address) {
        return address.isLoopbackAddress()
            || address.isSiteLocalAddress()
            || address.isLinkLocalAddress()
            || address.isAnyLocalAddress();
    }
}
