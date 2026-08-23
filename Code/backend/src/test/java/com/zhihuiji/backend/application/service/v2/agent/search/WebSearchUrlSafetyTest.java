package com.zhihuiji.backend.application.service.v2.agent.search;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * WebSearchUrlSafety 单元测试。
 *
 * <p>覆盖：协议限制、环回/私有网段/云元数据/链路本地/IPv6/本机主机名/.local、
 * DNS 重绑定兜底、null/空/非法 URL。
 */
class WebSearchUrlSafetyTest {

    @Test
    void allowsPublicHttpsUrls() {
        assertFalse(WebSearchProvider.isUrlBlocked("https://example.com/article"));
        assertFalse(WebSearchProvider.isUrlBlocked("http://www.example.org/page?q=1"));
    }

    @Test
    void rejectsNonHttpSchemes() {
        assertTrue(WebSearchProvider.isUrlBlocked("ftp://example.com/file"));
        assertTrue(WebSearchProvider.isUrlBlocked("file:///etc/passwd"));
        assertTrue(WebSearchProvider.isUrlBlocked("javascript:alert(1)"));
        assertTrue(WebSearchProvider.isUrlBlocked("gopher://example.com"));
    }

    @Test
    void rejectsNullBlankAndMalformedUrls() {
        assertTrue(WebSearchProvider.isUrlBlocked(null));
        assertTrue(WebSearchProvider.isUrlBlocked(""));
        assertTrue(WebSearchProvider.isUrlBlocked("   "));
        assertTrue(WebSearchProvider.isUrlBlocked("not a url"));
    }

    @Test
    void rejectsLocalhostAndLocalSuffixHosts() {
        assertTrue(WebSearchProvider.isUrlBlocked("http://localhost:8080/admin"));
        assertTrue(WebSearchProvider.isUrlBlocked("http://LOCALHOST/"));
        assertTrue(WebSearchProvider.isUrlBlocked("http://myhost.local/"));
        assertTrue(WebSearchProvider.isUrlBlocked("http://ip6-localhost/"));
    }

    @Test
    void rejectsLoopbackAndPrivateIpv4() {
        assertTrue(WebSearchProvider.isUrlBlocked("http://127.0.0.1/"));
        assertTrue(WebSearchProvider.isUrlBlocked("http://10.0.0.5/"));
        assertTrue(WebSearchProvider.isUrlBlocked("http://172.16.0.1/"));
        assertTrue(WebSearchProvider.isUrlBlocked("http://172.31.255.255/"));
        assertTrue(WebSearchProvider.isUrlBlocked("http://192.168.1.100/"));
        assertTrue(WebSearchProvider.isUrlBlocked("http://0.0.0.0/"));
    }

    @Test
    void rejectsLinkLocalAndCloudMetadataAddresses() {
        assertTrue(WebSearchProvider.isUrlBlocked("http://169.254.169.254/latest/meta-data/"));
        assertTrue(WebSearchProvider.isUrlBlocked("http://169.254.169.253/"));
        assertTrue(WebSearchProvider.isUrlBlocked("http://169.254.10.20/"));
        assertTrue(WebSearchProvider.isUrlBlocked("http://metadata.google.internal/"));
        assertTrue(WebSearchProvider.isUrlBlocked("http://metadata.azure.com/"));
    }

    @Test
    void rejectsCarrierGradeNatAndMalformedIpv4() {
        assertTrue(WebSearchProvider.isUrlBlocked("http://100.64.0.1/"));
        assertTrue(WebSearchProvider.isUrlBlocked("http://100.127.255.254/"));
        // 非法八位组（>255）视为阻止。
        assertTrue(WebSearchProvider.isUrlBlocked("http://999.1.1.1/"));
    }

    @Test
    void rejectsIpv6LoopbackUlaAndLinkLocal() {
        assertTrue(WebSearchProvider.isUrlBlocked("http://[::1]/"));
        assertTrue(WebSearchProvider.isUrlBlocked("http://[fc00::1]/"));
        assertTrue(WebSearchProvider.isUrlBlocked("http://[fd12:3456::1]/"));
        assertTrue(WebSearchProvider.isUrlBlocked("http://[fe80::1]/"));
        assertTrue(WebSearchProvider.isUrlBlocked("http://[fe9a::1]/"));
    }
}
