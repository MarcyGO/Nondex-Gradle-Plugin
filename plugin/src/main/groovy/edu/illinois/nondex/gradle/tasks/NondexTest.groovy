package edu.illinois.nondex.gradle.tasks

import edu.illinois.nondex.gradle.internal.NondexTestExecuter
import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.tasks.testing.Test

import java.lang.reflect.Method

class NondexTest extends Test {
    static final String NAME = "nondexTest"

    void init() {
        setDescription("Test with NonDex")
        setGroup("NonDex")
        testLogging {
            exceptionFormat 'full'
        }
        NondexTestExecuter nondexTestExecuter = createNondexExecuter()
        setNondexAsTestExecuter(nondexTestExecuter)
    }

    private NondexTestExecuter createNondexExecuter() {
        try {
            Method getExecuter = Test.getDeclaredMethod("createTestExecuter")
            getExecuter.setAccessible(true)
            TestExecuter<JvmTestExecutionSpec> delegate = getExecuter.invoke(this)
            return new NondexTestExecuter(this, delegate)
        } catch (Exception e) {
            throw new RuntimeException(e)
        }
    }
    private setNondexAsTestExecuter(NondexTestExecuter nondexExecuter) {
        try {
            Method setTestExecuter = Test.getDeclaredMethod("setTestExecuter", TestExecuter.class)
            setTestExecuter.setAccessible(true)
            setTestExecuter.invoke(this, nondexExecuter)
        } catch (Exception e) {
            throw new RuntimeException(e)
        }
    }
}