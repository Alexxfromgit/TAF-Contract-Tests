package io.github.alexxfromgit.taf.contract.core.allure;

import io.github.alexxfromgit.taf.contract.core.config.TafConfig;
import io.github.alexxfromgit.taf.contract.core.failure.FrameworkException;
import io.qameta.allure.Allure;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/** Helpers around the Allure results directory and attachments. */
public final class AllureResults {

    private AllureResults() {
    }

    /** Same lookup order as Allure itself: system property, then {@code allure.properties}, then default. */
    public static Path directory() {
        String fromSystem = System.getProperty("allure.results.directory");
        if (fromSystem != null && !fromSystem.isBlank()) {
            return Path.of(fromSystem);
        }
        try (InputStream in = classLoader().getResourceAsStream("allure.properties")) {
            if (in != null) {
                Properties properties = new Properties();
                properties.load(in);
                String value = properties.getProperty("allure.results.directory");
                if (value != null && !value.isBlank()) {
                    return Path.of(value);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return Path.of("allure-results");
    }

    /**
     * Copies {@code allure/categories.json} from the project (if present) or the framework default
     * {@code taf/allure/categories.json} into the results directory. Categories group failures by the
     * failure taxonomy: product defects vs. contract violations vs. test data vs. environment.
     */
    public static void installCategories() {
        String resource = classLoader().getResource("allure/categories.json") != null
                ? "allure/categories.json" : "taf/allure/categories.json";
        try (InputStream in = classLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new FrameworkException("categories.json not found on classpath");
            }
            Path dir = Files.createDirectories(directory());
            Files.copy(in, dir.resolve("categories.json"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot install Allure categories", e);
        }
    }

    /** Writes {@code environment.properties} shown on the Allure overview page. */
    public static void writeEnvironment(Map<String, String> extra) {
        Map<String, String> values = new TreeMap<>();
        values.put("env", TafConfig.get().env());
        values.put("java.version", System.getProperty("java.version"));
        values.put("os", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        Map<String, String> github = new LinkedHashMap<>();
        github.put("git.commit", System.getenv("GITHUB_SHA"));
        github.put("ci.run", System.getenv("GITHUB_RUN_ID"));
        github.forEach((k, v) -> {
            if (v != null) {
                values.put(k, v);
            }
        });
        values.putAll(extra);
        StringBuilder out = new StringBuilder();
        values.forEach((k, v) -> out.append(k).append('=').append(v.replace("\\", "\\\\")).append('\n'));
        try {
            Path dir = Files.createDirectories(directory());
            Files.writeString(dir.resolve("environment.properties"), out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write Allure environment.properties", e);
        }
    }

    /** Attaches text when a test, fixture or step is running; silently ignored otherwise. */
    public static void attach(String name, String mimeType, String extension, String content) {
        if (Allure.getLifecycle().getCurrentTestCaseOrStep().isPresent()) {
            Allure.addAttachment(name, mimeType, content, extension);
        }
    }

    public static void attachJson(String name, String json) {
        attach(name, "application/json", ".json", json);
    }

    public static void attachHtml(String name, String html) {
        attach(name, "text/html", ".html", html);
    }

    public static void attachText(String name, String text) {
        attach(name, "text/plain", ".txt", text);
    }

    private static ClassLoader classLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}
