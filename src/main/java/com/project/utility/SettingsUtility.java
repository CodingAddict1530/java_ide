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

package com.project.utility;

import com.project.custom_classes.SettingsResult;
import com.project.custom_classes.Theme;
import javafx.scene.control.TabPane;
import javafx.scene.control.Dialog;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javax.tools.ToolProvider;
import java.io.File;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Utility dealing with settings dialog.
 */
public class SettingsUtility {

    /**
     * The DirectoryChooser.
     */
    private static DirectoryChooser directoryChooser;

    /**
     * The TabPane.
     */
    private static TabPane tabPane;

    /**
     * Creates a dialog to act as the settings page.
     *
     * @param javaPath The Path to the jdk
     * @return The Dialog.
     */
    public static Dialog<SettingsResult> createSettingsDialog(String javaPath) {

        Dialog<SettingsResult> dialog = new Dialog<>();
        dialog.setTitle("Settings");

        ButtonType apply = new ButtonType("Apply", ButtonType.OK.getButtonData());
        dialog.getDialogPane().getButtonTypes().addAll(apply, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Label folderLabel = new Label("Folder:");
        TextField folderTextField = new TextField();
        folderTextField.setText((javaPath == null) ? "" : javaPath);
        Button folderButton = new Button("Browse");

        // Set an event handler on the button to open the Directory chooser.
        folderButton.setOnAction(event -> {

            directoryChooser.setTitle("Select folder");
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            File selectedDir = directoryChooser.showDialog(tabPane.getScene().getWindow());
            if (selectedDir != null) {
                folderTextField.setText(selectedDir.getAbsolutePath());
            }

        });

        Label themeLabel = new Label("Theme:");
        ComboBox<String> themeComboBox = new ComboBox<>();
        themeComboBox.getItems().addAll("Light", "Dark");
        themeComboBox.getSelectionModel().selectFirst();

        Label fontSizeLabel = new Label("Font Size:");
        Spinner<Integer> fontSizeSpinner = new Spinner<>(10, 40, 14);
        fontSizeSpinner.setEditable(true);

        grid.add(folderLabel, 0, 0);
        grid.add(folderTextField, 1, 0);
        grid.add(folderButton, 2, 0);
        grid.add(themeLabel, 0, 1);
        grid.add(themeComboBox, 1, 1);
        grid.add(fontSizeLabel, 0, 2);
        grid.add(fontSizeSpinner, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == apply) {
                return new SettingsResult(
                        Paths.get(folderTextField.getText()),
                        (Objects.equals(themeComboBox.getValue(), "Light")) ? Theme.LIGHT : Theme.DARK,
                        fontSizeSpinner.getValue()
                );
            }
            return null;
        });

        return dialog;
    }

    /**
     * Get the path to jdk
     *
     * @return The Path.
     */
    public static String getJavaPath() {

        if (ToolProvider.getSystemJavaCompiler() != null) {
            return System.getProperty("java.home");
        }
        return null;
    }

    /**
     * Sets up the DirectoryChooser.
     *
     * @param directoryChooser The directoryChooser.
     */
    public static void setDirectoryChooser(DirectoryChooser directoryChooser) {

        SettingsUtility.directoryChooser = directoryChooser;
    }

    /**
     * Sets yp the TabPane.
     *
     * @param tabPane The TabPane.
     */
    public static void setTabPane(TabPane tabPane) {

        SettingsUtility.tabPane = tabPane;
    }

}
