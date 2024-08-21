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
import com.project.custom_classes.OpenFile;
import com.project.custom_classes.OpenFilesTracker;
import com.project.custom_classes.CustomCanvas;
import com.project.custom_classes.CustomTextArea;
import com.project.managers.DirectoryManager;
import com.project.managers.ProjectManager;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import com.project.managers.FileManager;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Popup;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Main utility class for the application.
 */
public class MainUtility {

    /**
     * The logger for the class.
     */
    private static final Logger logger = LoggerFactory.getLogger(MainUtility.class);

    private static final Path TEMP = Paths.get("files/temp.fus").toAbsolutePath();

    /**
     * An ArrayList containing the Path to the open project.
     */
    private static ArrayList<Path> openProjectPath;

    /**
     * A Popup to inform the user of something.
     */
    private static final Popup popup = new Popup();

    /**
     * The primary stage of the application.
     */
    private static Stage stage;

    // 1 Failed to write
    // 2 Failed to make readonly
    // 3 Nothing to write
    /**
     * Writes the open content to a file for storage.
     *
     * @param path The Path to the file to write to.
     * @param readOnly Whether to set it read only or not.
     * @return 1 if it can't write to the file, 2 if it can't make the file read only, 3 if there is no data to write.
     */
    public static int writeOpenData(Path path, boolean readOnly) {

        File file = path.toFile();
        if (file.exists() && !file.setWritable(true)) {
            logger.info("File {} not writable", file.getPath());
            return 1;
        }

        // Check if there is an open project.
        if (openProjectPath.isEmpty()) {
            logger.info("No open files found");
            return 3;
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(openProjectPath.get(0).toString()).append("\n");
        if (!OpenFilesTracker.getOpenFiles().isEmpty()) {
            for (OpenFile o : OpenFilesTracker.getOpenFiles()) {
                stringBuilder.append(o.getFile().getPath()).append("\n");
            }
        }

        // Remove redundant '\n'.
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        boolean result = FileManager.writeToFile(path, stringBuilder.toString(), true, false);
        if (result) {
            logger.info("File {} successfully written", file.getPath());
            if (readOnly) {
                if (!file.setReadOnly()) {
                    logger.info("Failed to make {} read-only", file.getPath());
                    return 2;
                }
            }
            return 0;
        } else {
            logger.info("Failed to write to {}", file.getPath());
            return 1;
        }

    }

    /**
     * Reads data store in the storage file.
     *
     * @param path The Path to the file to read.
     * @return An ArrayList of the Paths read from the file.
     */
    public static ArrayList<Path> readOpenData(Path path) {

        // Read the file.
        ArrayList<String> lines = FileManager.readFile(path);
        ArrayList<Path> returnValue = new ArrayList<>();
        boolean valid = true;
        if (lines != null) {
            for (String s : lines) {

                // Check if that file exists.
                if (new File(s).exists()) {
                    returnValue.add(Paths.get(s));
                } else {
                    valid = false;
                }
            }
        } else {
            valid = false;
        }

        return (valid ? returnValue : null);

    }

    /**
     * Checks the project home directory and creates it if it is missing.
     */
    public static void checkAndFix() {

        File appHome = ProjectManager.APP_HOME;
        if (!appHome.exists()) {
            if (appHome.mkdir()) {
                logger.info("App home directory created");
            } else {
                logger.error("App home directory could not be created");
            }
        }

    }

    /**
     * Displays a basic Dialog that prompts user for an input.
     *
     * @param title The title of the dialog.
     * @param text The content of the dialog (Prompt text).
     * @return The user input.
     */
    public static String quickDialog(String title, String text) {

        final String[] output = new String[1];
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(text);
        dialog.setGraphic(null);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.getDialogPane().getStylesheets().add(Objects.requireNonNull(MainUtility.class.getResource("css/alert-style.css")).toExternalForm());
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(userInput -> output[0] = userInput);
        return output[0];

    }

    /**
     * Displays a confirmation dialog.
     *
     * @param title The title of the dialog.
     * @param text The content of the dialog.
     * @return Whether the user confirmed or not.
     */
    public static boolean confirm(String title, String text) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.initModality(Modality.APPLICATION_MODAL);

        // Use a danger image.
        ImageView confirmImage =new ImageView(new Image(Objects.requireNonNull(MainUtility.class.getResourceAsStream("icons/warning.png"))));
        sizeImage(confirmImage, 50, 50);
        alert.setGraphic(confirmImage);

        // Remove default styles
        alert.initStyle(StageStyle.UNDECORATED);

        // Add custom style.
        alert.getDialogPane().getStylesheets().add(Objects.requireNonNull(MainUtility.class.getResource("css/alert-style.css")).toExternalForm());
        Optional<ButtonType> result = alert.showAndWait();

        return (result.isPresent() && result.get() == ButtonType.OK);

    }

