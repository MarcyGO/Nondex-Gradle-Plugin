package edu.illinois.nondex.gradle.internal;

import edu.illinois.nondex.common.ConfigurationDefaults;
import edu.illinois.nondex.instr.Main;
import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec;
import org.gradle.api.internal.tasks.testing.TestExecuter;
import org.gradle.api.internal.tasks.testing.TestResultProcessor;
import org.gradle.api.tasks.testing.Test;

import java.io.File;
import java.nio.file.Paths;

public abstract class AbstractNondexExecuter implements TestExecuter<JvmTestExecutionSpec> {
    protected final Test testTask;
    protected final TestExecuter<JvmTestExecutionSpec> delegate;

    protected int seed;
    protected int numRuns;
    protected String baseDir;   // maven plugin use File here, and later convert it to string
    protected String runId;

    public AbstractNondexExecuter(Test testTask, TestExecuter<JvmTestExecutionSpec> delegate) {
        this.testTask = testTask;
        this.delegate = delegate;
        this.seed = Integer.parseInt(System.getProperty(ConfigurationDefaults.PROPERTY_SEED, ConfigurationDefaults.DEFAULT_SEED_STR));
        this.numRuns = Integer.parseInt(System.getProperty(ConfigurationDefaults.PROPERTY_NUM_RUNS, ConfigurationDefaults.DEFAULT_NUM_RUNS_STR));
        this.baseDir = System.getProperty("user.dir");
        this.runId = ConfigurationDefaults.PROPERTY_DEFAULT_RUN_ID;
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
    }

    @Override
    public void stopNow() {
        delegate.stopNow();
    }
}
