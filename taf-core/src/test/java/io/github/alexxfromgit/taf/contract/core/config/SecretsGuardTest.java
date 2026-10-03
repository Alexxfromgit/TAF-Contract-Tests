package io.github.alexxfromgit.taf.contract.core.config;

import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SecretsGuardTest {

    @Test
    public void flagsLiteralSecrets() {
        assertThat(SecretsGuard.findViolations(Map.of(
                "services.api.auth.client-secret", "abc",
                "db.password", "p4ss",
                "services.api.auth.token", "xyz",
                "cloud.api-key", "k")))
                .containsExactly("cloud.api-key", "db.password", "services.api.auth.client-secret",
                        "services.api.auth.token");
    }

    @Test
    public void allowsEmptyPlaceholdersSwitchesAndNonSecretSuffixes() {
        assertThat(SecretsGuard.findViolations(Map.of(
                "db.password", "",
                "services.api.auth.token", "${OTHER}",
                "services.api.auth.token-url", "https://idp/token",
                "services.api.auth.header", "X-Token",
                "lint.secrets", "true",
                "services.api.base-uri", "https://api"))).isEmpty();
    }
}
