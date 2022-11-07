package edu.illinois.nondex.gradle.internal;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.illinois.nondex.common.Configuration;
import edu.illinois.nondex.common.ConfigurationDefaults;
import edu.illinois.nondex.common.Level;
import edu.illinois.nondex.common.Logger;
import edu.illinois.nondex.common.Utils;

import org.apache.commons.lang3.tuple.Pair;

import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec;
import org.gradle.api.internal.tasks.testing.TestExecuter;

public class DebugTask {
    private String test;
    private List<Configuration> failingConfigurations;

    private final TestExecuter<JvmTestExecutionSpec> delegate;
    private JvmTestExecutionSpec originalSpec;
    private RetryTestProcessor testResultProcessor;

    public DebugTask(String test, TestExecuter<JvmTestExecutionSpec> delegate, JvmTestExecutionSpec originalSpec,
                     RetryTestProcessor testResultProcessor,
                     List<Configuration> failingConfigurations) {
        this.test = test;
        this.delegate = delegate;
        this.originalSpec = originalSpec;
        this.testResultProcessor = testResultProcessor;
        this.failingConfigurations = failingConfigurations;
    }

    public String debug() {
        assert (!this.failingConfigurations.isEmpty());
        String result = this.tryDebugSeeds();
        if (result != null) {
            return result;
        }

        return "cannot reproduce. may be flaky due to other causes";
    }

    private String tryDebugSeeds() {
        // get a list of configuration to debug
        // if all configuration (seeds) pass dry, empty list
        List<Configuration> debuggedOnes = this.debugWithConfigurations(this.failingConfigurations);

        if (debuggedOnes.size() > 0) {
            return makeResultString(debuggedOnes);
        }

        // The seeds that failed with the full test-suite no longer fail
        // Searching for different seeds
        // Logger.getGlobal().log(Level.FINE, "TRYING NEW SEEDS");
        // List<Configuration> retryWOtherSeeds = this.createNewSeedsToRetry();
        // debuggedOnes = this.debugWithConfigurations(retryWOtherSeeds);

        // if (debuggedOnes.size() > 0) {
        //     return makeResultString(debuggedOnes);
        // }

        return null;
    }

    private String makeResultString(List<Configuration> debuggedOnes) {
        StringBuilder sb = new StringBuilder();
        for (Configuration config : debuggedOnes) {
            if (config == null) {
                continue;
            }
            sb.append(config.toArgLine());
            sb.append("\nDEBUG RESULTS FOR ");
            sb.append(config.testName);
            sb.append(" AND SEED: ");
            sb.append(config.seed);
            sb.append(" AT: ");
            sb.append(config.getDebugPath());
            sb.append('\n');
        }
        return sb.toString();
    }

    private List<Configuration> debugWithConfigurations(List<Configuration> failingConfigurations) {
        List<Configuration> allDebuggedConfigs = new LinkedList<Configuration>();
        /**
         * for each configuration in the failingConfiguration list, 
         * if the configuration fail the test, add configuration with different start and end to the list
         * use them to debug
         */
        for (Configuration config : failingConfigurations) {
            Configuration dryConfig;
            if ((dryConfig = this.failsOnDry(config)) != null) {
                // Get all debugged points and just add them to the full list
                List<Configuration> debuggedConfigs = this.startDebugBinary(dryConfig);
                allDebuggedConfigs.addAll(debuggedConfigs);
            }
        }

        return allDebuggedConfigs;
    }

