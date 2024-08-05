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

import com.project.managers.ProjectManager;
import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchService;
import java.nio.file.FileSystems;
import java.nio.file.WatchKey;
import java.nio.file.Files;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Watches the files and directories in a project.
 */
public class ProjectWatcher {

    /**
     * The logger for the class.
     */
    private static final Logger logger = LogManager.getLogger(ProjectWatcher.class);

    /**
     * The WatchService Object.
     */
    private static final WatchService watchService;
    static {
        WatchService temp = null;
        try {
            temp = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            logger.error(e);
            System.out.println("WatchService could not be created");
        } finally {
            watchService = temp;
        }
    }

    /**
     * Whether to keep watching.
     */
    private static final AtomicBoolean keepWatching = new AtomicBoolean(true);

    /**
     * Whether the WatchService is watching.
     */
    private static boolean isWatching = false;

    /**
     * A map containing each WatchKey and the file it corresponds to.
     */
    private static final Map<String, WatchKey> watchKeyMap = new HashMap<>();

    /**
     * Registers a directory to be watched.
     *
     * @param path The Path to the directory.
     */
    public static void registerPath(Path path) {

        // Check if the path is not already registered.
        if (watchKeyMap.containsKey(path.toString())) {
            return;
        }
        try {
            Files.walk(path).filter(Files::isDirectory).forEach(dir -> {
                try {

                    // Register the WatchKey
                    WatchKey key = dir.register(
                            watchService,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE
                    );

                    // Put the key into the map.
                    watchKeyMap.put(dir.toString(), key);
                } catch (Exception e) {
                    logger.error("Failed to register path: {}", dir);
                }
            });
            logger.info("Registered path: {}", path);
        } catch (Exception e) {
            logger.error(e);
            logger.info("Could not register path: {}", path);
        }

    }

    /**
     * Unregisters a directory.
     *
     * @param path The Path to the directory.
     */
    public static void unregisterPath(Path path) {

        // Check if the directory is actually being watched.
        if (!watchKeyMap.containsKey(path.toString())) {
            return;
        }
        WatchKey key = watchKeyMap.remove(path.toString());
        if (key != null) {
            key.cancel();
            logger.info("Unregistered path: {}", path);
            System.out.println("Unregistered path: " + path);
        }

    }

    /**
     * Unregisters all directories.
     */
    public static void unregisterAllPaths() {

        for (WatchKey key : watchKeyMap.values()) {
            key.cancel();
        }
        watchKeyMap.clear();
        logger.info("Unregistered all paths");

    }

    /**
     * Starts watching all registered directories.
     */
    public static void startWatching() {

        // If already watching, no further action is taken.
        if (isWatching) {
            return;
        }
        logger.info("Watching started...");
        isWatching = true;

        // Use a different thread to avoid blocking the main thread.
        new Thread(() -> {
            WatchKey key;
            try {
                while (keepWatching.get()) {
                    key = watchService.take();
                    Path dir = null;
                    for (Map.Entry<String, WatchKey> entry : watchKeyMap.entrySet()) {
                        if (entry.getValue().equals(key)) {
                            dir = Paths.get(entry.getKey());
                            break;
                        }
                    }

                    if (dir == null) {
                        System.err.println("WatchKey not recognized!");
                        continue;
                    }

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path filename = ev.context();

                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            logger.info("File created: {}", filename);
                            if (ProjectManager.getCurrentProject() != null) {

                                // Process wil involve UI updates, so run on JavaFX thread.
                                Platform.runLater(() -> ProjectManager.openProject(ProjectManager.getCurrentProject().getPath()));
                            }
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            logger.info("File deleted: {}", filename);
                            if (ProjectManager.getCurrentProject() != null) {

                                // Process wil involve UI updates, so run on JavaFX thread.
                                Platform.runLater(() -> ProjectManager.openProject(ProjectManager.getCurrentProject().getPath()));
                            }
                        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            logger.info("File modified: {}", filename);
                        }
                    }

                    boolean valid = key.reset();
                    if (!valid) {
                        logger.info("WatchKey no longer valid for path: {}", dir);
                        watchKeyMap.remove(dir.toString());
                        if (watchKeyMap.isEmpty()) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(e);
            }
        }).start();
        keepWatching.set(true);

    }

    /**
     * Stops watching the directories.
     */
    public static void stopWatching() {

        if (!isWatching) {
            return;
        }
        keepWatching.set(false);
        try {
            watchService.close();
        } catch (IOException e) {
            logger.error(e);
        }
        isWatching = false;
        System.out.println("Stopped watching.");

    }

    /**
     * Retrieves whether directories are being watched.
     *
     * @return Whether watching is taking place.
     */
    public static boolean getIsWatching() {

        return isWatching;
    }

    /**
     * Retrieves the watchKeyMap.
     *
     * @return watchKeyMap.
     */
    public static Map<String, WatchKey> getWatchKeyMap() {

        return watchKeyMap;
    }

}
