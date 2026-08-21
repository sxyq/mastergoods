package com.zhihuiji.backend.application.service.v2.agent.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.util.StringUtils;

/** Stable identity for one model-selected tool invocation. */
public final class ToolInvocationIdentity {
    private ToolInvocationIdentity() {
    }

    public static String key(
        String toolName,
        String modelToolCallId,
        JsonNode arguments,
        ObjectMapper objectMapper
    ) {
        String prefix = StringUtils.hasText(modelToolCallId)
            ? "call:" + modelToolCallId
            : "tool:" + (StringUtils.hasText(toolName) ? toolName : "unknown");
        return prefix + "|args:" + fingerprint(arguments, objectMapper);
    }

    public static String fingerprint(JsonNode arguments, ObjectMapper objectMapper) {
        ObjectMapper mapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        JsonNode normalized = canonicalize(arguments == null || arguments.isNull()
            ? JsonNodeFactory.instance.objectNode()
            : arguments, mapper);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(mapper.writeValueAsString(normalized).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException | RuntimeException | com.fasterxml.jackson.core.JsonProcessingException ex) {
            return normalized.toString();
        }
    }

    private static JsonNode canonicalize(JsonNode node, ObjectMapper mapper) {
        if (node == null || node.isNull()) {
            return JsonNodeFactory.instance.nullNode();
        }
        if (node.isObject()) {
            ObjectNode result = mapper.createObjectNode();
            List<String> names = new ArrayList<>();
            node.fieldNames().forEachRemaining(names::add);
            Collections.sort(names);
            for (String name : names) {
                result.set(name, canonicalize(node.get(name), mapper));
            }
            return result;
        }
        if (node.isArray()) {
            ArrayNode result = mapper.createArrayNode();
            for (JsonNode item : node) {
                result.add(canonicalize(item, mapper));
            }
            return result;
        }
        return node;
    }
}
