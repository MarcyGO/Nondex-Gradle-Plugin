package edu.illinois.nondex.gradle.tasks;

import edu.illinois.nondex.gradle.internal.NondexTestExecuter;
import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec;
import org.gradle.api.internal.tasks.testing.TestExecuter;
import org.gradle.api.tasks.testing.Test;

import java.lang.reflect.Method;

public class NondexTest extends AbstractNondexTest {
    static final String NAME = "nondexTest";

    public static String getNAME() { return NAME; }

    public void init() {
        setDescription("Test with NonDex");
        setGroup("NonDex");
    }

    @Override
    public void executeTests() {
        setUpNondexTesting();
        NondexTestExecuter nondexTestExecuter = createNondexTestExecuter();
        setNondexAsTestExecuter(nondexTestExecuter);
        super.executeTests();
    }

    private NondexTestExecuter createNondexTestExecuter() {
        try {
            Method getExecuter = Test.class.getDeclaredMethod("createTestExecuter");
            getExecuter.setAccessible(true);
            TestExecuter<JvmTestExecutionSpec> delegate = (TestExecuter<JvmTestExecutionSpec>) getExecuter.invoke(this);
            return new NondexTestExecuter(this, delegate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setNondexAsTestExecuter(NondexTestExecuter nondexExecuter) {
        try {
            Method setTestExecuter = Test.class.getDeclaredMethod("setTestExecuter", TestExecuter.class);
            setTestExecuter.setAccessible(true);
            setTestExecuter.invoke(this, nondexExecuter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}