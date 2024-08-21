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

import com.project.managers.JLSManager;
import com.project.utility.DatabaseUtility;
import com.project.utility.MainUtility;
import com.project.utility.ProjectWatcher;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The Main class where execution starts from.
 */
public class Application extends javafx.application.Application {

    /**
     * The logger for the class.
     */
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    /**
     * Stores whether the project has fully initialized or not.
     */
    private static boolean isInitialized = false;

    /**
     * Determines if it should keep checking on the WatchService.
     */
    private static final AtomicBoolean keepChecking =  new AtomicBoolean(false);

    /**
     * The primary stage of the application.
     */
    private static Stage primaryStage;

    /**
     * The controller of the application.
     */
    private static Controller controller;

    /**
     * Launches the application.
     *
     * @param stage the primary stage for this application, onto which
     * the application scene can be set.
     * Applications may create other stages, if needed, but they will not be
     * primary stages.
     */
    @Override
    public void start(Stage stage) {

        Platform.setImplicitExit(true);
        primaryStage = stage;
        logger.info("Application started");
        ApplicationView.setStage(stage);
        MainUtility.setStage(stage);

        // Start the language server.
        ApplicationModel.startServer();

        // Initialize the server.
        JLSManager.initializeServer();

        // Retrieve the controller.
        controller = ApplicationView.setUp(Application.class);
        if (controller == null) {
            throw new RuntimeException("Controller is null");
        }
        stage.show();
        isInitialized = true;

        logger.info("Application setup complete");

        // Watch server logs and recycle them.
        ProjectWatcher.watchServerLogs();

        // Keep checking if WatchService is not idle and stop it.
        Thread thread = new Thread(()-> {
            while (keepChecking.get()) {
                if (ProjectWatcher.getIsWatching().get()) {
                    if (ProjectWatcher.getWatchKeyMap().isEmpty()) {
                        ProjectWatcher.stopWatching();
                    }
                }
                try {

                    // Wait 2 seconds before checking again.
                    Thread.sleep(2000);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        });
        thread.setDaemon(true);
        thread.start();

    }

    /**
     * Code to execute once the application is stopped.
     */
    @Override
    public void stop() {

        logger.info("Application stopped");

        if (isInitialized) {

            // Save relevant data.
            ApplicationModel.save();
        }

        // Stop thread that checks the current file path.
        controller.stopFilePathThread();

        // Close any open database connection.
        DatabaseUtility.closeAll();

        // Stop WatchService.
        ProjectWatcher.stopWatching();

        // Stop Thread that check WatchService.
        keepChecking.set(false);

        // Stop language server.
        ApplicationModel.stopServer();
        ApplicationModel.cleanTrash();
        logger.info("Application cleanup complete");

    }

    /**
     * Creates an animation.
     */
    public static void fadeStage() {

        MainUtility.fadeOutStage(primaryStage);
    }

    /**
     * Method where execution of the application starts.
     *
     * @param args Input stream from command line.
     */
    public static void main(String[] args) {

        launch();
    }

}