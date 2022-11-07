package edu.illinois.nondex.gradle.internal;

import edu.illinois.nondex.common.Configuration;
import edu.illinois.nondex.common.Level;
import edu.illinois.nondex.common.Logger;
import edu.illinois.nondex.common.Utils;
import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec;
import org.gradle.api.internal.tasks.testing.TestExecuter;
import org.gradle.api.tasks.testing.Test;

import java.util.Set;

public class CleanRun {
    protected Configuration configuration;
    protected final String executionId;

    protected final Test testTask;
    private final TestExecuter<JvmTestExecutionSpec> delegate;
    protected JvmTestExecutionSpec originalSpec;
    private RetryTestProcessor testResultProcessor;

    protected CleanRun(Test testTask, TestExecuter<JvmTestExecutionSpec> delegate, JvmTestExecutionSpec originalSpec,
                             RetryTestProcessor testResultProcessor, String executionId, String nondexDir) {
        this.testTask = testTask;
        this.delegate = delegate;
        this.originalSpec = originalSpec;
        this.testResultProcessor = testResultProcessor;
        this.executionId = executionId;
        this.configuration = new Configuration(executionId, nondexDir);
    }

    public CleanRun(Test testTask, TestExecuter<JvmTestExecutionSpec> delegate, JvmTestExecutionSpec originalSpec,
                          RetryTestProcessor testResultProcessor, String nondexDir) {
        this(testTask, delegate, originalSpec, testResultProcessor, "clean_" + Utils.getFreshExecutionId(), nondexDir);
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    public RetryTestProcessor run() {
        Logger.getGlobal().log(Level.CONFIG, this.configuration.toString());
        this.delegate.execute(this.originalSpec, this.testResultProcessor);
        this.setFailures();
        return this.testResultProcessor;
    }

    private void setFailures() {
        Set<String> failingTests = this.testResultProcessor.getFailingTests();
        this.configuration.setFailures(failingTests);
    }
}
