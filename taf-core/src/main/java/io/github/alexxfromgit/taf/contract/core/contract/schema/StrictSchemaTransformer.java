package io.github.alexxfromgit.taf.contract.core.contract.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.SpecificationVersion;

import java.util.List;

/**
 * Turns a consumer-tolerant schema into a strict one without editing the file: every object schema
 * that declares {@code properties} and says nothing about extra properties gets
 * {@code "additionalProperties": false}.
 * <p>
 * Composition caveat: {@code additionalProperties} does not see properties declared in sibling
 * {@code allOf/anyOf/oneOf} branches. Members of those keywords are therefore left alone; for
 * 2019-09+ schemas the composing parent gets {@code "unevaluatedProperties": false} instead, which
 * does understand composition. Older drafts that use composition are strict on a best-effort basis.
 */
public final class StrictSchemaTransformer {

    private static final List<String> COMPOSITION = List.of("allOf", "anyOf", "oneOf");
    private static final List<String> SCHEMA_MAPS = List.of(
            "properties", "patternProperties", "$defs", "definitions", "dependentSchemas");
    private static final List<String> SCHEMA_VALUES = List.of(
            "additionalProperties", "unevaluatedProperties", "items", "additionalItems", "unevaluatedItems",
            "contains", "propertyNames", "not", "if", "then", "else");

    private StrictSchemaTransformer() {
    }

    /** Returns a strict deep copy; the input is not modified. */
    public static JsonNode strict(JsonNode schema) {
        JsonNode copy = schema.deepCopy();
        boolean modern = SchemaValidator.detectVersion(schema).getOrder()
                >= SpecificationVersion.DRAFT_2019_09.getOrder();
        visit(copy, false, modern);
        return copy;
    }

    private static void visit(JsonNode node, boolean compositionMember, boolean modern) {
        if (!(node instanceof ObjectNode schema)) {
            return;
        }
        boolean composes = COMPOSITION.stream().anyMatch(schema::has);
        boolean declaresExtras = schema.has("additionalProperties") || schema.has("unevaluatedProperties");
        boolean usesRef = schema.has("$ref");

        if (!declaresExtras && !compositionMember) {
            if (composes && modern) {
                schema.put("unevaluatedProperties", false);
            } else if (!composes && !usesRef && schema.has("properties")) {
                schema.put("additionalProperties", false);
            }
        }

        for (String keyword : SCHEMA_MAPS) {
            JsonNode map = schema.get(keyword);
            if (map != null && map.isObject()) {
                map.properties().forEach(e -> visit(e.getValue(), false, modern));
            }
        }
        for (String keyword : SCHEMA_VALUES) {
            JsonNode value = schema.get(keyword);
            if (value instanceof ArrayNode array) {          // draft-04/07 tuple "items"
                array.forEach(item -> visit(item, false, modern));
            } else if (value != null) {
                visit(value, false, modern);
            }
        }
        JsonNode prefixItems = schema.get("prefixItems");
        if (prefixItems != null) {
            prefixItems.forEach(item -> visit(item, false, modern));
        }
        for (String keyword : COMPOSITION) {
            JsonNode members = schema.get(keyword);
            if (members != null) {
                members.forEach(member -> visit(member, true, modern));
            }
        }
    }
}
