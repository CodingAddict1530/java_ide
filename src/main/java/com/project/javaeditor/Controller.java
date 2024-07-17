package com.project.javaeditor;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.ResourceBundle;
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

    private int moveCaret = 0;

    private final ArrayList<Tab> tabs = new ArrayList<>();
    private final ArrayList<String> filePaths = new ArrayList<>();
    private final ArrayList<Boolean> saved = new ArrayList<>();

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        newFile(tabPane, null, null);

        addAccelerator(newFile, KeyCode.N, KeyCodeCombination.CONTROL_DOWN);
        addAccelerator(openFile, KeyCode.O, KeyCodeCombination.CONTROL_DOWN);
        addAccelerator(saveFile, KeyCode.S, KeyCodeCombination.CONTROL_DOWN);
        addAccelerator(closeFile, KeyCode.Q, KeyCodeCombination.CONTROL_DOWN);
        addAccelerator(cut, KeyCode.X, KeyCodeCombination.CONTROL_DOWN);
        addAccelerator(copy, KeyCode.C, KeyCodeCombination.CONTROL_DOWN);
        addAccelerator(paste, KeyCode.V, KeyCodeCombination.CONTROL_DOWN);

    }

    @FXML
    public void newFile() {

        newFile(tabPane, null, null);
    }

    public void newFile(TabPane tabPane, String path, String text) {

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
        closeBtn.setOnAction(event -> {
            tabPane.getTabs().remove(newTab);
        });
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
        addEventHandlers(textArea, newTab);
        textArea.replaceText((text == null) ? "" : text);
        color(textArea);

        newTab.setContent(textArea);
        tabPane.getTabs().add(newTab);

        //Focus new tab.
        tabPane.getSelectionModel().select(newTab);
    }

    @FXML
    public void closeFile() {

        closeFile(tabPane);
    }

    public void closeFile(TabPane tabPane) {

        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            saved.remove(tabs.indexOf(selectedTab));
            filePaths.remove(tabs.indexOf(selectedTab));
            tabs.remove(selectedTab);
            tabPane.getTabs().remove(selectedTab);
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
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save File");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Java files", "*.java"),
                    new FileChooser.ExtensionFilter("All files", "*.*")
            );
            file = fileChooser.showSaveDialog(tabPane.getScene().getWindow());
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
                System.out.println(e.getMessage());;
            }
        }
    }

    @FXML
    public void openFile() {

        openFile(tabPane);
    }

    public void openFile(TabPane tabPane) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Java files", "*.java"),
                new FileChooser.ExtensionFilter("All files", "*.*")
        );
        File file = fileChooser.showOpenDialog(tabPane.getScene().getWindow());

        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line);
                text.append("\n");
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());;
        }

        newFile(tabPane, file.getPath(), text.toString());

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

    public void addAccelerator(MenuItem menuItem, KeyCode keyCode, KeyCombination.Modifier keyCodeComb) {

        KeyCombination newComb = new KeyCodeCombination(keyCode, keyCodeComb);
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
                            (caretLeft == '[' && caretRight == ']')) {
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

        // Split with space, don't include space characters like \n, \t etc.
        String[] lineArray = line.split("\\s+");
        for (String word: lineArray) {
            if (ORANGE_KEY_WORDS.contains(word)) {
                colorThis(textArea, "red", word, line);
            } else {
                colorThis(textArea, "black", word, line);
            }
        }
    }

    public void colorThis(InlineCssTextArea textArea, String color, String word, String line) {

        Pattern pattern = Pattern.compile("(?<!\\w)" + Pattern.quote(word) + "(?!\\w)");
        Matcher matcher = pattern.matcher(line);

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            textArea.setStyle(start, end, "-fx-fill: " + color + ";");
        }

    }

}