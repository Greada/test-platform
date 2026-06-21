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
    private static final int MAX_REF_DEPTH = 5;

    public static List<EndpointDef> parse(String openapiJson) {
        ArrayList<EndpointDef> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(openapiJson);

            String baseUrl = extractBaseUrl(root);
            JsonNode components = root.get("components");

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
                    def.setRequestUrl((baseUrl + path).replaceAll("\\{[^}]+\\}", "1"));
                    def.setRequestMethod(method);
                    def.setRequestHeaders("{\"Content-Type\":\"application/json\"}");
                    def.setRequestParams(extractSchema(details, "requestBody", components));
                    def.setResponseSchema(extractSchema(details, "responses", components));
                    result.add(def);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("OpenAPI parse failed: " + e.getMessage(), e);
        }

        return result;
    }

    private static String extractSchema(JsonNode details, String type, JsonNode components) {
        try {
            JsonNode content;
            if ("requestBody".equals(type)) {
                content = details.path("requestBody").path("content");
            } else if ("responses".equals(type)) {
                content = details.path("responses").path("200").path("content");
            } else {
                return "{}";
            }

            if (content == null || !content.isObject()) {
                return "{}";
            }

            // 先找 application/json，找不到就取第一个 content type
            JsonNode mediaType = content.get("application/json");
            if (mediaType == null) {
                Iterator<Map.Entry<String, JsonNode>> fields = content.fields();
                if (fields.hasNext()) {
                    mediaType = fields.next().getValue();
                }
            }

            if (mediaType == null) {
                return "{}";
            }

            JsonNode schema = mediaType.get("schema");
            if (schema == null) {
                return "{}";
            }

            JsonNode resolved = resolveRef(schema, components, 0);
            return objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(resolved);
        } catch (Exception ignored) {
        }
        return "{}";
    }

    /**
     * 递归解析 $ref 引用，将引用替换为实际定义内容。
     */
    private static JsonNode resolveRef(JsonNode node, JsonNode components, int depth) {
        if (node == null || depth > MAX_REF_DEPTH) {
            return objectMapper.getNodeFactory().objectNode();
        }

        if (node.isObject() && node.has("$ref")) {
            String ref = node.get("$ref").asText();
            if (ref.startsWith("#/components/schemas/")) {
                String schemaName = ref.substring("#/components/schemas/".length());
                JsonNode target = null;
                if (components != null) {
                    target = components.path("schemas").path(schemaName);
                }
                if (target != null && !target.isMissingNode()) {
                    // 用浅拷贝避免修改原始树，然后递归解析
                    JsonNode resolved = target.deepCopy();
                    return resolveRef(resolved, components, depth + 1);
                }
            }
            return node;
        }

        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode child = resolveRef(entry.getValue(), components, depth);
                if (child != entry.getValue()) {
                    ((com.fasterxml.jackson.databind.node.ObjectNode) node).set(entry.getKey(), child);
                }
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                JsonNode child = resolveRef(node.get(i), components, depth);
                if (child != node.get(i)) {
                    ((com.fasterxml.jackson.databind.node.ArrayNode) node).set(i, child);
                }
            }
        }

        return node;
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
