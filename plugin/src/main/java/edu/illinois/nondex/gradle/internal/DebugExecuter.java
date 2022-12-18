package edu.illinois.nondex.gradle.internal;

import edu.illinois.nondex.common.Configuration;
import edu.illinois.nondex.gradle.tasks.AbstractNondexTest;
import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec;
import org.gradle.api.internal.tasks.testing.TestExecuter;
import org.gradle.api.internal.tasks.testing.TestResultProcessor;

public class DebugExecuter implements TestExecuter<JvmTestExecutionSpec> {

    private final AbstractNondexTest nondexTestTask;
    private final TestExecuter<JvmTestExecutionSpec> delegate;
    private Configuration configuration;

    public DebugExecuter(AbstractNondexTest nondexTestTask, TestExecuter<JvmTestExecutionSpec> delegate) {
        this.nondexTestTask = nondexTestTask;
        this.delegate = delegate;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void execute(JvmTestExecutionSpec spec, TestResultProcessor testResultProcessor) {
        NondexTestProcessor nondexTestProcessor = new NondexTestProcessor(testResultProcessor);
        NondexRun nondexRun  = new NondexRun(configuration, nondexTestTask, this.delegate, spec, nondexTestProcessor);
        nondexRun.run();
    }

    @Override
    public void stopNow() {
        delegate.stopNow();
    }
}
