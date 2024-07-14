package com.project.javaeditor;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.Arrays;
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

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        newFile();

        KeyCombination newFileComb = new KeyCodeCombination(KeyCode.N, KeyCodeCombination.CONTROL_DOWN);
        KeyCombination openFileComb = new KeyCodeCombination(KeyCode.O, KeyCodeCombination.CONTROL_DOWN);
        KeyCombination saveFileComb = new KeyCodeCombination(KeyCode.S, KeyCodeCombination.CONTROL_DOWN);
        KeyCombination closeFileComb = new KeyCodeCombination(KeyCode.Q, KeyCodeCombination.CONTROL_DOWN);
        KeyCombination cutComb = new KeyCodeCombination(KeyCode.X, KeyCodeCombination.CONTROL_DOWN);
        KeyCombination copyComb = new KeyCodeCombination(KeyCode.C, KeyCodeCombination.CONTROL_DOWN);
        KeyCombination pasteComb = new KeyCodeCombination(KeyCode.V, KeyCodeCombination.CONTROL_DOWN);

        newFile.setAccelerator(newFileComb);
        openFile.setAccelerator(openFileComb);
        saveFile.setAccelerator(saveFileComb);
        closeFile.setAccelerator(closeFileComb);
        cut.setAccelerator(cutComb);
        copy.setAccelerator(copyComb);
        paste.setAccelerator(pasteComb);

    }

    public void newFile() {

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
        textArea.setOnKeyTyped(event -> {

            int caretPosition = textArea.getCaretPosition();
            event.consume();
            switch (event.getCharacter()) {
                case "(" -> textArea.replaceText(caretPosition, caretPosition, ")");
                case "{" -> textArea.replaceText(caretPosition, caretPosition, "}");
                case "[" -> textArea.replaceText(caretPosition, caretPosition, "]");
            }
            textArea.positionCaret(caretPosition + 1);
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
                    textArea.appendText(textHandler(textArea));
                    event.consume();
                    break;
            }

        });
        newTab.setContent(textArea);
        tabPane.getTabs().add(newTab);

        //Focus new tab.
        tabPane.getSelectionModel().select(newTab);
    }

    public void closeFile() {

        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            tabPane.getTabs().remove(selectedTab);
        }
    }

    private String textHandler(TextArea textArea) {

        String line = getLine(textArea);
        return applyIndent(line);

    }

    private String getLine(TextArea textArea) {

        int caretPosition = textArea.getCaretPosition();
        int start = caretPosition - 1;
        int end = caretPosition - 1;
        while (start > 0 && textArea.getText().charAt(start - 1) != '\n') {
            start--;
        }
        while (end < textArea.getLength() && textArea.getText().charAt(end) != '\n') {
            end++;
        }
        return textArea.getText(start, end);

    }

    private String applyIndent(String line) {

        StringBuilder indent = new StringBuilder();
        int openBracketCount = 0;
        int openSquareBracketCount = 0;
        int openCurlyBracketCount = 0;
        int closedBracketCount = 0;
        int closedSquareBracketCount = 0;
        int closedCurlyBracketCount = 0;
        System.out.println(Arrays.toString(line.toCharArray()));

        for (char c : line.toCharArray()) {
            if (c != '\t') {
                break;
            } else {
                indent.append(c);
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

        return indent.toString();
    }

}