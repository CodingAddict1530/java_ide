package com.project.utility;

import com.project.custom_classes.SettingsResult;
import com.project.custom_classes.Theme;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;

import javax.tools.ToolProvider;
import java.io.File;
import java.nio.file.Paths;
import java.util.Objects;

public class SettingsUtility {

    private static DirectoryChooser directoryChooser;
    private static TabPane tabPane;

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

    public static String getJavaPath() {

        if (ToolProvider.getSystemJavaCompiler() != null) {
            return System.getProperty("java.home");
        }
        return null;
    }

    public static void setDirectoryChooser(DirectoryChooser directoryChooser) {

        SettingsUtility.directoryChooser = directoryChooser;
    }

    public static void setTabPane(TabPane tabPane) {

        SettingsUtility.tabPane = tabPane;
    }
}
