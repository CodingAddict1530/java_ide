package com.project.managers;

import com.project.custom_classes.RootTreeNode;
import com.project.gradle.GradleWrapper;
import com.project.utility.MainUtility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ProjectManager {

    private static final Logger logger = LogManager.getLogger(ProjectManager.class);

    public static final File APP_HOME = new File(System.getProperty("user.home"), "NotAnIDE_Projects");
    private static RootTreeNode currentProject;
    private static ArrayList<GradleWrapper> gradleWrappers = new ArrayList<>();

    public static RootTreeNode createProject(String name) {

        if (name == null || name.isEmpty()) {
            return null;
        }

        File projectDir = new File(APP_HOME, name);
        if (!projectDir.exists()) {
            if (projectDir.mkdirs()) {
                GradleWrapper gradleWrapper = new GradleWrapper(new File("lib/gradle-8.9"), projectDir);
                gradleWrappers.add(gradleWrapper);
                gradleWrapper.runInit();
                gradleWrapper.runWrapper();
            } else {
                logger.info("Failed to create project {}", name);
                return null;
            }
        } else {
            logger.info("Project {} already exists", name);
        }

        File gradleBuild = new File(projectDir, "build.gradle");

        while (!gradleBuild.exists()) {}
        try {
            Files.write(gradleBuild.toPath(), List.of(
                    "plugins {",
                        "\tid(\"java\")",
                        "}\n",
                        "group = \"com.project\"",
                        "version = \"1.0-SNAPSHOT\"\n",
                        "repositories {",
                        "\tmavenCentral()",
                        "}\n",
                        "dependencies {",
                        "\ttestImplementation(platform(\"org.junit:junit-bom:5.10.0\"))",
                        "\ttestImplementation(\"org.junit.jupiter:junit-jupiter\")",
                        "}\n",
                        "tasks.test {",
                        "\tuseJUnitPlatform()",
                        "}"
            ));
        } catch (Exception e) {
            logger.error(e);
        }

        openProject(projectDir.toPath());

        RootTreeNode root = new RootTreeNode(projectDir.toPath());
        openProject(root.getPath());

        return root;
    }

    public static void openProject(Path path) {

        path = DirectoryManager.openProject(path);
        if (path == null) {
            return;
        }
        boolean isProject = true;
        for (String s: new String[] {"src", "build.gradle", "settings.gradle"}) {
            File file = new File(path.toString(), s);
            if (!file.exists()) {
                isProject = false;
                break;
            }
        }
        if (isProject) {
            currentProject = new RootTreeNode(path);
        } else {
            currentProject = null;
        }

    }

    public static void deleteProject() {

        Path path = currentProject.getPath();
        if (MainUtility.confirm("Delete "+ path.getFileName(), "This entire project will be deleted")) {
            currentProject = null;
            DirectoryManager.deleteDirectory(path);
        }
        openProject(APP_HOME.toPath());

    }

    public static RootTreeNode getCurrentProject() {

        return currentProject;
    }

}
