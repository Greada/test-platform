package com.testplatform.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** UrlValidator unit tests (B1.5 SSRF 防护) */
class UrlValidatorTest {

    @Test
    @DisplayName("[URL-01] 公网 http/https 放行")
    void validate_publicUrls_pass() {
        assertDoesNotThrow(() -> UrlValidator.validate("http://example.com/api", false));
        assertDoesNotThrow(() -> UrlValidator.validate("https://httpbin.org/get", false));
        assertDoesNotThrow(
                () -> UrlValidator.validate("HTTPS://EXAMPLE.COM/x".toLowerCase(), false));
    }

    @Test
    @DisplayName("[URL-02] 非 http/https 协议拒绝（scheme 大小写不敏感：大写 HTTP 合法放行）")
    void validate_nonHttpScheme_rejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlValidator.validate("ftp://example.com/file", false));
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlValidator.validate("file:///etc/passwd", false));
        // scheme 大小写不敏感（RFC 3986）：大写写法合法，应放行
        assertDoesNotThrow(() -> UrlValidator.validate("HTTP://example.com", false));
    }

    @Test
    @DisplayName("[URL-03] allowInternal=true 时内网地址放行（本地自测场景）")
    void validate_internalAllowed_pass() {
        assertDoesNotThrow(() -> UrlValidator.validate("http://localhost:8080/api/testcases", true));
        assertDoesNotThrow(() -> UrlValidator.validate("http://127.0.0.1:8080/", true));
        assertDoesNotThrow(() -> UrlValidator.validate("http://192.168.1.5:3000/", true));
        assertDoesNotThrow(() -> UrlValidator.validate("http://10.0.0.8/api", true));
    }

    @Test
    @DisplayName("[URL-04] allowInternal=false 时各类内网/保留地址拒绝")
    void validate_internalDenied_rejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlValidator.validate("http://localhost:8080/api/testcases", false));
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlValidator.validate("http://127.0.0.1:8080/", false));
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlValidator.validate("http://127.8.8.8/", false));
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlValidator.validate("http://10.0.0.8/api", false));
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlValidator.validate("http://172.16.0.9/", false));
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlValidator.validate("http://172.31.255.1/", false));
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlValidator.validate("http://192.168.1.5:3000/", false));
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlValidator.validate("http://169.254.169.254/latest/meta-data", false));
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlValidator.validate("http://0.0.0.0/", false));
    }

    @Test
    @DisplayName("[URL-05] IPv6 目标拒绝（宁严勿松取舍）")
    void validate_ipv6_rejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlValidator.validate("http://[::1]:8080/", false));
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlValidator.validate("http://[fe80::1]/", false));
    }

    @Test
    @DisplayName("[URL-06] 非内网 IPv4 公网地址放行")
    void validate_publicIp_pass() {
        assertDoesNotThrow(() -> UrlValidator.validate("http://8.8.8.8/dns", false));
        assertDoesNotThrow(() -> UrlValidator.validate("http://172.32.0.1/", false));
    }

    @Test
    @DisplayName("[URL-07] 空值/格式非法/缺主机名拒绝")
    void validate_malformed_rejected() {
        assertThrows(IllegalArgumentException.class, () -> UrlValidator.validate(null, false));
        assertThrows(IllegalArgumentException.class, () -> UrlValidator.validate("", false));
        assertThrows(IllegalArgumentException.class, () -> UrlValidator.validate("   ", false));
        assertThrows(IllegalArgumentException.class, () -> UrlValidator.validate("http://", false));
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlValidator.validate("http://a b.com/", false));
    }
}
