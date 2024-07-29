package com.project.managers;

import com.project.custom_classes.ConsoleTextArea;
import com.project.utility.ProjectWatcher;
import com.project.custom_classes.RootTreeNode;
import com.project.gradle.GradleWrapper;
import com.project.javaeditor.Application;
import com.project.utility.MainUtility;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ProjectManager {

    private static final Logger logger = LogManager.getLogger(ProjectManager.class);
    private static ConsoleTextArea textArea;

    public static final File APP_HOME = new File(System.getProperty("user.home"), "NotAnIDE_Projects");
    private static RootTreeNode currentProject;
    private static GradleWrapper gradleWrapper = null;

    public static void createProject(String name) {

        if (name == null || name.isEmpty()) {
            return;
        }

        File projectDir = new File(APP_HOME, name);
        Task<Void> backgroundTask = new Task<>() {

            @Override
            protected Void call() {
                if (!projectDir.exists()) {
                    if (projectDir.mkdirs()) {
                        Application.fadeStage();
                        Platform.runLater(() -> openProject(projectDir.toPath()));
                        gradleWrapper = new GradleWrapper(new File("lib/gradle-8.9"), projectDir, textArea);
                        try {
                            CountDownLatch latch = new CountDownLatch(1);
                            gradleWrapper.runInit(latch);
                            latch.await();
                            gradleWrapper.runWrapper(latch);
                        } catch (Exception e) {
                            logger.error(e);
                        }
                    } else {
                        logger.info("Failed to create project {}", name);
                        return null;
                    }
                } else {
                    logger.info("Project {} already exists", name);
                }
                return null;
            }

        };

        backgroundTask.setOnSucceeded(event -> {
            File gradleBuild = new File(projectDir, "build.gradle");
            try {
                Files.write(gradleBuild.toPath(), List.of(
                        "plugins {",
                        "\tid(\"java\")",
                        "}\n",
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
                        "}\n",
                        "task run(type: JavaExec) {",
                        "\tif (project.hasProperty('mainClass')) {",
                        "\t\tmain = project.mainClass",
                        "\t} else {",
                        "\t\tmain = 'Main'",
                        "\t}",
                        "\tclasspath = sourceSets.main.runtimeClasspath",
                        "}"
                ));
            } catch (Exception e) {
                logger.error(e);
            }

            for (String file : new String[] {"src", "src/main", "src/test", "src/main/java",
                    "src/main/resources", "src/test/java", "src/test/resources"}) {
                if (new File(projectDir, file).mkdir()) {
                    logger.info("Created {}", file);
                } else {
                    logger.error("Failed to create {}", file);
                }
            }
            RootTreeNode root = new RootTreeNode(projectDir.toPath());
            Platform.runLater(() -> openProject(root.getPath()));
            System.out.println("Project build done");

        });

        backgroundTask.setOnFailed(event -> {
            System.out.println("Background task failed because: " + Arrays.toString(backgroundTask.getException().getStackTrace()));
            logger.error("Background task failed");
        });

        new Thread(backgroundTask).start();

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
            gradleWrapper = new GradleWrapper(new File("lib/gradle-8.9"), path.toFile(), textArea);
            JLSManager.changeWorkSpaceFolder(path.toUri().toString(), path.toFile().getName(), true);
        } else {
            currentProject = null;
        }
        if (!ProjectWatcher.getIsWatching()) {
            ProjectWatcher.startWatching();
        }
        ProjectWatcher.unregisterAllPaths();
        ProjectWatcher.registerPath(path);

    }

    public static void deleteProject() {

        Path path = currentProject.getPath();
        if (MainUtility.confirm("Delete "+ path.getFileName(), "This entire project will be deleted")) {
            currentProject = null;
            DirectoryManager.deleteDirectory(path);
        }
        JLSManager.changeWorkSpaceFolder(path.toUri().toString(), path.toFile().getName(), false);
        openProject(APP_HOME.toPath());

    }

    public static RootTreeNode getCurrentProject() {

        return currentProject;
    }

    public static void setTextArea(ConsoleTextArea textArea) {

        ProjectManager.textArea = textArea;
    }
}
