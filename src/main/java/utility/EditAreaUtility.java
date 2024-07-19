package utility;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import managers.TextManager;
import org.fxmisc.richtext.InlineCssTextArea;

import java.nio.file.Path;
import java.util.ArrayList;
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
            "while"
    ));

    private static Integer moveCaret;
    private static ArrayList<Tab> tabs;
    private static ArrayList<Path> filePaths;
    private static ArrayList<Boolean> saved;

    // **
    public static void addEventHandlers(InlineCssTextArea textArea, Tab tab) {

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
                if (textArea.getText().length() > 2 &&
                        textArea.getText().charAt(textArea.getCaretPosition() - 2) == '*' &&
                        textArea.getText().charAt(textArea.getCaretPosition()) == '*') {
                    textArea.replaceText(textArea.getCaretPosition(), textArea.getCaretPosition(), "\n ");
                    textArea.moveTo(textArea.getCaretPosition() - 2);
                    textArea.replaceText(textArea.getCaretPosition(), textArea.getCaretPosition(), " * ");
                }
            }

        });
        textArea.setOnKeyTyped(event -> {

            if (Character.isLetterOrDigit(event.getCharacter().charAt(0)) && saved.get(tabs.indexOf(tab))) {
                saved.set(tabs.indexOf(tab), false);
                HBox header = (HBox) tab.getGraphic();
                header.getChildren().remove(0);
                header.getChildren().add(0, new Label("* " +
                        filePaths.get(tabs.indexOf(tab)).toFile().getName() + "     "));
            }
            color(textArea);

        });

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
