package com.project.custom_classes;

import com.project.utility.EditAreaUtility;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import org.fxmisc.richtext.InlineCssTextArea;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Stack;

public class CustomTextArea extends InlineCssTextArea {

    private static final int MAX_UNDO_REDO_ENTRIES = 1000;
    private final InlineCssTextArea innerTextArea = new InlineCssTextArea();
    private int moveCaret = 0;
    private final LinkedList<TextAreaChange> undoStack = new LinkedList<>();
    private final LinkedList<TextAreaChange> redoStack = new LinkedList<>();

    public CustomTextArea(Boolean isColored) {

        super();

        this.addEventFilter(KeyEvent.KEY_PRESSED, event -> {

            int caretPosition = this.getCaretPosition();
            switch (event.getCode()) {
                case TAB:
                    event.consume();
                    if (EditAreaUtility.complitionTooltip.isShowing()) {
                        if (EditAreaUtility.completionTooltipCurrentFocus < 0) {
                            EditAreaUtility.completionTooltipCurrentFocus = 0;
                        }
                        GridPane gp = ((GridPane) ((ScrollPane) EditAreaUtility.complitionTooltip.getGraphic()).getContent());
                        Label label = ((Label) gp.getChildren().get(
                                EditAreaUtility.completionTooltipCurrentFocus
                        ));
                        for (int i = this.getCaretPosition() - 1; i >= 0; i--) {
                            if (Character.isLetterOrDigit(this.getText().charAt(i))) {
                                this.replaceText(i, i + 1, "");
                            } else {
                                break;
                            }
                        }
                        this.replaceText(this.getCaretPosition(), this.getCaretPosition(), label.getText().split(" ")[0]);
                        EditAreaUtility.complitionTooltip.hide();
                        break;
                    }
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
                case DOWN:
                    if (EditAreaUtility.complitionTooltip.isShowing()) {
                        event.consume();
                        GridPane gp = ((GridPane) ((ScrollPane) EditAreaUtility.complitionTooltip.getGraphic()).getContent());
                        if (EditAreaUtility.completionTooltipCurrentFocus >= gp.getChildren().size() - 1) {
                            EditAreaUtility.completionTooltipCurrentFocus = -1;
                            ((ScrollPane) EditAreaUtility.complitionTooltip.getGraphic()).setVvalue(0);
                        }
                        gp.getChildren().get(
                                (EditAreaUtility.completionTooltipCurrentFocus < 0) ?
                                        gp.getChildren().size() - 1 : EditAreaUtility.completionTooltipCurrentFocus
                        ).getStyleClass().remove("label-focused");
                        gp.getChildren().get(
                                ++EditAreaUtility.completionTooltipCurrentFocus
                        ).getStyleClass().add("label-focused");
                        if (EditAreaUtility.completionTooltipCurrentFocus % 10 == 0 && EditAreaUtility.completionTooltipCurrentFocus != 0) {
                            double scrollAmount = 1.0 / ((gp.getChildren().size() % 10 != 0) ?
                                    (gp.getChildren().size() / 10) + 1 :
                                    (gp.getChildren().size() / 10));
                            ((ScrollPane) EditAreaUtility.complitionTooltip.getGraphic()).setVvalue(
                                    (((ScrollPane) EditAreaUtility.complitionTooltip.getGraphic()).getVvalue() + scrollAmount > 1) ?
                                            1 :
                                            ((ScrollPane) EditAreaUtility.complitionTooltip.getGraphic()).getVvalue() + scrollAmount
                            );
                        }
                    }
                    break;
                case UP:
                    if (EditAreaUtility.complitionTooltip.isShowing()) {
                        event.consume();
                        GridPane gp = ((GridPane) ((ScrollPane) EditAreaUtility.complitionTooltip.getGraphic()).getContent());
                        if (EditAreaUtility.completionTooltipCurrentFocus <=  0) {
                            EditAreaUtility.completionTooltipCurrentFocus = gp.getChildren().size();
                            ((ScrollPane) EditAreaUtility.complitionTooltip.getGraphic()).setVvalue(1);
                        }
                        gp.getChildren().get(
                                (EditAreaUtility.completionTooltipCurrentFocus >= gp.getChildren().size()) ?
                                        0 : EditAreaUtility.completionTooltipCurrentFocus
                        ).getStyleClass().remove("label-focused");
                        gp.getChildren().get(
                                --EditAreaUtility.completionTooltipCurrentFocus
                        ).getStyleClass().add("label-focused");
                        if (EditAreaUtility.completionTooltipCurrentFocus % 10 == 0 && EditAreaUtility.completionTooltipCurrentFocus != gp.getChildren().size() - 1) {
                            double scrollAmount = 1.0 / ((gp.getChildren().size() % 10 != 0) ?
                                    (gp.getChildren().size() / 10) + 1 :
                                    (gp.getChildren().size() / 10));
                            ((ScrollPane) EditAreaUtility.complitionTooltip.getGraphic()).setVvalue(
                                    (((ScrollPane) EditAreaUtility.complitionTooltip.getGraphic()).getVvalue() - scrollAmount < 0) ?
                                            0 :
                                            ((ScrollPane) EditAreaUtility.complitionTooltip.getGraphic()).getVvalue() - scrollAmount
                            );
                        }
                    }
                    break;
            }
            if (new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN).match(event)) {
                event.consume();
                EditAreaUtility.undo(this, true);
            } else if (new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN).match(event)) {
                event.consume();
                EditAreaUtility.undo(this, false);
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

    public void pushUndo(TextAreaChange change) {

        this.undoStack.addLast(change);
        if (this.undoStack.size() > MAX_UNDO_REDO_ENTRIES) {
            this.undoStack.removeFirst();
        }
    }

    public TextAreaChange popUndo() {

        return this.undoStack.pollLast();
    }

    public void pushRedo(TextAreaChange change) {

        this.redoStack.addLast(change);
        if (this.redoStack.size() > MAX_UNDO_REDO_ENTRIES) {
            this.redoStack.removeFirst();
        }
    }

    public TextAreaChange popRedo() {

        return this.redoStack.pollLast();
    }

}
