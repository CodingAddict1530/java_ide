package com.project.javaeditor;

import com.project.managers.JLSManager;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Application extends javafx.application.Application {

    private static final Logger logger = LogManager.getLogger(Application.class);
    private static boolean isInitialized = false;
    private static ApplicationModel applicationModel;
    private static ApplicationView applicationView;

    @Override
    public void start(Stage stage) throws Exception {

        logger.info("Application started");
        applicationModel = new ApplicationModel();
        applicationView = new ApplicationView(stage);
        applicationModel.startServer();
        JLSManager.initializeServer();
        applicationView.setUp(applicationModel, Application.class);
        stage.show();
        isInitialized = true;
        stage.setOnCloseRequest(event -> applicationView.checkForUnsavedFiles());

        logger.info("Application setup complete");
    }

    @Override
    public void stop() {

        logger.info("Application stopped");

        if (isInitialized) {
            applicationModel.save();
        }

        applicationModel.stopServer();
        logger.info("Application cleanup complete");

    }

    public static void main(String[] args) {

        launch();
    }

}