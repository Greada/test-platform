package com.testplatform.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.common.EndpointDef;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author admin
 * @version 1.0.0
 */
public class OpenApiParser {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static List<EndpointDef> parse(String openapiJson) {
        ArrayList<EndpointDef> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(openapiJson);

            String baseUrl = extractBaseUrl(root);

            JsonNode paths = root.get("paths");
            if (paths == null || !paths.isObject()) {
                return result;
            }

            Iterator<Map.Entry<String, JsonNode>> pathFields = paths.fields();
            while (pathFields.hasNext()) {
                Map.Entry<String, JsonNode> pathEntry = pathFields.next();
                String path = pathEntry.getKey();
                JsonNode methods = pathEntry.getValue();

                Iterator<Map.Entry<String, JsonNode>> methodFields = methods.fields();
                while (methodFields.hasNext()) {
                    Map.Entry<String, JsonNode> m = methodFields.next();
                    String method = m.getKey().toUpperCase();
                    if (!isHttpMethod(method)) {
                        continue;
                    }

                    JsonNode details = m.getValue();
                    EndpointDef def = new EndpointDef();
                    def.setName(extractName(details));
                    def.setRequestUrl(baseUrl + path);
                    def.setRequestMethod(method);
                    def.setRequestHeaders("{\"Content-Type\":\"application/json\"}");
                    def.setRequestParams(extractSchema(details, "requestBody"));
                    def.setResponseSchema(extractSchema(details, "responses"));
                    result.add(def);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("OpenAPI parse failed: " + e.getMessage(), e);
        }

        return result;
    }

    private static String extractSchema(JsonNode details, String type) {
        try {
            if ("requestBody".equals(type)) {
                JsonNode content = details.path("requestBody").path("content");
                JsonNode jsonContent = content.get("application/json");
                if (jsonContent != null) {
                    JsonNode schema = jsonContent.get("schema");
                    if (schema != null) {
                        return objectMapper
                                .writerWithDefaultPrettyPrinter()
                                .writeValueAsString(schema);
                    }
                }
            } else if ("responses".equals(type)) {
                JsonNode response = details.path("responses").path("200").path("content");
                JsonNode jsonContent = response.get("application/json");
                if (jsonContent != null) {
                    JsonNode schema = jsonContent.get("schema");
                    if (schema != null) {
                        return objectMapper
                                .writerWithDefaultPrettyPrinter()
                                .writeValueAsString(schema);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "{}";
    }

    private static String extractName(JsonNode details) {
        String name = details.path("summary").asText(null);
        if (name == null || name.isEmpty()) {
            name = details.path("operationId").asText(null);
        }
        return name != null ? name : "";
    }

    private static boolean isHttpMethod(String m) {
        return "GET".equals(m)
                || "POST".equals(m)
                || "PUT".equals(m)
                || "PATCH".equals(m)
                || "DELETE".equals(m);
    }

    private static String extractBaseUrl(JsonNode root) {
        JsonNode servers = root.get("servers");
        if (servers != null && servers.isArray() && !servers.isEmpty()) {
            String u = servers.get(0).get("url").asText();
            return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
        }
        String host = root.path("host").asText("");
        String basePath = root.path("basePath").asText("");
        if (!host.isEmpty()) {
            String scheme =
                    root.path("schemes").isArray()
                            ? root.get("schemes").get(0).asText("https")
                            : "https";
            return scheme + "://" + host + basePath;
        }
        return "";
    }
}
