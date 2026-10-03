package io.github.alexxfromgit.taf.contract.core.contract;

import io.github.alexxfromgit.taf.contract.core.allure.AllureResults;
import io.github.alexxfromgit.taf.contract.core.config.TafConfig;
import io.github.alexxfromgit.taf.contract.core.contract.coverage.CoverageReport;
import io.github.alexxfromgit.taf.contract.core.contract.drift.DriftCollector;
import io.github.alexxfromgit.taf.contract.core.failure.PotentialDefectException;
import io.github.alexxfromgit.taf.contract.core.log.Log;
import io.github.alexxfromgit.taf.contract.core.report.ReportFiles;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import org.testng.annotations.Test;

/**
 * Suite-level reports, run as ordinary tests so they appear in Allure (with HTML/JSON attachments) and can
 * fail the build. Add this class as the LAST {@code <test>} of a suite:
 * <pre>{@code
 * <test name="Framework reports">
 *     <classes><class name="io.github.alexxfromgit.taf.contract.core.contract.ContractReports"/></classes>
 * </test>
 * }</pre>
 */
@Epic("Framework reports")
@Owner("contract-taf")
public class ContractReports {

    @Test(description = "API coverage meets contract.coverage.fail-under")
    @Feature("API coverage")
    public void apiCoverage() {
        CoverageReport report = CoverageReport.build();
        ReportFiles.write("api-coverage.json", report.toJson());
        ReportFiles.write("api-coverage.html", report.toHtml());
        AllureResults.attachHtml("API coverage", report.toHtml());
        AllureResults.attachJson("API coverage (json)", report.toJson());
        Log.info(report.summary());
        System.out.println(report.summary());

        double required = Double.parseDouble(TafConfig.get().string("contract.coverage.fail-under", "0"));
        if (report.minPercent() < required) {
            throw new PotentialDefectException(String.format(
                    "API coverage %.1f%% is below contract.coverage.fail-under=%.1f%%%n%s",
                    report.minPercent(), required, report.summary()));
        }
    }

    @Test(description = "API drift: undeclared response fields")
    @Feature("API drift")
    public void apiDrift() {
        ReportFiles.write("api-drift.json", DriftCollector.toJson());
        ReportFiles.write("api-drift.html", DriftCollector.toHtml());
        AllureResults.attachHtml("API drift", DriftCollector.toHtml());
        AllureResults.attachJson("API drift (json)", DriftCollector.toJson());
        int endpoints = DriftCollector.snapshot().size();
        Log.info(endpoints == 0 ? "No drift detected"
                : "Drift detected on " + endpoints + " endpoint(s) - see the 'API drift' attachment");
        if (endpoints > 0 && TafConfig.get().bool("contract.drift.fail", false)) {
            throw new PotentialDefectException("API drift detected (contract.drift.fail=true):\n"
                    + DriftCollector.snapshot());
        }
    }
}
