package io.github.alexxfromgit.taf.contract.core.contract.coverage;

import io.github.alexxfromgit.taf.contract.core.contract.openapi.OpenApiSpecsAccess;
import org.testng.annotations.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class PathTemplateTest {

    @Test
    public void matchesParametersAndLiterals() {
        PathTemplate template = PathTemplate.of("/pet/{petId}/uploadImage");
        assertThat(template.matches("/pet/10/uploadImage")).isTrue();
        assertThat(template.matches("/pet/10/uploadImage/")).isTrue();
        assertThat(template.matches("/pet/10")).isFalse();
        assertThat(template.matches("/pet/10/uploadImage?x=1")).isTrue();
        assertThat(PathTemplate.of("/file/{name}.json").matches("/file/report.json")).isTrue();
        assertThat(PathTemplate.of("/").matches("/")).isTrue();
    }

    @Test
    public void literalSegmentsAreMoreSpecific() {
        assertThat(PathTemplate.of("/pet/findByStatus").specificity())
                .isGreaterThan(PathTemplate.of("/pet/{petId}").specificity());
    }

    @Test
    public void catalogPicksTheMostSpecificEndpoint() {
        EndpointCatalog catalog = EndpointCatalog.from(OpenApiSpecsAccess.load("classpath:openapi/petstore.json").model());
        assertThat(catalog.endpoints()).hasSize(19);
        assertThat(catalog.match("GET", "/pet/findByStatus").orElseThrow().label()).isEqualTo("GET /pet/findByStatus");
        assertThat(catalog.match("GET", "/pet/10").orElseThrow().label()).isEqualTo("GET /pet/{petId}");
        assertThat(catalog.match("PATCH", "/pet/10")).isEmpty();
    }

    @Test
    public void coverageCombinesExecutedAndDeclaredCalls() {
        EndpointCatalog catalog = EndpointCatalog.from(OpenApiSpecsAccess.load("classpath:openapi/petstore.json").model());
        CoverageReport.ServiceCoverage coverage = CoverageReport.build("petstore", catalog,
                Set.of(new CoverageCollector.ExecutedCall("petstore", "GET", "/pet/10", 200),
                        new CoverageCollector.ExecutedCall("petstore", "GET", "/pet/11", 404),
                        new CoverageCollector.ExecutedCall("petstore", "GET", "/not/in/spec", 404),
                        new CoverageCollector.ExecutedCall("other", "GET", "/store/inventory", 200)),
                Set.of(new CoverageCollector.DeclaredCoverage("", "POST", "/pet/{petId}/uploadImage", "T.upload")));

        assertThat(coverage.coveredCount()).isEqualTo(2);
        assertThat(coverage.endpoints()).hasSize(19);
        assertThat(coverage.undocumented()).containsExactly("GET /not/in/spec");
        assertThat(coverage.endpoints().stream()
                .filter(e -> e.endpoint().label().equals("GET /pet/{petId}")).findFirst().orElseThrow().statuses())
                .containsExactly(200, 404);
    }
}
