package com.project.javaeditor;

import com.project.managers.JLSManager;
import com.project.utility.MainUtility;
import com.project.utility.ProjectWatcher;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;


public class Application extends javafx.application.Application {

    private static final Logger logger = LogManager.getLogger(Application.class);
    private static boolean isInitialized = false;
    private static AtomicBoolean keepChecking =  new AtomicBoolean(false);
    private static ApplicationModel applicationModel;
    private static ApplicationView applicationView;
    private static Stage primaryStage;
    private static Controller controller;

    @Override
    public void start(Stage stage) {

        primaryStage = stage;
        logger.info("Application started");
        applicationModel = new ApplicationModel();
        applicationView = new ApplicationView(stage);
        applicationModel.startServer();
        JLSManager.initializeServer();
        controller = applicationView.setUp(applicationModel, Application.class);
        if (controller == null) {
            throw new RuntimeException("Controller is null");
        }
        stage.show();
        isInitialized = true;
        stage.setOnCloseRequest(event -> applicationView.checkForUnsavedFiles());

        logger.info("Application setup complete");
        new Thread(()-> {
            while (keepChecking.get()) {
                checkIfIdleWatching();
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                    logger.error(e);
                }
            }
        }).start();
    }

    @Override
    public void stop() {

        logger.info("Application stopped");

        if (isInitialized) {
            applicationModel.save();
        }

        controller.stopFilePathThread();
        ProjectWatcher.stopWatching();
        keepChecking.set(false);
        applicationModel.stopServer();
        logger.info("Application cleanup complete");

    }

    public static void fadeStage() {

        MainUtility.fadeStage(primaryStage);
    }

    public static void checkIfIdleWatching() {

        if (ProjectWatcher.getIsWatching()) {
            if (ProjectWatcher.getWatchKeyMap().isEmpty()) {
                ProjectWatcher.stopWatching();
            }
        }

    }

    public static void main(String[] args) {

        launch();
    }

}