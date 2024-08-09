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

package com.project.managers;

import com.project.custom_classes.*;
import com.project.gradle.GradleWrapper;
import com.project.utility.SettingsUtility;
import com.sun.jdi.Location;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Tab;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.fxmisc.richtext.event.MouseOverTextEvent;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles Edit area operations.
 */
public class EditAreaManager {

    /**
     * A list of the keywords to be colored orange.
     */
    public static final ArrayList<String> ORANGE_KEY_WORDS = new ArrayList<>(List.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "null",
            "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while", "@interface"
    ));

    /**
     * The logger for the class.
     */
    private static final Logger logger = LogManager.getLogger(GradleWrapper.class);

    /**
     * Button on footer displaying current line and character.
     */
    private static Button goToBtn;

    /**
     * A list of non-alphanumeric completion trigger characters.
     */
    private static final ArrayList<Character> completionTriggers = new ArrayList<>(List.of('.', '(', '"', '\'', ':', '@', '[', '{', ' '));

    /**
     * A list of signature help trigger characters.
     */
    private static final ArrayList<Character> sHelpTriggers = new ArrayList<>(List.of('(', ','));

    /**
     * A tooltip to display hover messages.
     */
    private static final Tooltip tooltip = new Tooltip();
    static {
        tooltip.setWrapText(true);
        tooltip.setMaxHeight(300);
        tooltip.setMaxWidth(500);
    }

    /**
     * Stores the index of the focused item in the completion tooltip.
     */
    private static Integer completionTooltipCurrentFocus = -1;

    /**
     * A tooltip to display completion prompts
     */
    private static final Tooltip complitionTooltip = new Tooltip();
    static {
        complitionTooltip.setOnHiding(event -> completionTooltipCurrentFocus = -1);
    }

    /**
     * Stores all mouse event, event handlers to have a reference to them.
     */
    private static final ArrayList<EventHandler<MouseEvent>> mouseEvents = new ArrayList<>();

    /**
     * Stores the current version of each file (tab).
     * Used by the language server.
     */
    private static final Map<Tab, Integer> currentVersions = new HashMap<>();

    /**
     * Whether the operation is an undo.
     */
    private static boolean isUndo = false;

    /**
     * Whether the operation is a redo.
     */
    private static boolean isRedo = false;

    /**
     * Adds event handlers to the CustomTextArea.
     *
     * @param textArea The TextArea.
     * @param tab The tab containing it.
     */
    public static void addEventHandlers(CustomTextArea textArea, Tab tab) {

        // Start keeping track of the version of the file.
        currentVersions.put(tab, 1);

        // Remove default undo manager to allow the use of a custom one.
        textArea.setUndoManager(null);

        // Listen for changes in the inner text of the CustomTextArea.
        textArea.getInnerTextArea().textProperty().addListener((observable, oldValue, newValue) -> {

            // If change wasn't cause by undo or redo, empty the redo stack (LinkedList).
            if (!isRedo && !isUndo && textArea.popRedo() != null) {

                // Use a different thread to avoid blocking JavaFX thread when the stack has
                // a lot of entries.
                new Thread(() -> {
                    while (textArea.popRedo() != null) {
                    }
                }).start();
            }

            // Check whether it is an undo action. This will be ignored.
            if (!isUndo) {
                diff_match_patch dmp = new diff_match_patch();

                // Determine the changes.
                LinkedList<diff_match_patch.Diff> diffs = dmp.diff_main(oldValue, newValue);
                dmp.diff_cleanupSemantic(diffs);
                int index = 0;
                for (diff_match_patch.Diff diff : diffs) {
                    if (diff.operation == diff_match_patch.Operation.EQUAL) {
                        index = diff.text.length();
                    } else {

                        // Push the changes onto the undo stack.
                        textArea.pushUndo(new TextAreaChange(diff.operation, diff.text, index));
                    }

                }
            } else {
                isUndo = false;
            }

            // Check whether the contents have changed effectively.
            if (!oldValue.equals(newValue)) {
                int size = mouseEvents.size();

                // Empty mouseEvents.
                for (int i = 0; i < size; i++) {
                    textArea.removeEventHandler(MouseEvent.MOUSE_MOVED, mouseEvents.get(0));
                    mouseEvents.remove(0);
                }

                // Hide the completion tooltip if it is showing.
                if (complitionTooltip.isShowing()) {
                    complitionTooltip.hide();
                }

                // Increment the version of the file.
                currentVersions.replace(tab, currentVersions.get(tab) + 1);

                // Notify the server that the file contents have changed.
                JLSManager.didChange(OpenFilesTracker.getOpenFile(tab).getFile().toPath(), newValue, currentVersions.get(tab));

                // Start new thread to avoid blocking main thread during communication with server.
                new Thread(() -> {
                    if (textArea.getCaretPosition() > 0) {
                        List<CompletionItem> items = null;
                        char currentChar = textArea.getInnerTextArea().getText().charAt(textArea.getInnerTextArea().getCaretPosition() - 1);

                        // Check whether the input character is a signature help trigger character.
                        if (sHelpTriggers.contains(currentChar)) {

                            // If so request signature help from server.
                            SignatureHelp s = JLSManager.getSignatureHelp(OpenFilesTracker.getOpenFile(tab).getFile().toPath(), getPosition(textArea, null));
                        } // Check whether the input character is a completion trigger character.
                        else if (completionTriggers.contains(currentChar) || Character.isAlphabetic(currentChar)) {

                            // If so request for completion from the server.
                            items = JLSManager.complete(OpenFilesTracker.getOpenFile(tab).getFile().toPath(), getPosition(textArea, null));
                        }

                        // Check whether an item was returned.
                        if (items != null) {

                            // Create a copy to use in the lambda.
                            List<CompletionItem> finalItems = items;

                            // Execute UI changes on the JavaFX Thread.
                            Platform.runLater(() -> {

                                if (complitionTooltip.isShowing()) {
                                    complitionTooltip.hide();
                                }
                                // Add items to the completion tooltip.
                                populateCompletionTooltip(finalItems, textArea);

                                // If completion tooltip is still empty, carry no further action.
                                if (((GridPane) ((ScrollPane) EditAreaManager.complitionTooltip.getGraphic()).getContent()).getChildren().isEmpty()) {
                                    return;
                                }

                                // Show the tooltip.
                                textArea.getCaretBounds().ifPresent(bounds -> {
                                    Point2D pos2D = new Point2D(bounds.getMaxX(), bounds.getMaxY());
                                    complitionTooltip.show(textArea, pos2D.getX(), pos2D.getY());
                                });
                            });
                        }

                    }
                }).start();

                // Check whether file was saved before, and mark it as unsaved.
                if (Boolean.TRUE.equals(OpenFilesTracker.isSaved(tab))) {
                    OpenFilesTracker.getOpenFile(tab).setIsSaved(false);
                    HBox header = (HBox) tab.getGraphic();
                    header.getChildren().remove(0);
                    Label headerLabel = new Label("* " +
                            OpenFilesTracker.getOpenFile(tab).getFile().getName() + "     ");
                    headerLabel.setStyle("-fx-text-fill: white");
                    header.getChildren().add(0, headerLabel);
                }
            }
        });

        // Listen for changes in the position of the caret of the inner TextArea.
        textArea.getInnerTextArea().caretPositionProperty().addListener((observable, oldValue, newValue) -> {

            // Adjust what goTo displays
            int line = getPosition(textArea, textArea.getCaretPosition()).getLine();
            int character = getPosition(textArea, textArea.getCaretPosition()).getCharacter();
            goToBtn.setText(String.format("%d:%d", line + 1, character));

        });

        // Set a hover to be determined by the mouse stopping for 300 milliseconds.
        textArea.setMouseOverTextDelay(Duration.ofMillis(300));

        // Listen for when the mouse starts to hover.
        textArea.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_BEGIN, event -> {

            int index = event.getCharacterIndex();
            if (index > 0 && index < textArea.getLength()) {

                // Run on new thread to avoid blocking the main thread during server interactions.
                new Thread(() -> {

                    // Request for a Hover object from the server.
                    Hover hoverResult = JLSManager.getHover(OpenFilesTracker.getOpenFile(tab).getFile().toPath(), getPosition(textArea, index));

                    // Check for content in the hover object.
                    if (hoverResult != null) {
                        if (hoverResult.getContents() != null) {
                            if (hoverResult.getContents().getRight() != null) {
                                displayHoverResult(hoverResult.getContents().getRight().getValue(), textArea);
                            } else if (hoverResult.getContents().getLeft() != null) {
                                for (Either<String, MarkedString> obj : hoverResult.getContents().getLeft()) {
                                    if (obj.getRight() != null) {
                                        displayHoverResult(obj.getRight().getValue(), textArea);
                                    } else if (obj.getLeft() != null) {
                                        displayHoverResult(obj.getLeft(), textArea);
                                    }
                                }
                            }
                        }
                    }
                }).start();
            }

        });

        // Listen for when the mouse stops hovering.
        textArea.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_END, event -> {

            tooltip.hide();
            Tooltip.uninstall(textArea, tooltip);

        });

        // Listen for a mouse click.
        textArea.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {

            // Hide the completion tooltip.
            if (complitionTooltip.isShowing()) {
                complitionTooltip.hide();
            }

        });

    }

    /**
     * Undoes or redoes the previous action.
     *
     * @param textArea The CustomTextArea.
     * @param undo whether it is an undo or redo
     */
    public static void undoOrRedo(CustomTextArea textArea, boolean undo) {

        TextAreaChange previous = (undo) ? textArea.popUndo() : textArea.popRedo();
        if (previous != null) {

            // Check whether data was added to the TextArea.
            if (previous.operation() == diff_match_patch.Operation.INSERT) {
                if (undo) {
                    isUndo = true;

                    // If it is an undo, push it onto the redo stack.
                    textArea.pushRedo(new TextAreaChange(diff_match_patch.Operation.DELETE, previous.text(), previous.position()));
                } else {
                    isRedo = true;
                }

                // Remove the text that was added.
                textArea.replaceText(
                        previous.position(),
                        previous.position() + previous.text().length(),
                        ""
                );
            } else {
                if (undo) {
                    isUndo = true;
                    textArea.pushRedo(new TextAreaChange(diff_match_patch.Operation.INSERT, previous.text(), previous.position()));
                } else {
                    isRedo = true;
                }

                // Add back the text that was removed.
                textArea.replaceText(
                        previous.position(),
                        previous.position(),
                        previous.text()
                );
            }
        }

    }

    /**
     * Returns the position of the caret as a Position object.
     *
     * @param textArea The CustomTextArea.
     * @param index The index of the caret.
     * @return The Position.
     */
    private static org.eclipse.lsp4j.Position getPosition(CustomTextArea textArea, Integer index) {

        // Use the text in the inner TextArea.
        String text = textArea.getInnerTextArea().getText();
        int lineCount = 0;
        int charCount = 0;
        for (int i = 0; i < text.length(); i++) {
            charCount++;
            if (text.charAt(i) == '\n') {
                lineCount++;
                charCount = 0;
            }
            if (i == ((index == null) ? textArea.getInnerTextArea().getCaretPosition(): index) - 1) {
                break;
            }
        }

        return new org.eclipse.lsp4j.Position(lineCount, charCount);
    }

    /**
     * Adds completion data to the completion Tooltip.
     *
     * @param items The completion items.
     * @param textArea The CustomTextArea.
     */
    private static void populateCompletionTooltip(List<CompletionItem> items, CustomTextArea textArea) {

        GridPane gridPane = new GridPane();
        gridPane.setStyle("-fx-background: black;-fx-fill: white");
        for (int i = 0; i < items.size(); i++) {
            Label lineLabel = new Label(items.get(i).getLabel());
            lineLabel.setStyle("-fx-padding: 6px;-fx-font-size: 12px");

            // A copy of i to use in the lambda.
            int finalI = i;

            // Listen for clicks on the items in the Tooltip.
            lineLabel.setOnMouseClicked(event -> {

                // Single click focuses the item.
                if (event.getClickCount() == 1) {
                    lineLabel.getStyleClass().add("label-focused");
                    completionTooltipCurrentFocus = finalI;
                } // Double click fires the tab key (This autocompletes with the selected item).
                else if (event.getClickCount() == 2) {
                    KeyEvent tabKeyEvent = new KeyEvent(KeyEvent.KEY_PRESSED,
                            KeyCode.TAB.getChar(),
                            KeyCode.TAB.getChar(),
                            KeyCode.TAB,
                            false,
                            false,
                            false,
                            false
                    );
                    textArea.fireEvent(tabKeyEvent);
                }
            });
            gridPane.add(lineLabel, 0, i);

        }

        ScrollPane scrollPane = new ScrollPane(gridPane);
        scrollPane.setMaxWidth(300); // Set the preferred width for the scrollable area
        scrollPane.setMaxHeight(300); // Set the preferred height for the scrollable area

        // Customize ScrollPane appearance
        scrollPane.setStyle("-fx-background: black;"); // Make background transparent
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Hide horizontal scrollbar
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        complitionTooltip.setGraphic(scrollPane);

    }

    /**
     * Colors the CustomTextArea.
     *
     * @param textArea The CustomTextArea.
     * @return The Thread.
     */
    public static Thread color(CustomTextArea textArea) {

        // Run on different thread since the operation might block the main thread.
        Thread t = new Thread(() -> {
            String line = textArea.getInnerTextArea().getText();

            // Javadoc comments in green.
            line = matchAndColor(line, textArea, "/\\*.*?\\*/",
                    "green", true, true);

            // Normal comments in grey.
            line = matchAndColor(line, textArea, "(//[^\\n]*)",
                    "grey", false, false);

            // Strings and chars in green.
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
                        break;
                    }
                    index++;
                }
                endIndex = index;

                try {

                    // Check if it is an integer and color it blue.
                    // NumberFormatException will mean it is not one.
                    Integer.parseInt(line.substring(startIndex, endIndex));
                    colorThis(textArea, "#348CEB", new int[]{startIndex, endIndex});
                } catch (NumberFormatException ignored) {
                    try {

                        // Check if it is a double and color it blue.
                        // NumberFormatException will mean it is not one.
                        Double.parseDouble(line.substring(startIndex, endIndex));
                        colorThis(textArea, "#348CEB", new int[]{startIndex, endIndex});
                    } catch (NumberFormatException ignored2) {
                        // If it is a keyword, color it orange (#FF9D00).
                        if (ORANGE_KEY_WORDS.contains(line.substring(startIndex, endIndex))) {
                            colorThis(textArea, "FF9D00", new int[]{startIndex, endIndex});
                        } // Otherwise color it white.
                        else if (!line.substring(startIndex, endIndex).contains("\u0000")) {
                            colorThis(textArea, "white", new int[]{startIndex, endIndex});
                        }
                    }
                }
            }

            // Remove any styles that could be on white spaces.
            Platform.runLater(() -> {
                for (int i = 0; i < textArea.getText().length(); i++) {
                    if (textArea.getText().charAt(i) == ' ') {
                        textArea.setStyle(i, i + 1, "");
                    }
                }
            });
        });
        t.start();
        return t;

    }

    /**
     * Colors all TextAreas.
     */
    public static void colorAll() {

        for (OpenFile file : OpenFilesTracker.getOpenFiles()) {
            color((CustomTextArea) file.getTab().getContent());
        }

    }

    /**
     * Check if the line matches the regex pattern and color it.
     *
     * @param line The line to be checked.
     * @param textArea The CustomTextArea.
     * @param regex The regex pattern.
     * @param color The color to apply.
     * @param dotAll Whether to enable dotAll mode or not. Dot mode will match any character, even '\n'.
     * @param boundaries Whether to color the boundaries as well.
     * @return The formatted string.
     */
    private static String matchAndColor(String line, CustomTextArea textArea, String regex,
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

    /**
     * Colors a part of the text in the CustomTextArea.
     *
     * @param textArea The CustomTextArea.
     * @param color The color to apply
     * @param word Store beginning and end indexes of the word.
     */
    private static void colorThis(CustomTextArea textArea, String color, int[] word) {

        // Run UI changes on JavaFX Thread.
        Platform.runLater(() -> textArea.setStyle(word[0], word[1], "-fx-fill: " + color + ";"));
    }

    /**
     * Generates a context menu for right clicks.
     *
     * @param menus Indefinite number of arrays of context menus and a number defining their action.
     * @return The ContextMenu.
     */
    public static ContextMenu getContextMenu(Object[]... menus) {

        ContextMenu contextMenu = new ContextMenu();
        for (Object[] menu : menus) {
            MenuItem menuItem = new MenuItem((String)menu[0]);
            addAccelerator(menuItem, (KeyCode) menu[1]);
            switch ((Integer) menu[2]) {
                case 1:
                    menuItem.setOnAction(event -> TextManager.cut());
                    break;
                case 2:
                    menuItem.setOnAction(event -> TextManager.copy());
                    break;
                case 3:
                    menuItem.setOnAction(event -> TextManager.paste());
                    break;
            }
            contextMenu.getItems().add(menuItem);
        }
        return contextMenu;

    }

    /**
     * Adds an accelerator to the menuItem.
     * Assumes the key will be pressed while holding Control.
     *
     * @param menuItem The MenuItem.
     * @param keyCode The KeyCode.
     */
    public static void addAccelerator(MenuItem menuItem, KeyCode keyCode) {

        KeyCombination newComb = new KeyCodeCombination(keyCode, KeyCombination.CONTROL_DOWN);
        menuItem.setAccelerator(newComb);
    }

    /**
     * Adds an accelerator to the menuItem.
     *
     * @param menuItem The MenuItem.
     * @param keyCode The KeyCode.
     * @param modifier An indefinite list of modifiers.
     */
    public static void addAccelerator(MenuItem menuItem, KeyCode keyCode, KeyCombination.Modifier... modifier) {

        KeyCombination newComb = new KeyCodeCombination(keyCode, modifier);
        menuItem.setAccelerator(newComb);
    }

    /**
     * Processes a diagnostic from the server (Errors in the file).
     *
     * @param diagnostic The Diagnostic.
     * @param file The Path to the file.
     */
    public static void processDiagnostic(Diagnostic diagnostic, Path file) {

        int startLine = diagnostic.getRange().getStart().getLine();
        int endLine = diagnostic.getRange().getEnd().getLine();
        int startChar = diagnostic.getRange().getStart().getCharacter();
        int endChar = diagnostic.getRange().getEnd().getCharacter();

        Tab tab = null;

        // A diagnostic referencing files used by the server will cause NullPointerException.
        // This typically leaves tab null.
        try {
            tab = OpenFilesTracker.getOpenFile(file).getTab();
        } catch (NullPointerException ignored) {}

        if (tab == null) {
            return;
        }

        // Empty mouseEvents.
        int size = mouseEvents.size();
        CustomTextArea textArea = (CustomTextArea) tab.getContent();
        for (int i = 0; i < size; i++) {
            textArea.removeEventHandler(MouseEvent.MOUSE_MOVED, mouseEvents.get(0));
            mouseEvents.remove(0);
        }

        // Recolor the TextArea to avoid previous formats from interfering.
        color(textArea);

        int start = 0;
        int end = 0;

        String text = textArea.getInnerTextArea().getText();

        int currentLine = 0;
        for (int i = 0; i < text.length(); i++) {

            if (text.charAt(i) == '\n') {
                currentLine++;

            }
            if (currentLine == startLine) {
                for (int j = i; j < text.length(); j++) {
                    if (j - i == startChar) {
                        start = j;
                        break;
                    }
                }
            }
            if (currentLine == endLine) {
                for (int j = i; j < text.length(); j++) {
                    if (j - i == endChar) {
                        end = j;
                        break;
                    }
                }
            }
            if (start != 0 || end != 0) {
                break;
            }

        }

        // Style the specified length in text with errors. Make it red.
        for (int i = start; i < end; i++) {
            String currentStyle = textArea.getStyleOfChar(i);
            textArea.setStyle(i + 1, i + 2, currentStyle + " -fx-fill: red;");
        }

        Tooltip tooltip = new Tooltip(diagnostic.getMessage());

        // Add event handler for showing the Tooltip
        int finalStart = start + 1;
        int finalEnd = end + 1;
        mouseEvents.add(event -> {
            int index = textArea.hit(event.getX(), event.getY()).getCharacterIndex().orElse(-1);
            if (index >= finalStart && index < finalEnd) {
                if (!tooltip.isShowing()) {
                    tooltip.show(textArea, event.getScreenX(), event.getScreenY() + 10);
                }
            } else {
                tooltip.hide();
            }
            new Timeline(new KeyFrame(
                    javafx.util.Duration.millis(4000),
                    event2 -> tooltip.hide()
            )).play();
        });
        textArea.addEventHandler(MouseEvent.MOUSE_MOVED, mouseEvents.get(mouseEvents.size() - 1));

    }

    /**
     * Sets up goToBtn.
     *
     * @param goToBtn goToBtn.
     */
    public static void setGoTo(Button goToBtn) {

        EditAreaManager.goToBtn = goToBtn;
    }

    /**
     * Displays the results of a hover request.
     *
     * @param text The text.
     * @param textArea The CustomTextArea.
     */
    private static void displayHoverResult(String text, CustomTextArea textArea) {

        // Updates being made to UI, so JavaFX thread is used.
        Platform.runLater(() -> {
            tooltip.setText(text);
            if (tooltip.getText().isEmpty()) {
                return;
            }
            Tooltip.install(textArea, tooltip);
            textArea.getCaretBounds().ifPresent(bounds -> {
                Point2D pos2D = new Point2D(bounds.getMaxX(), bounds.getMaxY());
                tooltip.show(textArea, pos2D.getX(), pos2D.getY());
            });
        });

    }

    /**
     * Retrieves the compilationTooltip.
     *
     * @return compilationTooltip.
     */
    public static Tooltip getComplitionTooltip() {

        return complitionTooltip;
    }

    /**
     * Highlights the next line to be executed during debugging.
     *
     * @param location The location of the line.
     * @return Whether it was successful.
     */
    public static boolean highlightDebugLine(Location location) {

        String className = location.declaringType().name();
        int lineNumber = location.lineNumber();
        String[] parts = className.split("\\.");
        Path path = null;

        // Find the source file.
        for (int i = parts.length - 1; i >= 0; i--) {
            Optional<Path> optionalPath = DirectoryManager.findFile(parts[i], null);
            if (optionalPath == null || optionalPath.isEmpty()) {
                if (SettingsUtility.getJavaPath() != null) {
                    optionalPath = DirectoryManager.findFile(parts[i],
                            Paths.get("src/main/files/src"));
                }
            }
            if (optionalPath != null && optionalPath.isPresent()) {
                path = optionalPath.get();
                break;
            }
        }
        if (path == null) {
            return false;
        }
        Path finalPath = path;
        CountDownLatch latch = new CountDownLatch(1);

        // Final arrays to be able to use them in lambda.
        final CustomTextArea[] textArea = new CustomTextArea[1];
        final AtomicInteger[] start = {new AtomicInteger()};
        final AtomicInteger[] end = {new AtomicInteger()};

        // UI changes on JavaFX Thread.
        Platform.runLater(() -> {
            OpenFile file = OpenFilesTracker.getOpenFile(finalPath);
            if (file == null) {

                // Open the file if it is not already open.
                FileManager.openFile(finalPath);
            }
            Tab tab = OpenFilesTracker.getOpenFile(finalPath).getTab();
            textArea[0] = (CustomTextArea) tab.getContent();

            // Get the absolute position in the TextArea of the line.
            start[0].set(textArea[0].getAbsolutePosition(lineNumber - 1, 0));
            end[0].set(textArea[0].getAbsolutePosition(lineNumber, 0));

            // Scroll to the line.
            textArea[0].showParagraphAtCenter(location.lineNumber() - 1);
            if (file != null) {
                try {
                    color(textArea[0]).join();
                } catch (Exception e) {
                    logger.error(e);
                }
            }

            // Release the thread being blocked by the latch.
            latch.countDown();
        });

        // Different call to ensure these tasks are executed last.
        new Thread(() -> {
            try {

                // Wait.
                latch.await();
            } catch (Exception e) {
                logger.error(e);
            }
            Platform.runLater(() -> {
                for (int i = start[0].get(); i < end[0].get(); i++) {

                    // Add highlight to each of these characters.
                    textArea[0].setStyle(i, i + 1, textArea[0].getStyleOfChar(i) + "-rtfx-background-color: grey;");
                }
            });
        }).start();
        return true;

    }

    /**
     * Retrieves the completionTooltipCurrentFocus.
     *
     * @return completionTooltipCurrentFocus.
     */
    public static Integer getCompletionTooltipCurrentFocus() {

        return completionTooltipCurrentFocus;
    }

}
