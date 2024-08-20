/*
 * Copyright 2024 Alexis Mugisha
 * https://github.com/CodingAddict1530
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.project.custom_classes;

import com.project.managers.EditAreaManager;
import com.project.managers.FileManager;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import org.fxmisc.richtext.CodeArea;
import java.io.File;
import java.util.LinkedList;
import java.util.Objects;

/**
 * A custom CodeArea with extended functionality.
 */
public class CustomTextArea extends CodeArea {

    /**
     * Maximum number of redo and undo entries each.
     */
    private static final int MAX_UNDO_REDO_ENTRIES = 1000;

    /**
     * An inner CodeArea to hold the actual contents of the TextArea.
     * This allows the outer TextArea to be modified for user interaction while preserving actual text.
     */
    private final CodeArea innerTextArea = new CodeArea();

    /**
     * Number of time caret has to be moved.
     */
    private int moveCaret = 0;

    /**
     * A Stack (implemented as LinkedList) for undo operations.
     */
    private final LinkedList<TextAreaChange> undoStack = new LinkedList<>();

    /**
     * A Stack (implemented as LinkedList) for redo operations.
     */
    private final LinkedList<TextAreaChange> redoStack = new LinkedList<>();

    /**
     * A tooltip to display completion prompts
     */
    private static final Tooltip complitionTooltip = EditAreaManager.getComplitionTooltip();

    /**
     * Stores the index of the focused item in the completion tooltip.
     */
    private static Integer completionTooltipCurrentFocus = EditAreaManager.getCompletionTooltipCurrentFocus();

