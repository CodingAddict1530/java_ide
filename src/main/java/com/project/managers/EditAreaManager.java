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
import com.project.utility.MainUtility;
import com.project.utility.SettingsUtility;
import com.sun.jdi.Location;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.fxmisc.richtext.event.MouseOverTextEvent;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles Edit area operations.
 */
public class EditAreaManager {

    /**
     * A list of the keywords.
     */
    public static final ArrayList<String> KEY_WORDS = new ArrayList<>(List.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "null",
            "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while", "@interface"
    ));

    /**
     * Regex to match keywords.
     */
    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEY_WORDS) + ")\\b";

    /**
     * Regex to match single line comments.
     */
    private static final String COMMENT_PATTERN = "//[^\n]*";

    /**
     * Regex to match block comments.
     */
    private static final String BLOCK_COMMENT_PATTERN = "/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/";

    /**
     * Regex to match Strings.
     */
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";

    /**
     * Regex to match characters.
     */
    private static final String CHAR_PATTERN = "'([^'\\\\]|\\\\.)*'";

    /**
     * Regex to match numbers and decimals.
     */
    private static final String NUMBER_PATTERN = "\\b\\d+(\\.\\d+)?\\b";

    /**
     * Pattern to match differently colored words.
     */
    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<BLOCKCOMMENT>" + BLOCK_COMMENT_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<CHAR>" + CHAR_PATTERN + ")"
                    + "|(?<NUMBER>" + NUMBER_PATTERN + ")"
    );

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

    private static final Map<Diagnostic, Path> diagnostics = new HashMap<>();

    private static EventHandler<ScrollEvent> scrollEV;

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

                // Get a copy of the keys to be able to delete diagnostics for the file being edited.
                Set<Diagnostic> copy = new HashSet<>(diagnostics.keySet());
                for (Diagnostic diagnostic : copy) {

                    // Check if the diagnostic belongs to the current file.
                    if (diagnostics.get(diagnostic).equals(OpenFilesTracker.getOpenFile(tab).getFile().toPath())) {
                        diagnostics.remove(diagnostic);
                    }
                }

                // Remove the Error Canvas to allow a new one to be generated.
                if (((StackPane) textArea.getParent()).getChildren().size() > 1 && ((CustomCanvas) ((StackPane) textArea.getParent()).getChildren().get(1)).getType().equals("Error")) {
                    ((StackPane) textArea.getParent()).getChildren().remove(1);
                }

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
     */
    public static void color(CustomTextArea textArea) {

        // Run on different thread since the operation might block the main thread.
        new Thread(() -> {

            String text = textArea.getText();
            Matcher matcher = PATTERN.matcher(text);

            // Used to tell whether there are character in between words being colored.
            int lastKeyWordEnd = 0;

            StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
            while (matcher.find()) {
                String styleClass =
                        matcher.group("KEYWORD") != null ? "keyword" :
                        matcher.group("BLOCKCOMMENT") != null ? "block-comment" :
                        matcher.group("COMMENT") != null ? "comment" :
                        matcher.group("STRING") != null ? "string" :
                        matcher.group("CHAR") != null ? "char" :
                        matcher.group("NUMBER") != null ? "number" :
                        "default";

                // If there were characters in between words being colored.
                // Set them to the default color.
                if (matcher.start() > lastKeyWordEnd) {
                    spansBuilder.add(Collections.singleton("default"), matcher.start() - lastKeyWordEnd);
                }

                // Color the word.
                spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
                lastKeyWordEnd = matcher.end();
            }

            // Color the remaining words with the default color.
            spansBuilder.add(Collections.singleton("default"), text.length() - lastKeyWordEnd);

            // Run UI updates on JavaFX Thread.
            Platform.runLater(() -> textArea.setStyleSpans(0, spansBuilder.create()));

        }).start();

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
     * Adds diagnostics from the server into the diagnostics map.
     *
     * @param diagnostic The Diagnostic.
     * @param path The Path to the file it belongs to.
     */
    public static void addDiagnostic(Diagnostic diagnostic, Path path) {

        diagnostics.put(diagnostic, path);
    }

    /**
     * Processes the diagnostics of the file open in the selected tab.
     */
    public static void processDiagnostics() {

        for (Diagnostic diagnostic : diagnostics.keySet()) {
            Tab tab = null;

            // A diagnostic referencing files used by the server will cause NullPointerException.
            // This typically leaves tab null.
            try {
                tab = OpenFilesTracker.getOpenFile(diagnostics.get(diagnostic)).getTab();

                // Check whether the tab for the file is the selected tab.
                // If not continue to next diagnostic.
                if (!tab.getTabPane().getSelectionModel().getSelectedItem().equals(tab)) {
                    continue;
                }
            } catch (NullPointerException ignored) {
            }

            if (tab == null) {
                continue;
            }

            int startLine = diagnostic.getRange().getStart().getLine();
            int endLine = diagnostic.getRange().getEnd().getLine();
            int startChar = diagnostic.getRange().getStart().getCharacter();
            int endChar = diagnostic.getRange().getEnd().getCharacter();
            CustomTextArea textArea = (CustomTextArea) ((StackPane) tab.getContent()).getChildren().get(0);

            Tooltip tooltip = new Tooltip(diagnostic.getMessage());

            // Add event handler for showing the Tooltip
            int finalStart = textArea.getAbsolutePosition(startLine, startChar);
            int finalEnd = textArea.getAbsolutePosition(endLine, endChar);
            final CustomTextArea[] textAreaArray = new CustomTextArea[] {textArea};
            mouseEvents.add(event -> {
                int index = textAreaArray[0].hit(event.getX(), event.getY()).getCharacterIndex().orElse(-1);
                if (index >= finalStart && index < finalEnd) {
                    if (!tooltip.isShowing()) {
                        tooltip.show(textAreaArray[0], event.getScreenX(), event.getScreenY() + 10);
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

            // Check whether server is pointing to an unused variable.
            if (diagnostic.getMessage().startsWith("The value of") && diagnostic.getMessage().endsWith("is not used")) {

                // Simply color it and move to next diagnostic.
                textArea.setStyleClass(textArea.position(startLine, startChar).toOffset(),
                        textArea.position(endLine, endChar).toOffset(), "unused-var");
                continue;
            }

            StackPane stackPane = (StackPane) textArea.getParent();
            CustomCanvas canvas = null;

            // Check whether there is an existing Canvas to show errors and use it.
            if (stackPane.getChildren().size() > 1 && ((CustomCanvas) stackPane.getChildren().get(1)).getType().equals("Error")) {
                canvas = (CustomCanvas) stackPane.getChildren().get(1);
            }

            // Otherwise create one.
            if (canvas == null) {
                canvas = new CustomCanvas(textArea.getScene().getWidth(), textArea.getScene().getHeight(), "Error");
                stackPane.getChildren().add(1, canvas);
                canvas.setMouseTransparent(true);
            }

            double startX;
            double startY;
            double endX;
            double endY;

            int originalCaretPosition = textArea.getCaretPosition();

            // Use caret to get the coordinates of whether the error is.
            // When the line is not in the viewport, getCaretBounds doesn't return.
            // This will generate a NoSuchElementException or potentially a NullPointerException.
            // Since we can't see the line, we move to the next diagnostic.
            try {
                textArea.moveTo(textArea.getAbsolutePosition(startLine, startChar));
                startX = textArea.screenToLocal(textArea.getCaretBounds().get()).getMaxX();
                startY = textArea.screenToLocal(textArea.getCaretBounds().get()).getMaxY();

                textArea.moveTo(textArea.getAbsolutePosition(endLine, endChar));
                endX = textArea.screenToLocal(textArea.getCaretBounds().get()).getMaxX();
                endY = textArea.screenToLocal(textArea.getCaretBounds().get()).getMaxY();
            } catch (NullPointerException | NoSuchElementException ignored) {
                continue;
            }
            textArea.moveTo(originalCaretPosition);

            // Set up GraphicsContent in order to draw.
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setStroke(Color.RED);
            gc.setLineWidth(1);

            // Clear path is any and go to start point.
            gc.beginPath();
            gc.moveTo(startX, startY);

            // Loop through the width while zigzagging.
            boolean down = true;
            for (double x = startX; x < endX; x+=2) {
                if (down) {
                    gc.lineTo(x, startY + 0);
                    down = false;
                } else {
                    gc.lineTo(x, startY - 2);
                    down = true;
                }
            }

            // Move to the end point.
            gc.lineTo(endX, endY);

            // Color the path with predefined color.
            gc.stroke();

        }

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
            textArea[0] = (CustomTextArea) ((StackPane) OpenFilesTracker.getOpenFile(finalPath).getTab().getContent()).getChildren().get(0);

            // Get the absolute position in the TextArea of the line.
            start[0].set(textArea[0].getAbsolutePosition(lineNumber - 1, 0));
            end[0].set(textArea[0].getAbsolutePosition(lineNumber, 0));

            // Scroll to the line.
            textArea[0].showParagraphInViewport(location.lineNumber() - 1);

            // Set up a canvas to highlight the current line.
            MainUtility.setDebugCanvas(textArea[0], lineNumber);

            // Set up a listener for scrolling to adjust the position of the highlight accordingly.
            scrollEV = event -> MainUtility.setDebugCanvas(textArea[0], lineNumber);
            textArea[0].addEventFilter(ScrollEvent.SCROLL, scrollEV);

        });
        return true;

    }

    /**
     * Removes all Debug Canvas from all open tabs, if any.
     */
    public static void clearDebugCanvases() {

        for (OpenFile file : OpenFilesTracker.getOpenFiles()) {
            StackPane stackPane = (StackPane) file.getTab().getContent();

            // Check whether there is a Debug canvas.
            if (stackPane.getChildren().size() > 1 && ((CustomCanvas) stackPane.getChildren().get(1)).getType().equals("Debug")) {
                stackPane.getChildren().remove(1);
            } else if (stackPane.getChildren().size() > 2) {
                stackPane.getChildren().remove(2);
            }

            stackPane.getChildren().get(0).removeEventFilter(ScrollEvent.SCROLL, scrollEV);
        }

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
