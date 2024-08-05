/*
 * Copyright 2024 Alexis Mugisha
 * https://github.com/CodingAddict1530
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.project.managers;

import com.project.custom_classes.ConsoleTextArea;
import com.project.utility.ProjectWatcher;
import com.project.custom_classes.RootTreeNode;
import com.project.gradle.GradleWrapper;
import com.project.javaeditor.Application;
import com.project.utility.MainUtility;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Handles project related operations.
 */
public class ProjectManager {

    /**
     * The logger for the class.
     */
    private static final Logger logger = LogManager.getLogger(ProjectManager.class);

    /**
     * The ConsoleTextArea.
     */
    private static ConsoleTextArea textArea;

    /**
     * The Label displaying the current project.
     */
    private static Label projectName;

    /**
     * Constant defining the home of all projects.
     */
    public static final File APP_HOME = new File(System.getProperty("user.home"), "FusionProjects");

    /**
     * The current project.
     */
    private static RootTreeNode currentProject;

    /**
     * A GradleWrapper instance for the current project.
     */
    private static GradleWrapper gradleWrapper = null;

    /**
     * Creates a new project.
     *
     * @param name The name of the project.
     */
    public static void createProject(String name) {

        if (name == null || name.isEmpty()) {
            return;
        }

        File projectDir = new File(APP_HOME, name);

        // Run the code below in the background.
        Task<Void> backgroundTask = new Task<>() {

            /**
             * The code to execute.
             *
             * @return null.
             */
            @Override
            protected Void call() {

                // Check id the project doesn't already exist.
                if (!projectDir.exists()) {
                    if (projectDir.mkdirs()) {

                        // Create an animation for opening a new project.
                        Application.fadeStage();

                        // Open the new project from the JavaFX thread and continue execution.
                        Platform.runLater(() -> openProject(projectDir.toPath()));

                        gradleWrapper = new GradleWrapper(new File("lib/gradle-8.9"), projectDir, textArea);
                        try {
                            CountDownLatch latch = new CountDownLatch(1);

                            // Initialize the gradle project.
                            gradleWrapper.runInit(latch);

                            // Wait for the task to finish.
                            // This avoids gradle wrapper from creating files before init is done.
                            // Which would otherwise make gradle throw an Exception.
                            latch.await();

                            // Run gradle wrapper and create the wrapper.
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

        // Execute the code below if the task completes successfully.
        // It is crucial that this code executes after gradle innit has finished executing.
        // Otherwise, the newly created directories will make gradle throw an exception.
        backgroundTask.setOnSucceeded(event -> {

            // Edit build.gradle.
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

            // src directory and its components (Basic gradle project doesn't automatically create them).
            for (String file : new String[] {"src", "src/main", "src/test", "src/main/java",
                    "src/main/resources", "src/test/java", "src/test/resources"}) {
                if (new File(projectDir, file).mkdir()) {
                    logger.info("Created {}", file);
                } else {
                    logger.error("Failed to create {}", file);
                }
            }
            System.out.println("Project build done");

        });

        backgroundTask.setOnFailed(event -> {
            System.out.println("Background task failed because: " + Arrays.toString(backgroundTask.getException().getStackTrace()));
            logger.error("Background task failed");
        });

        // Start the background task on a new thread to avoid blocking the main thread.
        new Thread(backgroundTask).start();

    }

    /**
     * Opens an existing project.
     *
     * @param path The Path to the root directory.
     */
    public static void openProject(Path path) {

        path = DirectoryManager.openProject(path);
        if (path == null) {
            return;
        }

        // Check whether it is a valid project.
        boolean isProject = true;
        for (String s: new String[] {"src", "build.gradle", "settings.gradle"}) {
            File file = new File(path.toString(), s);
            if (!file.exists()) {
                isProject = false;
                break;
            }
        }
        if (isProject) {

            // Update relevant data.
            projectName.setText(path.toFile().getName());
            currentProject = new RootTreeNode(path);

            // Create a new GradleWrapper for the project.
            gradleWrapper = new GradleWrapper(new File("lib/gradle-8.9"), path.toFile(), textArea);

            // Notify the server about the new project.
            JLSManager.changeWorkSpaceFolder(path.toUri().toString(), path.toFile().getName(), true);
        } else {
            currentProject = null;
        }

        // Start watching the entire project for changes on the directories.
        // Changes in contents won't be detected.
        if (!ProjectWatcher.getIsWatching()) {
            ProjectWatcher.startWatching();
        }
        ProjectWatcher.unregisterAllPaths();
        ProjectWatcher.registerPath(path);

    }

    /**
     * Deletes the current project.
     */
    public static void deleteProject() {

        Path path = currentProject.getPath();

        // Prompt user to confirm.
        if (MainUtility.confirm("Delete "+ path.getFileName(), "This entire project will be deleted")) {
            currentProject = null;
            DirectoryManager.deleteDirectory(path);
        }

        // Notify the server about the change in workspace folders.
        JLSManager.changeWorkSpaceFolder(path.toUri().toString(), path.toFile().getName(), false);

        // Open the home of projects.
        openProject(APP_HOME.toPath());

    }

    /**
     * Retrieves the current project.
     *
     * @return The current project.
     */
    public static RootTreeNode getCurrentProject() {

        return currentProject;
    }

    /**
     * Sets up the ConsoleTextArea.
     *
     * @param textArea The ConsoleTextArea.
     */
    public static void setConsoleTextArea(ConsoleTextArea textArea) {

        ProjectManager.textArea = textArea;
    }

    /**
     * Sets up projectName.
     *
     * @param projectName projectName.
     */
    public static void setProjectName(Label projectName) {

        ProjectManager.projectName = projectName;
    }

}
