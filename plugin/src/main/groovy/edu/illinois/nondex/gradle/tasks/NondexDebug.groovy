package edu.illinois.nondex.gradle.tasks

import edu.illinois.nondex.gradle.internal.NondexDebugExecuter
import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.tasks.testing.Test

import java.lang.reflect.Method

class NondexDebug extends Test {
    static final String NAME = "nondexDebug"

    void init() {
        setDescription("Debug with NonDex")
        setGroup("NonDex")
        testLogging {
            exceptionFormat 'full'
        }
        NondexDebugExecuter nondexDebugExecuter = createNondexExecuter()
        setNondexAsTestExecuter(nondexDebugExecuter)
    }

    private NondexDebugExecuter createNondexExecuter() {
        try {
            Method getExecuter = Test.getDeclaredMethod("createTestExecuter")
            getExecuter.setAccessible(true)
            TestExecuter<JvmTestExecutionSpec> delegate = getExecuter.invoke(this)
            return new NondexDebugExecuter(delegate)
        } catch (Exception e) {
            throw new RuntimeException(e)
        }
    }
    private setNondexAsTestExecuter(NondexDebugExecuter nondexExecuter) {
        try {
            Method setTestExecuter = Test.getDeclaredMethod("setTestExecuter", TestExecuter.class)
            setTestExecuter.setAccessible(true)
            setTestExecuter.invoke(this, nondexExecuter)
        } catch (Exception e) {
            throw new RuntimeException(e)
        }
    }

}