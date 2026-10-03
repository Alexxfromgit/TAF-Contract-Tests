package io.github.alexxfromgit.taf.contract.core.contract.schema;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.alexxfromgit.taf.contract.core.contract.drift.DriftCollector;
import io.github.alexxfromgit.taf.contract.core.failure.ContractViolationException;
import io.github.alexxfromgit.taf.contract.core.report.Json;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SchemaValidationTest {

    private static final String PET_2020 = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "required": ["id", "name"],
              "properties": {
                "id": {"type": "integer"},
                "name": {"type": "string"},
                "tags": {"type": "array", "items": {
                  "type": "object", "required": ["name"], "properties": {"name": {"type": "string"}}}}
              }
            }""";

    private static final String PET_07 = PET_2020.replace(
            "https://json-schema.org/draft/2020-12/schema", "http://json-schema.org/draft-07/schema#");

    @BeforeMethod
    public void resetDrift() {
        DriftCollector.clear();
    }

    @Test
    public void validPayloadPassesInBothDrafts() {
        JsonNode pet = Json.parse("{\"id\": 1, \"name\": \"Rex\", \"tags\": [{\"name\": \"good\"}]}");
        assertThat(SchemaValidator.validate(null, Json.parse(PET_2020), pet, true)).isEmpty();
        assertThat(SchemaValidator.validate(null, Json.parse(PET_07), pet, true)).isEmpty();
    }

    @Test
    public void violationsAreFlattenedToJsonPaths() {
        JsonNode pet = Json.parse("{\"id\": \"one\", \"tags\": [{\"label\": \"x\"}]}");
        List<Violation> violations = SchemaValidator.validate(null, Json.parse(PET_07), pet, true);

        assertThat(violations).extracting(Violation::fullPath, Violation::keyword)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("$.id", "type"),
                        org.assertj.core.groups.Tuple.tuple("$.name", "required"),
                        org.assertj.core.groups.Tuple.tuple("$.tags[0].name", "required"));
    }

    @Test
    public void extraFieldsPassTolerantButFailStrict() {
        JsonNode pet = Json.parse("{\"id\": 1, \"name\": \"Rex\", \"nickname\": \"R\", \"tags\": [{\"name\": \"a\", \"color\": \"red\"}]}");
        JsonNode schema = Json.parse(PET_2020);

        assertThat(SchemaValidator.validate(null, schema, pet, true)).isEmpty();
        List<Violation> strict = SchemaValidator.validate(null, StrictSchemaTransformer.strict(schema), pet, true);
        assertThat(strict).extracting(Violation::fullPath)
                .containsExactlyInAnyOrder("$.nickname", "$.tags[0].color");
        assertThat(strict).allMatch(v -> v.keyword().equals("additionalProperties"));
    }

    @Test
    public void strictTransformRespectsCompositionAndExplicitSettings() {
        JsonNode schema = Json.parse("""
                {"$schema": "https://json-schema.org/draft/2020-12/schema",
                 "allOf": [{"type": "object", "properties": {"a": {"type": "string"}}},
                           {"type": "object", "properties": {"b": {"type": "string"}}}],
                 "$defs": {"open": {"type": "object", "properties": {"x": {}}, "additionalProperties": true}}}""");
        JsonNode strict = StrictSchemaTransformer.strict(schema);

        assertThat(strict.get("unevaluatedProperties").asBoolean(true)).isFalse();
        assertThat(strict.at("/allOf/0").has("additionalProperties")).isFalse();
        assertThat(strict.at("/$defs/open/additionalProperties").asBoolean()).isTrue();
        assertThat(schema.has("unevaluatedProperties")).as("input must not be modified").isFalse();

        assertThat(SchemaValidator.validate(null, strict, Json.parse("{\"a\": \"1\", \"b\": \"2\"}"), true)).isEmpty();
        assertThat(SchemaValidator.validate(null, strict, Json.parse("{\"a\": \"1\", \"c\": \"3\"}"), true))
                .extracting(Violation::keyword).containsExactly("unevaluatedProperties");
    }

    @Test
    public void contractAssertReportsReadableFailure() {
        assertThatThrownBy(() -> ContractAssert.assertThat(Json.parse("{\"id\": 1}"))
                .endpoint("GET /pet/{petId}")
                .matchesSchema("schemas/unit/pet.json"))
                .isInstanceOf(ContractViolationException.class)
                .hasMessageContaining("GET /pet/{petId} does not match schemas/unit/pet.json")
                .hasMessageContaining("$.name -> required");
    }

    @Test
    public void tolerantMatchRecordsDrift() {
        String response = "{\"id\": 1, \"name\": \"Rex\", \"tags\": [{\"name\": \"a\", \"color\": \"red\"}, {\"name\": \"b\", \"color\": \"blue\"}]}";
        for (int i = 0; i < 2; i++) {
            ContractAssert.assertThat(Json.parse(response))
                    .endpoint("GET /pet/{petId}")
                    .matchesSchema("schemas/unit/pet.json");
        }

        // reported once per test, however many responses or array elements contain the field
        assertThat(DriftCollector.snapshot()).containsOnlyKeys("GET /pet/{petId}");
        assertThat(DriftCollector.snapshot().get("GET /pet/{petId}")).containsOnlyKeys("$.tags[*].color");
    }

    @Test
    public void jsonPointerSelectsNestedPayload() {
        ContractAssert.assertThat(Json.parse("{\"data\": {\"pet\": {\"id\": 1, \"name\": \"Rex\"}}}"))
                .at("/data/pet").strict()
                .matchesSchema("schemas/unit/pet.json");

        assertThatThrownBy(() -> ContractAssert.assertThat(Json.parse("{\"data\": {}}"))
                .at("/data/pet").matchesSchema("schemas/unit/pet.json"))
                .isInstanceOf(ContractViolationException.class)
                .hasMessageContaining("no node at JSON Pointer '/data/pet'");
    }
}
