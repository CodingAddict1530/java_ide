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

import com.project.custom_classes.OpenFile;
import com.project.custom_classes.OpenFilesTracker;
import com.project.custom_classes.CustomTextArea;
import com.project.custom_classes.CustomFile;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fxmisc.richtext.LineNumberFactory;
import com.project.utility.MainUtility;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Handles file related operations.
 */
public class FileManager {

    /**
     * The logger for the class.
     */
    private static final Logger logger = LogManager.getLogger(FileManager.class);

    /**
     * The TabPane.
     */
    private static TabPane tabPane;

    /**
     * The FileChooser to select files from device.
     */
    private static FileChooser fileChooser;

    /**
     * The system clipboard.
     */
    private static Clipboard clipboard;

    /**
     * Whether the operation is a cut or a copy.
     */
    private static ArrayList<Boolean> shouldCut;

    /**
     * Creates a new tab.
     *
     * @param path The Path of the file being opened.
     * @param text The contents of the file.
     * @param isColored Whether the TextArea will format the text.
     */
    public static void newTab(String path, String text, boolean isColored) {

        // Send didOpen notification to language server.
        JLSManager.didOpen(Paths.get(path), text);

        Tab newTab = new Tab();
        String[] parts = path.split("\\\\");
        StringBuilder packageName = new StringBuilder();
        boolean start = false;
        for (String part : parts) {
            if (part.equals("java") || start) {
                if (start) {
                    packageName.append(part).append(".");
                }
                start = true;
            }
        }
        if(!packageName.isEmpty()) {
            for (int i = 0; i < 6; i++) {
                packageName.deleteCharAt(packageName.length() - 1);
            }
        }

        // Add the file to OpenFilesTracker.
        OpenFilesTracker.addOpenFile(new OpenFile(new CustomFile(path, packageName.toString()),
                newTab, true));

        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER);
        Label headerLabel = new Label(new File(path).getName() + "     ");
        headerLabel.setStyle("-fx-text-fill: white");
        Button closeBtn = new Button("x");
        closeBtn.getStyleClass().add("close-button");
        closeBtn.setOnAction(event -> closeFile(newTab));
        header.getChildren().addAll(headerLabel, closeBtn);
        newTab.setGraphic(header);

        CustomTextArea textArea = new CustomTextArea(isColored);
        textArea.getStyleClass().add("custom-text-area");

        // Create a Number line.
        IntFunction<Node> lineNumberFactory = LineNumberFactory.get(textArea);
        IntFunction<Node> customLineNumberFactory = line -> {
            Node node = lineNumberFactory.apply(line);
            if (node instanceof Label label) {
                label.setFont(Font.font("Roboto", FontWeight.BOLD, 13));
                label.setAlignment(Pos.CENTER_RIGHT);
                label.setStyle("-fx-padding: 0 5 0 0; -fx-background-color: #2d2e2e; -fx-text-fill: white");
            }
            return node;
        };

        // Add it to the InlineCssTextArea.
        textArea.setParagraphGraphicFactory(customLineNumberFactory);
        textArea.replaceText((text == null) ? "" : text);
        EditAreaManager.addEventHandlers(textArea, newTab);
        ContextMenu contextMenu = EditAreaManager.getContextMenu(
                new Object[]{"Cut", KeyCode.X, 1},
                new Object[]{"Copy", KeyCode.C, 2},
                new Object[]{"Paste", KeyCode.V, 3}
        );
        textArea.setContextMenu(contextMenu);
        EditAreaManager.color(textArea);

        newTab.setContent(textArea);
        tabPane.getTabs().add(newTab);

