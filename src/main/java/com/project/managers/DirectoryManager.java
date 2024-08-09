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

package com.project.managers;

import com.project.custom_classes.CustomTreeItem;
import com.project.custom_classes.RootTreeNode;
import com.project.custom_classes.DirectoryTreeNode;
import com.project.custom_classes.FileTreeNode;
import com.project.custom_classes.CustomTreeLabel;
import com.project.custom_classes.TreeNode;
import javafx.geometry.Pos;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeCell;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.project.utility.MainUtility;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Handles directory related operations.
 */
public class DirectoryManager {

    /**
     * The logger for the class.
     */
    private static final Logger logger = LogManager.getLogger(DirectoryManager.class);

    /**
     * Stores a CustomTreeItem and whether is it open or closed.
     */
    private static final Map<String, Boolean> nodeIsOpen = new HashMap<>();

    /**
     * An instance of a DirectoryChooser to choose files from the device.
     */
    private static DirectoryChooser directoryChooser;

    /**
     * The TabPane.
     */
    private static TabPane tabPane;

    /**
     * An array containing the path of the currently open project.
     */
    private static ArrayList<Path> openProjectPath;

    /**
     * Contains the TreeView of the current project.
     */
    private static VBox projectView;

    /**
     * The system clipboard.
     */
    private static Clipboard clipboard;

    /**
     * Whether the operation is a cut or a copy.
     */
    private static ArrayList<Boolean> shouldCut;

    /**
     * The file used for caching classes that take time to locate.
     */
    private static final File CACHE = new File("/src/main/files/cache.fus");

    /**
     * Opens an existing project.
     *
     * @param path The Path to the root directory
     * @return The path to the root directory.
     */
    public static Path openProject(Path path) {

        File dir;

        // Check whether there was a Path passed
        if (path == null) {

            // If none prompt user to pick one.
            directoryChooser.setTitle("Select a Directory");
            if (ProjectManager.APP_HOME.exists()) {
                directoryChooser.setInitialDirectory(ProjectManager.APP_HOME);
            } else {
                directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            }

            dir = directoryChooser.showDialog(tabPane.getScene().getWindow());
        } else {
            dir = path.toFile();
        }

        if (dir != null && dir.exists() && dir.isDirectory()) {

            RootTreeNode root = parseDirectory(dir.toPath());
            if (root != null) {
                if (openProjectPath.isEmpty()) {
                    openProjectPath.add(root.getPath());
                } else {
                openProjectPath.set(0, root.getPath());
                }

                // Create a TreeView and place it in projectView.
                TreeView<HBox> treeView = createTree(root);
                projectView.getChildren().clear();
                projectView.getChildren().add(treeView);
                VBox.setVgrow(treeView, Priority.ALWAYS);
            }

        } else {
            logger.info("Couldn't open directory: {}", (dir == null) ? "\u0000" : dir.getPath());
        }

        return (dir != null) ? dir.toPath() : null;

    }

