package io.github.alexxfromgit.taf.contract.examples.lint;

import io.github.alexxfromgit.taf.contract.core.lint.MetadataLinter;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import org.testng.annotations.Test;

import static io.github.alexxfromgit.taf.contract.examples.support.Owners.PLATFORM_TEAM;

/** Runs first in every suite: undocumented tests or leaked secrets fail the build before anything else runs. */
@Epic("Quality gates")
@Feature("Test metadata")
@Owner(PLATFORM_TEAM)
public class MetadataLintTest {

    @Test(description = "Every test has an owner and a feature; no secrets in properties files")
    public void testsAreDocumentedAndSecretFree() {
        MetadataLinter.forPackages("io.github.alexxfromgit.taf.contract.examples").assertClean();
    }
}
