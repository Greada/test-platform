package com.testplatform.util;

import java.net.*;
import java.util.Set;

/**
 * @author greada
 * @version 1.0.0
 */
public final class UrlValidator {
    private UrlValidator() {}

    public static void validate(String url, boolean allowInternal) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("请求 URL 不能为空");
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URL 格式非法: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null || !Set.of("http", "https").contains(scheme.toLowerCase())) {
            throw new IllegalArgumentException("仅允许 http/https 协议");
        }

        String host = uri.getHost();
        if (host == null) {
            throw new IllegalArgumentException("URL 缺少主机名");
        }
        if (!allowInternal && isInternalAddress(host)) {
            throw new IllegalArgumentException("目标为内网/保留地址，禁止访问");
        }
    }

    public static boolean isInternalAddress(String host) {
        // ① 字面 localhost / *.localhost（如 myapp.localhost 现代浏览器解析到 127.0.0.1）
        if ("localhost".equalsIgnoreCase(host) || host.toLowerCase().endsWith(".localhost")) {
            return true;
        }
        // ② IPv6 字面量：含 ":" 就按 IPv6 处理
        //    "::1"、"::"、"fe80::"（链路本地）、"fc00::/7"（唯一本地）都算内网
        //    实现：Inet6Address addr = InetAddress.getByName(host) 判
        // isLoopback/isLinkLocal/isSiteLocal
        //    简化版（推荐，够用）：host 含 ":" 就 return true（IPv6 场景整个拒绝——内网测试平台场景下公网 IPv6 目标罕见，宁严勿松，注释说明这个取舍）
        if (host.contains(":")) {
            return true;
        }
        // ③ IPv4 字面量：InetAddress.getByName(host) 拿 InetAddress，用它的判定方法：
        //    addr.isLoopbackAddress()        → 127.0.0.0/8（127.x.x.x 全部，不只是 127.0.0.1）
        //    addr.isLinkLocalAddress()       → 169.254.0.0/16（含云元数据 169.254.169.254）
        //    addr.isSiteLocalAddress()       → 10/8、172.16/12、192.168/16
        //    addr.isAnyLocalAddress()        → 0.0.0.0
        //    host.startsWith("192.168.") 等手写段判断【不要】——用 InetAddress 的方法，标准且不易漏
        if (host.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) {
            InetAddress address;
            try {
                address = InetAddress.getByName(host);
            } catch (UnknownHostException e) {
                return true;
            }
            return address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isAnyLocalAddress()
                    || address.isSiteLocalAddress();
        }
        // ④ 域名：不拦（DNS 解析问题遗留，注释声明）
        return false;
    }
}
