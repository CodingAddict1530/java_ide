package com.project.javaeditor;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;

import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular;
import org.kordamp.ikonli.javafx.FontIcon;
import utility.*;

import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static utility.Utility.ORANGE_KEY_WORDS;

public class Controller implements Initializable {

    @FXML
    private TabPane tabPane;
    @FXML
    private MenuItem newFile;
    @FXML
    private MenuItem openFile;
    @FXML
    private MenuItem saveFile;
    @FXML
    private MenuItem closeFile;
    @FXML
    private MenuItem cut;
    @FXML
    private MenuItem copy;
    @FXML
    private MenuItem paste;
    @FXML
    private MenuItem openProject;
    @FXML
    private VBox projectView;
    @FXML
    private SplitPane splitPane;

    private int moveCaret = 0;

    private final ArrayList<Tab> tabs = new ArrayList<>();
    private final ArrayList<String> filePaths = new ArrayList<>();
    private final ArrayList<Boolean> saved = new ArrayList<>();
    private final FileChooser fileChooser = new FileChooser();
    private final Clipboard clipboard = Clipboard.getSystemClipboard();
    private Path openProjectPath = null;
    private final ArrayList<Path> openFilesPaths = new ArrayList<>();

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        addAccelerator(newFile, KeyCode.N);
        addAccelerator(openFile, KeyCode.O);
        addAccelerator(saveFile, KeyCode.S);
        addAccelerator(closeFile, KeyCode.Q);
        addAccelerator(cut, KeyCode.X);
        addAccelerator(copy, KeyCode.C);
        addAccelerator(paste, KeyCode.V);
        addAccelerator(openProject, KeyCode.P);

