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

import com.project.custom_classes.CustomTextArea;
import javafx.scene.control.TabPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;

/**
 * Handles text related operations (Cut, Copy and Paste).
 */
public class TextManager {

    /**
     * The system clipboard.
     */
    private static  Clipboard clipboard;

    /**
     * The TabPane
     */
    private static TabPane tabPane;

    /**
     * Cuts the selected text.
     */
    public static void cut() {

        copy();
        CustomTextArea textArea = (CustomTextArea) ((StackPane) tabPane.getSelectionModel().getSelectedItem().getContent()).getChildren().get(0);
        textArea.deleteText(textArea.getSelection().getStart(), textArea.getSelection().getEnd());

    }

    /**
     * Copies the selected text.
     */
    public static void copy() {

        CustomTextArea textArea = (CustomTextArea) ((StackPane) tabPane.getSelectionModel().getSelectedItem().getContent()).getChildren().get(0);
        ClipboardContent content = new ClipboardContent();
        content.putString(textArea.getSelectedText());
        clipboard.setContent(content);

    }

    /**
     * Pastes the contents of the clipboard.
     */
    public static void paste() {

        if (clipboard.hasString()) {
            CustomTextArea textArea = (CustomTextArea) ((StackPane) tabPane.getSelectionModel().getSelectedItem().getContent()).getChildren().get(0);

            // Replace any selected text.
            textArea.deleteText(textArea.getSelection().getStart(), textArea.getSelection().getEnd());
            textArea.insertText(textArea.getCaretPosition(), clipboard.getString());

        }

    }

    /**
     * Sets up the TabPane.
     *
     * @param tabPane The TabPane.
     */
    public static void setTabPane(TabPane tabPane) {

        TextManager.tabPane = tabPane;
    }

    /**
     * Sets up the clipboard.
     *
     * @param clipboard The clipboard.
     */
    public static void setClipboard(Clipboard clipboard) {

        TextManager.clipboard = clipboard;
    }

}