    /**
     * Instantiates a CustomTextArea object.
     *
     * @param isColored whether the TextArea will be formatted.
     */
    public CustomTextArea(Boolean isColored) {

        super();

        // Filters for certain key presses and acts before they modify anything.
        this.addEventFilter(KeyEvent.KEY_PRESSED, event -> {

            int caretPosition = this.getCaretPosition();
            switch (event.getCode()) {

                case TAB:

                    // Prevent event from propagating any further.
                    event.consume();

                    // If Tooltip is showing, tab will autocomplete.
                    if (complitionTooltip.isShowing()) {

                        // If value is less than 0, the user hasn't selected an option.
                        // Default is the first.
                        if (completionTooltipCurrentFocus < 0) {
                            completionTooltipCurrentFocus = 0;
                        }
                        GridPane gp = ((GridPane) ((ScrollPane) complitionTooltip.getGraphic()).getContent());
                        Label label = ((Label) gp.getChildren().get(
                                completionTooltipCurrentFocus
                        ));

                        // Delete any characters user had typed related to the word to autocomplete.
                        for (int i = this.getCaretPosition() - 1; i >= 0; i--) {
                            if (Character.isLetterOrDigit(this.getText().charAt(i))) {
                                this.replaceText(i, i + 1, "");
                            } else {
                                break;
                            }
                        }

                        // Add the word.
                        this.replaceText(this.getCaretPosition(), this.getCaretPosition(), label.getText().split(" ")[0]);

                        // Hide the Tooltip.
                        complitionTooltip.hide();
                        break;
                    }

                    // Here, tab will represent 4 spaces.
                    this.replaceText(caretPosition, caretPosition, "    ");
                    this.moveTo(caretPosition + 4);
                    break;

                case BACK_SPACE:
                    String text = this.getText();

                    // Check whether an opening bracket or quotation is being deleted.
                    // Delete adjacent closing bracket is there is any.
                    char caretLeft = caretPosition > 0 ? text.charAt(caretPosition - 1) : '\u0000';
                    char caretRight = caretPosition < text.length() ? text.charAt(caretPosition) : '\u0000';
                    if ((caretLeft == '(' && caretRight == ')') || (caretLeft == '{' && caretRight == '}') ||
                            (caretLeft == '[' && caretRight == ']') || (caretLeft == '\"' && caretRight == '\"') ||
                            (caretLeft == '\'' && caretRight == '\'')) {
                        this.replaceText(caretPosition, caretPosition + 1, "");
                    }
                    break;

                case DOWN:

                    // If Tooltip is showing, down arrow will navigate the Tooltip.
                    if (complitionTooltip.isShowing()) {

                        // Prevent event from propagating any further.
                        event.consume();

                        GridPane gp = ((GridPane) ((ScrollPane) complitionTooltip.getGraphic()).getContent());
                        if (completionTooltipCurrentFocus >= gp.getChildren().size() - 1) {
                            completionTooltipCurrentFocus = -1;
                            ((ScrollPane) complitionTooltip.getGraphic()).setVvalue(0);
                        }

                        // Add "label-focused" style class to the current Label being focused.
                        // This styles it to show it is focused.
                        gp.getChildren().get(
                                (completionTooltipCurrentFocus < 0) ?
                                        gp.getChildren().size() - 1 : completionTooltipCurrentFocus
                        ).getStyleClass().remove("label-focused");

                        // Remove the style from the previous Label.
                        gp.getChildren().get(
                                ++completionTooltipCurrentFocus
                        ).getStyleClass().add("label-focused");

                        // Scroll the Tooltip if needed.
                        if (completionTooltipCurrentFocus % 10 == 0 && completionTooltipCurrentFocus != 0) {
                            double scrollAmount = 1.0 / ((gp.getChildren().size() % 10 != 0) ?
                                    (gp.getChildren().size() / 10) + 1 :
                                    (gp.getChildren().size() / 10));
                            ((ScrollPane) complitionTooltip.getGraphic()).setVvalue(
                                    (((ScrollPane) complitionTooltip.getGraphic()).getVvalue() + scrollAmount > 1) ?
                                            1 :
                                            ((ScrollPane) complitionTooltip.getGraphic()).getVvalue() + scrollAmount
                            );
                        }
                    }
                    break;

                case UP:

                    // If Tooltip is showing, up arrow will navigate the Tooltip.
                    if (complitionTooltip.isShowing()) {

                        // Prevent event from propagating any further.
                        event.consume();

                        GridPane gp = ((GridPane) ((ScrollPane) complitionTooltip.getGraphic()).getContent());
                        if (completionTooltipCurrentFocus <=  0) {
                            completionTooltipCurrentFocus = gp.getChildren().size();
                            ((ScrollPane) complitionTooltip.getGraphic()).setVvalue(1);
                        }

                        // Add "label-focused" style class to the current Label being focused.
                        // This styles it to show it is focused.
                        gp.getChildren().get(
                                (completionTooltipCurrentFocus >= gp.getChildren().size()) ?
                                        0 : completionTooltipCurrentFocus
                        ).getStyleClass().remove("label-focused");

                        // Remove the style from the previous Label.
                        gp.getChildren().get(
                                --completionTooltipCurrentFocus
                        ).getStyleClass().add("label-focused");

                        // Scroll Tooltip if needed.
                        if (completionTooltipCurrentFocus % 10 == 0 && completionTooltipCurrentFocus != gp.getChildren().size() - 1) {
                            double scrollAmount = 1.0 / ((gp.getChildren().size() % 10 != 0) ?
                                    (gp.getChildren().size() / 10) + 1 :
                                    (gp.getChildren().size() / 10));
                            ((ScrollPane) complitionTooltip.getGraphic()).setVvalue(
                                    (((ScrollPane) complitionTooltip.getGraphic()).getVvalue() - scrollAmount < 0) ?
                                            0 :
                                            ((ScrollPane) complitionTooltip.getGraphic()).getVvalue() - scrollAmount
                            );
                        }
                    }
                    break;

            }
            if (new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN).match(event)) {

                // Prevent event from propagating any further.
                event.consume();
                EditAreaManager.undoOrRedo(this, true);
            } else if (new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN).match(event)) {

                // Prevent event from propagating any further.
                event.consume();
                EditAreaManager.undoOrRedo(this, false);
            }

        });

        // Filters for certain key being typed and acts before they modify anything.
        this.addEventFilter(KeyEvent.KEY_TYPED, event -> {

            int caretPosition = this.getCaretPosition();
            switch (event.getCharacter()) {
                case "(":

                    // Prevent event from propagating any further.
                    event.consume();

                    // Check whether there is selected text.
                    if (this.getSelectedText().isEmpty()) {

                        // Autocomplete with a closing bracket.
                        this.replaceText(caretPosition, caretPosition, "()");
                        this.moveTo(caretPosition + 1);
                    } else {

                        // Enclose selected text in brackets.
                        this.replaceSelection(String.format("(%s)", this.getSelectedText()));
                    }

                    // Recolor the TextArea.
                    EditAreaManager.color(this);
                    break;

                case "{":

                    // Prevent event from propagating any further.
                    event.consume();

                    // Check whether there is selected text.
                    if (this.getSelectedText().isEmpty()) {

                        // Autocomplete with a closing brace.
                        this.replaceText(caretPosition, caretPosition, "{}");
                        this.moveTo(caretPosition + 1);
                    } else {

                        // Enclose selected text in braces.
                        this.replaceSelection(String.format("{%s}", this.getSelectedText()));
                    }

                    // Recolor the TextArea.
                    EditAreaManager.color(this);
                    break;

                case "[":

                    // Prevent event from propagating any further.
                    event.consume();

                    // Check whether there is selected text.
                    if (this.getSelectedText().isEmpty()) {

                        // Autocomplete with a closing square bracket.
                        this.replaceText(caretPosition, caretPosition, "[]");
                        this.moveTo(caretPosition + 1);
                    } else {

                        // Enclose selected text in square brackets.
                        this.replaceSelection(String.format("[%s]", this.getSelectedText()));
                    }

                    // Recolor the TextArea.
                    EditAreaManager.color(this);
                    break;

                case "<":

                    // Prevent event from propagating any further.
                    event.consume();

                    // Check whether there is selected text.
                    if (this.getSelectedText().isEmpty()) {

                        // Autocomplete with a closing generic symbol.
                        this.replaceText(caretPosition, caretPosition, "<>");
                        this.moveTo(caretPosition + 1);
                    } else {

                        // Enclose selected text in Generic symbols.
                        this.replaceSelection(String.format("<%s>", this.getSelectedText()));
                    }

                    // Recolor the TextArea.
                    EditAreaManager.color(this);
                    break;

                case "\"":

                    // Prevent event from propagating any further.
                    event.consume();

                    // Check whether there is selected text.
                    if (this.getSelectedText().isEmpty()) {

                        // Autocomplete with a closing double quotation.
                        this.replaceText(caretPosition, caretPosition, "\"\"");
                        this.moveTo(caretPosition + 1);
                    } else {

                        // Enclose selected text in double quotations.
                        this.replaceSelection(String.format("\"%s\"", this.getSelectedText()));
                    }

                    // Recolor the TextArea.
                    EditAreaManager.color(this);
                    break;

                case "'":

                    // Prevent event from propagating any further.
                    event.consume();

                    // Check whether there is selected text.
                    if (this.getSelectedText().isEmpty()) {

                        // Autocomplete with a closing single quotation.
                        this.replaceText(caretPosition, caretPosition, "''");
                        this.moveTo(caretPosition + 1);
                    } else {

                        // Enclose selected text in single quotations.
                        this.replaceSelection(String.format("'%s'", this.getSelectedText()));
                    }

                    // Recolor the TextArea.
                    EditAreaManager.color(this);
                    break;

                case "*":

                    // Prevent event from propagating any further.
                    event.consume();

                    if (!this.getText().isEmpty() &&
                            this.getText().charAt(this.getCaretPosition() - 1) == '/') {

                        // Autocomplete comment.
                        this.replaceText(caretPosition, caretPosition, "**/");
                        this.moveTo(caretPosition + 1);
                    } else {

                        // Rewrite the * since event was consumed.
                        this.replaceText(caretPosition, caretPosition, "*");
                    }

                    // Recolor the TextArea.
                    EditAreaManager.color(this);
                    break;
            }

        });

        // Reacts to a key press.
        this.setOnKeyPressed(event -> {

            // Check if the key was ENTER.
            if (Objects.requireNonNull(event.getCode()) == KeyCode.ENTER) {
                int caretPosition = this.getCaretPosition();

                // Autocomplete javadoc comment style.
                if (this.getCaretPosition() >= 2 && this.getText().length() > 2 &&
                        this.getText().charAt(this.getCaretPosition() - 2) == '*' &&
                        this.getText().charAt(this.getCaretPosition()) == '*') {
                    this.replaceText(this.getCaretPosition(), this.getCaretPosition(), "\n ");
                    this.moveTo(this.getCaretPosition() - 2);
                    this.replaceText(this.getCaretPosition(), this.getCaretPosition(), " * ");
                } else {
                    String textHandlerResult = textHandler(this);
                    this.insertText(caretPosition, textHandlerResult);
                    if (moveCaret != 0) {
                        this.moveTo(this.getCaretPosition() - (moveCaret - 1) * 4 - 1);
                        moveCaret = 0;
                    }
                }
            }

        });

        // Mirror changes of TextArea to inner TextArea.
        this.textProperty().addListener((observable, oldValue, newValue) -> {
            this.innerTextArea.replaceText(newValue);
            if (isColored) {
                EditAreaManager.color(this);
            }
        });

        // Move caret of inner TextArea with that of the TextArea.
        this.caretPositionProperty().addListener((observable, oldValue, newValue) -> this.innerTextArea.moveTo(newValue));

        // Process diagnostics as user scrolls.
        this.addEventFilter(ScrollEvent.SCROLL, event -> {
            StackPane stackPane = (StackPane) this.getParent();
            if (stackPane.getChildren().size() > 1 && ((CustomCanvas) stackPane.getChildren().get(1)).getType().equals("Error")) {
                stackPane.getChildren().remove(1);
            }
            EditAreaManager.processDiagnostics();
        });

        this.setOnDragOver(event -> {
            if (event.getGestureSource() != this && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        this.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            if (dragboard.hasFiles()) {
                for (File file : dragboard.getFiles()) {
                    FileManager.openFile(file.toPath());
                }
                event.setDropCompleted(true);
            } else {
                event.setDropCompleted(false);
            }
            event.consume();
        });

    }

    /**
     * Retrieves the inner InlineCssTextArea
     *
     * @return the inner InlineCssTextArea.
     */
    public CodeArea getInnerTextArea() {

        return this.innerTextArea;
    }

    /**
     * Uses other methods to apply indent.
     *
     * @param textArea The TextArea.
     * @return The indent String.
     */
    public String textHandler(CustomTextArea textArea) {

        String line = getLine(textArea);
        int caretPosition = textArea.getCaretPosition();
        String text = textArea.getText();
        char caretLeft = caretPosition > 1 ? text.charAt(caretPosition - 2) : '\u0000';
        char caretRight = caretPosition < text.length() ? text.charAt(caretPosition) : '\u0000';
        return applyIndent(line, caretLeft, caretRight);

    }

    /**
     * Retrieves the current line in the TextArea (Where the caret is).
     *
     * @param textArea The TextArea.
     * @return The line.
     */
    public String getLine(CustomTextArea textArea) {

        int caretPosition = textArea.getCaretPosition();
        int start = (caretPosition == 0) ? 0 : caretPosition - 1;
        while (start > 0 && textArea.getText().charAt(start - 1) != '\n') {
            start--;
        }
        return textArea.getText(start, caretPosition);

    }

    /**
     * Applies indent depending on the given line.
     *
     * @param line The current line where the caret is.
     * @param caretLeft Character on the left of the caret.
     * @param caretRight Character on the right of the caret.
     * @return An indented String.
     */
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

    /**
     * Add an entry to the end of the undo stack.
     *
     * @param change The change in text.
     */
    public void pushUndo(TextAreaChange change) {

        this.undoStack.addLast(change);
        if (this.undoStack.size() > MAX_UNDO_REDO_ENTRIES) {
            this.undoStack.removeFirst();
        }
    }

    /**
     * Remove the last entry in the undo stack.
     *
     * @return The change.
     */
    public TextAreaChange popUndo() {

        return this.undoStack.pollLast();
    }

    /**
     * Add an entry to the end of the redo stack.
     *
     * @param change The change in text.
     */
    public void pushRedo(TextAreaChange change) {

        this.redoStack.addLast(change);
        if (this.redoStack.size() > MAX_UNDO_REDO_ENTRIES) {
            this.redoStack.removeFirst();
        }
    }

    /**
     * Remove the last entry in the redo stack.
     *
     * @return The change.
     */
    public TextAreaChange popRedo() {

        return this.redoStack.pollLast();
    }

    /**
     * Removes all entries on the redo stack.
     */
    public void clearRedo() {

        this.redoStack.clear();
    }

}
