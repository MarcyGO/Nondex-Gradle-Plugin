package edu.illinois.nondex.gradle.plugin;

import edu.illinois.nondex.gradle.tasks.NondexClean;
import edu.illinois.nondex.gradle.tasks.NondexDebug;
import edu.illinois.nondex.gradle.tasks.NondexTest;
import org.gradle.api.Project;
import org.gradle.api.Plugin;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

import static edu.illinois.nondex.gradle.constants.NondexGradlePluginConstants.NONDEX_VERSION;

public class NondexGradlePlugin implements Plugin<Project> {
    public void apply(Project project) {
        project.getTasks().create(NondexTest.getNAME(), NondexTest.class).init();
        project.getTasks().create(NondexClean.getNAME(), NondexClean.class).init();
        project.getTasks().create(NondexDebug.getNAME(), NondexDebug.class).init();
        downloadNonDexCommonJar(project);
    }
    private void downloadNonDexCommonJar(Project project) {
        Configuration config = project.getConfigurations().create("downloadNonDexCommonJar");
        MavenArtifactRepository mavenCentral = project.getRepositories().mavenCentral();
        project.getRepositories().add(mavenCentral);
        project.getDependencies().add(config.getName(), "edu.illinois:nondex-common:" + NONDEX_VERSION);
        config.resolve();
        project.getRepositories().remove(mavenCentral);
    }
}

