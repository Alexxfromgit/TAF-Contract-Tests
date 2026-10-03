package io.github.alexxfromgit.taf.contract.core.contract.openapi;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenApiValidationFilterTest {

    @Test
    public void jsonPointersBecomeJsonPaths() {
        assertThat(OpenApiValidationFilter.toJsonPath("")).isEqualTo("$");
        assertThat(OpenApiValidationFilter.toJsonPath("/tags/0")).isEqualTo("$.tags[0]");
        assertThat(OpenApiValidationFilter.toJsonPath("/a~1b/c~0d")).isEqualTo("$.a/b.c~d");
    }
}
