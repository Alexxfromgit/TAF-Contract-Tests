package io.github.alexxfromgit.taf.contract.core.contract.record;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Infers a consumer-tolerant JSON Schema (draft 2020-12) from sample payloads.
 * <ul>
 *     <li>object properties = union of keys seen in all samples (sorted);</li>
 *     <li>{@code required} = keys present and non-null in every sample;</li>
 *     <li>a value seen as {@code null} becomes {@code ["<type>", "null"]};</li>
 *     <li>{@code integer} + {@code number} merge into {@code number}; other mixes become a type list;</li>
 *     <li>array items of all samples are merged into one {@code items} schema;</li>
 *     <li>no {@code additionalProperties}: extra fields stay allowed (and are reported as drift).</li>
 * </ul>
 * The result is a starting point for review, not a specification: tighten it (enums, formats) by hand.
 */
public final class SchemaInferrer {

    public static final String DRAFT_2020_12 = "https://json-schema.org/draft/2020-12/schema";

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    private SchemaInferrer() {
    }

    public static ObjectNode infer(JsonNode sample, String title) {
        ObjectNode root = NODES.objectNode();
        root.put("$schema", DRAFT_2020_12);
        if (title != null && !title.isBlank()) {
            root.put("title", title);
        }
        root.setAll(inferFrom(List.of(sample)));
        return root;
    }

    static ObjectNode inferFrom(Collection<JsonNode> samples) {
        List<JsonNode> present = new ArrayList<>();
        boolean nullable = false;
        for (JsonNode sample : samples) {
            if (sample == null || sample.isNull() || sample.isMissingNode()) {
                nullable = true;
            } else {
                present.add(sample);
            }
        }
        ObjectNode schema = NODES.objectNode();
        if (present.isEmpty()) {
            return schema;  // only nulls seen: accept anything
        }

        Set<String> types = new LinkedHashSet<>();
        present.forEach(node -> types.add(typeOf(node)));
        if (types.contains("integer") && types.contains("number")) {
            types.remove("integer");
        }

        if (types.size() == 1 && types.contains("object")) {
            describeObject(schema, present);
        } else if (types.size() == 1 && types.contains("array")) {
            describeArray(schema, present);
        }

        List<String> typeList = new ArrayList<>(types);
        if (nullable) {
            typeList.add("null");
        }
        ObjectNode ordered = NODES.objectNode();
        if (typeList.size() == 1) {
            ordered.put("type", typeList.get(0));
        } else {
            ArrayNode typeArray = ordered.putArray("type");
            typeList.forEach(typeArray::add);
        }
        ordered.setAll(schema);
        return ordered;
    }

    private static void describeObject(ObjectNode schema, List<JsonNode> objects) {
        Set<String> keys = new TreeSet<>();
        objects.forEach(o -> o.fieldNames().forEachRemaining(keys::add));

        ArrayNode required = NODES.arrayNode();
        ObjectNode properties = NODES.objectNode();
        for (String key : keys) {
            List<JsonNode> values = new ArrayList<>();
            boolean alwaysPresent = true;
            for (JsonNode object : objects) {
                JsonNode value = object.get(key);
                if (value == null || value.isNull()) {
                    alwaysPresent = false;
                }
                if (value != null) {
                    values.add(value);
                }
            }
            if (alwaysPresent) {
                required.add(key);
            }
            properties.set(key, inferFrom(values));
        }
        if (!required.isEmpty()) {
            schema.set("required", required);
        }
        schema.set("properties", properties);
    }

    private static void describeArray(ObjectNode schema, List<JsonNode> arrays) {
        List<JsonNode> items = new ArrayList<>();
        arrays.forEach(array -> array.forEach(items::add));
        schema.set("items", items.isEmpty() ? NODES.objectNode() : inferFrom(items));
    }

    private static String typeOf(JsonNode node) {
        if (node.isObject()) {
            return "object";
        }
        if (node.isArray()) {
            return "array";
        }
        if (node.isIntegralNumber()) {
            return "integer";
        }
        if (node.isNumber()) {
            return "number";
        }
        if (node.isBoolean()) {
            return "boolean";
        }
        return "string";
    }
}
