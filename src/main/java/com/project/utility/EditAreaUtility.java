package com.project.utility;

import com.project.custom_classes.CustomTextArea;
import com.project.custom_classes.OpenFilesTracker;
import com.project.managers.JLSManager;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import com.project.managers.TextManager;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.SignatureHelp;

import java.nio.file.Path;
import java.util.*;
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

    private static final ArrayList<Character> completionTriggers = new ArrayList<>(List.of('.', '(', '"', '\'', ':', '@', '[', '{', ' '));

    private static final ArrayList<Character> sHelpTriggers = new ArrayList<>(List.of('(', ','));

    private static final ArrayList<EventHandler<MouseEvent>> mouseEvents = new ArrayList<>();

    private static final Map<Tab, Integer> currentVersions = new HashMap<>();

    public static void addEventHandler(CustomTextArea textArea, Tab tab) {

        currentVersions.put(tab, 1);
        textArea.getInnerTextArea().textProperty().addListener((observable, oldValue, newValue) -> {

            if (!Objects.equals(oldValue, newValue)) {
                currentVersions.replace(tab, currentVersions.get(tab) + 1);
                JLSManager.didChange(OpenFilesTracker.getOpenFile(tab).getFile().toPath(), newValue, currentVersions.get(tab));
                if (textArea.getCaretPosition() > 0) {
                    List<CompletionItem> items = null;
                    if (sHelpTriggers.contains(textArea.getInnerTextArea().getText().charAt(textArea.getCaretPosition() - 1))) {
                        SignatureHelp s = JLSManager.getSignatureHelp(OpenFilesTracker.getOpenFile(tab).getFile().toPath(), getPosition(textArea));
                        System.out.println(s);
                    } else if (completionTriggers.contains(textArea.getInnerTextArea().getText().charAt(textArea.getCaretPosition() - 1))) {
                        items = JLSManager.complete(OpenFilesTracker.getOpenFile(tab).getFile().toPath(), getPosition(textArea));
                    }
                    if (items != null) {
                        Tooltip complitionTooltip = getCompletionTooltip(items);
                        if (!complitionTooltip.isShowing()) {
                            textArea.getCaretBounds().ifPresent(bounds -> {
                                Point2D pos2D = new Point2D(bounds.getMaxX(), bounds.getMaxY());
                                complitionTooltip.show(textArea, pos2D.getX(), pos2D.getY());
                            });

                        }
                    }

                }

                if (Boolean.TRUE.equals(OpenFilesTracker.isSaved(tab))) {
                    OpenFilesTracker.getOpenFile(tab).setIsSaved(false);
                    HBox header = (HBox) tab.getGraphic();
                    header.getChildren().remove(0);
                    header.getChildren().add(0, new Label("* " +
                            OpenFilesTracker.getOpenFile(tab).getFile().getName() + "     "));
                }
            }
        });

    }

    private static org.eclipse.lsp4j.Position getPosition(CustomTextArea textArea) {

        String text = textArea.getInnerTextArea().getText();
        int lineCount = 0;
        int charCount = 0;
        for (int i = 0; i < text.length(); i++) {
            charCount++;
            if (text.charAt(i) == '\n') {
                lineCount++;
                charCount = 0;
            }
            if (i == textArea.getCaretPosition() - 1) {
                break;
            }
        }

        return new org.eclipse.lsp4j.Position(lineCount, charCount - 1);
    }

    private static Tooltip getCompletionTooltip(List<CompletionItem> items) {

        StringBuilder tooltipText = new StringBuilder();
        for (CompletionItem item : items) {
            tooltipText.append(item.getLabel()).append("\n\n");
        }

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setMaxWidth(300);
        scrollPane.setMaxHeight(300);
        scrollPane.setFitToWidth(true);

        TextArea tooltipContent = new TextArea(tooltipText.toString());
        tooltipContent.setEditable(false);
        tooltipContent.setWrapText(true);
        tooltipContent.setMaxWidth(300);

        scrollPane.setContent(tooltipContent);

        Tooltip tooltip = new Tooltip();
        tooltip.setGraphic(scrollPane);
        tooltip.setAutoHide(true);
        return tooltip;

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
                colorThis(textArea, "black", new int[] { startIndex, endIndex });
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
            textArea.setStyle(i, i + 1, currentStyle + " -fx-fill: red;");
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
        });
        textArea.addEventHandler(MouseEvent.MOUSE_MOVED, mouseEvents.get(mouseEvents.size() - 1));

    }

}
