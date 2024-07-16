package com.project.javaeditor;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ResourceBundle;

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

        TextArea textArea = new TextArea();
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

    public String textHandler(TextArea textArea) {

        String line = getLine(textArea);
        int caretPosition = textArea.getCaretPosition();
        String text = textArea.getText();
        char caretLeft = caretPosition > 0 ? text.charAt(caretPosition - 2) : '\u0000';
        char caretRight = caretPosition < text.length() ? text.charAt(caretPosition) : '\u0000';
        return applyIndent(line, caretLeft, caretRight);

    }

    public String getLine(TextArea textArea) {

        int caretPosition = textArea.getCaretPosition();
        int start = caretPosition - 1;
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

    public void addEventHandlers(TextArea textArea) {

        textArea.setOnKeyTyped(event -> {

            int caretPosition = textArea.getCaretPosition();
            event.consume();
            switch (event.getCharacter()) {
                case "(":
                    textArea.replaceText(caretPosition, caretPosition, ")");
                    textArea.positionCaret(caretPosition);
                    break;
                case "{":
                    textArea.replaceText(caretPosition, caretPosition, "}");
                    textArea.positionCaret(caretPosition);
                    break;
                case "[":
                    textArea.replaceText(caretPosition, caretPosition, "]");
                    textArea.positionCaret(caretPosition);
                    break;
            }
        });
        textArea.setOnKeyPressed(event -> {

            int caretPosition = textArea.getCaretPosition();
            switch (event.getCode()) {
                case TAB:
                    event.consume();
                    textArea.replaceText(caretPosition, caretPosition, "\t");
                    textArea.positionCaret(caretPosition + 1);
                    break;

                case ENTER:
                    event.consume();
                    textArea.insertText(caretPosition, textHandler(textArea));
                    if (moveCaret != 0) {
                        textArea.positionCaret(textArea.getCaretPosition() - moveCaret);
                        moveCaret = 0;
                    }
                    break;
            }

        });

    }

}