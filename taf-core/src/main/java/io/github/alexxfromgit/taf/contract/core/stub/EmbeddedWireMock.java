package io.github.alexxfromgit.taf.contract.core.stub;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.alexxfromgit.taf.contract.core.config.RuntimeOverrides;
import io.github.alexxfromgit.taf.contract.core.config.TafConfig;
import io.github.alexxfromgit.taf.contract.core.failure.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Optional in-process WireMock server that serves the stub mappings under {@code src/test/resources/wiremock}
 * ({@code mappings/*.json}, {@code __files/*}). Started before the first test when {@code stub.enabled=true};
 * its URL is published as the runtime configuration value {@code stub.base-url}, so environment files can say
 * {@code services.petstore.base-uri=${stub.base-url}/api/v3}.
 * <p>
 * Use it to run contract tests offline and deterministically (CI), to reproduce provider edge cases,
 * or to develop tests before the provider exists.
 */
public final class EmbeddedWireMock {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedWireMock.class);
    private static volatile WireMockServer server;

    private EmbeddedWireMock() {
    }

    public static synchronized void startIfEnabled() {
        TafConfig config = TafConfig.get();
        if (server != null || !config.bool("stub.enabled", false)) {
            return;
        }
        WireMockServer instance = new WireMockServer(options()
                .port(config.integer("stub.port", 0))
                .usingFilesUnderClasspath(config.string("stub.root", "wiremock"))
                .globalTemplating(false));
        instance.start();
        server = instance;
        String baseUrl = "http://localhost:" + instance.port();
        RuntimeOverrides.put("stub.base-url", baseUrl);
        LOG.info("Embedded WireMock started at {} ({} stub mappings)", baseUrl, instance.getStubMappings().size());
    }

    public static synchronized void stop() {
        if (server != null) {
            server.stop();
            server = null;
            RuntimeOverrides.remove("stub.base-url");
        }
    }

    public static boolean isRunning() {
        return server != null && server.isRunning();
    }

    /** The running server, e.g. to add stubs or verify requests in a test. */
    public static WireMockServer server() {
        if (server == null) {
            throw new FrameworkException("Embedded WireMock is not running. Set stub.enabled=true.");
        }
        return server;
    }
}
