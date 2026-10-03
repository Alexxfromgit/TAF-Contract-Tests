package io.github.alexxfromgit.taf.contract.core.contract.record;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.alexxfromgit.taf.contract.core.contract.schema.SchemaValidator;
import io.github.alexxfromgit.taf.contract.core.report.Json;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SchemaInferrerTest {

    private static final String SAMPLE = """
            {
              "id": 10,
              "name": "Rex",
              "price": 9.5,
              "photoUrls": ["a.png"],
              "category": null,
              "tags": [
                {"id": 1, "name": "good", "weight": 1},
                {"id": 2, "name": null, "weight": 2.5}
              ],
              "empty": []
            }""";

    @Test
    public void infersConsumerTolerantSchema() {
        JsonNode schema = SchemaInferrer.infer(Json.parse(SAMPLE), "pet");

        assertThat(Json.pretty(schema)).isEqualToIgnoringWhitespace("""
                {
                  "$schema" : "https://json-schema.org/draft/2020-12/schema",
                  "title" : "pet",
                  "type" : "object",
                  "required" : [ "empty", "id", "name", "photoUrls", "price", "tags" ],
                  "properties" : {
                    "category" : { },
                    "empty" : { "type" : "array", "items" : { } },
                    "id" : { "type" : "integer" },
                    "name" : { "type" : "string" },
                    "photoUrls" : { "type" : "array", "items" : { "type" : "string" } },
                    "price" : { "type" : "number" },
                    "tags" : {
                      "type" : "array",
                      "items" : {
                        "type" : "object",
                        "required" : [ "id", "weight" ],
                        "properties" : {
                          "id" : { "type" : "integer" },
                          "name" : { "type" : [ "string", "null" ] },
                          "weight" : { "type" : "number" }
                        }
                      }
                    }
                  }
                }""");
    }

    @Test
    public void inferredSchemaAcceptsItsOwnSample() {
        JsonNode sample = Json.parse(SAMPLE);
        assertThat(SchemaValidator.validate(null, SchemaInferrer.infer(sample, "pet"), sample, true)).isEmpty();
    }
}
