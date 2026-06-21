package com.testplatform.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SchemaToJsonGenerator {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static List<String> batchGenerate(List<String> schemaJsons) {
        List<String> results = new ArrayList<>();
        for (String schemaJson : schemaJsons) {
            try {
                JsonNode schema = objectMapper.readTree(schemaJson);
                JsonNode generated = generate(schema);
                results.add(objectMapper
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(generated));
            } catch (Exception e) {
                results.add("{}");
            }
        }
        return results;
    }

    private static JsonNode generate(JsonNode schema) {
        if (schema == null || schema.isNull() || schema.isMissingNode()) {
            return objectMapper.getNodeFactory().objectNode();
        }

        if (schema.has("enum")) {
            JsonNode enumValues = schema.get("enum");
            if (enumValues.isArray() && enumValues.size() > 0) {
                return enumValues.get(0);
            }
        }

        String type = schema.has("type") ? schema.get("type").asText() : null;

        if ("string".equals(type)) {
            return generateString(schema);
        }

        if ("integer".equals(type)) {
            if (schema.has("minimum")) {
                return objectMapper.getNodeFactory().numberNode(schema.get("minimum").asLong());
            }
            return objectMapper.getNodeFactory().numberNode(0);
        }

        if ("number".equals(type)) {
            return objectMapper.getNodeFactory().numberNode(0.0);
        }

        if ("boolean".equals(type)) {
            return objectMapper.getNodeFactory().booleanNode(false);
        }

        if ("array".equals(type)) {
            JsonNode items = schema.get("items");
            if (items != null && !items.isNull()) {
                ArrayNode arr = objectMapper.getNodeFactory().arrayNode();
                arr.add(generate(items));
                return arr;
            }
            return objectMapper.getNodeFactory().arrayNode();
        }

        if ("object".equals(type) || schema.has("properties")) {
            return generateObject(schema);
        }

        if (schema.has("$ref")) {
            return objectMapper.getNodeFactory().objectNode();
        }

        return objectMapper.getNodeFactory().objectNode();
    }

    private static JsonNode generateString(JsonNode schema) {
        if (schema.has("format")) {
            String format = schema.get("format").asText();
            if ("date-time".equals(format)) {
                return objectMapper.getNodeFactory().textNode("2026-01-01T00:00:00");
            }
            if ("date".equals(format)) {
                return objectMapper.getNodeFactory().textNode("2026-01-01");
            }
            if ("email".equals(format)) {
                return objectMapper.getNodeFactory().textNode("user@example.com");
            }
            if ("uri".equals(format) || "url".equals(format)) {
                return objectMapper.getNodeFactory().textNode("https://example.com");
            }
        }
        return objectMapper.getNodeFactory().textNode("");
    }

    private static JsonNode generateObject(JsonNode schema) {
        ObjectNode obj = objectMapper.getNodeFactory().objectNode();
        JsonNode properties = schema.get("properties");
        if (properties != null) {
            Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> prop = fields.next();
                obj.set(prop.getKey(), generate(prop.getValue()));
            }
        }
        JsonNode additionalProps = schema.get("additionalProperties");
        if (additionalProps != null && additionalProps.isObject()) {
            obj.set("additionalProp", generate(additionalProps));
        }
        return obj;
    }
}
