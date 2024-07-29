package com.project.managers;

import com.project.custom_classes.CustomTextArea;
import javafx.scene.control.TabPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

public class TextManager {

    private static  Clipboard clipboard;
    private static TabPane tabPane;

    public static void cut() {

        copy();
        CustomTextArea textArea = (CustomTextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
        textArea.deleteText(textArea.getSelection().getStart(), textArea.getSelection().getEnd());

    }

    public static void copy() {

        CustomTextArea textArea = (CustomTextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
        ClipboardContent content = new ClipboardContent();
        content.putString(textArea.getSelectedText());
        clipboard.setContent(content);

    }

    public static void paste() {

        if (clipboard.hasString()) {
            CustomTextArea textArea = (CustomTextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
            textArea.deleteText(textArea.getSelection().getStart(), textArea.getSelection().getEnd());
            textArea.insertText(textArea.getCaretPosition(), clipboard.getString());

        }

    }

    public static void setTabPane(TabPane tabPane) {

        TextManager.tabPane = tabPane;
    }

    public static void setClipboard(Clipboard clipboard) {

        TextManager.clipboard = clipboard;
    }
}