    public List<Configuration> startDebugBinary(Configuration config) {
        List<Configuration> allFailingConfigurations = new LinkedList<Configuration>();

        List<Pair<Pair<Long, Long>, Configuration>> pairs = new LinkedList<Pair<Pair<Long, Long>, Configuration>>();
        pairs.add((Pair<Pair<Long, Long>, Configuration>)Pair.of((Pair<Long, Long>)Pair.of(0L,
            (long)config.getInvocationCount()), config));

        Configuration failingConfiguration = null;
        while (pairs.size() > 0) {
            Pair<Pair<Long, Long>, Configuration> pair = pairs.remove(0);
            Pair<Long, Long> range = pair.getLeft();
            failingConfiguration = pair.getRight();
            long start = range.getLeft();
            long end = range.getRight();

            if (start < end) {
                Logger.getGlobal().log(Level.INFO, "Debugging binary for " + this.test + " " + start + " : " + end);

                boolean binarySuccess = false;
                long midPoint = (start + end) / 2;
                if ((failingConfiguration = this.failsWithConfig(config, start, midPoint)) != null) {
                    pairs.add(Pair.of((Pair<Long, Long>)Pair.of(start, midPoint), failingConfiguration));
                    binarySuccess = true;
                }
                if ((failingConfiguration = this.failsWithConfig(config, midPoint + 1, end)) != null) {
                    pairs.add(Pair.of((Pair<Long, Long>)Pair.of(midPoint + 1, end), failingConfiguration));
                    binarySuccess = true;
                }

                // If both halves fail, try the entire range
                if (!binarySuccess) {
                    Logger.getGlobal().log(Level.SEVERE, "Binary splitting did not work. Going to linear");
                    allFailingConfigurations.addAll(this.startDebugLinear(config, start, end));
                }
            } else {
                // Since start <= end is always true, this branch means start == end, so reached end
                if (failingConfiguration != null) {
                    allFailingConfigurations.add(this.reportDebugInfo(failingConfiguration));
                }
            }
        }

        return allFailingConfigurations;
    }

    private Configuration failsOnDry(Configuration config) {
        // dry: full range of integer
        return this.failsWithConfig(config, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private Configuration reportDebugInfo(Configuration failingConfiguration) {
        return this.failsWithConfig(failingConfiguration, failingConfiguration.start, failingConfiguration.end, true);
    }

    private Configuration failsWithConfig(Configuration config, long start, long end) {
        return this.failsWithConfig(config, start, end, false);
    }

    public List<Configuration> startDebugLinear(Configuration config, long start, long end) {
        List<Configuration> allFailingConfigurations = new LinkedList<Configuration>();

        List<Pair<Pair<Long, Long>, Configuration>> pairs = new LinkedList<Pair<Pair<Long, Long>, Configuration>>();
        pairs.add((Pair<Pair<Long, Long>, Configuration>)Pair.of((Pair<Long, Long>)Pair.of(start, end),
            config));

        Configuration failingConfiguration = null;
        while (pairs.size() > 0) {
            Pair<Pair<Long, Long>, Configuration> pair = pairs.remove(0);
            Pair<Long, Long> range = pair.getLeft();
            failingConfiguration = pair.getRight();
            long localStart = range.getLeft();
            long localEnd = range.getRight();

            if (localStart < localEnd) {
                Logger.getGlobal().log(Level.INFO, "Debugging linear for " + this.test + " "
                    + localStart + " : " + localEnd);

                boolean found = false;
                if ((failingConfiguration = this.failsWithConfig(config, localStart, localEnd - 1)) != null) {
                    pairs.add(Pair.of((Pair<Long, Long>)Pair.of(localStart, localEnd - 1), failingConfiguration));
                    found = true;
                }
                if ((failingConfiguration = this.failsWithConfig(config, localStart + 1, localEnd)) != null) {
                    pairs.add(Pair.of((Pair<Long, Long>)Pair.of(localStart + 1, localEnd), failingConfiguration));
                    found = true;
                }

                if (!found) {
                    Logger.getGlobal().log(Level.FINE, "Refining did not work. Does not fail with linear on range "
                        + localStart + " : " + localEnd + ".");
                }
            } else {
                // Since start <= end is always true, this branch means start == end, so reached end
                if (failingConfiguration != null) {
                    allFailingConfigurations.add(this.reportDebugInfo(failingConfiguration));
                }
            }
        }
        return allFailingConfigurations;
    }

    /**
     * return null if the configuration does not fail the test
     */
    private Configuration failsWithConfig(Configuration config, long start, long end, boolean print) {
        NondexRun execution = new NondexRun(config, start, end, print, this.test,
                     this.delegate, this.originalSpec, this.testResultProcessor);
        RetryTestProcessor result = execution.run();
        // check if fail
        Set<String> fail = result.getFailingTests();
        if (fail.isEmpty()) {
            return null;
        } else {
            return execution.getConfiguration();
        }
    }
}