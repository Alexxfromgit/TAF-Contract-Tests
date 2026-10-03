package io.github.alexxfromgit.taf.contract.core.testng;

import io.github.alexxfromgit.taf.contract.core.allure.AllureResults;
import io.github.alexxfromgit.taf.contract.core.config.TafConfig;
import io.github.alexxfromgit.taf.contract.core.contract.coverage.CoverageReport;
import io.github.alexxfromgit.taf.contract.core.contract.drift.DriftCollector;
import io.github.alexxfromgit.taf.contract.core.report.ReportFiles;
import io.github.alexxfromgit.taf.contract.core.stub.EmbeddedWireMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IExecutionListener;

import java.util.Map;
import java.util.TreeMap;

/**
 * Run-level lifecycle: installs Allure categories and starts the embedded stub server before the first test;
 * writes the coverage/drift files, the Allure environment and stops the stub server after the last one.
 */
public class TafExecutionListener implements IExecutionListener {

    private static final Logger LOG = LoggerFactory.getLogger(TafExecutionListener.class);

    @Override
    public void onExecutionStart() {
        AllureResults.installCategories();
        EmbeddedWireMock.startIfEnabled();
    }

    @Override
    public void onExecutionFinish() {
        try {
            CoverageReport coverage = CoverageReport.build();
            if (!coverage.services().isEmpty()) {
                ReportFiles.write("api-coverage.json", coverage.toJson());
                ReportFiles.write("api-coverage.html", coverage.toHtml());
                LOG.info(coverage.summary());
            }
            if (!DriftCollector.isEmpty()) {
                ReportFiles.write("api-drift.json", DriftCollector.toJson());
                ReportFiles.write("api-drift.html", DriftCollector.toHtml());
                LOG.warn("API drift detected on {} endpoint(s): see {}", DriftCollector.snapshot().size(),
                        ReportFiles.directory().resolve("api-drift.html"));
            }
            AllureResults.writeEnvironment(serviceUris());
        } catch (RuntimeException e) {
            LOG.error("Could not write end-of-run reports", e);
        } finally {
            EmbeddedWireMock.stop();
        }
    }

    private static Map<String, String> serviceUris() {
        Map<String, String> uris = new TreeMap<>();
        TafConfig.get().withPrefix("services.").forEach((key, value) -> {
            if (key.endsWith(".base-uri")) {
                uris.put("service." + key.substring(0, key.length() - ".base-uri".length()), value);
            }
        });
        return uris;
    }
}
