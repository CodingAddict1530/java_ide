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

package com.project.gradle;

import com.project.custom_classes.ConsoleTextArea;
import com.project.java_code_processing.Debugger;
import com.project.utility.MainUtility;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

/**
 * Carries out gradle related tasks.
 */
public class GradleWrapper {

    /**
     * The logger for the class.
     */
    private static final Logger logger = LoggerFactory.getLogger(GradleWrapper.class);

    /**
     * The root directory of gradle files.
     */
    private final File gradleHome;

    /**
     * The root directory of the project.
     */
    private final File projectHome;

    /**
     * The ConsoleTextArea to output to.
     */
    private final ConsoleTextArea textArea;

    /**
     * Either "./gradlew.bat" or "./gradlew" depending on OS.
     */
    private final String wrapperScript;

    /**
     * The Node that displays current variable values while debugging.
     */
    private static ScrollPane variableArea;

    /**
     * The process for gradle run and debug tasks.
     */
    private Process runProcess;

    /**
     * Used to write to a gradle process.
     */
    private BufferedWriter bufferedWriter;

    /**
     * An Object used for synchronization.
     */
    private static final Object monitor = new Object();

    /**
     * The port number the jdk listens to when started in debug mode.
     */
    private int port =  0;

    /**
     * The debugger.
     */
    private Debugger debugger;

    /**
     * Instantiates a new GradleWrapper.
     *
     * @param gradleHome The root directory of gradle files.
     * @param projectHome The root directory of the project.
     * @param textArea The ConsoleTextArea to output to.
     */
    public GradleWrapper(File gradleHome, File projectHome, ConsoleTextArea textArea) {

        this.gradleHome = gradleHome;
        this.projectHome = projectHome;
        this.textArea = textArea;
        this.runProcess = null;

        // Determine the correct wrapper script based on OS
        this.wrapperScript = System.getProperty("os.name").toLowerCase().contains("windows")
                ? new File(projectHome, "gradlew.bat").toPath().toAbsolutePath().toString() :
                new File(projectHome, "gradlew").toPath().toAbsolutePath().toString();
    }

    /**
     * Sets up the ConsoleTextArea to output and take user input as well.
     *
     * @param process The Process.
     */
    private void setUp(Process process) {

        bufferedWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        textArea.setOnKeyPressed(event -> {

            // ENTER Key will mean the user has submitted their input.
            if (event.getCode().toString().equals("ENTER") && !event.isControlDown() && !event.isShiftDown()) {
                String input = getUserInput(textArea);
                try {
                    bufferedWriter.write(input + System.lineSeparator());
                    bufferedWriter.flush();
                } catch (Exception e) {
                    logger.error("Tried writing to process and failed:\n", e);
                }
                textArea.setStyle(textArea.getCaretPosition(), textArea.getCaretPosition(),
                        "-fx-fill: white;");
            }

        });

    }

    /**
     * Executes the application, or the class.
     * Executes gradle run command.
     *
     * @param packageName Class name with the package as prefix.
     */
    public void run(String packageName, String command) {

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(projectHome);

        // Check if it is a debug session.
        if (command.equals("debug")) {
            port = MainUtility.getPort();
            if (port == -1) {
                MainUtility.popup(new Label("Error getting port, try again"));
                return;
            }
        }

        // Check whether a package name is provided.
        if (packageName != null) {
            if (command.equals("debug")) {
                processBuilder.command(wrapperScript, command, "-PmainClass=" + packageName, "-Pport=" + port);
            } else {
                processBuilder.command(wrapperScript, command, "-PmainClass=" + packageName);
            }
        } else {

            // Else consider the project to be an application with a Main Class.
            if (command.equals("debug")) {
                processBuilder.command(wrapperScript, command, "-Pport=" + port);
            } else {
                processBuilder.command(wrapperScript, command);
            }
        }

        try {

            // Set working directory for the process.
            processBuilder.directory(projectHome);
            runProcess = processBuilder.start();
            setUp(runProcess);
            writeToConsole(runProcess, null);

            // Check if it is a debug session.
            if (command.equals("debug")) {

                // Wait for signal that jvm is ready.
                synchronized (monitor) {
                    monitor.wait(15000);
                }

                // Create a debugger and run it.
                debugger = new Debugger(port, variableArea);
                debugger.run();
            }

        } catch (Exception e) {
            logger.error("Error running Gradle command {}",command, e);
        }

    }

    /**
     * Executes gradle build command.
     */
    public void runBuild() {

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(projectHome);
        processBuilder.command(wrapperScript, "build");

        try {
            Process process = processBuilder.start();
            writeToConsole(process, null);
        } catch (Exception e) {
            logger.error("Error running Gradle command build", e);
        }
    }

