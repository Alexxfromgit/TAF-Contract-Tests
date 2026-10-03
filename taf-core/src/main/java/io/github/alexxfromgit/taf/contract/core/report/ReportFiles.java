package io.github.alexxfromgit.taf.contract.core.report;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes framework reports (coverage, drift, ...) to {@code taf.reports-dir} (default {@code target/taf-reports}). */
public final class ReportFiles {

    private ReportFiles() {
    }

    public static Path directory() {
        return Path.of(System.getProperty("taf.reports-dir", "target/taf-reports"));
    }

    public static Path write(String fileName, String content) {
        try {
            Path file = Files.createDirectories(directory()).resolve(fileName);
            Files.writeString(file, content, StandardCharsets.UTF_8);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write report " + fileName, e);
        }
    }
}