        splitPane.setDividerPositions(0.3);

    }

    @FXML
    public void newFile() {

        newFile(tabPane, null, null, true);
    }

    public void newFile(TabPane tabPane, String path, String text, boolean isColored) {

        Tab newTab = new Tab();
        tabs.add(newTab);
        filePaths.add(path);
        saved.add(path != null);
        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER);
        Label headerLabel = new Label((path == null) ? "* Untitled.java     " :
                new File(path).getName() + "     ");
        Button closeBtn = new Button("x");
        closeBtn.getStyleClass().add("close-button");
        closeBtn.setOnAction(event -> closeFile(tabPane, newTab));
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
            addEventHandlers(textArea, newTab);
        }
        textArea.replaceText((text == null) ? "" : text);
        ContextMenu contextMenu = getContextMenu(
                new Object[]{"Cut", KeyCode.X, 1},
                new Object[]{"Copy", KeyCode.C, 2},
                new Object[]{"Paste", KeyCode.V, 3}
        );
        textArea.setContextMenu(contextMenu);
        textArea.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(textArea, event.getScreenX(), event.getScreenY());
            }
        });
        color(textArea);

        newTab.setContent(textArea);
        tabPane.getTabs().add(newTab);

        //Focus new tab.
        tabPane.getSelectionModel().select(newTab);
    }

    @FXML
    public void closeFile() {

        closeFile(tabPane, null);
    }

    public void closeFile(TabPane tabPane, Tab tab) {

        if (tab == null) {
            tab = tabPane.getSelectionModel().getSelectedItem();
        }
        if (tab != null) {
            saved.remove(tabs.indexOf(tab));
            openFilesPaths.remove(Paths.get(filePaths.get(tabs.indexOf(tab))));
            filePaths.remove(tabs.indexOf(tab));
            tabs.remove(tab);
            tabPane.getTabs().remove(tab);
        }
    }

    @FXML
    public void saveFile() {

        saveFile(tabPane);
    }

    public void saveFile(TabPane tabPane) {

        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        if (tab == null) {
            return;
        }

        InlineCssTextArea textArea = (InlineCssTextArea) tab.getContent();
        File file;
        if (tabs.contains(tab) && filePaths.get(tabs.indexOf(tab)) != null) {
            file = new File(filePaths.get(tabs.indexOf(tab)));
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
                filePaths.set(tabs.indexOf(tab), file.getPath());
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

    @FXML
    public void openFile() {

        openFile(tabPane, null);
    }

    public void openFile(TabPane tabPane, Path path) {

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

        if (filePaths.contains(file.toPath().toString())) {
            tabPane.getSelectionModel().select(tabs.get(filePaths.indexOf(file.toPath().toString())));
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
        newFile(tabPane, file.getPath(), text.toString(), splitName[splitName.length - 1].equals("java"));

    }

    @FXML
    public void openProject() {

        openProject(tabPane, null);
    }

    public void openProject(TabPane tabPane, Path path) {

        File dir;
        if (path == null) {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select a Directory");
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            dir = directoryChooser.showDialog(tabPane.getScene().getWindow());
        } else {
            dir = path.toFile();
        }

        if (dir != null && dir.exists() && dir.isDirectory()) {

            RootTreeNode root = parseDirectory(dir.toPath());
            if (root != null) {
                openProjectPath = root.getPath();
                TreeView<HBox> treeView = createTree(root);
                projectView.getChildren().clear();
                projectView.getChildren().add(treeView);
                VBox.setVgrow(treeView, Priority.ALWAYS);
            }

        }

    }

    @FXML
    public void copy() {

        copy(tabPane);
    }

    public void copy(TabPane tabPane) {

        InlineCssTextArea textArea = (InlineCssTextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
        ClipboardContent content = new ClipboardContent();
        content.putString(textArea.getSelectedText());
        clipboard.setContent(content);

    }

    @FXML
    public void cut() {

        cut(tabPane);
    }

    public void cut(TabPane tabPane) {

        copy();
        InlineCssTextArea textArea = (InlineCssTextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
        textArea.deleteText(textArea.getSelection().getStart(), textArea.getSelection().getEnd());

    }

    @FXML
    public void paste() {

        paste(tabPane);
    }

    public void paste(TabPane tabPane) {

        if (clipboard.hasString()) {
            InlineCssTextArea textArea = (InlineCssTextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
            textArea.deleteText(textArea.getSelection().getStart(), textArea.getSelection().getEnd());
            textArea.insertText(textArea.getCaretPosition(), clipboard.getString());

        }

    }

    public String textHandler(InlineCssTextArea textArea) {

        String line = getLine(textArea);
        int caretPosition = textArea.getCaretPosition();
        String text = textArea.getText();
        char caretLeft = caretPosition > 1 ? text.charAt(caretPosition - 2) : '\u0000';
        char caretRight = caretPosition < text.length() ? text.charAt(caretPosition) : '\u0000';
        return applyIndent(line, caretLeft, caretRight);

    }

    public String getLine(InlineCssTextArea textArea) {

        int caretPosition = textArea.getCaretPosition();
        int start = (caretPosition == 0) ? 0 : caretPosition - 1;
        while (start > 0 && textArea.getText().charAt(start - 1) != '\n') {
            start--;
        }
        return textArea.getText(start, caretPosition);

    }

    public String applyIndent(String line, char caretLeft, char caretRight) {

        StringBuilder indent = new StringBuilder();
        int openBracketCount = 0;
        int openSquareBracketCount = 0;
        int openCurlyBracketCount = 0;
        int closedBracketCount = 0;
        int closedSquareBracketCount = 0;
        int closedCurlyBracketCount = 0;
        int count = 0;

        for (char c : line.toCharArray()) {
            if (c != '\t') {
                break;
            } else {
                indent.append(c);
                count++;
            }
        }

        for (char c : line.toCharArray()) {
            if (c == '(') {
                openBracketCount++;
            } else if (c == '[') {
                openSquareBracketCount++;
            } else if (c == '{') {
                openCurlyBracketCount++;
            } else if (c == ')') {
                closedBracketCount++;
            } else if (c == ']') {
                closedSquareBracketCount++;
            } else if (c == '}') {
                closedCurlyBracketCount++;
            }
        }

        if (openBracketCount > closedBracketCount ||
                openSquareBracketCount > closedSquareBracketCount ||
                openCurlyBracketCount > closedCurlyBracketCount) {
            indent.append('\t');
        }

        if ((caretLeft == '(' && caretRight == ')') || (caretLeft == '{' && caretRight == '}')
                || (caretLeft == '[' && caretRight == ']')) {
            indent.append('\n');
            indent.append("\t".repeat(Math.max(0, count)));
            moveCaret = count + 1;
        }

        return indent.toString();
    }

    public void addAccelerator(MenuItem menuItem, KeyCode keyCode) {

        KeyCombination newComb = new KeyCodeCombination(keyCode, KeyCombination.CONTROL_DOWN);
        menuItem.setAccelerator(newComb);

    }

    public void addEventHandlers(InlineCssTextArea textArea, Tab tab) {

        textArea.addEventFilter(KeyEvent.KEY_TYPED, event -> {

            int caretPosition = textArea.getCaretPosition();
            switch (event.getCharacter()) {
                case "(":
                    event.consume();
                    if (textArea.getSelectedText().isEmpty()) {
                        textArea.replaceText(caretPosition, caretPosition, "()");
                        textArea.moveTo(caretPosition + 1);
                    } else {
                        System.out.println("LOL");
                        textArea.replaceSelection(String.format("(%s)", textArea.getSelectedText()));
                    }
                    color(textArea);
                    break;
                case "{":
                    event.consume();
                    if (textArea.getSelectedText().isEmpty()) {
                        textArea.replaceText(caretPosition, caretPosition, "{}");
                        textArea.moveTo(caretPosition + 1);
                    } else {
                        textArea.replaceSelection(String.format("{%s}", textArea.getSelectedText()));
                    }
                    color(textArea);
                    break;
                case "[":
                    event.consume();
                    if (textArea.getSelectedText().isEmpty()) {
                        textArea.replaceText(caretPosition, caretPosition, "[]");
                        textArea.moveTo(caretPosition + 1);
                    } else {
                        textArea.replaceSelection(String.format("[%s]", textArea.getSelectedText()));
                    }
                    color(textArea);
                    break;
                case "\"":
                    event.consume();
                    if (textArea.getSelectedText().isEmpty()) {
                        textArea.replaceText(caretPosition, caretPosition, "\"\"");
                        textArea.moveTo(caretPosition + 1);
                    } else {
                        textArea.replaceSelection(String.format("\"%s\"", textArea.getSelectedText()));
                    }
                    color(textArea);
                    break;
                case "'":
                    event.consume();
                    if (textArea.getSelectedText().isEmpty()) {
                        textArea.replaceText(caretPosition, caretPosition, "''");
                        textArea.moveTo(caretPosition + 1);
                    } else {
                        textArea.replaceSelection(String.format("'%s'", textArea.getSelectedText()));
                    }
                    color(textArea);
                    break;
                case "*":
                    event.consume();
                    if (!textArea.getText().isEmpty() &&
                            textArea.getText().charAt(textArea.getCaretPosition() - 1) == '/') {
                        textArea.replaceText(caretPosition, caretPosition, "**/");
                        textArea.moveTo(caretPosition + 1);
                    } else {
                        textArea.replaceText(caretPosition, caretPosition, "*");
                    }
                    color(textArea);
                    break;
            }

        });
        textArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {

            int caretPosition = textArea.getCaretPosition();
            switch (event.getCode()) {
                case TAB:
                    textArea.replaceText(caretPosition, caretPosition, "\t");
                    textArea.moveTo(caretPosition + 1);
                    break;
                case BACK_SPACE:
                    String text = textArea.getText();
                    char caretLeft = caretPosition > 0 ? text.charAt(caretPosition - 1) : '\u0000';
                    char caretRight = caretPosition < text.length() ? text.charAt(caretPosition) : '\u0000';
                    if ((caretLeft == '(' && caretRight == ')') || (caretLeft == '{' && caretRight == '}') ||
                            (caretLeft == '[' && caretRight == ']') || (caretLeft == '\"' && caretRight == '\"') ||
                                (caretLeft == '\'' && caretRight == '\'')) {
                        textArea.replaceText(caretPosition, caretPosition + 1, "");
                    }
                    break;

            }
        });
        textArea.setOnKeyPressed(event -> {

            int caretPosition = textArea.getCaretPosition();
            if (Objects.requireNonNull(event.getCode()) == KeyCode.ENTER) {
                textArea.insertText(caretPosition, textHandler(textArea));
                if (moveCaret != 0) {
                    textArea.moveTo(textArea.getCaretPosition() - moveCaret);
                    moveCaret = 0;
                }
                if (textArea.getText().length() > 2 &&
                        textArea.getText().charAt(textArea.getCaretPosition() - 2) == '*' &&
                        textArea.getText().charAt(textArea.getCaretPosition()) == '*') {
                    textArea.replaceText(textArea.getCaretPosition(), textArea.getCaretPosition(), "\n ");
                    textArea.moveTo(textArea.getCaretPosition() - 2);
                    textArea.replaceText(textArea.getCaretPosition(), textArea.getCaretPosition(), " * ");
                }
            }

        });
        textArea.setOnKeyTyped(event -> {

            if (Character.isLetterOrDigit(event.getCharacter().charAt(0)) && saved.get(tabs.indexOf(tab))) {
                saved.set(tabs.indexOf(tab), false);
                HBox header = (HBox) tab.getGraphic();
                header.getChildren().remove(0);
                header.getChildren().add(0, new Label("* " +
                        new File(filePaths.get(tabs.indexOf(tab))).getName() + "     "));
            }
            color(textArea);

        });

    }

    public void color(InlineCssTextArea textArea) {
        String line = textArea.getText();

        line = matchAndColor(line, textArea, "/\\*.*?\\*/",
                "green", true, true);
        line = matchAndColor(line, textArea, "(//[^\\n]*)",
                "grey", false, false);
        line = matchAndColor(line, textArea, "\"([^\"]*)\"",
                "green", true, true);

        int index = 0;
        int startIndex, endIndex;
        boolean decrement = false;
        while (index < line.length()) {
            while (index < line.length() && (Character.isWhitespace(line.charAt(index)) ||
                    (index > 0 && line.charAt(index - 1) == '\u0000' && line.charAt(index) != '\u0000') ||
                    (index > 0 && line.charAt(index) == '\u0000' && line.charAt(index - 1) != '\u0000'))) {
                index++;
            }
            if (decrement) {
                decrement = false;
                startIndex = index - 1;
            } else {
                startIndex = index;
            }
            while (index < line.length() && !Character.isWhitespace(line.charAt(index))) {
                if (index > 0 && line.charAt(index - 1) == '\u0000' && line.charAt(index) != '\u0000') {
                    decrement = true;
                    break;
                }
                if (index > 0 && line.charAt(index) == '\u0000' && line.charAt(index - 1) != '\u0000') {
                    //decrement = true;
                    break;
                }
                index++;
            }
            endIndex = index;
            if (ORANGE_KEY_WORDS.contains(line.substring(startIndex, endIndex))) {
                colorThis(textArea, "FF9D00", new int[] { startIndex, endIndex });
            } else if (!line.substring(startIndex, endIndex).contains("\u0000")) {
                colorThis(textArea, "black", new int[] { startIndex, endIndex });
            }
        }

    }

    public String matchAndColor(String line, InlineCssTextArea textArea, String regex,
                                           String color, boolean dotAll, boolean boundaries) {
        Pattern pattern;
        if (dotAll) {
            pattern = Pattern.compile(regex, Pattern.DOTALL);
        } else {
            pattern = Pattern.compile(regex);
        }

        // Create a matcher for the input text
        Matcher matcher = pattern.matcher(line);

        ArrayList<int[]> lineArray = new ArrayList<>();

        // Find all matches
        while (matcher.find()) {
            if (boundaries) {
                lineArray.add(new int[]{matcher.start(), matcher.end()});
            } else {
                lineArray.add(new int[]{matcher.start(1), matcher.end(1)});
            }
        }
        StringBuilder modifiedLine = new StringBuilder(line);
        for (int[] word: lineArray) {
            colorThis(textArea, color, word);
            modifiedLine.replace(word[0], word[1], "\u0000".repeat(word[1] - word[0]));
        }

        return modifiedLine.toString();
    }

    public void colorThis(InlineCssTextArea textArea, String color, int[] word) {

        textArea.setStyle(word[0], word[1], "-fx-fill: " + color + ";");

    }

    public RootTreeNode parseDirectory(Path path) {

        RootTreeNode rootDirectory = new RootTreeNode(path);
        System.out.println(!Files.exists(path));
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            return null;
        }
        Map<Path, DirectoryTreeNode> pathMap = new HashMap<>();
        pathMap.put(path, rootDirectory);

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs){

                    DirectoryTreeNode parent = pathMap.get(filePath.getParent());
                    FileTreeNode file = new FileTreeNode(filePath, parent);
                    parent.addChild(file);
                    return FileVisitResult.CONTINUE;

                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {

                    if (!dir.equals(path)) {
                        DirectoryTreeNode parent = pathMap.get(dir.getParent());
                        DirectoryTreeNode directory = new DirectoryTreeNode(dir, parent);
                        parent.addChild(directory);
                        pathMap.put(dir, directory);
                    }
                    return FileVisitResult.CONTINUE;

                }

            });
        } catch (IOException e) {

            System.out.println(e.getMessage());
        }

        return rootDirectory;

    }

    public TreeView<HBox> createTree(RootTreeNode root) {

        HBox hBox = new HBox();
        FontIcon icon = new FontIcon(FontAwesomeRegular.FOLDER_OPEN);
        icon.setIconColor(Color.RED);
        hBox.getChildren().add(icon);
        hBox.getChildren().add(new Label("  " + root.getName()));
        hBox.setAlignment(Pos.CENTER_LEFT);
        CustomTreeItem<HBox> rootItem = new CustomTreeItem<>(hBox, root.getPath());
        rootItem.setExpanded(true);
        rootItem = addChildren(root, rootItem);
        TreeView<HBox> treeView = new TreeView<>(rootItem);
        treeView.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getClickCount() == 2) {
                TreeItem<HBox> selectedItem = treeView.getSelectionModel().getSelectedItem();
                CustomTreeItem<HBox> item = (CustomTreeItem<HBox>) selectedItem;
                if (selectedItem != null) {
                    openFile(tabPane, item.getPath());
                }
            }
        });
        return treeView;

    }

    public CustomTreeItem<HBox> addChildren(TreeNode parentNode, CustomTreeItem<HBox> parentItem) {

        for (TreeNode childNode : parentNode.getChildren()) {

            HBox hBox = new HBox();
            String[] parts = childNode.getName().split("\\.");
            FontIcon icon = new FontIcon((childNode instanceof DirectoryTreeNode) ?
                    FontAwesomeRegular.FOLDER_OPEN :
                    (Objects.equals(parts[parts.length - 1], "java")) ? FontAwesomeRegular.FILE_CODE :
                    FontAwesomeRegular.FILE);
            icon.setIconColor((childNode instanceof DirectoryTreeNode) ? Color.GREEN :
                    (Objects.equals(parts[parts.length - 1], "java")) ? Color.PURPLE : Color.GRAY);
            hBox.getChildren().add(icon);
            hBox.getChildren().add(new Label("  " + childNode.getName()));
            hBox.setAlignment(Pos.CENTER_LEFT);
            CustomTreeItem<HBox> childItem = new CustomTreeItem<>(hBox, childNode.getPath());
            childItem.setExpanded(true);
            parentItem.getChildren().add(childItem);
            if (childNode instanceof DirectoryTreeNode) {
                addChildren(childNode, childItem);
            }

        }

        return parentItem;
    }

    public ContextMenu getContextMenu(Object[]... menus) {

        ContextMenu contextMenu = new ContextMenu();
        for (Object[] menu : menus) {
            MenuItem menuItem = new MenuItem((String)menu[0]);
            addAccelerator(menuItem, (KeyCode) menu[1]);
            switch ((Integer) menu[2]) {
                case 1:
                    menuItem.setOnAction(event -> cut());
                    break;
                case 2:
                    menuItem.setOnAction(event -> copy());
                    break;
                case 3:
                    menuItem.setOnAction(event -> paste());
                    break;
            }
            contextMenu.getItems().add(menuItem);
        }
        return contextMenu;

    }

    public ArrayList<Path> getOpenFilesPaths() {

        return this.openFilesPaths;
    }

    public Path getOpenProjectPath() {

        return this.openProjectPath;
    }

    public void addPreviousContent(ArrayList<Path> paths) {

        if (paths != null) {
            openProject(tabPane, paths.get(0));
            if (paths.size() > 1) {
                paths.remove(0);
                for (Path file : paths) {
                    openFile(tabPane, file);
                }
            }
        } else {
            newFile(tabPane, null, null, true);
        }

    }

}