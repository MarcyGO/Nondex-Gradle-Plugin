package edu.illinois.nondex.gradle.internal;

import edu.illinois.nondex.common.Configuration;
import edu.illinois.nondex.common.ConfigurationDefaults;
import edu.illinois.nondex.common.Level;
import edu.illinois.nondex.common.Logger;
import edu.illinois.nondex.common.Utils;
import edu.illinois.nondex.instr.Main;
import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec;
import org.gradle.api.internal.tasks.testing.TestExecuter;
import org.gradle.api.internal.tasks.testing.TestResultProcessor;
import org.gradle.api.tasks.VerificationException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class NondexTestExecuter implements TestExecuter<JvmTestExecutionSpec> {

    private final TestExecuter<JvmTestExecutionSpec> delegate;
    private final List<NondexRun> nondexRuns = new LinkedList<>();
    private final int seed;
    private final int numRuns;

    public NondexTestExecuter(TestExecuter<JvmTestExecutionSpec> delegate) {
        this.delegate = delegate;
        this.seed = Integer.parseInt(System.getProperty(ConfigurationDefaults.PROPERTY_SEED, ConfigurationDefaults.DEFAULT_SEED_STR));
        this.numRuns = Integer.parseInt(System.getProperty(ConfigurationDefaults.PROPERTY_NUM_RUNS, ConfigurationDefaults.DEFAULT_NUM_RUNS_STR));
        if (this.numRuns < 1) {
            throw new IllegalArgumentException("The number of nondexRuns cannot be less than 1");
        }
    }

    @Override
    public void execute(JvmTestExecutionSpec spec, TestResultProcessor testResultProcessor) {
        try {
            File fileForJar = Paths.get(System.getProperty("user.dir"),
                    ConfigurationDefaults.DEFAULT_NONDEX_JAR_DIR).toFile();
            fileForJar.mkdirs();
            Main.main(Paths.get(fileForJar.getAbsolutePath(),
                    ConfigurationDefaults.INSTRUMENTATION_JAR).toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        NondexTestProcessor nondexTestProcessor = new NondexTestProcessor(testResultProcessor);

        CleanRun cleanRun = new CleanRun(this.delegate, spec, nondexTestProcessor,
                System.getProperty("user.dir")+ File.separator + ConfigurationDefaults.DEFAULT_NONDEX_DIR);
        nondexTestProcessor = cleanRun.run();

        for (int currentRun = 0; currentRun < numRuns; ++currentRun) {
            nondexTestProcessor.reset(currentRun + 1 == numRuns);
            NondexRun nondexRun = new NondexRun(Utils.computeIthSeed(currentRun - 1, false, this.seed),
                    this.delegate, spec, nondexTestProcessor,
                    System.getProperty("user.dir")+ File.separator + ConfigurationDefaults.DEFAULT_NONDEX_DIR,
                    System.getProperty("user.dir")+ File.separator + ConfigurationDefaults.DEFAULT_NONDEX_JAR_DIR);
            this.nondexRuns.add(nondexRun);
            nondexTestProcessor = nondexRun.run();
            this.writeCurrentRunInfo(nondexRun);
        }
        this.writeCurrentRunInfo(cleanRun);
        this.postProcessExecutions(cleanRun);

        Configuration config = this.nondexRuns.get(0).getConfiguration();
        this.printSummary(cleanRun, config);
    }

    @Override
    public void stopNow() {
        delegate.stopNow();
    }

    private void postProcessExecutions(CleanRun cleanRun) {
        Collection<String> failedInClean = cleanRun.getConfiguration().getFailedTests();
        for (NondexRun nondexRun : this.nondexRuns) {
            nondexRun.getConfiguration().filterTests(failedInClean);
        }
    }

    private void writeCurrentRunInfo(CleanRun run) {
        try {
            Files.write(this.nondexRuns.get(0).getConfiguration().getRunFilePath(),
                    (run.getConfiguration().executionId + String.format("%n")).getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            Logger.getGlobal().log(Level.SEVERE, "Cannot write execution id to current run file", ex);
        }
    }

    private void printSummary(CleanRun cleanRun, Configuration config) {
        Set<String> allFailures = new LinkedHashSet<>();
        Logger.getGlobal().log(Level.INFO, "NonDex SUMMARY:");
        for (CleanRun run : this.nondexRuns) {
            this.printExecutionResults(allFailures, run);
        }

        if (!cleanRun.getConfiguration().getFailedTests().isEmpty()) {
            Logger.getGlobal().log(Level.INFO, "Tests are failing without NonDex.");
            this.printExecutionResults(allFailures, cleanRun);
        }

        boolean cleanRunHasFailingTests = !cleanRun.getConfiguration().getFailedTests().isEmpty();
        allFailures.removeAll(cleanRun.getConfiguration().getFailedTests());

        Logger.getGlobal().log(Level.INFO, "Across all seeds:");
        for (String test : allFailures) {
            Logger.getGlobal().log(Level.INFO, test);
        }

        this.generateHtml(allFailures, config);

        if (cleanRunHasFailingTests || !allFailures.isEmpty()) {
            throw new VerificationException("There were failing tests");
        }
    }

    private void generateHtml(Set<String> allFailures, Configuration config) {
        String head = "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "<title>Test Results</title>"
                + "<style>"
                + "table { border-collapse: collapse; width: 100%; }"
                + "th { height: 50%; }"
                + "th, td { padding: 10px; text-align: left; }"
                + "tr:nth-child(even) {background-color:#f2f2f2;}"
                + ".x { color: red; font-size: 150%;}"
                + ".✓ { color: green; font-size: 150%;}"
                + "</style>"
                + "</head>";
        StringBuilder html = new StringBuilder(head + "<body>" + "<table>");

        html.append("<thead><tr>").append("<th>Test Name</th>");
        for (int iter = 0; iter < this.nondexRuns.size(); iter++) {
            html.append("<th>").append(this.nondexRuns.get(iter).getConfiguration().seed).append("</th>");
        }
        html.append("</tr></thead>").append("<tbody>");
        for (String failure : allFailures) {
            html.append("<tr><td>").append(failure).append("</td>");
            for (CleanRun run : this.nondexRuns) {
                boolean testDidFail = false;
                for (String test : run.getConfiguration().getFailedTests()) {
                    if (test.equals(failure)) {
                        testDidFail = true;
                    }
                }
                if (testDidFail) {
                    html.append("<td class=\"x\">&#10006;</td>");
                } else {
                    html.append("<td class=\"✓\">&#10004;</td>");
                }
            }
            html.append("</tr>");
        }
        html.append("</tbody></table></body></html>");

        File nondexDir = config.getNondexDir().toFile();
        File htmlFile = new File(nondexDir, "test_results.html");
        try {
            PrintWriter htmlPrinter = new PrintWriter(htmlFile);
            htmlPrinter.print(html);
            htmlPrinter.close();
        } catch (FileNotFoundException ex) {
            Logger.getGlobal().log(Level.INFO, "File Missing.  But that shouldn't happen...");
        }
        Logger.getGlobal().log(Level.INFO, "Test results can be found at: ");
        Logger.getGlobal().log(Level.INFO, "file://" + htmlFile.getPath());
    }

    private void printExecutionResults(Set<String> allFailures, CleanRun run) {
        Logger.getGlobal().log(Level.INFO, "*********");
        Logger.getGlobal().log(Level.INFO, "mvn nondex:nondex " + run.getConfiguration().toArgLine());
        Collection<String> failedTests = run.getConfiguration().getFailedTests();
        if (failedTests.isEmpty()) {
            Logger.getGlobal().log(Level.INFO, "No Test Failed with this configuration.");
        }
        for (String test : failedTests) {
            allFailures.add(test);
            Logger.getGlobal().log(Level.WARNING, test);
        }
        Logger.getGlobal().log(Level.INFO, "*********");
    }
}
