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
import com.project.custom_classes.TreeNode;
import com.project.custom_classes.CustomTreeLabel;
import com.project.custom_classes.FileChange;
import javafx.geometry.Pos;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeCell;
import javafx.scene.control.Label;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.input.Clipboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import com.project.utility.MainUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.StandardCopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Objects;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Handles directory related operations.
 */
public class DirectoryManager {

    /**
     * The logger for the class.
     */
    private static final Logger logger = LoggerFactory.getLogger(DirectoryManager.class);

    /**
     * The Path to the temporary storage of deleted files.
     */
    public static final Path TRASH = Paths.get(".trash");

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
            logger.error("Couldn't open directory: {}", path);
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
            logger.error(e.getMessage());
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

                        // Allow drag and drop.
                        setOnDragOver(event -> {
                            if (event.getGestureSource() != this && event.getDragboard().hasFiles()) {
                                event.acceptTransferModes(TransferMode.COPY);
                            }
                            event.consume();
                        });

                        setOnDragDropped(event -> {
                            Dragboard dragboard = event.getDragboard();
                            if (dragboard.hasFiles()) {
                                for (File file : dragboard.getFiles()) {
                                    FileManager.copyFile(file.toPath());
                                    DirectoryManager.pasteIntoDirectory(path);
                                }
                                event.setDropCompleted(true);
                            } else {
                                event.setDropCompleted(false);
                            }
                            event.consume();
                        });

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

                        MenuItem textFileItem = new MenuItem("Text File");
                        textFileItem.setOnAction(event -> FileManager.newTextFile(path));

                        newItem.getItems().addAll(fileItem, packageItem, textFileItem);

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
                        deleteItem.setOnAction(event -> FileManager.deleteFile(path, false, false));
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

