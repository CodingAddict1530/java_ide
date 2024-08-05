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

import com.project.custom_classes.OpenFile;
import com.project.custom_classes.OpenFilesTracker;
import com.project.managers.ProjectManager;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import com.project.managers.FileManager;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

/**
 * Main utility class for the application.
 */
public class MainUtility {

    /**
     * The logger for the class.
     */
    private static final Logger logger = LogManager.getLogger(MainUtility.class);

    /**
     * An ArrayList containing the Path to the open project.
     */
    private static ArrayList<Path> openProjectPath;

    // 1 Failed to write
    // 2 Failed to make readonly
    // 3 Nothing to write

    /**
     * Writes the open content to a file for storage.
     *
     * @param path The Path to the file to write to.
     * @param readOnly Whether to set it read only or not.
     * @return 1 if it can't write to the file, 2 if it can't make the file read only, 3 if there is no data to write.
     */
    public static int writeOpenData(Path path, boolean readOnly) {

        File file = path.toFile();
        if (file.exists() && !file.setWritable(true)) {
            logger.info("File {} not writable", file.getPath());
            return 1;
        }

        // Check if there is an open project.
        if (openProjectPath.isEmpty()) {
            logger.info("No open files found");
            return 3;
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(openProjectPath.get(0).toString()).append("\n");
        if (!OpenFilesTracker.getOpenFiles().isEmpty()) {
            for (OpenFile o : OpenFilesTracker.getOpenFiles()) {
                stringBuilder.append(o.getFile().getPath()).append("\n");
            }
        }

        // Remove redundant '\n'.
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        boolean result = FileManager.writeToFile(path, stringBuilder.toString(), true, false);
        if (result) {
            logger.info("File {} successfully written", file.getPath());
            if (readOnly) {
                if (!file.setReadOnly()) {
                    logger.info("Failed to make {} read-only", file.getPath());
                    return 2;
                }
            }
            return 0;
        } else {
            logger.info("Failed to write to {}", file.getPath());
            return 1;
        }

    }

    /**
     * Reads data store in the storage file.
     *
     * @param path The Path to the file to read.
     * @return An ArrayList of the Paths read from the file.
     */
    public static ArrayList<Path> readOpenData(Path path) {

        // Read the file.
        ArrayList<String> lines = FileManager.readFile(path);
        ArrayList<Path> returnValue = new ArrayList<>();
        boolean valid = true;
        if (lines != null) {
            for (String s : lines) {

                // Check if that file exists.
                if (new File(s).exists()) {
                    returnValue.add(Paths.get(s));
                } else {
                    valid = false;
                }
            }
        } else {
            valid = false;
        }

        return (valid ? returnValue : null);

    }

    /**
     * Checks the project home directory and creates it if it is missing.
     *
     * @return Whether all went well.
     */
    public static boolean checkAndFix() {

        File appHome = ProjectManager.APP_HOME;
        if (!appHome.exists()) {
            if (appHome.mkdir()) {
                logger.info("App home directory created");
                return true;
            } else {
                logger.error("App home directory could not be created");
                return false;
            }
        }

        return true;

    }

    /**
     * Displays a basic Dialog that prompts user for an input.
     *
     * @param title The title of the dialog.
     * @param text The content of the dialog (Prompt text).
     * @return The user input.
     */
    public static String quickDialog(String title, String text) {

        final String[] output = new String[1];
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(text);
        dialog.setGraphic(null);
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(userInput -> output[0] = userInput);
        return output[0];

    }

    /**
     * Displays a confirmation dialog.
     *
     * @param title The title of the dialog.
     * @param text The content of the dialog.
     * @return Whether the user confirmed or not.
     */
    public static boolean confirm(String title, String text) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);

        // Use a danger image.
        ImageView confirmImage =new ImageView(new Image(Objects.requireNonNull(MainUtility.class.getResourceAsStream("icons/warning.png"))));
        sizeImage(confirmImage, 50, 50);
        alert.setGraphic(confirmImage);

        // Remove default styles
        alert.initStyle(StageStyle.UNDECORATED);

        // Add custom style.
        alert.getDialogPane().getStylesheets().add(Objects.requireNonNull(MainUtility.class.getResource("css/alert-style.css")).toExternalForm());
        Optional<ButtonType> result = alert.showAndWait();

        return (result.isPresent() && result.get() == ButtonType.OK);

    }

    /**
     * Create a fade out animation.
     *
     * @param stage The primary stage.
     */
    public static void fadeOutStage(Stage stage) {

        FadeTransition fadeOut = new FadeTransition(Duration.millis(1000), stage.getScene().getRoot());
        fadeOut.setFromValue(stage.getScene().getRoot().getOpacity());
        fadeOut.setToValue(0.0);

        // Create scale-down transition
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(1000), stage.getScene().getRoot());
        scaleDown.setFromX(1.0);
        scaleDown.setFromY(1.0);
        scaleDown.setToX(0.0);
        scaleDown.setToY(0.0);

        // Play both transitions simultaneously
        fadeOut.play();
        scaleDown.play();

        fadeOut.setOnFinished(actionEvent -> fadeInStage(stage));

    }

    /**
     * Create a fade in animation.
     *
     * @param stage The primary stage.
     */
    public static void fadeInStage(Stage stage) {

        FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), stage.getScene().getRoot());
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        // Create scale-up transition
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(1000), stage.getScene().getRoot());
        scaleUp.setFromX(0.0);
        scaleUp.setFromY(0.0);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);

        // Play both transitions simultaneously
        fadeIn.play();
        scaleUp.play();

    }

    /**
     * Sizes an ImageView.
     *
     * @param image The ImageView.
     * @param width The width.
     * @param height The height.
     */
    public static void sizeImage(ImageView image, int width, int height) {

        image.setFitWidth(width);
        image.setFitHeight(height);
        image.setPreserveRatio(true);
        image.setSmooth(true);

    }

    /**
     * Sets up opeProjectPath.
     *
     * @param openProjectPath openProjectPath.
     */
    public static void setOpenProjectPath(ArrayList<Path> openProjectPath) {

        MainUtility.openProjectPath = openProjectPath;
    }

}
