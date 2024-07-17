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
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.net.URL;
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

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        newFile(tabPane);

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

        newFile(tabPane);
    }

    public void newFile(TabPane tabPane) {

        Tab newTab = new Tab();
        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER);
        Label headerLabel = new Label("Untitled.java     ");
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
        addEventHandlers(textArea);

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
            tabPane.getTabs().remove(selectedTab);
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

    public void addAccelerator(MenuItem menuItem, KeyCode keyCode, KeyCombination.Modifier keyCodeComb) {

        KeyCombination newComb = new KeyCodeCombination(keyCode, keyCodeComb);
        menuItem.setAccelerator(newComb);

    }

    public void addEventHandlers(InlineCssTextArea textArea) {

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
                    break;
                case "[":
                    event.consume();
                    if (textArea.getSelectedText().isEmpty()) {
                        textArea.replaceText(caretPosition, caretPosition, "[]");
                        textArea.moveTo(caretPosition + 1);
                    } else {
                        textArea.replaceSelection(String.format("[%s]", textArea.getSelectedText()));
                    }
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

            color(textArea);

        });

    }

    public void color(InlineCssTextArea textArea) {
        String line = textArea.getText();
        String[] lineArray = line.split(" ");
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