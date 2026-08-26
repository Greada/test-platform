package com.testplatform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.common.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * @author greada
 * @version 1.0.0
 */
@Component
public class CiAuthFilter extends OncePerRequestFilter {
    private static final String CI_TOKEN_HEADER = "X-CI-Token";
    private final String ciApiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CiAuthFilter(@Value("${ci.api-key:}") String ciApiKey) {
        this.ciApiKey = ciApiKey;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String presented = request.getHeader(CI_TOKEN_HEADER);
        if (!isValid(presented)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter()
                    .write(objectMapper.writeValueAsString(Result.error(401, "无效的 CI token")));
            return;
        }

        // 校验通过：以 CI 机器身份进入 SecurityContext，授权层据此放行 hasRole("CI")
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "ci-publisher", null, List.of(new SimpleGrantedAuthority("ROLE_CI")));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean isValid(String presented) {
        if (presented == null || presented.isEmpty()) {
            return false;
        }
        if (ciApiKey == null || ciApiKey.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(
                ciApiKey.getBytes(StandardCharsets.UTF_8),
                presented.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        boolean isCiPost =
                "POST".equalsIgnoreCase(request.getMethod())
                        && "/api/ci/builds".equals(request.getServletPath());
        return !isCiPost;
    }
}
