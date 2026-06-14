package com.testplatform.config;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author admin
 * @version 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "ai.agnes")
@Data
public class AiConfig {
    private String apiKey;
    private String baseUrl;
    private String model = "agnes-2.0-flash";
}