        //Focus new tab.
        tabPane.getSelectionModel().select(newTab);

    }

    /**
     * Closes current tab if none is specified.
     *
     * @param tab The tab to close.
     */
    public static void closeFile(Tab tab) {

        if (tab == null) {
            tab = tabPane.getSelectionModel().getSelectedItem();
        }
        if (tab != null) {
            OpenFile file = OpenFilesTracker.getOpenFile(tab);
            OpenFilesTracker.removeOpenFile(file);
            tabPane.getTabs().remove(tab);

            // Send didClose notification to language server.
            JLSManager.didClose(file.getFile().toPath());
        }

    }

    /**
     * Saves the current tab if none is specified.
     *
     * @param tab The tab to save.
     */
    public static void saveFile(Tab tab) {

        if (tab == null) {
            tab = tabPane.getSelectionModel().getSelectedItem();
            if (tab == null) {
                return;
            }
        }

        CustomTextArea textArea = (CustomTextArea) tab.getContent();
        File file = OpenFilesTracker.getOpenFile(tab).getFile();
        if (file != null) {

            // Send willSave notification to language server.
            JLSManager.sendWillSave(file.toURI().toString());
            if (writeToFile(file.toPath(), textArea.getInnerTextArea().getText(), true, false)) {

                // Send didSave notification to language server.
                JLSManager.sendDidSave(file.toURI().toString(), textArea.getInnerTextArea().getText());
                OpenFilesTracker.getOpenFile(tab).setIsSaved(true);
                HBox header = (HBox) tab.getGraphic();
                header.getChildren().remove(0);
                header.getChildren().add(0, new Label(OpenFilesTracker.getOpenFile(tab).getFile().getName() + "     "));
            } else {
                System.out.println("Error saving file");
            }
        }

    }

    /**
     * Saves all files given.
     *
     * @param openFiles An ArrayList of OpenFiles to save.
     */
    public static void saveFiles(ArrayList<OpenFile> openFiles) {

        for (OpenFile o : openFiles) {
            saveFile(o.getTab());
        }
    }

    /**
     * Opens a file or prompts user to select one if none is specified.
     *
     * @param path The Path to the file to open.
     */
    public static void openFile(Path path) {

        File file;
        if (path == null) {

            // Prompt user to select a file from the device.
            fileChooser.setTitle("Open File");
            fileChooser.getExtensionFilters().removeAll();
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Java files", "*.java")
            );
            fileChooser.setInitialDirectory(ProjectManager.getCurrentProject().getPath().toFile());
            file = fileChooser.showOpenDialog(tabPane.getScene().getWindow());
            if (file == null) {
                return;
            }
            path = file.toPath();
        } else {
            file = path.toFile();
        }

        // Check whether the file is not already open.
        if (OpenFilesTracker.getOpenFile(path) != null) {

            // Select the tab and return.
            tabPane.getSelectionModel().select(OpenFilesTracker.getOpenFile(path).getTab());
            return;
        }

        String[] splitName;

        if (!file.exists() || !file.isFile() || !file.canRead() || !file.canWrite()) {
            return;
        }
        splitName = file.getName().split("\\.");

        StringBuilder text = new StringBuilder();
        ArrayList<String> lines = readFile(file.toPath());
        if (lines != null) {
            for (String line : lines) {
                text.append(line).append("\n");
            }
        }

        // Open a tab for the new file.
        newTab(file.getPath(), text.toString(), splitName[splitName.length - 1].equals("java"));

    }

    /**
     * Closes all tabs.
     */
    public static void closeAll() {

        int size = tabPane.getTabs().size();
        for (int i = 0; i < size; i++) {
            closeFile(tabPane.getTabs().get(0));
        }

    }

    /**
     * Creates a new java file.
     *
     * @param path The parent directory of the file.
     * @param extraKeyWord An extra keyword depending on what class it is.
     */
    public static void newJavaFile(Path path, String extraKeyWord) {

        // If no path is provided use src\main\java.
        if (path == null) {

            // If not in a project return.
            if (ProjectManager.getCurrentProject() == null) {
                return;
            }
            path = new File(ProjectManager.getCurrentProject().getPath().toString(), "src\\main\\java").toPath();
        }

        // Check whether file is it right place.
        if (!path.startsWith(new File(ProjectManager.getCurrentProject().getPath().toString(), "src\\main\\java").toPath())) {
            System.out.println("Java Files Not Allowed HERE!");
            return;
        }

        // Prompt user to enter a name for the class.
        String name = MainUtility.quickDialog("New Java Class", "Enter class name");

        // Abort if no name is entered.
        if (name == null) {
            return;
        }
        File file = new File(path.toString(), name + ".java");
        String parentName = path.toFile().getName();
        String[] parentNameParts = parentName.split("\\\\");

        // Determine the package of the file.
        boolean packageStart = false;
        StringBuilder packageName = new StringBuilder();
        for (String s : parentNameParts) {
            if (packageStart) {
                packageName.append("s").append(".");
            }
            if (s.equals("java")) {
                packageStart = true;
            }
        }
        if (!packageName.isEmpty()) {

            // Remove redundant ".".
            packageName.deleteCharAt(packageName.length() - 1);
        }
        String[] fileNameParts = file.getName().split("\\.");
        List<String> content;
        if (parentName.equals("java")) {
            content = List.of(
                    "public " + extraKeyWord + fileNameParts[0] + " {\n    \n}"
            );
        } else {
            content = List.of(
                    "package " + packageName + ";\n\n",
                    "public " + extraKeyWord + fileNameParts[0] + " {\n    \n}"
            );
        }
        try {
            if (!file.exists()) {
                Files.write(file.toPath(), content);
                logger.info("New Java File Created: {}", file.getPath());
            } else {
                System.out.println("Java File Already Exists");
            }
        } catch (IOException e) {
            logger.error(e);
        }

        StringBuilder sb = new StringBuilder();
        for (String s : content) {
            sb.append(s).append("\n");
        }
        if (!sb.isEmpty()) {
            sb.deleteCharAt(sb.length() - 1);
        }

        // Open a tab for the file.
        openFile(file.toPath());

        // Send didSave notification to language server.
        JLSManager.sendDidSave(file.toURI().toString(), sb.toString());

    }

    /**
     * Creates a new java class.
     *
     * @param path The parent directory of the file.
     */
    public static void newJavaClass(Path path) {

        newJavaFile(path, "class ");
    }

    /**
     * Creates a new java abstract class.
     *
     * @param path The parent directory of the file.
     */
    public static void newJavaAbstractClass(Path path) {

        newJavaFile(path, "abstract class ");

    }

    /**
     * Creates a new java interface.
     *
     * @param path The parent directory of the file.
     */
    public static void newJavaInterface(Path path) {

        newJavaFile(path, "interface ");

    }

    /**
     * Creates a new java record.
     *
     * @param path The parent directory of the file.
     */
    public static void newJavaRecord(Path path) {

        newJavaFile(path, "record ");

    }

    /**
     * Creates a new java enum.
     *
     * @param path The parent directory of the file.
     */
    public static void newJavaEnum(Path path) {

        newJavaFile(path, "enum ");

    }

    /**
     * Creates a new java annotation.
     *
     * @param path The parent directory of the file.
     */
    public static void newJavaAnnotation(Path path) {

        newJavaFile(path, "@interface ");

    }

    /**
     * Deletes a file.
     *
     * @param path The Path to the file.
     * @param skipConfirm Whether to prompt user to confirm or not.
     */
    public static void deleteFile(Path path, boolean skipConfirm) {

        boolean confirm;
        if (skipConfirm) {
            confirm = true;
        } else {

            // Prompt user to confirm.
            confirm = MainUtility.confirm("Delete " + path.getFileName(), "This file will be deleted!");
        }
        if (confirm) {
            File file = new File(path.toString());
            if (OpenFilesTracker.getOpenFile(path) != null) {

                // Close the tab.
                closeFile(OpenFilesTracker.getOpenFile(path).getTab());
            }
            if (file.exists()) {
                if (file.delete()) {

                    // Send notification to language server.
                    JLSManager.sendDeletedFile(file.toURI().toString());
                    logger.info("Deleted {}", file.getPath());
                }
            } else {
                System.out.println("File Not Found");
            }
        }

    }

    /**
     * Cuts a file (Copies as well).
     *
     * @param path The Path to the file.
     * @param setCut Whether it is a cut or a copy.
     */
    public static void cutFile(Path path, boolean setCut) {

        ClipboardContent content = new ClipboardContent();
        content.putString(path.toString());
        clipboard.setContent(content);
        if (shouldCut.isEmpty()) {
            shouldCut.add(setCut);
        } else {
            shouldCut.set(0, setCut);
        }

    }

    /**
     * Copies a file.
     *
     * @param path The Path to the file.
     */
    public static void copyFile(Path path) {

        cutFile(path, false);
    }

    /**
     * Pastes into a file.
     *
     * @param path The Path to the file.
     */
    public static void pasteIntoFile(Path path) {

        // Ask user to confirm.
        boolean confirm = MainUtility.confirm("Paste into " + path.getFileName(), "Current contents of the file will be lost");
        if (confirm) {
            if (clipboard.hasString()) {
                String content = clipboard.getString();
                File potentialFile = new File(content);

                // Check whether the content is a file.
                if (potentialFile.exists()) {
                    if (potentialFile.isFile()) {
                        StringBuilder stringBuilder = new StringBuilder();

                        // Read the file.
                        ArrayList<String> lines = readFile(path);
                        if (lines != null) {
                            for (String line : lines) {
                                stringBuilder.append(line);
                            }
                        }
                        content = stringBuilder.toString();

                        // Check whether it is a cut and delete the file if it is.
                        if (!shouldCut.isEmpty() && shouldCut.get(0)) {
                            deleteFile(potentialFile.toPath(), true);
                            logger.info("Deleted {} on cut command", potentialFile.getPath());
                        }
                    } else if (potentialFile.isDirectory()) {
                        System.out.println("Paste dir into file not allowed!");
                        return;
                    }
                }
                writeToFile(path, content, true, false);
                logger.info("Pasted into {}", potentialFile.getPath());
            }
        }

    }

    /**
     * Renames a file.
     *
     * @param path The Path to the file.
     */
    public static void renameFile(Path path) {

        // Prompt user to input the new file name.
        String newName = MainUtility.quickDialog("Rename " + path.getFileName(), "Enter new name");

        if (newName == null || newName.isEmpty()) {
            return;
        }
        String[] oldParts = path.toString().split("\\.");
        String[] newParts = newName.split("\\.");
        if (newParts.length == 1 && oldParts.length > 1) {
            newName = newName + "." + oldParts[oldParts.length - 1];
        }
        Path newPath = new File(path.getParent().toString(), newName).toPath();
        try {
            Files.move(path, newPath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Renamed {} to {}", newName, newPath);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        //ProjectManager.openProject(openProjectPath.get(0));
        if (OpenFilesTracker.getOpenFile(path) != null) {

            // Close and open file to have the name change.
            closeFile(OpenFilesTracker.getOpenFile(path).getTab());
            openFile(newPath);
        }

    }

    /**
     * Reads a file.
     *
     * @param path The Path to the file.
     * @return An ArrayList of the lines in the file.
     */
    public static ArrayList<String> readFile(Path path) {

        try {
            List<String> lines = Files.readAllLines(path);
            ArrayList<String> returnValue = new ArrayList<>(lines);
            return (returnValue.isEmpty()) ? null : returnValue;
        } catch (IOException e) {
            logger.error(e);
        }

        return null;

    }

    /**
     * Writes to a file.
     *
     * @param path The Path to the file.
     * @param content The data to write to the file
     * @param overwrite Whether to overwrite.
     * @param append Whether to append.
     * @return Whether the operation was successful or not.
     */
    public static boolean writeToFile(Path path, String content, boolean overwrite, boolean append) {

        try {
            if (!overwrite) {
                if (new File(path.toString()).exists()) {
                    return false;
                }
            }
            if (append) {
                Files.write(path, List.of(content), StandardOpenOption.APPEND);
                logger.info("Appended to {}", path.toString());
            } else {
                Files.write(path, List.of(content));
                logger.info("Wrote to {}", path.toString());
            }
        } catch (IOException e) {
            logger.error(e);
            return false;
        }

        return true;
    }

    /**
     * Sets up the TabPane.
     *
     * @param tabPane The TabPane.
     */
    public static void setTabPane(TabPane tabPane) {

        FileManager.tabPane = tabPane;
    }

    /**
     * Sets up the FileChooser.
     *
     * @param fileChooser The FileChooser.
     */
    public static void setFileChooser(FileChooser fileChooser) {

        FileManager.fileChooser = fileChooser;
    }

    /**
     * Sets up the system clipboard.
     *
     * @param clipboard The Clipboard
     */
    public static void setClipboard(Clipboard clipboard) {

        FileManager.clipboard = clipboard;
    }

    /**
     * Sets up shouldCut.
     *
     * @param shouldCut whether to cut or not.
     */
    public static void setShouldCut(ArrayList<Boolean> shouldCut) {

        FileManager.shouldCut = shouldCut;
    }

}
