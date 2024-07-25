package com.project.gradle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class GradleWrapper {

    private static final Logger logger = LogManager.getLogger(GradleWrapper.class);

    private final File gradleHome;
    private final File projectHome;

    public GradleWrapper(File gradleHome, File projectHome) {

        this.gradleHome = gradleHome;
        this.projectHome = projectHome;
    }

    public void run() {

        // Determine the correct wrapper script based on OS
        String wrapperScript = System.getProperty("os.name").toLowerCase().contains("windows")
                ? "./gradlew.bat" : "./gradlew";

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(projectHome);
        processBuilder.command(wrapperScript, "run");
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.info("Gradle process failed with exit code {}", exitCode);
            }
        } catch (Exception e) {
            logger.error("Error running Gradle command init", e);
        }

    }

    public void runBuild() {

        // Determine the correct wrapper script based on OS
        String wrapperScript = System.getProperty("os.name").toLowerCase().contains("windows")
                ? "./gradlew.bat" : "./gradlew";

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(projectHome);
        processBuilder.command(wrapperScript, "build");
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.info("Gradle process failed with exit code {}", exitCode);
            }
        } catch (Exception e) {
            logger.error("Error running Gradle command init", e);
        }

    }

    public void runInit() {

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(projectHome);
        processBuilder.command(gradleHome.getAbsolutePath() + "\\bin\\gradle.bat", "init", "--type",
                "basic", "--dsl", "groovy");
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.info("Gradle process failed with exit code {}", exitCode);
            }
        } catch (Exception e) {
            logger.error("Error running Gradle command init", e);
        }

        for (String file : new String[] {"src", "src/main", "src/test", "src/main/java",
                "src/main/resources", "src/test/java", "src/test/resources"}) {
            if (new File(projectHome, file).mkdir()) {
                logger.info("Created {}", file);
            } else {
                logger.error("Failed to create {}", file);
            }
        }

    }

    public void runWrapper() {

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(projectHome);
        processBuilder.command(gradleHome.getAbsolutePath() + "\\bin\\gradle.bat", "wrapper");
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.info("Gradle process failed with exit code {}", exitCode);
            }
        } catch (Exception e) {
            logger.error("Error running Gradle command wrapper", e);
        }

    }

}
