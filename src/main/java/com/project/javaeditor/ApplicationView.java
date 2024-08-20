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

package com.project.javaeditor;

import com.project.custom_classes.OpenFile;
import com.project.custom_classes.OpenFilesTracker;
import com.project.managers.FileManager;
import com.project.utility.MainUtility;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Handles UI related application setup.
 */
public class ApplicationView {

    /**
     * The logger for the class.
     */
    private static final Logger logger = LoggerFactory.getLogger(ApplicationView.class);

    /**
     * The primary stage.
     */
    private static Stage stage;

    /**
     * Sets up the application.
     *
     * @param application The Application instance.
     * @return The controller.
     */
    public static Controller setUp(Class<Application> application) {

        // Remove stage default decorations.
        stage.initStyle(StageStyle.UNDECORATED);
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(application.getResource("fxml/editor.fxml"));
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root, 1000, 600);
            scene.getStylesheets().add(Objects.requireNonNull(application.getResource("css/style.css")).toExternalForm());
            stage.setTitle("Fusion IDE");
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.getIcons().add(new Image(Objects.requireNonNull(application.getResource("icons/icon.png")).toExternalForm()));
            ApplicationModel.setUp(fxmlLoader);
            return fxmlLoader.getController();
        } catch (IOException e) {
            logger.error(e.getMessage());
            return null;
        }

    }

    /**
     * Check whether there are unsaved files.
     */
    public static void checkForUnsavedFiles() {

        ArrayList<OpenFile> unsavedFiles = OpenFilesTracker.getAllUnSaved();
        if (!unsavedFiles.isEmpty()) {
            if (MainUtility.confirm("Unsaved Files", "Application is about to close\nSave unsaved files?")) {
                FileManager.saveFiles(unsavedFiles);
            }
        }

    }

    /**
     * Sets the stage the class uses.
     *
     * @param stage The primary stage.
     */
    public static void setStage(Stage stage) {

        ApplicationView.stage = stage;
    }

}
