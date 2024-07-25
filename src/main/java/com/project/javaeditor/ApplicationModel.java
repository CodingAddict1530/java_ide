package com.project.javaeditor;

import com.project.managers.JLSManager;
import com.project.utility.MainUtility;
import javafx.fxml.FXMLLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class ApplicationModel {

    private static final Logger logger = LogManager.getLogger(ApplicationModel.class);

    public ApplicationModel() {}

    public void startServer() {

        JLSManager.startServer();
        logger.info("Java Server started");

    }

    public void stopServer() {

        JLSManager.stopServer();
        logger.info("Java Server stopped");

    }

    public void setUp(FXMLLoader fxmlLoader) {

        ArrayList<Path> previousContent = MainUtility.readOpenData(Paths.get("src/main/files/records.dat"));
        Controller controller = fxmlLoader.getController();
        controller.addPreviousContent(previousContent);
        if (MainUtility.checkAndFix()) {
            logger.info("Fusion IDE home dir is OK");
        } else {
            logger.info("Fusion IDE home dir is NOT OK");
        }

    }

    public void save() {

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

}
