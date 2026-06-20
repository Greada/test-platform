package com.testplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.config.AiConfig;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author admin
 * @version 1.0.0
 */
@Service
public class AiService {
    private final RestTemplate restTemplate;
    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper;

    public AiService(RestTemplate restTemplate, AiConfig aiConfig, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.aiConfig = aiConfig;
        this.objectMapper = objectMapper;
    }

    public String generateExpectedResult(
            String requestUrl, String requestMethod, String requestHeaders, String requestParams) {

        LinkedHashMap<String, Object> info = new LinkedHashMap<>();
        info.put("method", requestMethod);
        info.put("url", requestUrl);
        if (requestHeaders != null && !requestHeaders.trim().isEmpty()) {
            info.put("headers", requestHeaders);
        }
        if (requestParams != null && !requestParams.trim().isEmpty()) {
            info.put("body", requestParams);
        }

        try {
            String userMessage =
                    "Predict the JSON response for this API request:\n"
                            + objectMapper
                                    .writerWithDefaultPrettyPrinter()
                                    .writeValueAsString(info);
            return callAgnes(userMessage);
        } catch (Exception e) {
            throw new RuntimeException("AI generation failed: " + e.getMessage(), e);
        }
    }

    private String callAgnes(String userMessage) {
        String url = aiConfig.getBaseUrl() + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiConfig.getApiKey());

        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put(
                "content",
                "You are an API test assistant. Given API information, "
                        + "predict the JSON response body. Return ONLY valid JSON, no extra text.");

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);

        Map<String, Object> body = new HashMap<>();
        body.put("model", aiConfig.getModel());
        body.put("messages", Arrays.asList(systemMsg, userMsg));
        body.put("temperature", 0.3);

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("AI generation failed: " + e.getMessage(), e);
        }
    }

    public String generateExpectedFromSchema(String schemaJson) {
        String prompt =
                "Generate a sample JSON response body based on the following JSON Schema. "
                        + "Use realistic values for all fields. Return ONLY valid JSON, no extra text:\n"
                        + schemaJson;
        return callAgnes(prompt);
    }
}
