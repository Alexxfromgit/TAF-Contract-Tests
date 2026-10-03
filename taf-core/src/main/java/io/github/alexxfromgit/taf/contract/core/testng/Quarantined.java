package io.github.alexxfromgit.taf.contract.core.testng;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Temporarily disables a flaky or broken test until a date. After that date the test runs again
 * automatically, so quarantine can never silently become permanent. The metadata linter rejects
 * quarantines longer than {@code lint.quarantine.max-days} (default 90).
 * <pre>{@code @Quarantined(until = "2026-11-01", reason = "Flaky on staging, see #17")}</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Quarantined {

    /** ISO date (yyyy-MM-dd), exclusive: the test runs again on this day. */
    String until();

    String reason();
}
