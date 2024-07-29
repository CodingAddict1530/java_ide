package com.project.managers;

import com.project.custom_classes.*;
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
    private static ArrayList<Path> openProjectPath;
    private static FileChooser fileChooser;
    private static HBox console;
    private static SplitPane verticalSplitPane;
    private static Clipboard clipboard;
    private static ArrayList<Boolean> shouldCut;

    public static void newFile(String path, String text, boolean isColored) {

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
        OpenFilesTracker.addOpenFile(new OpenFile(new CustomFile(path, packageName.toString()),
                newTab, true));
        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER);
        Label headerLabel = new Label(new File(path).getName() + "     ");
        Button closeBtn = new Button("x");
        closeBtn.getStyleClass().add("close-button");
        closeBtn.setOnAction(event -> closeFile(newTab));
        header.getChildren().addAll(headerLabel, closeBtn);
        newTab.setGraphic(header);

        CustomTextArea textArea = new CustomTextArea(isColored);
        textArea.getStyleClass().add("custom-text-area");

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
        EditAreaUtility.addEventHandler(textArea, newTab);
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
            OpenFile file = OpenFilesTracker.getOpenFile(tab);
            JLSManager.didClose(file.getFile().toPath());
            OpenFilesTracker.removeOpenFile(file);
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

        CustomTextArea textArea = (CustomTextArea) tab.getContent();
        File file = OpenFilesTracker.getOpenFile(tab).getFile();
        if (file != null) {
            JLSManager.sendWillSave(file.toURI().toString());
            writeToFile(file.toPath(), textArea.getInnerTextArea().getText(), true, false);
            JLSManager.sendDidSave(file.toURI().toString(), textArea.getInnerTextArea().getText());
            OpenFilesTracker.getOpenFile(tab).setIsSaved(true);
            HBox header = (HBox) tab.getGraphic();
            header.getChildren().remove(0);
            header.getChildren().add(0, new Label(OpenFilesTracker.getOpenFile(tab).getFile().getName() + "     "));
        } else {
            System.out.println("No File Selected");
        }
    }

    public static void saveFiles(ArrayList<OpenFile> openFiles) {

        for (OpenFile o : openFiles) {
            saveFile(o.getTab());
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
            path = file.toPath();
        } else {
            file = path.toFile();
        }

        if (OpenFilesTracker.getOpenFile(path) != null) {
            tabPane.getSelectionModel().select(OpenFilesTracker.getOpenFile(path).getTab());
            return;
        }

        String[] splitName;
        if (file != null) {
            splitName = file.getName().split("\\.");
            if (!file.exists() || !file.isFile() || !file.canRead() || !file.canWrite()) {
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
            if (s.equals("java")) {
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
        String[] parentNameParts = parentName.split("\\\\");
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
                    "package " + parentName + ";\n\n",
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
            System.out.println(e.getMessage());
        }

        StringBuilder sb = new StringBuilder();
        for (String s : content) {
            sb.append(s).append("\n");
        }
        if (!sb.isEmpty()) {
            sb.deleteCharAt(sb.length() - 1);
        }
        JLSManager.sendDidSave(file.toURI().toString(), sb.toString());
        openFile(file.toPath());

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
            if (OpenFilesTracker.getOpenFile(path) != null) {
                closeFile(OpenFilesTracker.getOpenFile(path).getTab());
            }
            if (file.exists()) {
                if (file.delete()) {
                    JLSManager.sendDeletedFile(file.toURI().toString());
                    logger.info("Deleted {}", file.getPath());
                }
            } else {
                System.out.println("File Not Found");
            }
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
            //ProjectManager.openProject(openProjectPath.get(0));
            if (OpenFilesTracker.getOpenFile(path) != null) {
                closeFile(OpenFilesTracker.getOpenFile(path).getTab());
                openFile(newPath);
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
