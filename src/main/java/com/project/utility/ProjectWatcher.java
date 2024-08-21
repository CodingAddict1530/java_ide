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

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.project.managers.DirectoryManager;
import com.project.managers.ProjectManager;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.FileSystems;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.NoSuchFileException;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Watches the files and directories in a project.
 */
public class ProjectWatcher {

    /**
     * The logger for the class.
     */
    private static final Logger logger = LoggerFactory.getLogger(ProjectWatcher.class);

    /**
     * The WatchService Object.
     */
    private static final WatchService watchService;
    static {
        WatchService temp = null;
        try {
            temp = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            logger.error(e.getMessage());
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
    private static final AtomicBoolean isWatching = new AtomicBoolean(false);

    /**
     * A map containing each WatchKey and the file it corresponds to.
     */
    private static final Map<String, WatchKey> watchKeyMap = new ConcurrentHashMap<>();

    /**
     * The Path to the server logs.
     */
    private static final Path serverLog = Paths.get("jdt_data/.metadata/.log");

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

        try (Stream<Path> walk = Files.walk(path)) {
            walk.filter(Files::isDirectory).forEach(dir -> {
                try {

                    // Register the WatchKey
                    WatchKey key = dir.register(
                            watchService,
                            //StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE
                    );

                    // Put the key into the map.
                    watchKeyMap.put(dir.toString(), key);
                } catch (Exception e) {
                    logger.error("Failed to register path: {}", dir, e);
                }
                logger.info("Registered path: {}", dir);
            });
        } catch (Exception e) {
            logger.error(e.getMessage());
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
        if (isWatching.get()) {
            return;
        }
        logger.info("Watching started...");
        isWatching.set(true);

        // Use a different thread to avoid blocking the main thread.
        Thread projectWatchThread = new Thread(() -> {
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
                            if (filename.toFile().isDirectory()) {
                                registerPath(filename);
                            }
                            if (ProjectManager.getCurrentRootDirectory() != null) {

                                // Process wil involve UI updates, so run on JavaFX thread.
                                Platform.runLater(() -> DirectoryManager.openProject(ProjectManager.getCurrentRootDirectory().getPath()));
                            }
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            logger.info("File deleted: {}", filename);
                            if (ProjectManager.getCurrentRootDirectory() != null) {

                                // Process wil involve UI updates, so run on JavaFX thread.
                                Platform.runLater(() -> DirectoryManager.openProject(ProjectManager.getCurrentRootDirectory().getPath()));
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
                logger.error(e.getMessage());
            }
        });
        projectWatchThread.setDaemon(true);
        projectWatchThread.start();
        keepWatching.set(true);

    }

    /**
     * Watches over the server logs and avoids overflow.
     */
    public static void watchServerLogs() {

        Thread serverLogsWatcher = new Thread(() -> {

            // Run while the watch service is watching.
            while (isWatching.get()) {
                try {
                    Thread.sleep(2000);
                    List<String> lines;
                    try {
                        lines = Files.readAllLines(serverLog);
                    } catch (NoSuchFileException ignored) {
                        continue;
                    }
                    int lineCount = lines.size();
                    if (lineCount > 6000) {
                        int i = 1;
                        while (true) {
                            if (!Paths.get("jdt_data/.metadata/" + i++ + ".log").toFile().exists()) {
                                break;
                            }
                        }
                        Files.write(Paths.get("jdt_data/.metadata/" + i + ".log"), lines);
                        Files.delete(serverLog);
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        });
        serverLogsWatcher.setDaemon(true);
        serverLogsWatcher.start();

    }

    /**
     * Periodically checks for new declared classes and inner classes and adds them to the database.
     *
     * @param startDir The directory where all source files are.
     */
    public static void watchClassPath(Path startDir) {

        Thread classPathWatcher = new Thread(() -> {

            // Create a database connection.
            Connection conn = DatabaseUtility.connect();

            int tries = 1;

            // A maximum of 5 tries to connect.
            while (tries <= 5) {
                if (conn != null) {
                    break;
                }
                tries++;
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
                conn = DatabaseUtility.connect();
            }
            if (conn == null) {
                logger.error("FAILED TO CONNECT TO DATABASE!");
                return;
            }

            // Run while watchServer is watching.
            while (isWatching.get()) {
                try {
                    Connection finalConn = conn;
                    Connection finalConn1 = conn;
                    Files.walkFileTree(startDir, new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            if (file.toString().endsWith(".java")) {
                                JavaParser parser = new JavaParser();
                                CompilationUnit cu = parser.parse(file).getResult().orElse(null);
                                if (cu != null) {
                                    String packageName = cu.getPackageDeclaration().map(NodeWithName::getNameAsString).orElse("");
                                    for (TypeDeclaration<?> type : cu.getTypes()) {
                                        String className = type.getNameAsString();

                                        // Check whether the file doesn't already exist.
                                        ResultSet rs = DatabaseUtility.executeQuery(finalConn, "SELECT id FROM ClassMetaData " +
                                                "WHERE path = ?", file.toAbsolutePath().toString());
                                        try {
                                            // If not add it.
                                            if (!rs.next()) {
                                                rs.getStatement().close();
                                                DatabaseUtility.executeUpdate(finalConn,
                                                        "INSERT INTO ClassMetaData(packageName, className, qualifiedName, path)" +
                                                                "VALUES (?, ?, ?, ?)",
                                                        packageName, className, (packageName.isEmpty()) ? className : packageName + "." + className, file.toAbsolutePath().toString()
                                                );
                                            }

                                            // Do the same for all inner classes.
                                            type.getMembers().forEach(member -> {
                                                if (member instanceof TypeDeclaration<?> innerClass) {

                                                    // Check whether that class doesn't exist in the database.
                                                    ResultSet rs2 = DatabaseUtility.executeQuery(finalConn1, "SELECT id FROM ClassMetaData " +
                                                            "WHERE className = ? AND path = ?", innerClass.getNameAsString(), file.toAbsolutePath().toString());
                                                    try {
                                                        if (!rs2.next()) {
                                                            rs2.getStatement().close();
                                                            DatabaseUtility.executeUpdate(
                                                                    finalConn1,
                                                                    "INSERT INTO ClassMetaData(packageName, className, qualifiedName, path)" +
                                                                            "VALUES (?, ?, ?, ?)",
                                                                    packageName, innerClass.getNameAsString(), (packageName.isEmpty()) ? className + "$" + innerClass.getNameAsString() : packageName + "." + className + "$" + innerClass.getNameAsString(), file.toAbsolutePath().toString()
                                                            );
                                                        }
                                                    } catch (SQLException e) {
                                                        logger.error(e.getMessage());
                                                    } finally {
                                                        try {
                                                            rs2.getStatement().close();
                                                        } catch (SQLException e) {
                                                            logger.error(e.getMessage());
                                                        }
                                                    }
                                                    MainUtility.addInnerClassMetaData(packageName, innerClass, className, file, finalConn);
                                                }
                                            });
                                        } catch (SQLException e) {
                                            logger.error(e.getMessage());
                                        } finally {
                                            try {
                                                rs.getStatement().close();
                                            } catch (SQLException e) {
                                                logger.error(e.getMessage());
                                            }
                                        }
                                    }
                                }
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                    Thread.sleep(3000);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }

            }

            // Close connection.
            DatabaseUtility.close(conn);
        });
        classPathWatcher.setDaemon(true);
        classPathWatcher.start();

    }

    /**
     * Stops watching the directories.
     */
    public static void stopWatching() {

        if (!isWatching.get()) {
            return;
        }
        keepWatching.set(false);
        try {
            watchService.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        isWatching.set(false);
        logger.info("Stopped watching.");

    }

    /**
     * Retrieves whether directories are being watched.
     *
     * @return Whether watching is taking place.
     */
    public static AtomicBoolean getIsWatching() {

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
