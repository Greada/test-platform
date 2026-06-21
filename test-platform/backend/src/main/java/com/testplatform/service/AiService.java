package com.testplatform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.config.AiConfig;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

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

    public List<String> generateExpectedBatch(List<String> schemaJsons)
            throws JsonProcessingException {
        int batchSize = 15;
        String[] results = new String[schemaJsons.size()];
        Arrays.fill(results, "{}");

        for (int start = 0; start < schemaJsons.size(); start += batchSize) {
            int end = Math.min(start + batchSize, schemaJsons.size());
            List<String> chunk = schemaJsons.subList(start, end);

            StringBuilder builder = new StringBuilder();
            builder.append("Generate a sample JSON response body for each of the following ")
                    .append(chunk.size())
                    .append(" JSON Schemas. Use realistic values.\n")
                    .append("Return a JSON array of objects, each with format:\n")
                    .append("  {\"index\": <number>, \"response\": <generated JSON>}\n")
                    .append("Include ALL items in original order. Return ONLY valid JSON.\n\n");

            for (int i = 0; i < chunk.size(); i++) {
                builder.append("--- Schema ")
                        .append(i)
                        .append(" ---\n")
                        .append(chunk.get(i))
                        .append("\n\n");
            }

            try {
                String raw = callAgnes(builder.toString());
                JsonNode root = objectMapper.readTree(raw);
                for (JsonNode item : root) {
                    int idxInChunk = item.get("index").asInt();
                    int globalIdx = start + idxInChunk;
                    if (globalIdx < results.length) {
                        results[globalIdx] = item.get("response").toString();
                    }
                }
            } catch (Exception e) {
                // 单批失败不影响其他批次
            }
        }

        return Arrays.asList(results);
    }
}
