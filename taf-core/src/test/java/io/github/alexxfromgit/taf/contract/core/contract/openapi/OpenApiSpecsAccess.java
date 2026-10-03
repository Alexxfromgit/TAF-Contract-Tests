package io.github.alexxfromgit.taf.contract.core.contract.openapi;

/** Test helper exposing package-private loading of OpenAPI documents. */
public final class OpenApiSpecsAccess {

    private OpenApiSpecsAccess() {
    }

    public static OpenApiSpecs.Spec load(String location) {
        return OpenApiSpecs.load(location);
    }
}
