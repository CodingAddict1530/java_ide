package com.project.custom_classes;

import com.project.utility.EditAreaUtility;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.fxmisc.richtext.InlineCssTextArea;

import java.util.Objects;

public class CustomTextArea extends InlineCssTextArea {

    private final InlineCssTextArea innerTextArea = new InlineCssTextArea();
    private int moveCaret = 0;

    public CustomTextArea(Boolean isColored) {

        super();

        this.addEventFilter(KeyEvent.KEY_PRESSED, event -> {

            int caretPosition = this.getCaretPosition();
            switch (event.getCode()) {
                case TAB:
                    event.consume();
                    this.replaceText(caretPosition, caretPosition, "    ");
                    this.moveTo(caretPosition + 4);
                    break;
                case BACK_SPACE:
                    String text = this.getText();
                    char caretLeft = caretPosition > 0 ? text.charAt(caretPosition - 1) : '\u0000';
                    char caretRight = caretPosition < text.length() ? text.charAt(caretPosition) : '\u0000';
                    if ((caretLeft == '(' && caretRight == ')') || (caretLeft == '{' && caretRight == '}') ||
                            (caretLeft == '[' && caretRight == ']') || (caretLeft == '\"' && caretRight == '\"') ||
                            (caretLeft == '\'' && caretRight == '\'')) {
                        this.replaceText(caretPosition, caretPosition + 1, "");
                    }
                    break;

            }
        });

        this.addEventFilter(KeyEvent.KEY_TYPED, event -> {

            int caretPosition = this.getCaretPosition();
            switch (event.getCharacter()) {
                case "(":
                    event.consume();
                    if (this.getSelectedText().isEmpty()) {
                        this.replaceText(caretPosition, caretPosition, "()");
                        this.moveTo(caretPosition + 1);
                    } else {
                        this.replaceSelection(String.format("(%s)", this.getSelectedText()));
                    }
                    EditAreaUtility.color(this);
                    break;
                case "{":
                    event.consume();
                    if (this.getSelectedText().isEmpty()) {
                        this.replaceText(caretPosition, caretPosition, "{}");
                        this.moveTo(caretPosition + 1);
                    } else {
                        this.replaceSelection(String.format("{%s}", this.getSelectedText()));
                    }
                    EditAreaUtility.color(this);
                    break;
                case "[":
                    event.consume();
                    if (this.getSelectedText().isEmpty()) {
                        this.replaceText(caretPosition, caretPosition, "[]");
                        this.moveTo(caretPosition + 1);
                    } else {
                        this.replaceSelection(String.format("[%s]", this.getSelectedText()));
                    }
                    EditAreaUtility.color(this);
                    break;
                case "\"":
                    event.consume();
                    if (this.getSelectedText().isEmpty()) {
                        this.replaceText(caretPosition, caretPosition, "\"\"");
                        this.moveTo(caretPosition + 1);
                    } else {
                        this.replaceSelection(String.format("\"%s\"", this.getSelectedText()));
                    }
                    EditAreaUtility.color(this);
                    break;
                case "'":
                    event.consume();
                    if (this.getSelectedText().isEmpty()) {
                        this.replaceText(caretPosition, caretPosition, "''");
                        this.moveTo(caretPosition + 1);
                    } else {
                        this.replaceSelection(String.format("'%s'", this.getSelectedText()));
                    }
                    EditAreaUtility.color(this);
                    break;
                case "*":
                    event.consume();
                    if (!this.getText().isEmpty() &&
                            this.getText().charAt(this.getCaretPosition() - 1) == '/') {
                        this.replaceText(caretPosition, caretPosition, "**/");
                        this.moveTo(caretPosition + 1);
                    } else {
                        this.replaceText(caretPosition, caretPosition, "*");
                    }
                    EditAreaUtility.color(this);
                    break;
            }

        });

        this.setOnKeyPressed(event -> {

            int caretPosition = this.getCaretPosition();
            if (Objects.requireNonNull(event.getCode()) == KeyCode.ENTER) {
                String textHandlerResult = textHandler(this);
                this.insertText(caretPosition, textHandlerResult);
                if (moveCaret != 0) {
                    this.moveTo(this.getCaretPosition() - moveCaret * 4);
                    moveCaret = 0;
                }
                if (this.getCaretPosition() >= 2 && this.getText().length() > 2 &&
                        this.getText().charAt(this.getCaretPosition() - 2) == '*' &&
                        this.getText().charAt(this.getCaretPosition()) == '*') {
                    this.replaceText(this.getCaretPosition(), this.getCaretPosition(), "\n ");
                    this.moveTo(this.getCaretPosition() - 2);
                    this.replaceText(this.getCaretPosition(), this.getCaretPosition(), " * ");
                }
            }

        });

        if (isColored) {
            this.setOnKeyTyped(event -> EditAreaUtility.color(this));
        }

        this.textProperty().addListener((observable, oldValue, newValue) -> {

            this.innerTextArea.replaceText(newValue);
        });

        this.caretPositionProperty().addListener((observable, oldValue, newValue) -> {

            this.innerTextArea.moveTo(newValue);
        });

    }

    public InlineCssTextArea getInnerTextArea() {

        return this.innerTextArea;
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

        int numberOfSpaces = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ' || c == '\t') {
                if (++numberOfSpaces == 4 || c == '\t') {
                    indent.append("    ");
                    count++;
                    numberOfSpaces = 0;
                }
            } else {
                break;
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
            indent.append("    ");
        }

        if ((caretLeft == '(' && caretRight == ')') || (caretLeft == '{' && caretRight == '}')
                || (caretLeft == '[' && caretRight == ']')) {
            indent.append('\n');
            indent.append("    ".repeat(Math.max(0, count)));
            moveCaret = count + 1;
        }

        return indent.toString();
    }

}
