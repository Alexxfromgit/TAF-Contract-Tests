package io.github.alexxfromgit.taf.contract.core.contract.coverage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a test covers an endpoint even though the call is not made through a {@code ServiceClient}
 * (e.g. through a generated client or another tool). Calls made through a {@code ServiceClient} are counted
 * automatically and need no annotation.
 * <pre>{@code @Covers(method = "POST", path = "/pet/{petId}/uploadImage")}</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Repeatable(Covers.List.class)
public @interface Covers {

    String method();

    String path();

    /** Service id; empty means "any service whose specification declares this endpoint". */
    String service() default "";

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @interface List {
        Covers[] value();
    }
}
