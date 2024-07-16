package com.project.javaeditor;

import javafx.embed.swing.JFXPanel;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    @Order(1)
    public void testAddAccelerator() {

        MenuItem menuItem = new MenuItem();
        controller.addAccelerator(menuItem, KeyCode.N, KeyCodeCombination.CONTROL_DOWN);
        KeyCombination keyComb = menuItem.getAccelerator();
        assertEquals(new KeyCodeCombination(KeyCode.N, KeyCodeCombination.CONTROL_DOWN), keyComb);

    }

    @RepeatedTest(5)
    @Order(2)
    public void testNewFile() {

        int tabCount = tabPane.getTabs().size();
        controller.newFile(tabPane);
        assertEquals(tabCount + 1, tabPane.getTabs().size());

    }

    @RepeatedTest(6)
    @Order(3)
    public void testCloseFile() {

        int tabCount = tabPane.getTabs().size();
        controller.closeFile(tabPane);
        assertEquals((tabCount == 0) ? 0 : tabCount - 1, tabPane.getTabs().size());

    }

    @ParameterizedTest
    @MethodSource("testGetLineProvider")
    @Order(4)
    public void testGetLine(String string, int index, String expectedString) {

        TextArea textArea = new TextArea();
        textArea.setText(string);
        textArea.positionCaret(index);
        String line = controller.getLine(textArea);
        assertEquals(expectedString, line);

    }

    @ParameterizedTest
    @MethodSource("testApplyIndentProvider")
    @Order(4)
    public void testApplyIndent(String line, String expectedLine, char charLeft, char charRight) {
        line = controller.applyIndent(line, charLeft, charRight);
        assertEquals(expectedLine, line);
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