    /**
     * Displays a Popup message for the user.
     *
     * @param message The message.
     */
    public static void popup(Label message) {

        // Run on JavaFX Thread.
        Platform.runLater(() -> {
            if (popup.isShowing()) {
                popup.hide();
            }

            // Clear any existing contents.
            popup.getContent().clear();
            popup.getContent().add(message);
            popup.setAutoHide(true);
            popup.setHideOnEscape(true);
            message.getStylesheets().add(Objects.requireNonNull(MainUtility.class.getResource("css/alert-style.css")).toExternalForm());
            message.getStyleClass().add("popup-label");

            popup.show(stage, stage.getWidth(), stage.getHeight());
        });

    }

    /**
     * Create a fade out animation.
     *
     * @param stage The primary stage.
     */
    public static void fadeOutStage(Stage stage) {

        FadeTransition fadeOut = new FadeTransition(Duration.millis(1000), stage.getScene().getRoot());
        fadeOut.setFromValue(stage.getScene().getRoot().getOpacity());
        fadeOut.setToValue(0.0);

        // Create scale-down transition
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(1000), stage.getScene().getRoot());
        scaleDown.setFromX(1.0);
        scaleDown.setFromY(1.0);
        scaleDown.setToX(0.0);
        scaleDown.setToY(0.0);

        // Play both transitions simultaneously
        fadeOut.play();
        scaleDown.play();

