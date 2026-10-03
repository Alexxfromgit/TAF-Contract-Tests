package io.github.alexxfromgit.taf.contract.core.contract.record;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.alexxfromgit.taf.contract.core.contract.schema.SchemaRepository;
import io.github.alexxfromgit.taf.contract.core.log.Log;
import io.github.alexxfromgit.taf.contract.core.report.Json;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes inferred schemas into the source tree ({@code taf.test-resources-dir}, default {@code src/test/resources}). */
public final class SchemaRecorder {

    private SchemaRecorder() {
    }

    public static Path record(String schemaPath, JsonNode sample) {
        String title = Path.of(schemaPath).getFileName().toString().replaceFirst("\\.json$", "");
        JsonNode schema = SchemaInferrer.infer(sample, title);
        Path target = resourcesDir().resolve(schemaPath).normalize();
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, Json.pretty(schema) + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write recorded schema " + target, e);
        }
        SchemaRepository.put(schemaPath, schema);
        Log.warn("Recorded schema {} - review it and commit", target.toAbsolutePath());
        return target;
    }

    static Path resourcesDir() {
        return Path.of(System.getProperty("taf.test-resources-dir", "src/test/resources"));
    }
}
