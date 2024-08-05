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
import com.project.utility.MainUtility;
import javafx.fxml.FXMLLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Takes care of the application setup.
 */
public class ApplicationModel {

    /**
     * The logger for the class.
     */
    private static final Logger logger = LogManager.getLogger(ApplicationModel.class);

    /**
     * Starts the language server.
     */
    public static void startServer() {

        JLSManager.startServer();
        logger.info("Java Server started");

    }

    /**
     * Stops the language server.
     */
    public static void stopServer() {

        JLSManager.stopServer();
        logger.info("Java Server stopped");

    }

    /**
     * Loads previous content anf checks if the application home directory is OK.
     *
     * @param fxmlLoader The FxmlLoader.
     */
    public static void setUp(FXMLLoader fxmlLoader) {

        ArrayList<Path> previousContent = MainUtility.readOpenData(Paths.get("src/main/files/records.fus"));
        Controller controller = fxmlLoader.getController();
        controller.addPreviousContent(previousContent);
        if (MainUtility.checkAndFix()) {
            logger.info("Fusion IDE home dir is OK");
        } else {
            logger.info("Fusion IDE home dir is NOT OK");
        }

    }

    /**
     * Saved data.
     */
    public static void save() {

        switch (MainUtility.writeOpenData(Paths.get("src/main/files/records.fus"), true)) {
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
