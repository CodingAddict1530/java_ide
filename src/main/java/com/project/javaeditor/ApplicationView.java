package com.project.javaeditor;

import com.project.custom_classes.OpenFile;
import com.project.custom_classes.OpenFilesTracker;
import com.project.managers.FileManager;
import com.project.utility.MainUtility;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class ApplicationView {

    private static final Logger logger = LogManager.getLogger(ApplicationView.class);

    private final Stage stage;
    private FXMLLoader fxmlLoader;

    public ApplicationView(Stage stage) {

        this.stage = stage;

    }

    public void setUp(ApplicationModel applicationModel, Class<Application> application) {

        try {
            fxmlLoader = new FXMLLoader(application.getResource("editor.fxml"));
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root, 1000, 600);
            scene.getStylesheets().add(Objects.requireNonNull(application.getResource("style.css")).toExternalForm());
            stage.setTitle("Fusion IDE");
            stage.setScene(scene);
            applicationModel.setUp(fxmlLoader);
        } catch (IOException e) {
            logger.error(e);
        }

    }

    public void checkForUnsavedFiles() {

        ArrayList<OpenFile> unsavedFiles = OpenFilesTracker.getAllUnSaved();
        if (!unsavedFiles.isEmpty()) {
            if (MainUtility.confirm("Unsaved Files", "Application is about to close\nSave unsaved files?")) {
                FileManager.saveFiles(unsavedFiles);
            }
        }

    }

}
