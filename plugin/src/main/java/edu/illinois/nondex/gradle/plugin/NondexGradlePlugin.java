package edu.illinois.nondex.gradle.plugin;

import edu.illinois.nondex.gradle.tasks.NondexClean;
import edu.illinois.nondex.gradle.tasks.NondexHelp;
import edu.illinois.nondex.gradle.tasks.NondexTest;
import edu.illinois.nondex.gradle.tasks.NondexDebug;
import org.gradle.api.Project;
import org.gradle.api.Plugin;

public class NondexGradlePlugin implements Plugin<Project> {
    public void apply(Project project) {
        project.getTasks().create(NondexTest.getNAME(), NondexTest.class).init();
        project.getTasks().create(NondexClean.getNAME(), NondexClean.class).init();
        project.getTasks().create(NondexHelp.getNAME(), NondexHelp.class).init();
        project.getTasks().create(NondexDebug.getNAME(), NondexDebug.class).init();
    }
}

