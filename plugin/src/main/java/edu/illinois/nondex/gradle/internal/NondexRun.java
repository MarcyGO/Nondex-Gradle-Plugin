package edu.illinois.nondex.gradle.internal;

import edu.illinois.nondex.common.Configuration;
import edu.illinois.nondex.common.ConfigurationDefaults;
import edu.illinois.nondex.common.Logger;
import edu.illinois.nondex.common.Utils;
import org.gradle.api.internal.artifacts.mvnsettings.DefaultMavenFileLocations;
import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec;
import org.gradle.api.internal.tasks.testing.TestExecuter;
import org.gradle.process.JavaForkOptions;
import org.gradle.util.GradleVersion;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class NondexRun extends CleanRun {
    private NondexRun(TestExecuter<JvmTestExecutionSpec> delegate, JvmTestExecutionSpec spec,
                      NondexTestProcessor testResultProcessor, String nondexDir) {
        super(delegate, spec, testResultProcessor, Utils.getFreshExecutionId(), nondexDir);
    }

    public NondexRun(int seed, TestExecuter<JvmTestExecutionSpec> delegate, JvmTestExecutionSpec originalSpec,
                     NondexTestProcessor testResultProcessor, String nondexDir, String nondexJarDir) {
        this(delegate, originalSpec, testResultProcessor, nondexDir);
        this.configuration = new Configuration(ConfigurationDefaults.DEFAULT_MODE, seed, Pattern.compile(ConfigurationDefaults.DEFAULT_FILTER),
                ConfigurationDefaults.DEFAULT_START, ConfigurationDefaults.DEFAULT_END, nondexDir, nondexJarDir, null,
                this.executionId, Logger.getGlobal().getLoggingLevel());
        this.originalSpec = this.createNondexJvmExecutionSpec();
    }

    private JvmTestExecutionSpec createNondexJvmExecutionSpec() {
        JvmTestExecutionSpec spec = this.originalSpec;
        JavaForkOptions option = spec.getJavaForkOptions();
        List<String> arg = this.setupArgline();
        option.setJvmArgs(arg);
        if (GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version("6.4")) >= 0) {
            // This constructor is in Gradle 6.4+
            return new JvmTestExecutionSpec(
                    spec.getTestFramework(),
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
                    spec.getPreviousFailedTestClasses()
            );
        } else {
            // This constructor is in Gradle 4.7+
            return new JvmTestExecutionSpec(
                    spec.getTestFramework(),
                    spec.getClasspath(),
                    spec.getCandidateClassFiles(),
                    spec.isScanForTestClasses(),
                    spec.getTestClassesDirs(),
                    spec.getPath(),
                    spec.getIdentityPath(),
                    spec.getForkEvery(),
                    option,
                    spec.getMaxParallelForks(),
                    spec.getPreviousFailedTestClasses()
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