        fadeOut.setOnFinished(actionEvent -> fadeInStage(stage));

    }

    /**
     * Create a fade in animation.
     *
     * @param stage The primary stage.
     */
    public static void fadeInStage(Stage stage) {

        FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), stage.getScene().getRoot());
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        // Create scale-up transition
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(1000), stage.getScene().getRoot());
        scaleUp.setFromX(0.0);
        scaleUp.setFromY(0.0);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);

        // Play both transitions simultaneously
        fadeIn.play();
        scaleUp.play();

    }

    /**
     * Imports Java source files from the jdk installation.
     */
    public static void importSrcFiles() {

        // Get the path to the jdk.
        Path jdkPath = SettingsUtility.getJavaPath();
        if (jdkPath == null) {
            logger.error("JDK not detected!");
            return;
        }

        // Try to locate the zip containing the source files.
        File src = new File(jdkPath.toFile(), "lib/src.zip");
        if (!src.exists()) {
            src = new File(jdkPath.toFile(), "src/src.zip");
            if (!src.exists()) {
                src = new File(jdkPath.toFile(), "src.zip");
                if (!src.exists()) {
                    return;
                }
            }
        }

        // Extract the source files.
        extractZip(src.toPath(), Paths.get("files/src"), false);

    }

    /**
     * Extracts a zip file to a given location.
     *
     * @param zipFile The zip file.
     * @param destination The destination directory.
     * @param overwrite Whether to overwrite an existing one.
     */
    public static void extractZip(Path zipFile, Path destination, boolean overwrite) {

        // Open a database connection.
        Connection conn = DatabaseUtility.connect();

        boolean start = true;
        if (TEMP.toFile().exists()) {
            start = false;
        } else {

            // Check if destination directory already exists.
            if (Files.exists(destination)) {
                if (overwrite) {
                    try {

                        // Delete the directory and its contents.
                        DirectoryManager.recursiveDelete(destination);
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                        return;
                    }
                } else {
                    return;
                }
            }

            // Create Temp file to indicate indexing is in progress.
            FileManager.writeToFile(TEMP, "", true, false);

            // Drop to the table to create a new one.
            DatabaseUtility.executeUpdate(conn, "DROP TABLE IF EXISTS ClassMetaData");
        }

        // Create the destination directory.
        if (destination.toFile().exists() || destination.toFile().mkdirs()) {
            try (InputStream in = new FileInputStream(zipFile.toFile());
                 ZipInputStream zin = new ZipInputStream(in)) {

                ZipEntry entry;
                logger.info("Started Indexing");
                Path lastEntry = null;
                if (!start) {
                    ResultSet rs = DatabaseUtility.executeQuery(conn, "SELECT path FROM ClassMetaData ORDER BY id DESC LIMIT 1");
                    lastEntry = Paths.get(rs.getString("path"));
                    rs.getStatement().close();
                }
                while ((entry = zin.getNextEntry()) != null) {
                    Path entryPath = destination.resolve(entry.getName());
                    if (!start) {
                        if (entryPath.toAbsolutePath().equals(lastEntry)) {
                            start = true;
                        }
                        continue;
                    }
                    if (entryPath.toFile().exists() && !entryPath.toFile().isDirectory()) {

                        // Add class meta data to the database.
                        addClassMetaData(entryPath, conn);
                        continue;
                    }
                    if (entry.isDirectory()) {
                        Files.createDirectories(entryPath);
                    } else {
                        if (Files.notExists(entryPath.getParent())) {
                            Files.createDirectories(entryPath.getParent());
                        }
                        try (OutputStream out = Files.newOutputStream(entryPath)) {

                            // Oversized buffer for safety.
                            byte[] buffer = new byte[10240];
                            int bytesRead;
                            while ((bytesRead = zin.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                        }

                        // Check what type of OS and set the files to readonly accordingly.
                        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                            Files.setAttribute(entryPath, "dos:readonly", true);
                        } else {
                            Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.OWNER_READ,
                                    PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ);
                            Files.setPosixFilePermissions(entryPath, perms);
                        }

                        // Add class meta data to the database.
                        addClassMetaData(entryPath, conn);
                    }
                    zin.closeEntry();
                }
                logger.info("Ended Indexing");
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        } else {
            logger.error("{} directory could not be created", destination);
        }

        try {

            // An open connection would mean the Thread actually got to the end of the zip.
            if (conn != null && !conn.isClosed()) {

                // Delete temp.fus to mark that indexing is complete.
                FileManager.deleteFile(TEMP, true, true);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        // Close the database connection.
        DatabaseUtility.close(conn);

    }

    /**
     * Installs a Tooltip onto a Node.
     *
     * @param text The text the Tooltip will contain.
     * @param node The node to install on.
     */
    public static void installTooltip(String text, Node node) {

        Tooltip.install(node, new Tooltip(text));
    }

    /**
     * Sizes an ImageView.
     *
     * @param image The ImageView.
     * @param width The width.
     * @param height The height.
     */
    public static void sizeImage(ImageView image, int width, int height) {

        image.setFitWidth(width);
        image.setFitHeight(height);
        image.setPreserveRatio(true);
        image.setSmooth(true);

    }

    /**
     * Requests for a port.
     *
     * @return The port number.
     */
    public static int getPort() {

        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            logger.error(e.getMessage());
            return -1;
        }

    }

    /**
     * Sets a Canvas to display the current line of execution. Or refreshes it.
     *
     * @param textArea The CustomTextArea to cover.
     * @param lineNumber The line number to be highlighted.
     */
    public static void setDebugCanvas(CustomTextArea textArea, int lineNumber) {

        CustomCanvas canvas;
        StackPane stackPane = (StackPane) textArea.getParent();

        // Check whether there is an existing Debug Canvas and remove it.
        if (stackPane.getChildren().size() > 1) {
            if (((CustomCanvas) stackPane.getChildren().get(1)).getType().equals("Debug")) {
                stackPane.getChildren().remove(1);
            } else if (stackPane.getChildren().size() > 2) {
                stackPane.getChildren().remove(2);
            }
        }

        // Add new Canvas.
        canvas = new CustomCanvas(textArea.getScene().getWidth(), textArea.getScene().getHeight(), "Debug");
        canvas.setMouseTransparent(true);
        stackPane.getChildren().add(canvas);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        int originalCaretPosition = textArea.getCaretPosition();

        // Get the Y position of the line and height
        textArea.moveTo(textArea.getAbsolutePosition(lineNumber, 0));
        try {
            double startY = textArea.screenToLocal(textArea.getCaretBounds().get()).getMinY();
            double lineHeight = textArea.screenToLocal(textArea.getCaretBounds().get()).getMaxY() - startY;

            // Draw the highlight
            gc.setFill(Color.web("#515453", 0.3));
            gc.fillRect(0, startY, textArea.getWidth(), lineHeight);
        } catch (NoSuchElementException ignored) {}

        // Restore the original caret position
        textArea.moveTo(originalCaretPosition);

    }

    /**
     * Add Metadata about a class.
     *
     * @param path The path to the class.
     * @param conn The connection to the database.
     * @throws IOException When something goes wrong with IO operations as the class is parsed.
     */
    public static void addClassMetaData(Path path, Connection conn) throws IOException {

        // Check if the file is a java file.
        if (path.toAbsolutePath().toString().endsWith(".java")) {
            JavaParser parser = new JavaParser();
            CompilationUnit cu = parser.parse(path).getResult().orElse(null);
            if (cu != null) {
                String packageName = cu.getPackageDeclaration().map(NodeWithName::getNameAsString).orElse("");
                for (TypeDeclaration<?> type : cu.getTypes()) {
                    String className = type.getNameAsString();
                    DatabaseUtility.executeUpdate(
                            conn,
                            "INSERT INTO ClassMetaData(packageName, className, qualifiedName, path)" +
                                    "VALUES (?, ?, ?, ?)",
                            packageName, className, (packageName.isEmpty()) ? className : packageName + "." + className, path.toAbsolutePath().toString()
                    );

                    // Add inner classes if there are any.
                    type.getMembers().forEach(member -> {
                        if (member instanceof TypeDeclaration<?> innerClass) {


                            // Check whether that class doesn't exist in the database.
                            ResultSet rs2 = DatabaseUtility.executeQuery(conn, "SELECT id FROM ClassMetaData " +
                                    "WHERE className = ? AND path = ?", innerClass.getNameAsString(), path.toAbsolutePath().toString());
                            try {
                                if (!rs2.next()) {
                                    rs2.getStatement().close();
                                    DatabaseUtility.executeUpdate(
                                            conn,
                                            "INSERT INTO ClassMetaData(packageName, className, qualifiedName, path)" +
                                                    "VALUES (?, ?, ?, ?)",
                                            packageName, innerClass.getNameAsString(), (packageName.isEmpty()) ? className + "$" + innerClass.getNameAsString() : packageName + "." + className + "$" + innerClass.getNameAsString(), path.toAbsolutePath().toString()
                                    );
                                }
                            }catch (SQLException e) {
                                logger.error(e.getMessage());
                            } finally {
                                try {
                                    rs2.getStatement().close();
                                } catch (SQLException e) {
                                    logger.error(e.getMessage());
                                }
                            }
                            addInnerClassMetaData(packageName, innerClass, className, path, conn);
                        }
                    });
                }
            }
        }

    }

    /**
     * Adds an inner class to the database.
     *
     * @param packageName The name of the package.
     * @param outerClass The outer class.
     * @param outerClassName The name of the outer class.
     * @param path The Path to the outer class.
     * @param conn The database connection.
     */
    public static void addInnerClassMetaData(String packageName, TypeDeclaration<?> outerClass, String outerClassName, Path path, Connection conn) {

        // Loop through each inner class.
        outerClass.getMembers().forEach(member -> {
            if (member instanceof TypeDeclaration<?> innerClass) {
                String innerClassName = innerClass.getNameAsString();

                // Check whether that class doesn't exist in the database.
                ResultSet rs = DatabaseUtility.executeQuery(conn, "SELECT id FROM ClassMetaData " +
                        "WHERE className = ? AND path = ?", innerClassName, path.toAbsolutePath().toString());
                try {
                    if (!rs.next()) {
                        rs.getStatement().close();
                        DatabaseUtility.executeUpdate(
                                conn,
                                "INSERT INTO ClassMetaData(packageName, className, qualifiedName, path)" +
                                        "VALUES (?, ?, ?, ?)",
                                packageName, innerClassName, (packageName.isEmpty()) ? outerClassName + "$" + innerClassName : packageName + "." + outerClassName + "$" + innerClassName, path.toAbsolutePath().toString()
                        );
                    }
                } catch (SQLException e) {
                    logger.error(e.getMessage());
                } finally {
                    try {
                        rs.getStatement().close();
                    } catch (SQLException e) {
                        logger.error(e.getMessage());
                    }
                }

                // Recursively handle nested inner classes
                addInnerClassMetaData(packageName, innerClass, outerClassName + "$" + innerClassName, path, conn);
            }
        });

    }

    /**
     * Sets up opeProjectPath.
     *
     * @param openProjectPath openProjectPath.
     */
    public static void setOpenProjectPath(ArrayList<Path> openProjectPath) {

        MainUtility.openProjectPath = openProjectPath;
    }

    /**
     * Sets up the primary stage.
     *
     * @param stage The primary Stage.
     */
    public static void setStage(Stage stage) {

        MainUtility.stage = stage;
    }
}
