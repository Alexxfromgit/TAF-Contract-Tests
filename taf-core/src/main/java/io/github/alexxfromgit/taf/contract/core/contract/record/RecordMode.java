package io.github.alexxfromgit.taf.contract.core.contract.record;

import io.github.alexxfromgit.taf.contract.core.config.TafConfig;
import io.github.alexxfromgit.taf.contract.core.failure.FrameworkException;

import java.util.Locale;

/**
 * {@code contract.record}:
 * <ul>
 *     <li>{@code off} (default) - a missing schema is a framework error;</li>
 *     <li>{@code missing} (or {@code true}) - missing schemas are inferred from the live response and written
 *     to {@code src/test/resources}; the test is skipped with "review &amp; commit";</li>
 *     <li>{@code overwrite} - every schema is re-recorded (use after an intended API change, then review the diff).</li>
 * </ul>
 */
public enum RecordMode {
    OFF, MISSING, OVERWRITE;

    public static RecordMode current() {
        String value = TafConfig.get().string("contract.record", "off").trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "off", "false", "none" -> OFF;
            case "missing", "true" -> MISSING;
            case "overwrite", "all" -> OVERWRITE;
            default -> throw new FrameworkException("contract.record must be off, missing or overwrite, not '"
                    + value + "'");
        };
    }
}