    /**
     * Executes gradle init command.
     *
     * @param latch to sync threads.
     */
    public void runInit(CountDownLatch latch) {

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(projectHome);
        processBuilder.command(gradleHome.getAbsolutePath() + "\\bin\\gradle.bat", "init", "--type",
                "basic", "--dsl", "groovy");

        try {
            Process process = processBuilder.start();
            writeToConsole(process, latch);
        } catch (Exception e) {
            logger.error("Error running Gradle command init", e);
        }

    }

    /**
     * Executes gradle wrapper command.
     *
     * @param latch to sync threads.
     */
    public void runWrapper(CountDownLatch latch) {

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(projectHome);
        processBuilder.command(gradleHome.getAbsolutePath() + "\\bin\\gradle.bat", "wrapper");

        try {
            Process process = processBuilder.start();
            writeToConsole(process, null);
        } catch (Exception e) {
            logger.error("Error running Gradle command wrapper", e);
        }
        latch.countDown();

    }

    /**
     * Writes output from a process on the ConsoleTextArea.
     *
     * @param process The process writing.
     * @param latch Blocks waiting thread until the process finishes.
     */
    public void writeToConsole(Process process, CountDownLatch latch) {

        Thread outputThread = new Thread(() -> {

            // Set up BufferedReader for the output and error streams.
            try (InputStream inputStream = process.getInputStream();
                 InputStream errorStream = process.getErrorStream();
                 BufferedReader oBufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                 BufferedReader eBufferedReader = new BufferedReader(new InputStreamReader(errorStream))) {

                String line;
                while ((line = oBufferedReader.readLine()) != null) {

                    if (line.contains("Listening for transport dt_socket at address")) {
                        synchronized (monitor) {
                            monitor.notify(); // Notify that JVM is ready
                        }
                    }
                    // Update TextArea on JavaFX Application Thread
                    String finalLine = line;
                    Platform.runLater(() -> appendStyledText(textArea, finalLine + "\n", "white"));
                }
                while ((line = eBufferedReader.readLine()) != null) {
                    // Update TextArea on JavaFX Application Thread
                    String finalLine = line;
                    Platform.runLater(() -> appendStyledText(textArea, finalLine + "\n", "red"));
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        });
        outputThread.setDaemon(true);
        outputThread.start();

        Thread statusThread = new Thread(() -> {

            try {
                int exitCode = process.waitFor();
                if (latch != null) {

                    // Release the blocked thread.
                    latch.countDown();
                }
                outputThread.join();

                // Scrap the Debugger.
                if (debugger != null && debugger.hasFinished()) {
                    debugger = null;
                }
                bufferedWriter.close();
                Platform.runLater(() -> textArea.appendText("\nProcess Finished with exit code " + exitCode + "\n"));
            } catch (Exception e) {
                logger.error(e.getMessage());
            }

        });
        statusThread.setDaemon(true);
        statusThread.start();

    }

    /**
     * Retrieves text entered by user.
     * User text is colored green and this is used to determine their input.
     *
     * @param textArea The ConsoleTextArea.
     * @return The user input.
     */
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

    /**
     * Appends text in the ConsoleTextArea with a particular color.
     *
     * @param textArea The ConsoleTextArea.
     * @param text The text.
     * @param color The color.
     */
    private static void appendStyledText(ConsoleTextArea textArea, String text, String color) {

        // Unprotect text to write to the TextArea.
        textArea.unprotectText();
        int start = textArea.getLength();
        textArea.appendText(text);
        int end = textArea.getLength();
        textArea.setStyle(start, end, "-fx-fill: " + color + ";");

        // Protect the text again.
        textArea.protectText();

    }

    /**
     * Terminates the runProcess.
     */
    public void terminate() {

        if (runProcess != null && runProcess.isAlive()) {
            runProcess.destroy();
            Platform.runLater(() -> {
                try {
                    textArea.appendText("\nProcess Finished with exit code " + runProcess.waitFor() + "\n");
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            });
        }

    }

    /**
     * Sets up variableArea.
     *
     * @param variableArea variableArea.
     */
    public void setVariableArea(ScrollPane variableArea) {

        GradleWrapper.variableArea = variableArea;
    }

    /**
     * Sends a notification to the Debugger.
     * Could be a step event or a continue notification.
     *
     * @param notification The notification.
     */
    public void notifyDebugger(String notification) {

        if (debugger == null) {
            return;
        }
        debugger.getNotification(notification);

    }

    /**
     * Retrieves the call stack from the Debugger.
     *
     * @return A Tooltip with the call stack.
     */
    public Tooltip getCallStack() {

        try {
            return debugger.getCallStack();
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }

    }

}
