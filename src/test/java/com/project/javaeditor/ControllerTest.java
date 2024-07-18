package com.project.javaeditor;

import javafx.embed.swing.JFXPanel;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import org.fxmisc.richtext.InlineCssTextArea;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ControllerTest {

    private static Controller controller;
    private static TabPane tabPane;

    @BeforeAll
    public static void setUp() {

        new JFXPanel();
        controller = new Controller();
        tabPane = new TabPane();
    }

    @AfterEach
    void tearDown() {
    }

    @ParameterizedTest
    @MethodSource("testAddAcceleratorProvider")
    @Order(1)
    public void testAddAccelerator(MenuItem menuItem, KeyCode keyCode) {

        controller.addAccelerator(menuItem, keyCode);
        KeyCombination keyComb = menuItem.getAccelerator();
        assertEquals(new KeyCodeCombination(keyCode, KeyCombination.CONTROL_DOWN), keyComb);

    }

    @ParameterizedTest
    @MethodSource("testNewFileProvider")
    @Order(2)
    public void testNewFile(String path, String text) {

        int tabCount = tabPane.getTabs().size();
        controller.newFile(tabPane, path, text, true);
        assertEquals(tabCount + 1, tabPane.getTabs().size());
        Tab tab = tabPane.getTabs().get(tabPane.getTabs().size() - 1);
        InlineCssTextArea textArea = (InlineCssTextArea) tab.getContent();
        assertEquals((text == null) ? "" : text, textArea.getText());
        HBox header = (HBox) tab.getGraphic();
        Label label = (Label) header.getChildren().get(0);
        if (path != null) {
            File file = new File(path);
            assertEquals(file.getName() + "     ", label.getText());
        } else {
            assertEquals("* Untitled.java     ",label.getText());
        }

    }

    @RepeatedTest(6)
    @Order(3)
    public void testCloseFile() {

        int tabCount = tabPane.getTabs().size();
        controller.closeFile(tabPane, null);
        assertEquals((tabCount == 0) ? 0 : tabCount - 1, tabPane.getTabs().size());

    }

    @ParameterizedTest
    @MethodSource("testGetLineProvider")
    @Order(4)
    public void testGetLine(String string, int index, String expectedString) {

        InlineCssTextArea textArea = new InlineCssTextArea();
        textArea.replaceText(string);
        textArea.moveTo(index);
        String line = controller.getLine(textArea);
        assertEquals(expectedString, line);

    }

    @ParameterizedTest
    @MethodSource("testApplyIndentProvider")
    @Order(5)
    public void testApplyIndent(String line, String expectedLine, char charLeft, char charRight) {
        line = controller.applyIndent(line, charLeft, charRight);
        assertEquals(expectedLine, line);
    }

    public static Stream<Arguments> testAddAcceleratorProvider() {

        return Stream.of(
                Arguments.of(new MenuItem(), KeyCode.N),
                Arguments.of(new MenuItem(), KeyCode.O),
                Arguments.of(new MenuItem(), KeyCode.S),
                Arguments.of(new MenuItem(), KeyCode.Q),
                Arguments.of(new MenuItem(), KeyCode.X),
                Arguments.of(new MenuItem(), KeyCode.C),
                Arguments.of(new MenuItem(), KeyCode.V)
        );
    }

    public static Stream<Arguments> testNewFileProvider() {

        return Stream.of(
                Arguments.of("\"C:\\Users\\hp\\OneDrive\\Desktop\\hey.java\"", "public static void{\n\t\n}"),
                Arguments.of(null, null)
        );
    }

    public static Stream<Arguments> testGetLineProvider() {

        return Stream.of(
                Arguments.of("Hello, world", 5, "Hello"),
                Arguments.of("Be ware of the dog", 9, "Be ware o"),
                Arguments.of("public static void main(String[] args) {", 15, "public static v"),
                Arguments.of("We love coding!", 1, "W"),
                Arguments.of("Pizza over Hamburger", 14, "Pizza over Ham")
        );
    }

    public static Stream<Arguments> testApplyIndentProvider() {

        return Stream.of(
                Arguments.of("\tlol", "\t", 'l', '\u0000'),
                Arguments.of("\t\t\t\t", "\t\t\t\t", '\t', '\u0000'),
                Arguments.of("him(", "\t", 'l', '\u0000'),
                Arguments.of("\thello(", "\t\t\n\t", '(', ')'),
                Arguments.of("\t\t\tdw{", "\t\t\t\t", '{', '\u0000')
        );
    }

}