package edu.illinois.nondex.gradle.plugin;

import edu.illinois.nondex.gradle.tasks.NonDexClean;
import edu.illinois.nondex.gradle.tasks.NonDexDebug;
import edu.illinois.nondex.gradle.tasks.NonDexTest;
import org.gradle.api.Project;
import org.gradle.api.Plugin;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

import static edu.illinois.nondex.gradle.constants.NonDexGradlePluginConstants.NONDEX_VERSION;

public class NonDexGradlePlugin implements Plugin<Project> {
    public void apply(Project project) {
        project.getTasks().create(NonDexTest.getNAME(), NonDexTest.class);
        project.getTasks().create(NonDexClean.getNAME(), NonDexClean.class);
        project.getTasks().create(NonDexDebug.getNAME(), NonDexDebug.class);
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