        // Detect different key presses and act accordingly.
        treeView.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            CustomTreeItem<HBox> selectedItem;
            switch (event.getCode()) {
                case DELETE:
                    selectedItem = (CustomTreeItem<HBox>) treeView.getSelectionModel().getSelectedItem();
                    if (selectedItem != null) {
                        FileManager.deleteFile(selectedItem.getPath(), false, false);
                    }
                    break;
                case C:
                    if (event.isControlDown()) {
                        selectedItem = (CustomTreeItem<HBox>) treeView.getSelectionModel().getSelectedItem();
                        if (selectedItem != null) {
                            FileManager.copyFile(selectedItem.getPath());
                        }
                    }
                    break;
                case X:
                    if (event.isControlDown()) {
                        selectedItem = (CustomTreeItem<HBox>) treeView.getSelectionModel().getSelectedItem();
                        if (selectedItem != null) {
                            FileManager.cutFile(selectedItem.getPath(), true);
                        }
                    }
                    break;
                case V:
                    if (event.isControlDown()) {
                        selectedItem = (CustomTreeItem<HBox>) treeView.getSelectionModel().getSelectedItem();
                        if (selectedItem != null) {
                            DirectoryManager.pasteIntoDirectory(selectedItem.getPath());
                        }
                    }
                    break;
                case Z:
                    if (event.isControlDown()  && event.isShiftDown()) {
                        redo();
                    } else if (event.isControlDown()) {
                        undo();
                    }
                    break;
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

                // Push the change onto the undo stack.
                UndoManager.pushUndo(new FileChange(null, dir.toPath()));
                DirectoryManager.UndoManager.clearRedo();
                logger.info("Package {} created", dir.getName());
            }
        } else {
            MainUtility.popup(new Label("Package already exists"));
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

                // Enable cutting to have the file deleted.
                shouldCut.clear();
                shouldCut.add(true);

                // Push the change onto the redo stack.
                UndoManager.pushUndo(new FileChange(dir.toPath(), null));
                DirectoryManager.UndoManager.clearRedo();
                moveFile(dir.toPath(), new File(TRASH.toAbsolutePath().toString(), dir.getName()).toPath());
                logger.info("Directory {} deleted", dir.getName());
            } catch (IOException e) {
                logger.error(e.getMessage());
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

        // Check if there is a String in the clipboard (A path) or a file.
        if (clipboard.hasString() || clipboard.hasFiles()) {
            File potentialFile;
            if (clipboard.hasString()) {
                String content = clipboard.getString();
                potentialFile = new File(content);
            } else {
                potentialFile = clipboard.getFiles().get(0);
            }


            // Check whether the string is indeed a legit path or the path exists.
            if (!potentialFile.exists()) {
                return;
            }
            File newFile = new File(path.toString(), potentialFile.getName());
            try {
                if (newFile.exists() &&
                    !MainUtility.confirm("File or directory already exists", "This file is already exists, overwriting it?")) {
                    return;
                }

                // Push changes onto the undo stack.
                UndoManager.pushUndo(new FileChange(potentialFile.toPath(), newFile.toPath()));
                moveFile(potentialFile.toPath(), newFile.toPath());

            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }

    }

    /**
     * Moves a whole directory from one location to another.
     * Can move a file as well.
     *
     * @param oldFile The old Path of the file/directory.
     * @param newFile The new Path to the file/directory.
     * @throws IOException When something goes wrong during the IO processes.
     */
    public static void moveFile(Path oldFile, Path newFile) throws IOException {

        Files.walkFileTree(oldFile, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

                // Resolve a path in the new directory and create the directory if it doesn't exist.
                Path targetPath = newFile.resolve(oldFile.relativize(dir));
                if (!Files.exists(targetPath)) {
                    Files.createDirectories(targetPath);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                // Create the parent directory if it doesn't exist.
                if (Files.notExists(newFile.getParent())) {
                    Files.createDirectories(newFile.getParent());
                }
                if (!shouldCut.isEmpty() && shouldCut.get(0)) {
                    Files.move(file, newFile.resolve(oldFile.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.copy(file, newFile.resolve(oldFile.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path file, IOException exc) throws IOException {
                if (!shouldCut.isEmpty() && shouldCut.get(0)) {
                    Files.delete(file);
                }
                return FileVisitResult.CONTINUE;
            }

        });
        shouldCut.clear();
        logger.info("{} moved/copied to {}", oldFile.toFile(), newFile);

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

    /**
     * Undoes delete, create or rename actions on the project files.
     */
    public static void undo() {

        // Get most recent change.
        FileChange previous = UndoManager.popUndo();
        try {
            if (previous != null) {
                if (previous.oldPath() == null) {

                    // Push change on redo stack to enable redo operations.
                    UndoManager.pushRedo(new FileChange(previous.newPath(), null));

                    // Set should cut to delete the file.
                    shouldCut.clear();
                    shouldCut.add(true);
                    moveFile(previous.newPath(), new File(TRASH.toAbsolutePath().toString(), previous.newPath().toFile().getName()).toPath());

                    // Send notification to language server.
                    JLSManager.sendDeletedFile(previous.newPath().toUri().toString());
                } else if (previous.newPath() == null) {

                    // Set should cut to delete the file.
                    shouldCut.clear();
                    shouldCut.add(true);
                    moveFile(new File(TRASH.toAbsolutePath().toString(), previous.oldPath().toFile().getName()).toPath(), previous.oldPath());

                    // Send didOpen notification to language server.
                    StringBuilder text = new StringBuilder();
                    ArrayList<String> lines = FileManager.readFile(previous.oldPath());
                    if (lines != null) {
                        for (String line : lines) {
                            text.append(line).append("\n");
                        }
                    }
                    JLSManager.didOpen(previous.oldPath(), text.toString());
                    JLSManager.sendDidSave(previous.oldPath().toUri().toString(), text.toString());

                    // Push change on redo stack to enable redo operations.
                    UndoManager.pushRedo(new FileChange(null, previous.oldPath()));
                } else {
                    Files.move(previous.newPath(), previous.oldPath());

                    // Send notification to language server.
                    JLSManager.sendDeletedFile(previous.newPath().toUri().toString());

                    // Send didOpen notification to language server.
                    StringBuilder text = new StringBuilder();
                    ArrayList<String> lines = FileManager.readFile(previous.oldPath());
                    if (lines != null) {
                        for (String line : lines) {
                            text.append(line).append("\n");
                        }
                    }
                    JLSManager.didOpen(previous.oldPath(), text.toString());
                    JLSManager.sendDidSave(previous.oldPath().toUri().toString(), text.toString());

                    // Push change on redo stack to enable redo operations.
                    UndoManager.pushRedo(new FileChange(previous.newPath(), previous.oldPath()));
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

    }

    /**
     * Redoes delete, create or rename actions on the project files.
     */
    public static void redo() {

        // Get most recent change.
        FileChange previous = UndoManager.popRedo();
        try {
            if (previous != null) {
                if (previous.oldPath() == null) {

                    // Set should cut to delete the file.
                    shouldCut.clear();
                    shouldCut.add(true);

                    // Get most recent change.
                    UndoManager.pushUndo(new FileChange(previous.newPath(), null));
                    moveFile(previous.newPath(), new File(TRASH.toAbsolutePath().toString(), previous.newPath().toFile().getName()).toPath());

                    // Send notification to language server.
                    JLSManager.sendDeletedFile(previous.newPath().toUri().toString());
                } else if (previous.newPath() == null) {

                    // Set should cut to delete the file.
                    shouldCut.clear();
                    shouldCut.add(true);

                    moveFile(new File(TRASH.toAbsolutePath().toString(), previous.oldPath().toFile().getName()).toPath(), previous.oldPath());

                    // Send didOpen notification to language server.
                    StringBuilder text = new StringBuilder();
                    ArrayList<String> lines = FileManager.readFile(previous.oldPath());
                    if (lines != null) {
                        for (String line : lines) {
                            text.append(line).append("\n");
                        }
                    }
                    JLSManager.didOpen(previous.oldPath(), text.toString());
                    JLSManager.sendDidSave(previous.oldPath().toUri().toString(), text.toString());

                    // Get most recent change.
                    UndoManager.pushUndo(new FileChange(null, previous.oldPath()));
                } else {
                    Files.move(previous.newPath(), previous.oldPath());

                    // Send notification to language server.
                    JLSManager.sendDeletedFile(previous.newPath().toUri().toString());

                    // Send didOpen notification to language server.
                    StringBuilder text = new StringBuilder();
                    ArrayList<String> lines = FileManager.readFile(previous.oldPath());
                    if (lines != null) {
                        for (String line : lines) {
                            text.append(line).append("\n");
                        }
                    }
                    JLSManager.didOpen(previous.oldPath(), text.toString());
                    JLSManager.sendDidSave(previous.oldPath().toUri().toString(), text.toString());

                    // Get most recent change.
                    UndoManager.pushUndo(new FileChange(previous.newPath(), previous.oldPath()));
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

    }

    /**
     * Manages undo and redo operations for files.
     */
    public static class UndoManager {

        /**
         * Maximum number of redo and undo entries each.
         */
        private static final int MAX_UNDO_REDO_ENTRIES = 100;

        /**
         * A Stack (implemented as LinkedList) for undo operations.
         */
        private static final LinkedList<FileChange> undoStack = new LinkedList<>();

        /**
         * A Stack (implemented as LinkedList) for redo operations.
         */
        private static final LinkedList<FileChange> redoStack = new LinkedList<>();

        /**
         * Add an entry to the end of the undo stack.
         *
         * @param change The change in text.
         */
        public static void pushUndo(FileChange change) {

            undoStack.addLast(change);
            if (undoStack.size() > MAX_UNDO_REDO_ENTRIES) {
                undoStack.removeFirst();
            }
        }

        /**
         * Remove the last entry in the undo stack.
         *
         * @return The change.
         */
        public static FileChange popUndo() {

            return undoStack.pollLast();
        }

        /**
         * Add an entry to the end of the redo stack.
         *
         * @param change The change in text.
         */
        public static void pushRedo(FileChange change) {

            redoStack.addLast(change);
            if (redoStack.size() > MAX_UNDO_REDO_ENTRIES) {
                redoStack.removeFirst();
            }
        }

        /**
         * Remove the last entry in the redo stack.
         *
         * @return The change.
         */
        public static FileChange popRedo() {

            return redoStack.pollLast();
        }

        /**
         * Removes all entries on the undo stack.
         */
        public static void clearRedo() {

            redoStack.clear();
        }


    }

}
