package com.project.utility;

import com.project.managers.JLSManager;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import com.project.managers.TextManager;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.model.TwoDimensional;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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

    private static Integer moveCaret = 0;
    private static ArrayList<Tab> tabs;
    private static ArrayList<Path> filePaths;
    private static ArrayList<Boolean> saved;
    private static ArrayList<EventHandler<MouseEvent>> mouseEvents = new ArrayList<>();

    private static ArrayList<Integer> currentVersions = new ArrayList<>();

    public static void addEventHandlers(InlineCssTextArea textArea, Tab tab, boolean isColored) {

        currentVersions.add(1);
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
                    color(textArea);
                    break;
                case "[":
                    event.consume();
                    if (textArea.getSelectedText().isEmpty()) {
                        textArea.replaceText(caretPosition, caretPosition, "[]");
                        textArea.moveTo(caretPosition + 1);
                    } else {
                        textArea.replaceSelection(String.format("[%s]", textArea.getSelectedText()));
                    }
                    color(textArea);
                    break;
                case "\"":
                    event.consume();
                    if (textArea.getSelectedText().isEmpty()) {
                        textArea.replaceText(caretPosition, caretPosition, "\"\"");
                        textArea.moveTo(caretPosition + 1);
                    } else {
                        textArea.replaceSelection(String.format("\"%s\"", textArea.getSelectedText()));
                    }
                    color(textArea);
                    break;
                case "'":
                    event.consume();
                    if (textArea.getSelectedText().isEmpty()) {
                        textArea.replaceText(caretPosition, caretPosition, "''");
                        textArea.moveTo(caretPosition + 1);
                    } else {
                        textArea.replaceSelection(String.format("'%s'", textArea.getSelectedText()));
                    }
                    color(textArea);
                    break;
                case "*":
                    event.consume();
                    if (!textArea.getText().isEmpty() &&
                            textArea.getText().charAt(textArea.getCaretPosition() - 1) == '/') {
                        textArea.replaceText(caretPosition, caretPosition, "**/");
                        textArea.moveTo(caretPosition + 1);
                    } else {
                        textArea.replaceText(caretPosition, caretPosition, "*");
                    }
                    color(textArea);
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
                            (caretLeft == '[' && caretRight == ']') || (caretLeft == '\"' && caretRight == '\"') ||
                            (caretLeft == '\'' && caretRight == '\'')) {
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
                if (textArea.getCaretPosition() >= 2 && textArea.getText().length() > 2 &&
                        textArea.getText().charAt(textArea.getCaretPosition() - 2) == '*' &&
                        textArea.getText().charAt(textArea.getCaretPosition()) == '*') {
                    textArea.replaceText(textArea.getCaretPosition(), textArea.getCaretPosition(), "\n ");
                    textArea.moveTo(textArea.getCaretPosition() - 2);
                    textArea.replaceText(textArea.getCaretPosition(), textArea.getCaretPosition(), " * ");
                }
            }

        });
        if (isColored) {
            textArea.setOnKeyTyped(event -> color(textArea));
        }
        textArea.textProperty().addListener((observable, oldValue, newValue) -> {

            int tabIndex = tabs.indexOf(tab);
            if (!Objects.equals(oldValue, newValue)) {
                currentVersions.set(tabs.indexOf(tab), currentVersions.get(tabIndex) + 1);
                JLSManager.didChange(filePaths.get(tabs.indexOf(tab)), newValue, currentVersions.get(tabs.indexOf(tab)));
                int size = mouseEvents.size();
                for (int i = 0; i < size; i++) {
                    textArea.removeEventHandler(MouseEvent.MOUSE_MOVED, mouseEvents.get(0));
                    mouseEvents.remove(0);
                }
                color(textArea);

                ArrayList<Character> triggers = new ArrayList<>(List.of('.',',','@','#','*',' '));
                if (triggers.contains(textArea.getText().charAt(textArea.getCaretPosition() - 1))) {
                    List<CompletionItem> items = JLSManager.complete(filePaths.get(tabs.indexOf(tab)), getPosition(textArea));
                    Tooltip complitionTooltip = getCompletionTooltip(items);
                    if (!complitionTooltip.isShowing()) {
                        textArea.getCaretBounds().ifPresent(bounds -> {
                            Point2D pos2D = new Point2D(bounds.getMaxX(), bounds.getMaxY());
                            complitionTooltip.show(textArea, pos2D.getX(), pos2D.getY());
                        });

                    }

                }

                if (saved.get(tabIndex)) {
                    saved.set(tabs.indexOf(tab), false);
                    HBox header = (HBox) tab.getGraphic();
                    header.getChildren().remove(0);
                    header.getChildren().add(0, new Label("* " +
                            filePaths.get(tabs.indexOf(tab)).toFile().getName() + "     "));
                }
            }
        });

    }

    private static Position getPosition(InlineCssTextArea textArea) {

        String text = textArea.getText();
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

        return new Position(lineCount, charCount);
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

    public static String textHandler(InlineCssTextArea textArea) {

        String line = getLine(textArea);
        int caretPosition = textArea.getCaretPosition();
        String text = textArea.getText();
        char caretLeft = caretPosition > 1 ? text.charAt(caretPosition - 2) : '\u0000';
        char caretRight = caretPosition < text.length() ? text.charAt(caretPosition) : '\u0000';
        return applyIndent(line, caretLeft, caretRight);

    }

    public static String getLine(InlineCssTextArea textArea) {

        int caretPosition = textArea.getCaretPosition();
        int start = (caretPosition == 0) ? 0 : caretPosition - 1;
        while (start > 0 && textArea.getText().charAt(start - 1) != '\n') {
            start--;
        }
        return textArea.getText(start, caretPosition);

    }

    public static String applyIndent(String line, char caretLeft, char caretRight) {

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

    public static void color(InlineCssTextArea textArea) {
        String line = textArea.getText();

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

    private static String matchAndColor(String line, InlineCssTextArea textArea, String regex,
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

    private static void colorThis(InlineCssTextArea textArea, String color, int[] word) {

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

        Tab tab;
        if (filePaths.contains(file)) {
            tab = tabs.get(filePaths.indexOf(file));
        } else {
            return;
        }

        int start = 0;
        int end = 0;

        InlineCssTextArea textArea = (InlineCssTextArea) tab.getContent();
        String text = textArea.getText();

        int currentLine = 0;
        for (int i = 0; i < text.length(); i++) {

            if (text.charAt(i) == '\n') {
                currentLine++;
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
            }

        }

        for (int i = start + 1; i <= end; i++) {
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

    public static void setTabs(ArrayList<Tab> tabs) {

        EditAreaUtility.tabs = tabs;
    }

    public static void setFilePaths(ArrayList<Path> filePaths) {

        EditAreaUtility.filePaths = filePaths;
    }

    public static void setSaved(ArrayList<Boolean> saved) {

        EditAreaUtility.saved = saved;
    }
}
