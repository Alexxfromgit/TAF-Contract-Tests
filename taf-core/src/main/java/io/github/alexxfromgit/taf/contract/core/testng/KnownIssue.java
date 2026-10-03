package io.github.alexxfromgit.taf.contract.core.testng;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test that is expected to fail because of a reported bug. The test still runs; if it fails,
 * the failure is reported under "Known issues" with a link to the bug instead of looking like a new defect.
 * If it passes, a warning reminds you to remove the annotation.
 * <pre>{@code @KnownIssue(id = "42")  // link built from allure.link.issue.pattern}</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface KnownIssue {

    /** Bug id, e.g. {@code "PROJ-123"} or a GitHub issue number. */
    String id();

    /** Full link; when empty the link is built from {@code allure.link.issue.pattern}. */
    String url() default "";
}
