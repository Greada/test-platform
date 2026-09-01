package com.testplatform.config;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author greada
 * @version 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {
    private String allowedOrigins = "";

    public List<String> getAllowedOriginsAsList() {
        if (allowedOrigins == null || allowedOrigins.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
