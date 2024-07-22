package com.project.managers;

import com.project.custom_classes.ConsoleTextArea;
import com.project.java_code_processing.JavaCodeExecutor;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.LineNumberFactory;
import com.project.utility.EditAreaUtility;
import com.project.utility.MainUtility;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

public class FileManager {

    private static final Logger logger = LogManager.getLogger(FileManager.class);

    private static TabPane tabPane;
    private static ArrayList<Path> openFilesPaths;
    private static ArrayList<Tab> tabs;
    private static ArrayList<Path> filePaths;
    private static ArrayList<Boolean> saved;
    private static ArrayList<Path> openProjectPath;
    private static FileChooser fileChooser;
    private static HBox console;
    private static SplitPane verticalSplitPane;
    private static Clipboard clipboard;
    private static ArrayList<Boolean> shouldCut;

    public static void newFile(String path, String text, boolean isColored) {

        Tab newTab = new Tab();
        tabs.add(newTab);
        filePaths.add((path == null) ? null : Paths.get(path));
        saved.add(path != null);
        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER);
        Label headerLabel = new Label((path == null) ? "* Untitled.java     " :
                new File(path).getName() + "     ");
        Button closeBtn = new Button("x");
        closeBtn.getStyleClass().add("close-button");
        closeBtn.setOnAction(event -> closeFile(newTab));
        header.getChildren().addAll(headerLabel, closeBtn);
        newTab.setGraphic(header);

        InlineCssTextArea textArea = new InlineCssTextArea();
        textArea.getStyleClass().add("inline-css-text-area");

        IntFunction<Node> lineNumberFactory = LineNumberFactory.get(textArea);
        IntFunction<Node> customLineNumberFactory = line -> {
            Node node = lineNumberFactory.apply(line);
            if (node instanceof Label label) {
                label.setFont(Font.font("Roboto", FontWeight.BOLD, 13));
                label.setAlignment(Pos.CENTER_RIGHT);
                label.setStyle("-fx-padding: 0 5 0 0; -fx-background-color: white;");
            }
            return node;
        };

        textArea.setParagraphGraphicFactory(customLineNumberFactory);
        textArea.replaceText((text == null) ? "" : text);
        EditAreaUtility.addEventHandlers(textArea, newTab, isColored);
        ContextMenu contextMenu = EditAreaUtility.getContextMenu(
                new Object[]{"Cut", KeyCode.X, 1},
                new Object[]{"Copy", KeyCode.C, 2},
                new Object[]{"Paste", KeyCode.V, 3}
        );
        textArea.setContextMenu(contextMenu);
        EditAreaUtility.color(textArea);

        newTab.setContent(textArea);
        tabPane.getTabs().add(newTab);

