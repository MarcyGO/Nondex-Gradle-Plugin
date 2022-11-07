package edu.illinois.nondex.gradle.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.illinois.nondex.common.Configuration;
import edu.illinois.nondex.common.ConfigurationDefaults;
import edu.illinois.nondex.common.Level;
import edu.illinois.nondex.common.Logger;
import edu.illinois.nondex.common.Utils;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;

import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec;
import org.gradle.api.internal.tasks.testing.TestExecuter;
import org.gradle.api.internal.tasks.testing.TestResultProcessor;

public class NondexDebugExecuter extends AbstractNondexExecuter {

    private List<String> executions = new LinkedList<>(); // a list of executionId
    private ListMultimap<String, Configuration> testsFailing = LinkedListMultimap.create(); // failed tests and their configurations

    public NondexDebugExecuter(TestExecuter<JvmTestExecutionSpec> delegate) {
        super(delegate);
    }

    @Override
    public void execute(JvmTestExecutionSpec spec, TestResultProcessor testResultProcessor) {
        super.execute(spec, testResultProcessor);
        this.parseExecutions();
        this.parseTests();

        Map<String, String> testToRepro = new HashMap<>();
        RetryTestProcessor retryTestProcessor = new RetryTestProcessor(testResultProcessor);

        /* 
         * for each failed (flaky) tests, use their failing configurations to debug
         */
        for (String test : this.testsFailing.keySet()) {
            DebugTask debugging = new DebugTask(test, this.delegate, spec,
                    retryTestProcessor, this.testsFailing.get(test));
            String repro = debugging.debug();
            testToRepro.put(test, repro);
        }

        Logger.getGlobal().log(Level.WARNING, "*********");
        for (Map.Entry<String, String> test : testToRepro.entrySet()) {
            Logger.getGlobal().log(Level.WARNING, "REPRO for " + test.getKey() + ":" + String.format("%n")
                               + "mvn nondex:nondex " + test.getValue());
            
        }
    }

    private void parseTests() {
        for (String execution : this.executions) {
            Properties props = Utils.openPropertiesFrom(Paths.get(this.baseDir,
                    ConfigurationDefaults.DEFAULT_NONDEX_DIR, execution,
                    ConfigurationDefaults.CONFIGURATION_FILE));
            Configuration config = Configuration.parseArgs(props);
            for (String test : config.getFailedTests()) {
                this.testsFailing.put(test, config);
            }
        }
    }

    private void parseExecutions() {
        File run = Paths.get(this.baseDir, ConfigurationDefaults.DEFAULT_NONDEX_DIR, this.runId)
                .toFile();

        try (BufferedReader br = new BufferedReader(new FileReader(run))) {
            String line;
            while ((line = br.readLine()) != null) {
                this.executions.add(line.trim());
            }
        } catch (IOException ex) {
            Logger.getGlobal().log(Level.SEVERE, "Could not open run file to parse executions", ex);
        }
    }
}
