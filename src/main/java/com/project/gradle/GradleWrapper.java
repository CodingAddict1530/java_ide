package com.project.gradle;

import com.project.custom_classes.ConsoleTextArea;
import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.concurrent.CountDownLatch;

public class GradleWrapper {

    private static final Logger logger = LogManager.getLogger(GradleWrapper.class);

    private final File gradleHome;
    private final File projectHome;
    private final ConsoleTextArea textArea;

    public GradleWrapper(File gradleHome, File projectHome, ConsoleTextArea textArea) {

        this.gradleHome = gradleHome;
        this.projectHome = projectHome;
        this.textArea = textArea;
    }

    private void setUp(BufferedWriter bufferedWriter) {

        textArea.setOnKeyPressed(event -> {

            if (event.getCode().toString().equals("ENTER")) {
                String input = getUserInput(textArea);
                try {
                    bufferedWriter.write(input + System.lineSeparator());
                    bufferedWriter.flush();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                textArea.setStyle(textArea.getCaretPosition(), textArea.getCaretPosition(),
                        "-fx-fill: white;");
            };

        });

    }

    public void run(String packageName) {

        // Determine the correct wrapper script based on OS
        String wrapperScript = System.getProperty("os.name").toLowerCase().contains("windows")
                ? "./gradlew.bat" : "./gradlew";

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(projectHome);
        if (packageName != null) {
            processBuilder.command(wrapperScript, "run", "-PmainClass=" + packageName);
        } else {
            processBuilder.command(wrapperScript, "run");
        }
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            setUp(bufferedWriter);
            writeToConsole(process, null);
        } catch (Exception e) {
            logger.error("Error running Gradle command run", e);
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
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            setUp(bufferedWriter);
            writeToConsole(process, null);
        } catch (Exception e) {
            logger.error("Error running Gradle command build", e);
        }

    }

    public void runInit(CountDownLatch latch) {

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(projectHome);
        processBuilder.command(gradleHome.getAbsolutePath() + "\\bin\\gradle.bat", "init", "--type",
                "basic", "--dsl", "groovy");
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            setUp(bufferedWriter);
            writeToConsole(process, latch);
        } catch (Exception e) {
            logger.error("Error running Gradle command init", e);
        }

    }

    public void runWrapper(CountDownLatch latch) {

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(projectHome);
        processBuilder.command(gradleHome.getAbsolutePath() + "\\bin\\gradle.bat", "wrapper");
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            setUp(bufferedWriter);
            writeToConsole(process, null);
        } catch (Exception e) {
            logger.error("Error running Gradle command wrapper", e);
        }
        latch.countDown();

    }

    public void writeToConsole(Process process, CountDownLatch latch) {

        Thread outputThread = new Thread(() -> {
            try (InputStream inputStream = process.getInputStream();
                 BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    // Update TextArea on JavaFX Application Thread
                    String finalLine = line;
                    Platform.runLater(() -> appendStyledText(textArea, finalLine + "\n", "white"));
                }
            } catch (IOException e) {
                logger.error(e);
            }
        });
        outputThread.start();

        Thread statusThread = new Thread(() -> {

            try {
                int exitCode = process.waitFor();
                if (latch != null) {
                    latch.countDown();
                }
                outputThread.join();
                Platform.runLater(() -> textArea.appendText("\nProcess Finished with exit code " + exitCode + "\n"));
            } catch (InterruptedException e) {
                logger.error(e);
            }

        });
        statusThread.start();

    }

    private static String getUserInput(ConsoleTextArea textArea) {

        int caretPosition = textArea.getCaretPosition();
        int start = caretPosition;

        for (int i = caretPosition - 1; i >= 0; i--) {
            String style = textArea.getStyleAtPosition(i);
            if (style.contains("-fx-fill: green;")) {
                start = i;
            } else {
                break;
            }
        }
        return textArea.getText(start, caretPosition);

    }

    private static void appendStyledText(ConsoleTextArea textArea, String text, String color) {

        int start = textArea.getLength();
        textArea.appendText(text);
        int end = textArea.getLength();
        textArea.setStyle(start, end, "-fx-fill: " + color + ";");

    }

}
