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

package com.project.java_code_processing;

import com.project.custom_classes.ConsoleTextArea;
import com.project.custom_classes.RootTreeNode;
import com.project.managers.FileManager;
import com.project.utility.MainUtility;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import javax.tools.SimpleJavaFileObject;
import javax.tools.JavaFileManager;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Executes java code.
 */
public class JavaCodeExecutor {

    /**
     * The logger for the class.
     */
    private static final Logger logger = LoggerFactory.getLogger(JavaCodeExecutor.class);

    /**
     * A process to execute the tasks.
     */
    private static Process process;

    /**
     * A BufferedWriter for writing output.
     */
    private static BufferedWriter bufferedWriter;

    /**
     * The ConsoleTextArea to interact with.
     */
    private static ConsoleTextArea consoleTextArea;

    /**
     * The Node that displays current variable values while debugging.
     */
    private static ScrollPane variableArea;

    /**
     * An Object used for Synchronization.
     */
    private static final Object monitor = new Object();

    /**
     * The Debugger.
     */
    private static Debugger debugger;

    /**
     * Compiles and executes a java file.
     *
     * @param file The file to be executed.
     * @param project The project the file belongs to.
     * @return 1 if it couldn't read the file, 2 due to a compilation error, 3 if it couldn't delete the .class file, otherwise 0.
     */
    public static int run(File file, RootTreeNode project, String command) {

        logger.info("Running {}", file.getAbsolutePath());

        int returnValue = 0;

        // Read the contents of the file.
        String sourceCode = readSourceFile(file);
        if (sourceCode == null) {
            return 1;
        }

        Path buildPath;
        if (project == null) {
            buildPath = null;
        } else {

            // If it's part of a project, .class files will be in bin folder.
            buildPath = new File(project.getPath().toString(), "bin").toPath();
        }

        // Compile the code.
        boolean compile = compile(sourceCode, file, consoleTextArea, (buildPath == null) ? null :
                buildPath.toString());
        if (compile) {
            logger.info("Compilation was successful");
            consoleTextArea.setOnKeyPressed(event -> {

                if (event.getCode().toString().equals("ENTER")) {
                    String input = getUserInput(consoleTextArea);
                    try {
                        bufferedWriter.write(input + System.lineSeparator());
                        bufferedWriter.flush();
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                    }
                    consoleTextArea.setStyle(consoleTextArea.getCaretPosition(), consoleTextArea.getCaretPosition(),
                            "-fx-fill: white;");
                }

            });
            logger.info("Console input handling setup complete");
            switch (execute(file, consoleTextArea, buildPath, command)) {
                case 0:
                    logger.info("Execution successful");
                    break;
                case 1:
                    logger.info("File not under src, but in project");
                    break;
                case 2:
                    logger.info("Couldn't delete .class file");
                    break;
            }

            // Remove the event handler.
            consoleTextArea.setOnKeyPressed(null);
            logger.info("Console input handling removal complete");
        } else {
            returnValue = 2;
            logger.info("Compilation failed");
        }
        return returnValue;

    }

