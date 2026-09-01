package com.testplatform.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * @author admin
 * @version 1.0.0
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter(CorsProperties props) {
        List<String> origins = props.getAllowedOriginsAsList();
        if (origins.isEmpty()) {
            throw new IllegalStateException("cors.allowed-origins 未配置，拒绝启动：空白名单会静默拒绝所有跨域请求");
        }
        CorsConfiguration config = new CorsConfiguration();
        origins.forEach(config::addAllowedOrigin);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
