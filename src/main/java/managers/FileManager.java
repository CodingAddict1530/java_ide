package managers;

import custom_classes.ConsoleTextArea;
import java_code_processing.JavaCodeExecutor;
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
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.LineNumberFactory;
import utility.EditAreaUtility;
import utility.MainUtility;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

public class FileManager {

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
        if (isColored) {
            EditAreaUtility.addEventHandlers(textArea, newTab);
        }
        textArea.replaceText((text == null) ? "" : text);
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

    public static void saveFile() {

        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        if (tab == null) {
            return;
        }

        InlineCssTextArea textArea = (InlineCssTextArea) tab.getContent();
        File file;
        if (tabs.contains(tab) && filePaths.get(tabs.indexOf(tab)) != null) {
            file = filePaths.get(tabs.indexOf(tab)).toFile();
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
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(textArea.getText());
                filePaths.set(tabs.indexOf(tab), file.toPath());
                saved.set(tabs.indexOf(tab), true);
                String fileName = file.getName();
                HBox header = (HBox) tab.getGraphic();
                header.getChildren().remove(0);
                header.getChildren().add(0, new Label(fileName + "     "));
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        } else {
            System.out.println("No File Selected");
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
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line);
                text.append("\n");
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        openFilesPaths.add(file.toPath());
        newFile(file.getPath(), text.toString(),splitName[splitName.length - 1].equals("java"));

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
        JavaCodeExecutor.run(path.toFile(),  consoleTextArea, ProjectManager.getCurrentProject());
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
                file.delete();
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
                        }
                    } else if (potentialFile.isDirectory()) {
                        System.out.println("Cut dir into file not allowed!");
                        return;
                    }
                }
                writeToFile(path, content, true, false);
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

        ArrayList<String> returnValue = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(path);
            returnValue.addAll(lines);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return (returnValue.isEmpty()) ? null : returnValue;

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
            } else {
                Files.write(path, List.of(content));
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
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
