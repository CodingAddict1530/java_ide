package com.project.utility;

import com.project.managers.ProjectManager;
import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProjectWatcher {

    private static final Logger logger = LogManager.getLogger(ProjectWatcher.class);

    private static final WatchService watchService;
    private static final AtomicBoolean keepWatching = new AtomicBoolean(true);
    private static boolean isWatching = false;

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

    private static final Map<Path, WatchKey> watchKeyMap = new HashMap<>();

    public static void registerPath(Path path) {

        if (watchKeyMap.containsKey(path)) {
            return;
        }
        try {
            Files.walk(path).filter(Files::isDirectory).forEach(dir -> {
                try {
                    WatchKey key = dir.register(
                            watchService,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE
                    );
                    watchKeyMap.put(dir, key);
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

    public static void unregisterPath(Path path) {

        if (!watchKeyMap.containsKey(path)) {
            return;
        }
        WatchKey key = watchKeyMap.remove(path);
        if (key != null) {
            key.cancel();
            logger.info("Unregistered path: {}", path);
            System.out.println("Unregistered path: " + path);
        }

    }

    public static void unregisterAllPaths() {

        for (WatchKey key : watchKeyMap.values()) {
            key.cancel();
        }
        watchKeyMap.clear();
        logger.info("Unregistered all paths");

    }

    public static void startWatching() {

        if (isWatching) {
            return;
        }
        logger.info("Watching started...");
        isWatching = true;
        new Thread(() -> {
            WatchKey key;
            try {
                while (keepWatching.get()) {
                    key = watchService.take();
                    Path dir = null;
                    for (Map.Entry<Path, WatchKey> entry : watchKeyMap.entrySet()) {
                        if (entry.getValue().equals(key)) {
                            dir = entry.getKey();
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
                                Platform.runLater(() -> ProjectManager.openProject(ProjectManager.getCurrentProject().getPath()));
                            }
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            logger.info("File deleted: {}", filename);
                            if (ProjectManager.getCurrentProject() != null) {
                                Platform.runLater(() -> ProjectManager.openProject(ProjectManager.getCurrentProject().getPath()));
                            }
                        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            logger.info("File modified: {}", filename);
                        }
                    }

                    boolean valid = key.reset();
                    if (!valid) {
                        logger.info("WatchKey no longer valid for path: {}", dir);
                        watchKeyMap.remove(dir);
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

    public static boolean getIsWatching() {

        return isWatching;
    }

    public static Map<Path, WatchKey> getWatchKeyMap() {

        return watchKeyMap;
    }

}
