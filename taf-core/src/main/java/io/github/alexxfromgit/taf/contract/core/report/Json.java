package io.github.alexxfromgit.taf.contract.core.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.alexxfromgit.taf.contract.core.failure.FrameworkException;

import java.util.Optional;

/** Shared Jackson mapper and helpers. */
public final class Json {

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    private Json() {
    }

    public static String pretty(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new FrameworkException("Cannot serialize " + value.getClass().getSimpleName() + " to JSON", e);
        }
    }

    /** Parses JSON text; empty when the text is blank or not JSON. */
    public static Optional<JsonNode> tryParse(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(MAPPER.readTree(text));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    public static JsonNode parse(String text) {
        try {
            return MAPPER.readTree(text);
        } catch (JsonProcessingException e) {
            throw new FrameworkException("Invalid JSON: " + e.getOriginalMessage(), e);
        }
    }
}