    /**
     * Executes the .class file.
     *
     * @param file The actual file.
     * @param textArea The ConsoleTextArea.
     * @param buildPath The build path.
     * @return 1 if file is in a project but not in src directory,
     * 2 if .class file couldn't be deleted,
     * 3 for unknown command,
     * 4 if JVM cant be found on designated port number for debugging,
     * otherwise 0.
     */
    private static int execute(File file, ConsoleTextArea textArea, Path buildPath, String command) {

        String[] parts1 = file.getName().split("\\.");
        String[] parts2 = file.getPath().split("\\\\");
        ArrayList<String> fromSrc = new ArrayList<>();
        boolean start = false;
        for (String part : parts2) {
            if (part.equals("src") || start) {
                if (start) {
                    fromSrc.add(part);
                }
                start = true;
            }
        }
        if (fromSrc.isEmpty() && buildPath != null) {
            MainUtility.popup(new Label("File not in src!"));
            return 1;
        }
        fromSrc.remove(fromSrc.size() - 1);
        StringBuilder filePath = new StringBuilder();
        for (String part : fromSrc) {
            filePath.append(part).append(".");
        }

        // final one item array to be able to use it in a lambda.
        final int[] returnValue = {0};

        // Get a port.
        int port = MainUtility.getPort();
        ProcessBuilder processBuilder;
        if (command.equals("run")) {
            processBuilder = new ProcessBuilder("java", "-cp",
                    ".", (filePath.isEmpty()) ? parts1[0] : filePath + parts1[0]);
        } else if (command.equals("debug")) {
            processBuilder = new ProcessBuilder("java",
                    "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:" + port, "-cp",
                    ".", (filePath.isEmpty()) ? parts1[0] : filePath + parts1[0]);
        } else {
            return 3;
        }
        processBuilder.directory((buildPath == null) ? new File("src/classes") :
                buildPath.toFile());

        try {
            process = processBuilder.start();
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

            // Check if it is a debug session.
            if (command.equals("debug")) {

                // Wait for signal that jvm is ready.
                synchronized (monitor) {
                    monitor.wait(15000);
                }

                // Create a debugger and run it.
                debugger = new Debugger(port,variableArea);
                debugger.run();
            }

            // Thread to read output to textArea
            Thread outputThread = new Thread(() -> {

                try (InputStream inputStream = process.getInputStream();
                     InputStream errorStream = process.getErrorStream();
                     BufferedReader oBufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                     BufferedReader eBufferedReader = new BufferedReader(new InputStreamReader(errorStream))) {

                    String line;
                    while ((line = oBufferedReader.readLine()) != null) {

                        // Update TextArea on JavaFX Application Thread
                        String finalLine = line;
                        javafx.application.Platform.runLater(() -> appendStyledText(textArea, finalLine + "\n", "black"));
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

            // Thread to check exit status
            Thread statusThread = new Thread(() -> {

                try {
                    int exitCode = process.waitFor();
                    if (debugger != null && debugger.hasFinished()) {
                        debugger = null;
                    }
                    outputThread.join();
                    javafx.application.Platform.runLater(() -> textArea.appendText("\nProcess Finished with exit code " + exitCode + "\n"));
                    if (buildPath == null) {
                        if (!new File("src/classes/" + parts1[0] + ".class").delete()) {
                            returnValue[0] = 2;
                        }
                    }
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                }

            });
            statusThread.setDaemon(true);
            statusThread.start();

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return returnValue[0];

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
     * Compiles source code.
     *
     * @param sourceCode The source code.
     * @param file The actual file.
     * @param textArea The ConsoleTextArea.
     * @param buildPath The build path.
     * @return Whether the compilation was successful or not.
     */
    private static boolean compile(String sourceCode,  File file, ConsoleTextArea textArea,
                                   String buildPath) {

        // Get compiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            logger.info("Java compiler not available");
            return false;
        }

        // Create file in memory
        JavaFileObject fileObject = new SimpleJavaFileObject(
                new File(file.getName()).toURI(),
                JavaFileObject.Kind.SOURCE
        ) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return sourceCode;
            }
        };

        // Prepare compilation task
        JavaFileManager fileManager = new ForwardingJavaFileManager<>(
                compiler.getStandardFileManager(null, null, null)
        ) {};

        // Set up writer
        StringWriter stdOutWriter = new StringWriter();
        StringWriter stdErrWriter = new StringWriter();
        PrintWriter stdOut = new PrintWriter(stdOutWriter);
        PrintWriter stdErr = new PrintWriter(stdErrWriter);

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        // Compile
        JavaCompiler.CompilationTask task = compiler.getTask(stdOut, fileManager,
                diagnostics, Arrays.asList("-d",
                        (buildPath != null) ?  buildPath : "src/classes"),
                null, Collections.singleton(fileObject));

        boolean result = task.call();

        stdOut.close();
        stdErr.close();

        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            appendStyledText(
                    textArea,
                    diagnostic.getMessage(null),
                    (diagnostic.getKind() == Diagnostic.Kind.ERROR) ? "red" : "white"
            );
        }

        return result;

    }

    /**
     * Reads the source file contents into a String Object.
     *
     * @param file The file.
     * @return The contents.
     */
    private static String readSourceFile(File file) {

        StringBuilder sourceCode = new StringBuilder();
        ArrayList<String> lines = FileManager.readFile(file.toPath());
        if (lines == null) {
            return null;
        }
        for (String line : lines) {
            sourceCode.append(line).append("\n");
        }
        return sourceCode.toString();

    }

    /**
     * Appends text in the ConsoleTextArea with a particular color.
     *
     * @param textArea The ConsoleTextArea.
     * @param text The text.
     * @param color The color.
     */
    private static void appendStyledText(ConsoleTextArea textArea, String text, String color) {

        int start = textArea.getLength();
        textArea.appendText(text);
        int end = textArea.getLength();
        textArea.setStyle(start, end, "-fx-fill: " + color + ";");

    }

    /**
     * Sets up the ConsoleTextArea to be used.
     *
     * @param consoleTextArea The ConsoleTextArea.
     */
    public static void setConsoleTextArea(ConsoleTextArea consoleTextArea) {

        JavaCodeExecutor.consoleTextArea = consoleTextArea;
    }

    /**
     * Terminates the process.
     */
    public static void terminate() {

        if (process != null && process.isAlive()) {
            process.destroy();
            Platform.runLater(() -> {
                try {
                    consoleTextArea.appendText("\nProcess Finished with exit code " + process.waitFor() + "\n");
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
    public static void setVariableArea(ScrollPane variableArea) {

        JavaCodeExecutor.variableArea = variableArea;
    }

    /**
     * Sends a notification to the Debugger.
     * Could be a step event or a continue notification.
     *
     * @param notification The notification.
     */
    public static void notifyDebugger(String notification) {

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
    public static Tooltip getCallStack() {

        try {
            return debugger.getCallStack();
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }

    }

}