    /**
     * Replicates a directory structure as tree using TreeNode Objects.
     *
     * @param path The Path to the directory
     * @return The RootTreeNode.
     */
    public static RootTreeNode parseDirectory(Path path) {

        if (!Files.exists(path) || !Files.isDirectory(path)) {
            logger.error("Couldn't open directory: {}", path.toString());
            return null;
        }

        RootTreeNode rootDirectory = new RootTreeNode(path);
        Map<Path, DirectoryTreeNode> pathMap = new HashMap<>();
        pathMap.put(path, rootDirectory);

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {

                    DirectoryTreeNode parent = pathMap.get(filePath.getParent());
                    FileTreeNode file = new FileTreeNode(filePath, parent);
                    parent.addChild(file);
                    return FileVisitResult.CONTINUE;

                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {

                    if (!dir.equals(path)) {
                        DirectoryTreeNode parent = pathMap.get(dir.getParent());
                        DirectoryTreeNode directory = new DirectoryTreeNode(dir, parent);
                        parent.addChild(directory);
                        pathMap.put(dir, directory);
                    }
                    return FileVisitResult.CONTINUE;

                }

            });
        } catch (IOException | SecurityException e) {
            logger.error(e);
        }

        return rootDirectory;

    }

    /**
     * Creates a TreeView from the RootTreeNode.
     *
     * @param root The RootTreeNode.
     * @return The TreeView.
     */
    public static TreeView<HBox> createTree(RootTreeNode root) {

        CustomTreeItem<HBox> rootItem = initTreeItem(new ImageView(
                new Image(Objects.requireNonNull(DirectoryManager.class.getResourceAsStream("icons/folder.png")))), root);

        // Check whether the TreeItem had a state before and use it.
        if (nodeIsOpen.containsKey(rootItem.getPath().toString())) {
            rootItem.setExpanded(nodeIsOpen.get(rootItem.getPath().toString()));
        } else {

            // Else, default the root to be expanded.
            rootItem.setExpanded(true);
            nodeIsOpen.put(rootItem.getPath().toString(), Boolean.TRUE);
        }

        // Add a listener to detect when the TreeItem is expanded or closed.
        rootItem.expandedProperty().addListener((observable, oldValue, newValue) -> {
            if (!nodeIsOpen.containsKey(rootItem.getPath().toString())) {
                logger.error("For some reason, {} is not in nodeIsOpen", rootItem.getPath());
            } else {
                nodeIsOpen.replace(rootItem.getPath().toString(), newValue);
            }
        });

        // Add children to the root TreeItem.
        addChildren(root, rootItem);

        // Create a TreeView.
        TreeView<HBox> treeView = new TreeView<>(rootItem);

        // Add an event filter for when an item is double-clicked.
        treeView.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getClickCount() == 2) {
                CustomTreeItem<HBox> selectedItem = (CustomTreeItem<HBox>) treeView.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {

                    // Open the file.
                    FileManager.openFile(selectedItem.getPath());
                }
            }
        });

        treeView.setCellFactory(tc -> new TreeCell<>() {

            @Override
            protected void updateItem(HBox item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setContextMenu(null);
                    setDisable(true);
                } else {
                    CustomTreeLabel label = (CustomTreeLabel) item.getChildren().get(1);
                    label.setStyle("-fx-text-fill: white;");
                    Path path = label.getPath();
                    setGraphic(item);
                    setDisable(false);

                    ContextMenu contextMenu = new ContextMenu();
                    contextMenu.getStyleClass().add("context-menu");
                    if (path.toFile().isDirectory()) {
                        Menu newItem = new Menu("New");

                        Menu fileItem = new Menu("Java File");

                        MenuItem classItem = new MenuItem("Class");
                        classItem.setOnAction(event -> FileManager.newJavaClass(path));
                        MenuItem abstractClassItem = new MenuItem("Abstract Class");
                        abstractClassItem.setOnAction(event -> FileManager.newJavaAbstractClass(path));
                        MenuItem interfaceItem = new MenuItem("Interface");
                        interfaceItem.setOnAction(event -> FileManager.newJavaInterface(path));
                        MenuItem recordItem = new MenuItem("Record");
                        recordItem.setOnAction(event -> FileManager.newJavaRecord(path));
                        MenuItem enumItem = new MenuItem("Enum");
                        enumItem.setOnAction(event -> FileManager.newJavaEnum(path));
                        MenuItem annotationItem = new MenuItem("Annotation");
                        annotationItem.setOnAction(event -> FileManager.newJavaAnnotation(path));

                        fileItem.getItems().addAll(classItem, abstractClassItem, interfaceItem,
                                recordItem, enumItem, annotationItem);

                        MenuItem packageItem = new MenuItem("Package");
                        packageItem.setOnAction(event -> newPackage(path));

                        newItem.getItems().addAll(fileItem, packageItem);

                        MenuItem deleteItem = new MenuItem("Delete");
                        deleteItem.setOnAction(event -> DirectoryManager.deleteDirectory(path));
                        MenuItem cutItem = new MenuItem("Cut");
                        cutItem.setOnAction(event -> DirectoryManager.cutDirectory(path));
                        MenuItem copyItem = new MenuItem("Copy");
                        copyItem.setOnAction(event -> DirectoryManager.copyDirectory(path));
                        MenuItem pasteItem = new MenuItem("Paste");
                        pasteItem.setOnAction(event -> DirectoryManager.pasteIntoDirectory(path));
                        MenuItem renameItem = new MenuItem("Rename");
                        renameItem.setOnAction(event -> DirectoryManager.renameDirectory(path));
                        contextMenu.getItems().addAll(newItem, deleteItem, cutItem, copyItem, pasteItem, renameItem);
                    } else {
                        MenuItem deleteItem = new MenuItem("Delete");
                        deleteItem.setOnAction(event -> FileManager.deleteFile(path, false));
                        MenuItem cutItem = new MenuItem("Cut");
                        cutItem.setOnAction(event -> FileManager.cutFile(path, true));
                        MenuItem copyItem = new MenuItem("Copy");
                        copyItem.setOnAction(event -> FileManager.copyFile(path));
                        MenuItem pasteItem = new MenuItem("Paste");
                        pasteItem.setOnAction(event -> FileManager.pasteIntoFile(path));
                        MenuItem renameItem = new MenuItem("Rename");
                        renameItem.setOnAction(event -> FileManager.renameFile(path));
                        contextMenu.getItems().addAll(deleteItem, cutItem, copyItem, pasteItem, renameItem);
                    }

                    setContextMenu(contextMenu);
                }
            }

        });
        return treeView;

    }

    /**
     * Initializes a CustomTreeItem.
     *
     * @param icon The icon.
     * @param root The TreeNode.
     * @return A CustomTreeItem.
     */
    private static CustomTreeItem<HBox> initTreeItem(ImageView icon, TreeNode root) {

        HBox hBox = new HBox();
        MainUtility.sizeImage(icon, 18, 18);
        hBox.getChildren().add(icon);
        hBox.getChildren().add(new CustomTreeLabel("  " + root.getName(), root.getPath()));
        hBox.setAlignment(Pos.CENTER_LEFT);

        return new CustomTreeItem<>(hBox, root.getPath());
    }

    /**
     * Adds children to a TreeItem if any.
     *
     * @param parentNode The TreeNode. (Represented on the TreeNode hierarchy).
     * @param parentItem The TreeItem. (Represented on the TreeView).
     */
    public static void addChildren(TreeNode parentNode, CustomTreeItem<HBox> parentItem) {

        // They look the same, but this allows directories to come before files in the TreeView.
        for (TreeNode childNode : parentNode.getChildren()) {

            if (childNode.getPath().toFile().isDirectory()) {
                add(parentItem, childNode);
            }

        }

        for (TreeNode childNode : parentNode.getChildren()) {

            if (childNode.getPath().toFile().isFile()) {
                add(parentItem, childNode);
            }

        }
    }

    /**
     * Adds a child to a TreeItem.
     *
     * @param parentItem The parent.
     * @param childNode The child.
     */
    private static void add(CustomTreeItem<HBox> parentItem, TreeNode childNode) {

        String[] parts = childNode.getName().split("\\.");

        // Determine what icon to use.
        ImageView icon = new ImageView((childNode instanceof DirectoryTreeNode) ?
                new Image(Objects.requireNonNull(DirectoryManager.class.getResourceAsStream("icons/folder.png"))) :
                (Objects.equals(parts[parts.length - 1], "java")) ? new Image(Objects.requireNonNull(DirectoryManager.class.getResourceAsStream("icons/java.png"))) :
                        new Image(Objects.requireNonNull(DirectoryManager.class.getResourceAsStream("icons/file.png"))));

        // Create the CustomTreeItem for the child.
        CustomTreeItem<HBox> childItem = initTreeItem(icon, childNode);

        // Check previous state of the node.
        if (nodeIsOpen.containsKey(childItem.getPath().toString())) {
            childItem.setExpanded(nodeIsOpen.get(childItem.getPath().toString()));
        } else {
            childItem.setExpanded(false);
            nodeIsOpen.put(childItem.getPath().toString(), Boolean.FALSE);
        }

        // Listen for when the TreeItem is expanded or closed.
        childItem.expandedProperty().addListener((observable, oldValue, newValue) -> {
            if (!nodeIsOpen.containsKey(childItem.getPath().toString())) {
                logger.error("For some reason, {} is not in nodeIsOpen", childItem.getPath());
            } else {
                nodeIsOpen.replace(childItem.getPath().toString(), newValue);
            }
        });

        // Add the childItem.
        parentItem.getChildren().add(childItem);

        // If the childItem is a directory, add children to the childItem.
        if (childNode instanceof DirectoryTreeNode) {
            addChildren(childNode, childItem);
        }

    }

    /**
     * Creates a new package (directory).
     *
     * @param path The Path of the parent directory.
     */
    public static void newPackage(Path path) {

        // Prompt user to enter a name for the new package.
        String name = MainUtility.quickDialog("New package", "Enter package name");
        if (name == null) {
            return;
        }
        File dir = new File(path.toString(), name);
        if (!dir.exists()) {
            if (dir.mkdir()) {
                logger.info("Package {} created", dir.getName());
            }
        } else {
            System.out.println("Package already exists");
        }

    }

    /**
     * Deletes a directory.
     *
     * @param path The Path to the directory.
     */
    public static void deleteDirectory(Path path) {

        // Ask user to confirm.
        boolean confirm = MainUtility.confirm("Delete " + path.getFileName(), "This directory and all contents will be deleted!");
        if (confirm) {
            File dir = new File(path.toString());
            try {
                recursiveDelete(dir.toPath());
                logger.info("Directory {} deleted", dir.getName());
            } catch (IOException e) {
                logger.error(e);
            }
        }

    }

    /**
     * Cuts a directory.
     *
     * @param path The Path to the directory.
     */
    public static void cutDirectory(Path path) {

        FileManager.cutFile(path, true);
    }

    /**
     * Copies a directory.
     *
     * @param path The Path to the directory.
     */
    public static void copyDirectory(Path path) {

        FileManager.copyFile(path);
    }

    /**
     * Pastes into a directory.
     *
     * @param path The Path to the directory.
     */
    public static void pasteIntoDirectory(Path path) {

        // Check if there is a String in the clipboard (A path).
        if (clipboard.hasString()) {
            String content = clipboard.getString();
            File potentialFile = new File(content);

            // Check whether the string is indeed a legit path.
            if (!potentialFile.exists()) {
                return;
            }
            File newFile = new File(path.toString(), potentialFile.getName());
            try {
                if (newFile.exists() &&
                    !MainUtility.confirm("File or directory already exists", "This file is already exists, overwriting it?")) {
                    return;
                }
                // Check whether it was a cut or copy.
                if (shouldCut.get(0)) {
                    Files.move(potentialFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    logger.info("{} moved to {}", potentialFile.getName(), newFile.getName());
                } else {
                    ArrayList<String> newContent = FileManager.readFile(potentialFile.toPath());
                    if (newContent == null) {
                        if (newFile.mkdir()) {
                            logger.info("{} created", newFile.getName());
                        }
                    } else {
                        Files.write(newFile.toPath(), newContent);
                        logger.info("{} created", newFile.getName());
                    }
                }
            } catch (IOException e) {
                logger.error(e);
            }
        }

    }

    /**
     * Renames a directory.
     *
     * @param path The Path to the directory.
     */
    public static void renameDirectory(Path path) {

        FileManager.renameFile(path);
    }

    /**
     * Deletes a directory and all its contents.
     *
     * @param path The Path to the directory.
     * @throws IOException IOException
     */
    public static void recursiveDelete(Path path) throws IOException {

        // Check it's indeed a directory.
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
                for (Path item : directoryStream) {
                    recursiveDelete(item);
                }
            }
        }
        Files.delete(path);

    }

    /**
     * Walks through a directories contents looking for a file.
     *
     * @param className The name of the class to look for.
     * @param startDir The directory to start searching from.
     * @return The file if any.
     */
    public static Optional<Path> findFile(String className, Path startDir) {


        // Check if the file in not in the cache already.
        ArrayList<String> cachedData =  FileManager.readFile(CACHE.toPath());
        if (cachedData != null) {
            for (String data : cachedData) {
                if (data.split("\u001F")[1].endsWith(className + ".java")) {
                    return Optional.of(Paths.get(data.split("\u001F")[0]));
                }
            }
        }
        long startTime = 0;
        long endTime = 0;
        if (startDir == null) {
            startDir = Paths.get(ProjectManager.getCurrentProject().getPath() + "/src/main/java");
        }

        // final array to be able to use it in a lambda.
        final Optional<Path>[] foundFile = new Optional[1];
        try {
            startTime = System.currentTimeMillis();
            Files.walkFileTree(startDir, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {

                    if (filePath.getFileName().toString().endsWith(className + ".java")) {
                        foundFile[0] = Optional.of(filePath);
                        return FileVisitResult.TERMINATE; // Stop searching once the file is found
                    }
                    return FileVisitResult.CONTINUE;

                }

            });
            endTime = System.currentTimeMillis();
        } catch (IOException | SecurityException e) {
            logger.error(e);
        }

        // Check whether the search time exceeded the threshold.
        if (endTime - startTime > 100) {
            if (foundFile[0].isPresent()) {

                // Store the path to the file and the name.
                // Using delimiter '\u001F' since it is very uncommon (Not even a printable character).
                FileManager.writeToFile(CACHE.toPath(), foundFile[0].get() + "\u001F" + className, false, true);
            }
        }
        return foundFile[0];

    }

    /**
     * Sets up the DirectoryChooser.
     *
     * @param directoryChooser The DirectoryChooser.
     */
    public static void setDirectoryChooser(DirectoryChooser directoryChooser) {

        DirectoryManager.directoryChooser = directoryChooser;
    }

    /**
     * Sets up the TabPane.
     *
     * @param tabPane The TabPane.
     */
    public static void setTabPane(TabPane tabPane) {

        DirectoryManager.tabPane = tabPane;
    }

    /**
     * Sets up OpenProjectPath.
     *
     * @param openProjectPath The OpenProjectPath.
     */
    public static void setOpenProjectPath(ArrayList<Path> openProjectPath) {

        DirectoryManager.openProjectPath = openProjectPath;
    }

    /**
     * Sets up ProjectView.
     *
     * @param projectView The ProjectView.
     */
    public static void setProjectView(VBox projectView) {

        DirectoryManager.projectView = projectView;
    }

    /**
     * Sets up the Clipboard.
     *
     * @param clipboard The Clipboard.
     */
    public static void setClipboard(Clipboard clipboard) {

        DirectoryManager.clipboard = clipboard;
    }

    /**
     * Sets up ShouldCut.
     *
     * @param shouldCut The ShouldCut.
     */
    public static void setShouldCut(ArrayList<Boolean> shouldCut) {

        DirectoryManager.shouldCut = shouldCut;
    }

}
