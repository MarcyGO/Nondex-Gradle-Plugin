package edu.illinois.nondex.gradle.internal;

import edu.illinois.nondex.common.Configuration;
import edu.illinois.nondex.common.ConfigurationDefaults;
import edu.illinois.nondex.common.Logger;
import edu.illinois.nondex.common.Utils;
import org.gradle.api.internal.artifacts.mvnsettings.DefaultMavenFileLocations;
import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec;
import org.gradle.api.internal.tasks.testing.TestExecuter;
import org.gradle.api.internal.tasks.testing.TestFramework;
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.TestFilter;
import org.gradle.process.JavaForkOptions;
import org.gradle.api.internal.tasks.testing.junit.JUnitTestFramework;
import org.gradle.util.GradleVersion;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.regex.Pattern;

public class NondexRun extends CleanRun {
    private NondexRun(Test testTask, TestExecuter<JvmTestExecutionSpec> delegate, JvmTestExecutionSpec spec,
                      RetryTestProcessor testResultProcessor, String nondexDir) {
        super(testTask, delegate, spec, testResultProcessor, Utils.getFreshExecutionId(), nondexDir);
    }

    public NondexRun(int seed, Test testTask, TestExecuter<JvmTestExecutionSpec> delegate, JvmTestExecutionSpec originalSpec,
                           RetryTestProcessor testResultProcessor, String nondexDir, String nondexJarDir) {
        this(testTask, delegate, originalSpec, testResultProcessor, nondexDir);
        // does not support all parameter reading at this point
        this.configuration = new Configuration(ConfigurationDefaults.DEFAULT_MODE, seed, Pattern.compile(ConfigurationDefaults.DEFAULT_FILTER),
                ConfigurationDefaults.DEFAULT_START, ConfigurationDefaults.DEFAULT_END, nondexDir, nondexJarDir, null,
                this.executionId, Logger.getGlobal().getLoggingLevel());
        this.originalSpec = this.createRetryJvmExecutionSpec();
    }

    // constructor used for debug
    public NondexRun(Configuration config, long start, long end, boolean print, String test,
                     Test testTask, TestExecuter<JvmTestExecutionSpec> delegate, JvmTestExecutionSpec spec,
                     RetryTestProcessor testResultProcessor) {
        this(testTask, delegate, spec, testResultProcessor, config.nondexDir);
        this.configuration = new Configuration(config.mode, config.seed, config.filter, start,
                end, config.nondexDir, config.nondexJarDir, test, this.executionId,
                Logger.getGlobal().getLoggingLevel(), print);
        this.originalSpec = this.createRetryJvmExecutionSpec();
    }


    private JvmTestExecutionSpec createRetryJvmExecutionSpec() {
        JvmTestExecutionSpec spec = this.originalSpec;
        JavaForkOptions option = spec.getJavaForkOptions();
        TestFramework testFramework = spec.getTestFramework();
        if (this.configuration.testName != null) {
            DefaultTestFilter filter = new DefaultTestFilter();
            filter.setIncludePatterns(this.configuration.testName);
            // testFramework = testFramework.copyWithFilters(filter);
            testFramework = new JUnitTestFramework(this.testTask, filter);
        }
        Set<String> test;
        if (this.configuration.testName != null) {
            test = new HashSet<>();
            test.add(this.configuration.testName);
            test = Collections.unmodifiableSet(test);
        } else {
            test = spec.getPreviousFailedTestClasses();
        }
        List<String> arg = this.setupArgline();
        option.setJvmArgs(arg);
        if (GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version("6.4")) >= 0) {
            // This constructor is in Gradle 6.4+
            return new JvmTestExecutionSpec(
                    testFramework,
                    spec.getClasspath(),
                    spec.getModulePath(),
                    spec.getCandidateClassFiles(),
                    spec.isScanForTestClasses(),
                    spec.getTestClassesDirs(),
                    spec.getPath(),
                    spec.getIdentityPath(),
                    spec.getForkEvery(),
                    option,
                    spec.getMaxParallelForks(),
                    test
            );
        } else {
            // This constructor is in Gradle 4.7+
            return new JvmTestExecutionSpec(
                    testFramework,
                    spec.getClasspath(),
                    spec.getCandidateClassFiles(),
                    spec.isScanForTestClasses(),
                    spec.getTestClassesDirs(),
                    spec.getPath(),
                    spec.getIdentityPath(),
                    spec.getForkEvery(),
                    option,
                    spec.getMaxParallelForks(),
                    test
            );
        }
    }

    private List<String> setupArgline() {
        String pathToNondex = getPathToNondexJar();
        List<String> arg = new ArrayList<>();
        if (!System.getProperty("java.version").startsWith("1.")) {
            arg.add("--patch-module=java.base=" + pathToNondex);
            arg.add("--add-exports=java.base/edu.illinois.nondex.common=ALL-UNNAMED");
            arg.add("--add-exports=java.base/edu.illinois.nondex.shuffling=ALL-UNNAMED");
        } else {
            arg.add("-Xbootclasspath/p:" + pathToNondex);
        }
        arg.add("-D" + ConfigurationDefaults.PROPERTY_EXECUTION_ID + "=" + this.configuration.executionId);
        arg.add("-D" + ConfigurationDefaults.PROPERTY_SEED + "=" + this.configuration.seed);
        // if (this.configuration.testName != null) {
        //     arg.add("--tests " + this.configuration.testName);
        // }
        return arg;
    }

    private String getPathToNondexJar() {
        DefaultMavenFileLocations loc = new DefaultMavenFileLocations();
        File mvnLoc = loc.getUserMavenDir();
        String result = Paths.get(this.configuration.nondexJarDir, ConfigurationDefaults.INSTRUMENTATION_JAR) + File.pathSeparator
                + Paths.get(mvnLoc.toString(),
                "repository", "edu", "illinois", "nondex-common", ConfigurationDefaults.VERSION,
                "nondex-common-" + ConfigurationDefaults.VERSION + ".jar");
        return result;
    }
}
