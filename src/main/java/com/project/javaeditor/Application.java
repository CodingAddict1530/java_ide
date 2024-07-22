package com.project.javaeditor;

import com.project.managers.FileManager;
import com.project.utility.JLSManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.project.utility.MainUtility;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;


public class Application extends javafx.application.Application {

    private static final Logger logger = LogManager.getLogger(Application.class);
    private static boolean isInitialized = false;

    @Override
    public void start(Stage stage) {

        logger.info("Application started");

        try {

            JLSManager.startServer();
            logger.info("Java Server started");
            FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("editor.fxml"));
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root, 1000, 600);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("style.css")).toExternalForm());
            stage.setTitle("Fusion IDE");
            stage.setScene(scene);
            ArrayList<Path> previousContent = MainUtility.readOpenData(Paths.get("src/main/files/records.dat"));
            Controller controller = fxmlLoader.getController();
            controller.addPreviousContent(previousContent);
            if (MainUtility.checkAndFix()) {
                logger.info("Fusion IDE home dir is OK");
            } else {
                logger.info("Fusion IDE home dir is NOT OK");
            }
            stage.show();
            isInitialized = true;
            stage.setOnCloseRequest(event -> checkForUnsavedFiles(controller));
        } catch (IOException | NullPointerException e) {
            logger.error(e);
            Platform.exit();
        } catch (Exception e) {
            logger.fatal(e);
        }


        logger.info("Application setup complete");
    }

    @Override
    public void stop() {

        logger.info("Application stopped");

        if (isInitialized) {
            switch (MainUtility.writeOpenData(Paths.get("src/main/files/records.dat"), true)) {
                case 0:
                    logger.info("Successfully wrote to records.dat");
                    break;
                case 1:
                    logger.info("Failed to write to records.dat");
                    break;
                case 2:
                    logger.info("Failed to make records.dat read only");
                    break;
            }
        }

        JLSManager.stopServer();
        logger.info("Java Server stopped");
        logger.info("Application cleanup complete");

    }

    public static void main(String[] args) {

        launch();
    }

    public static void checkForUnsavedFiles(Controller controller) {

        ArrayList<Integer> unsavedIndexes = new ArrayList<>();
        for (int i = 0; i < controller.getSaved().size(); i++) {
            if (!controller.getSaved().get(i)) {
                unsavedIndexes.add(i);
            }
        }
        if (!unsavedIndexes.isEmpty()) {
            if (MainUtility.confirm("Unsaved Files", "Application is about to close\nSave unsaved files?")) {
                FileManager.saveFiles(unsavedIndexes);
            }
        }

    }
}