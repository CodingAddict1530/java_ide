package com.project.utility;

import com.project.custom_classes.CustomTextArea;
import com.project.custom_classes.OpenFilesTracker;
import com.project.custom_classes.TextAreaChange;
import com.project.custom_classes.diff_match_patch;
import com.project.managers.JLSManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import com.project.managers.TextManager;
import javafx.scene.text.Text;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.fxmisc.richtext.event.MouseOverTextEvent;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.TextChange;
import org.fxmisc.undo.UndoManagerFactory;

import java.awt.*;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditAreaUtility {

    public static final ArrayList<String> ORANGE_KEY_WORDS = new ArrayList<>(List.of(
            "abstract",
            "assert",
            "boolean",
            "break",
            "byte",
            "case",
            "catch",
            "char",
            "class",
            "const",
            "continue",
            "default",
            "do",
            "double",
            "else",
            "enum",
            "extends",
            "final",
            "finally",
            "float",
            "for",
            "goto",
            "if",
            "implements",
            "import",
            "instanceof",
            "int",
            "interface",
            "long",
            "native",
            "new",
            "null",
            "package",
            "private",
            "protected",
            "public",
            "return",
            "short",
            "static",
            "strictfp",
            "super",
            "switch",
            "synchronized",
            "this",
            "throw",
            "throws",
            "transient",
            "try",
            "void",
            "volatile",
            "while",
            "@interface"
    ));

    private static Button goToBtn;

    private static final ArrayList<Character> completionTriggers = new ArrayList<>(List.of('.', '(', '"', '\'', ':', '@', '[', '{', ' '));

    private static final ArrayList<Character> sHelpTriggers = new ArrayList<>(List.of('(', ','));

    private static final Tooltip tooltip = new Tooltip();
    static {
        tooltip.setWrapText(true);
        tooltip.setMaxHeight(300);
        tooltip.setMaxWidth(500);
    }

    public static int completionTooltipCurrentFocus = -1;
    public static final Tooltip complitionTooltip = new Tooltip();
    static {
        complitionTooltip.setOnHiding(event -> completionTooltipCurrentFocus = 0);
    }

    private static final ArrayList<EventHandler<MouseEvent>> mouseEvents = new ArrayList<>();
    private static final Map<Tab, Integer> currentVersions = new HashMap<>();

    private static boolean isUndo = false;
    private static boolean isRedo = false;

    public static void addEventHandler(CustomTextArea textArea, Tab tab) {

        currentVersions.put(tab, 1);
        textArea.setUndoManager(null);
        textArea.getInnerTextArea().textProperty().addListener((observable, oldValue, newValue) -> {

            if (!isRedo && !isUndo) {
                while (textArea.popRedo() != null) {}
            }
            if (isRedo) {
                isUndo = false;
            }
            if (!isUndo) {
                diff_match_patch dmp = new diff_match_patch();
                LinkedList<diff_match_patch.Diff> diffs = dmp.diff_main(oldValue, newValue);
                dmp.diff_cleanupSemantic(diffs);
                int index = 0;
                for (diff_match_patch.Diff diff : diffs) {
                    if (diff.operation == diff_match_patch.Operation.EQUAL) {
                        index = diff.text.length();
                    } else {
                        textArea.pushUndo(new TextAreaChange(diff.operation, diff.text, index));
                    }

                }
            } else {
                isUndo = false;
            }

            if (!Objects.equals(oldValue, newValue)) {
                int size = mouseEvents.size();
                for (int i = 0; i < size; i++) {
                    textArea.removeEventHandler(MouseEvent.MOUSE_MOVED, mouseEvents.get(0));
                    mouseEvents.remove(0);
                }
                if (complitionTooltip.isShowing()) {
                    complitionTooltip.hide();
                }
                currentVersions.replace(tab, currentVersions.get(tab) + 1);
                JLSManager.didChange(OpenFilesTracker.getOpenFile(tab).getFile().toPath(), newValue, currentVersions.get(tab));
                new Thread(() -> {
                    if (textArea.getCaretPosition() > 0) {
                    List<CompletionItem> items = null;
                    char currentChar = textArea.getInnerTextArea().getText().charAt(textArea.getInnerTextArea().getCaretPosition() - 1);
                    if (sHelpTriggers.contains(currentChar)) {
                        SignatureHelp s = JLSManager.getSignatureHelp(OpenFilesTracker.getOpenFile(tab).getFile().toPath(), getPosition(textArea, null));
                    } else if (completionTriggers.contains(currentChar) || Character.isAlphabetic(currentChar)) {
                        items = JLSManager.complete(OpenFilesTracker.getOpenFile(tab).getFile().toPath(), getPosition(textArea, null));
                    }
                    if (items != null) {
                        List<CompletionItem> finalItems = items;
                        Platform.runLater(() -> {
                            populateCompletionTooltip(finalItems, textArea);
                            if (((GridPane) ((ScrollPane) EditAreaUtility.complitionTooltip.getGraphic()).getContent()).getChildren().isEmpty()) {
                                return;
                            }
                            if (complitionTooltip.isShowing()) {
                                complitionTooltip.hide();
                            }
                            textArea.getCaretBounds().ifPresent(bounds -> {
                                Point2D pos2D = new Point2D(bounds.getMaxX(), bounds.getMaxY());
                                complitionTooltip.show(textArea, pos2D.getX(), pos2D.getY());
                            });
                        });
                    }

                }
                }).start();

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
        textArea.getInnerTextArea().caretPositionProperty().addListener((observable, oldValue, newValue) -> {
            int line = getPosition(textArea, textArea.getCaretPosition()).getLine();
            int character = getPosition(textArea, textArea.getCaretPosition()).getCharacter();
            goToBtn.setText(String.format("%d:%d", line + 1, character));
        });
        textArea.setMouseOverTextDelay(Duration.ofMillis(300));
        textArea.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_BEGIN, event -> {
            int index = event.getCharacterIndex();
            if (index > 0 && index < textArea.getLength()) {
                new Thread(() -> {
                    Hover hoverResult = JLSManager.getHover(OpenFilesTracker.getOpenFile(tab).getFile().toPath(), getPosition(textArea, index));
                    if (hoverResult != null) {
                        if (hoverResult.getContents() != null) {
                            if (hoverResult.getContents().getRight() != null) {
                                Platform.runLater(() -> {
                                    tooltip.setText(hoverResult.getContents().getRight().getValue());
                                    if (tooltip.getText().isEmpty()) {
                                        return;
                                    }
                                    Tooltip.install(textArea, tooltip);
                                    textArea.getCaretBounds().ifPresent(bounds -> {
                                        Point2D pos2D = new Point2D(bounds.getMaxX(), bounds.getMaxY());
                                        tooltip.show(textArea, pos2D.getX(), pos2D.getY());
                                    });
                                });
                            } else if (hoverResult.getContents().getLeft() != null) {
                                for (Either<String, MarkedString> obj : hoverResult.getContents().getLeft()) {
                                    if (obj.getRight() != null) {
                                        Platform.runLater(() -> {
                                            tooltip.setText(obj.getRight().getValue());
                                            if (tooltip.getText().isEmpty()) {
                                                return;
                                            }
                                            Tooltip.install(textArea, tooltip);
                                            textArea.getCaretBounds().ifPresent(bounds -> {
                                                Point2D pos2D = new Point2D(bounds.getMaxX(), bounds.getMaxY());
                                                tooltip.show(textArea, pos2D.getX(), pos2D.getY());
                                            });
                                        });
                                    } else if (obj.getLeft() != null) {
                                        Platform.runLater(() -> {
                                            tooltip.setText(obj.getLeft());
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
                                }
                            }
                        }
                    }
                }).start();
            }
        });
        textArea.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_END, event -> {
            tooltip.hide();
            Tooltip.uninstall(textArea, tooltip);
        });
        textArea.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (complitionTooltip.isShowing()) {
                complitionTooltip.hide();
            }
        });
    }

    public static void undo(CustomTextArea textArea, boolean undo) {

        TextAreaChange previous;
        previous = (undo) ? textArea.popUndo() : textArea.popRedo();
        if (previous != null) {
            if (previous.operation() == diff_match_patch.Operation.INSERT) {
                if (undo) {
                    isUndo = true;
                    textArea.pushRedo(new TextAreaChange(diff_match_patch.Operation.DELETE, previous.text(), previous.newPosition()));
                } else {
                    isRedo = true;
                }
                textArea.replaceText(
                        previous.newPosition(),
                        previous.newPosition() + previous.text().length(),
                        ""
                );
            } else {
                if (undo) {
                    isUndo = true;
                    textArea.pushRedo(new TextAreaChange(diff_match_patch.Operation.INSERT, previous.text(), previous.newPosition()));
                } else {
                    isRedo = true;
                }
                textArea.replaceText(
                        previous.newPosition(),
                        previous.newPosition(),
                        previous.text()
                );
            }
        }

    }

    private static org.eclipse.lsp4j.Position getPosition(CustomTextArea textArea, Integer index) {

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

    private static void populateCompletionTooltip(List<CompletionItem> items, CustomTextArea textArea) {

        GridPane gridPane = new GridPane();
        gridPane.setStyle("-fx-background: black;-fx-fill: white");
        for (int i = 0; i < items.size(); i++) {
            Label lineLabel = new Label(items.get(i).getLabel());
            lineLabel.setStyle("-fx-padding: 6px;-fx-font-size: 12px");
            lineLabel.setOnMouseClicked(event -> {
                if (event.getClickCount() == 1) {
                    lineLabel.getStyleClass().add("label-focused");
                } else if (event.getClickCount() == 2) {
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

    public static void color(CustomTextArea textArea) {
        String line = textArea.getInnerTextArea().getText();

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
                colorThis(textArea, "white", new int[] { startIndex, endIndex });
            }
        }

    }

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

    private static void colorThis(CustomTextArea textArea, String color, int[] word) {

        textArea.setStyle(word[0], word[1], "-fx-fill: " + color + ";");

    }

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

    public static void addAccelerator(MenuItem menuItem, KeyCode keyCode) {

        KeyCombination newComb = new KeyCodeCombination(keyCode, KeyCombination.CONTROL_DOWN);
        menuItem.setAccelerator(newComb);

    }

    public static void addAccelerator(MenuItem menuItem, KeyCode keyCode, KeyCombination.Modifier... modifier) {

        KeyCombination newComb = new KeyCodeCombination(keyCode, modifier);
        menuItem.setAccelerator(newComb);

    }

    public static void processDiagnostic(Diagnostic diagnostic, Path file) {

        int startLine = diagnostic.getRange().getStart().getLine();
        int endLine = diagnostic.getRange().getEnd().getLine();
        int startChar = diagnostic.getRange().getStart().getCharacter();
        int endChar = diagnostic.getRange().getEnd().getCharacter();

        Tab tab = null;
        try {
            tab = OpenFilesTracker.getOpenFile(file).getTab();
        } catch (NullPointerException ignored) {}

        if (tab == null) {
            return;
        }

        int size = mouseEvents.size();
        CustomTextArea textArea = (CustomTextArea) tab.getContent();
        for (int i = 0; i < size; i++) {
            textArea.removeEventHandler(MouseEvent.MOUSE_MOVED, mouseEvents.get(0));
            mouseEvents.remove(0);
        }
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

    public static void setGoTo(Button goToBtn) {

        EditAreaUtility.goToBtn = goToBtn;
    }
}
