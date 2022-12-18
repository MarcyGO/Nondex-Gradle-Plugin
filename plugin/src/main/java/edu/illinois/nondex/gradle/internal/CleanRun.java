package edu.illinois.nondex.gradle.internal;

import edu.illinois.nondex.common.Configuration;
import edu.illinois.nondex.common.Level;
import edu.illinois.nondex.common.Logger;
import edu.illinois.nondex.common.Utils;
import edu.illinois.nondex.gradle.tasks.AbstractNondexTest;
import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec;
import org.gradle.api.internal.tasks.testing.TestExecuter;
import org.gradle.process.JavaForkOptions;
import org.gradle.util.GradleVersion;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class CleanRun {
    protected Configuration configuration;
    protected final String executionId;
    protected JvmTestExecutionSpec originalSpec;
    protected AbstractNondexTest nondexTestTask;
    private final TestExecuter<JvmTestExecutionSpec> delegate;
    private NondexTestProcessor nondexTestProcessor;

    protected CleanRun(AbstractNondexTest nondexTestTask, TestExecuter<JvmTestExecutionSpec> delegate, JvmTestExecutionSpec originalSpec,
                       NondexTestProcessor nondexTestProcessor, String executionId, String nondexDir) {
        this.nondexTestTask = nondexTestTask;
        this.delegate = delegate;
        this.nondexTestProcessor = nondexTestProcessor;
        this.executionId = executionId;
        this.configuration = new Configuration(executionId, nondexDir);
        this.originalSpec = this.createJvmExecutionSpecWithArgs(setupArgline(), originalSpec);
    }

    public CleanRun(AbstractNondexTest nondexTestTask, TestExecuter<JvmTestExecutionSpec> delegate, JvmTestExecutionSpec originalSpec,
                    NondexTestProcessor nondexTestProcessor, String nondexDir) {
        this(nondexTestTask, delegate, originalSpec, nondexTestProcessor, "clean_" + Utils.getFreshExecutionId(), nondexDir);
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    public NondexTestProcessor run() {
        Logger.getGlobal().log(Level.CONFIG, this.configuration.toString());
        this.delegate.execute(this.originalSpec, this.nondexTestProcessor);
        this.setFailures();
        return this.nondexTestProcessor;
    }

    private void setFailures() {
        Set<String> failingTests = this.nondexTestProcessor.getFailingTests();
        this.configuration.setFailures(failingTests);
    }

    protected JvmTestExecutionSpec createJvmExecutionSpecWithArgs(List<String> args, JvmTestExecutionSpec originalSpec) {
        JavaForkOptions option = originalSpec.getJavaForkOptions();
        option.setJvmArgs(args);
        if (GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version("6.4")) >= 0) {
            // This constructor is in Gradle 6.4+
            return new JvmTestExecutionSpec(
                    originalSpec.getTestFramework(),
                    originalSpec.getClasspath(),
                    originalSpec.getModulePath(),
                    originalSpec.getCandidateClassFiles(),
                    originalSpec.isScanForTestClasses(),
                    originalSpec.getTestClassesDirs(),
                    originalSpec.getPath(),
                    originalSpec.getIdentityPath(),
                    originalSpec.getForkEvery(),
                    option,
                    originalSpec.getMaxParallelForks(),
                    originalSpec.getPreviousFailedTestClasses()
            );
        } else {
            // This constructor is in Gradle 4.7+
            return new JvmTestExecutionSpec(
                    originalSpec.getTestFramework(),
                    originalSpec.getClasspath(),
                    originalSpec.getCandidateClassFiles(),
                    originalSpec.isScanForTestClasses(),
                    originalSpec.getTestClassesDirs(),
                    originalSpec.getPath(),
                    originalSpec.getIdentityPath(),
                    originalSpec.getForkEvery(),
                    option,
                    originalSpec.getMaxParallelForks(),
                    originalSpec.getPreviousFailedTestClasses()
            );
        }
    }

    protected List<String> setupArgline() {
        List<String> argline = new LinkedList<>();
        argline.addAll(nondexTestTask.getOriginalArgLine());
        return argline;
    }
}