        //Focus new tab.
        tabPane.getSelectionModel().select(newTab);
    }

    public static void closeFile(Tab tab) {

        if (tab == null) {
            tab = tabPane.getSelectionModel().getSelectedItem();
        }
        if (tab != null) {
            saved.remove(tabs.indexOf(tab));
            openFilesPaths.remove(filePaths.get(tabs.indexOf(tab)));
            filePaths.remove(tabs.indexOf(tab));
            tabs.remove(tab);
            tabPane.getTabs().remove(tab);
        }
    }

    public static void saveFile(Tab tab) {

        if (tab == null) {
            tab = tabPane.getSelectionModel().getSelectedItem();
            if (tab == null) {
                return;
            }
        }

        InlineCssTextArea textArea = (InlineCssTextArea) tab.getContent();
        File file;
        if (tabs.contains(tab) && filePaths.get(tabs.indexOf(tab)) != null) {
            file = filePaths.get(tabs.indexOf(tab)).toFile();
            saved.set(tabs.indexOf(tab), true);
            HBox header = (HBox) tab.getGraphic();
            header.getChildren().remove(0);
            header.getChildren().add(0, new Label(filePaths.get(tabs.indexOf(tab)).toFile().getName() + "     "));
        } else {
            fileChooser.setTitle("Save File");
            fileChooser.getExtensionFilters().removeAll();
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Java files", "*.java")
            );
            file = fileChooser.showSaveDialog(tabPane.getScene().getWindow());
            if (file != null) {
                openFilesPaths.add(file.toPath());
            }
        }
        if (file != null) {
            writeToFile(file.toPath(), textArea.getText(), true, false);
        } else {
            System.out.println("No File Selected");
        }
    }

    public static void saveFiles(ArrayList<Integer> indexes) {
        if (indexes != null) {
            for (Integer i : indexes) {
                saveFile(tabPane.getTabs().get(i));
            }
        }
    }

    public static void openFile(Path path) {

        File file;
        if (path == null) {
            fileChooser.setTitle("Open File");
            fileChooser.getExtensionFilters().removeAll();
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Java files", "*.java")
            );
            file = fileChooser.showOpenDialog(tabPane.getScene().getWindow());
        } else {
            file = path.toFile();
        }

        if (filePaths.contains(file.toPath())) {
            tabPane.getSelectionModel().select(tabs.get(filePaths.indexOf(file.toPath())));
            return;
        }

        String[] splitName;
        if (file != null) {
            splitName = file.getName().split("\\.");
            if (!file.exists() || !file.isFile() || !file.canRead() ||
                    !file.canWrite() || (!splitName[splitName.length - 1].equals("java") &&
                    !splitName[splitName.length - 1].equals("txt"))) {
                return;
            }
        } else {
            return;
        }

        StringBuilder text = new StringBuilder();
        ArrayList<String> lines = readFile(file.toPath());
        if (lines != null) {
            for (String line : lines) {
                text.append(line).append("\n");
            }
        }

        openFilesPaths.add(file.toPath());
        newFile(file.getPath(), text.toString(), splitName[splitName.length - 1].equals("java"));

    }

    public static void closeAll() {

        int size = tabPane.getTabs().size();
        for (int i = 0; i < size; i++) {
            closeFile(tabPane.getTabs().get(0));
        }

    }

    public static void newJavaFile(Path path, String extraKeyWord) {

        String[] splitName = path.toString().split("\\\\");
        boolean found = false;
        for (String s : splitName) {
            if (s.equals("src")) {
                found = true;
                break;
            }
        }
        if (!found) {
            System.out.println("Java Files Not Allowed HERE!");
            return;
        }
        String name = MainUtility.quickDialog("New Java Class", "Enter class name");
        if (name == null) {
            return;
        }
        File file = new File(path.toString(), name + ".java");
        String parentName = path.toFile().getName();
        String[] fileNameParts = file.getName().split("\\.");
        List<String> content;
        if (parentName.equals("src")) {
            content = List.of(
                    "public " + extraKeyWord + fileNameParts[0] + " {\n\t\n}"
            );
        } else {
            content = List.of(
                    "package " + parentName + ";\n\n",
                    "public " + extraKeyWord + fileNameParts[0] + " {\n\t\n}"
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
            System.out.println(e.getMessage());
        }

        openFile(file.toPath());
        ProjectManager.openProject(openProjectPath.get(0));

    }

    public static void newJavaClass(Path path) {

        newJavaFile(path, "class ");
    }

    public static void newJavaAbstractClass(Path path) {

        newJavaFile(path, "abstract class ");

    }

    public static void newJavaInterface(Path path) {

        newJavaFile(path, "interface ");

    }

    public static void newJavaRecord(Path path) {

        newJavaFile(path, "record ");

    }

    public static void newJavaEnum(Path path) {

        newJavaFile(path, "enum ");

    }

    public static void newJavaAnnotation(Path path) {

        newJavaFile(path, "@interface ");

    }

    public static void runFile(Path path) {

        verticalSplitPane.setDividerPositions(0.7);
        ConsoleTextArea consoleTextArea = (ConsoleTextArea) console.getChildren().get(0);
        consoleTextArea.unprotectText();
        consoleTextArea.replaceText("");
        consoleTextArea.protectText();
        switch (JavaCodeExecutor.run(path.toFile(), ProjectManager.getCurrentProject())) {
            case 1:
                logger.info("Couldn't read {} to execute", path.toString());
                break;
            case 2:
                logger.info("Compilation of {} failed", path.toString());
                break;
            case 3:
                logger.info("Couldn't delete .class file of {}", path.toString());
                break;
        }
    }

    public static void deleteFile(Path path, boolean skipConfirm) {

        boolean confirm;
        if (skipConfirm) {
            confirm = true;
        } else {
            confirm = MainUtility.confirm("Delete " + path.getFileName(), "This file will be deleted!");
        }
        if (confirm) {
            File file = new File(path.toString());
            Tab tab = tabs.get(filePaths.indexOf(file.toPath()));
            if (tab != null) {
                closeFile(tab);
            }
            if (file.exists()) {
                if (file.delete()) {
                    logger.info("Deleted {}", file.getPath());
                }
            } else {
                System.out.println("File Not Found");
            }
            ProjectManager.openProject(openProjectPath.get(0));
        }

    }

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

    public static void copyFile(Path path) {

        cutFile(path, false);
    }

    public static void pasteIntoFile(Path path) {

        boolean confirm = MainUtility.confirm("Paste into " + path.getFileName(), "Current contents of the file will be lost");
        if (confirm) {
            if (clipboard.hasString()) {
                String content = clipboard.getString();
                File potentialFile = new File(content);
                if (potentialFile.exists()) {
                    if (potentialFile.isFile()) {
                        StringBuilder stringBuilder = new StringBuilder();
                        ArrayList<String> lines = readFile(path);
                        if (lines != null) {
                            for (String line : lines) {
                                stringBuilder.append(line);
                            }
                        }
                        content = stringBuilder.toString();
                        if (!shouldCut.isEmpty() && shouldCut.get(0)) {
                            deleteFile(potentialFile.toPath(), true);
                            logger.info("Deleted {} on cut command", potentialFile.getPath());
                        }
                    } else if (potentialFile.isDirectory()) {
                        System.out.println("Cut dir into file not allowed!");
                        return;
                    }
                }
                writeToFile(path, content, true, false);
                logger.info("Pasted into {}", potentialFile.getPath());
            }
        }

    }

    public static void renameFile(Path path) {

        String newName = MainUtility.quickDialog("Rename " + path.getFileName(), "Enter new name");
        boolean confirm = MainUtility.confirm("Rename " + path.getFileName(), "Name will be changed!");
        if (confirm) {
            if (newName == null || Objects.equals(newName, "")) {
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
            ProjectManager.openProject(openProjectPath.get(0));
            if (filePaths.contains(path)) {
                Tab tab = tabs.get(filePaths.indexOf(path));
                if (tab != null) {
                    closeFile(tab);
                    openFile(newPath);
                }
            }
        }

    }

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

    public static void setTabPane(TabPane tabPane) {

        FileManager.tabPane = tabPane;
    }

    public static void setOpenFilesPaths(ArrayList<Path> openFilesPaths) {

        FileManager.openFilesPaths = openFilesPaths;
    }

    public static void setTabs(ArrayList<Tab> tabs) {

        FileManager.tabs = tabs;
    }

    public static void setFilePaths(ArrayList<Path> filePaths) {

        FileManager.filePaths = filePaths;
    }

    public static void setSaved(ArrayList<Boolean> saved) {

        FileManager.saved = saved;
    }

    public static void setFileChooser(FileChooser fileChooser) {

        FileManager.fileChooser = fileChooser;
    }

    public static void setConsole(HBox console) {

        FileManager.console = console;
    }

    public static void setVerticalSplitPane(SplitPane verticalSplitPane) {

        FileManager.verticalSplitPane = verticalSplitPane;
    }

    public static void setOpenProjectPath(ArrayList<Path> openProjectPath) {

        FileManager.openProjectPath = openProjectPath;
    }

    public static void setClipboard(Clipboard clipboard) {

        FileManager.clipboard = clipboard;
    }

    public static void setShouldCut(ArrayList<Boolean> shouldCut) {

        FileManager.shouldCut = shouldCut;
    }
}
