package com.project.managers;

import com.project.custom_classes.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular;
import org.kordamp.ikonli.javafx.FontIcon;
import com.project.utility.MainUtility;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DirectoryManager {

    private static final Logger logger = LogManager.getLogger(DirectoryManager.class);
    private static final Map<CustomTreeItem<HBox>, Boolean> nodeIsOpen = new HashMap<>();

    private static DirectoryChooser directoryChooser;
    private static TabPane tabPane;
    private static ArrayList<Path> openProjectPath;
    private static VBox projectView;
    private static Clipboard clipboard;
    private static ArrayList<Boolean> shouldCut;

    public static Path openProject(Path path) {

        File dir;
        if (path == null) {
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

    public static RootTreeNode parseDirectory(Path path) {

        RootTreeNode rootDirectory = new RootTreeNode(path);
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            logger.error("Couldn't open directory: {}", path.toString());
            return null;
        }
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

    public static TreeView<HBox> createTree(RootTreeNode root) {

        HBox hBox = new HBox();
        FontIcon icon = new FontIcon(FontAwesomeRegular.FOLDER_OPEN);
        icon.setIconColor(Color.RED);
        hBox.getChildren().add(icon);
        hBox.getChildren().add(new CustomTreeLabel("  " + root.getName(), root.getPath()));
        hBox.setAlignment(Pos.CENTER_LEFT);
        CustomTreeItem<HBox> rootItem = new CustomTreeItem<>(hBox, root.getPath());
        if (nodeIsOpen.containsKey(rootItem)) {
            rootItem.setExpanded(nodeIsOpen.get(rootItem));
        } else {
            rootItem.setExpanded(true);
            nodeIsOpen.put(rootItem, Boolean.TRUE);
        }
        rootItem.expandedProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (!nodeIsOpen.containsKey(rootItem)) {
                    logger.error("For some reason, {} is not in nodeIsOpen", rootItem.getPath());
                } else {
                    nodeIsOpen.replace(rootItem, newValue);
                }
            }

        });
        addChildren(root, rootItem);
        TreeView<HBox> treeView = new TreeView<>(rootItem);
        treeView.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getClickCount() == 2) {
                TreeItem<HBox> selectedItem = treeView.getSelectionModel().getSelectedItem();
                CustomTreeItem<HBox> item = (CustomTreeItem<HBox>) selectedItem;
                if (selectedItem != null) {
                    FileManager.openFile(item.getPath());
                }
            }
        });

        treeView.setCellFactory(tv -> new TreeCell<>() {

            @Override
            protected void updateItem(HBox item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setContextMenu(null);
                } else {
                    FontIcon icon = (FontIcon) item.getChildren().get(0);
                    CustomTreeLabel label = (CustomTreeLabel) item.getChildren().get(1);
                    Path path = label.getPath();
                    setGraphic(item);

                    ContextMenu contextMenu = new ContextMenu();
                    if (icon.getIconCode() == FontAwesomeRegular.FOLDER_OPEN) {
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
                        MenuItem runItem = new MenuItem("Run");
                        runItem.setOnAction(event -> FileManager.runFile(path));
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
                        contextMenu.getItems().addAll(runItem, deleteItem, cutItem, copyItem, pasteItem, renameItem);
                    }

                    setContextMenu(contextMenu);
                }
            }

        });
        return treeView;

    }

    public static void addChildren(TreeNode parentNode, CustomTreeItem<HBox> parentItem) {

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

    private static void add(CustomTreeItem<HBox> parentItem, TreeNode childNode) {
        HBox hBox = new HBox();
        String[] parts = childNode.getName().split("\\.");
        FontIcon icon = new FontIcon((childNode instanceof DirectoryTreeNode) ?
                FontAwesomeRegular.FOLDER_OPEN :
                (Objects.equals(parts[parts.length - 1], "java")) ? FontAwesomeRegular.FILE_CODE :
                        FontAwesomeRegular.FILE);
        icon.setIconColor((childNode instanceof DirectoryTreeNode) ? Color.GREEN :
                (Objects.equals(parts[parts.length - 1], "java")) ? Color.PURPLE : Color.GRAY);
        hBox.getChildren().add(icon);
        hBox.getChildren().add(new CustomTreeLabel("  " + childNode.getName(), childNode.getPath()));
        hBox.setAlignment(Pos.CENTER_LEFT);
        CustomTreeItem<HBox> childItem = new CustomTreeItem<>(hBox, childNode.getPath());
        if (nodeIsOpen.containsKey(childItem)) {
            childItem.setExpanded(nodeIsOpen.get(childItem));
        } else {
            childItem.setExpanded(false);
            nodeIsOpen.put(childItem, Boolean.FALSE);
        }
        childItem.expandedProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (!nodeIsOpen.containsKey(childItem)) {
                    logger.error("For some reason, {} is not in nodeIsOpen", childItem.getPath());
                } else {
                    nodeIsOpen.replace(childItem, newValue);
                }
            }

        });
        parentItem.getChildren().add(childItem);
        if (childNode instanceof DirectoryTreeNode) {
            addChildren(childNode, childItem);
        }
    }

    public static void newPackage(Path path) {

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
        ProjectManager.openProject(openProjectPath.get(0));

    }

    public static void deleteDirectory(Path path) {

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
        ProjectManager.openProject(openProjectPath.get(0));

    }

    public static void cutDirectory(Path path) {

        FileManager.cutFile(path, true);
    }

    public static void copyDirectory(Path path) {

        FileManager.copyFile(path);
    }

    public static void pasteIntoDirectory(Path path) {

        if (clipboard.hasString()) {
            String content = clipboard.getString();
            File potentialFile = new File(content);
            if (!potentialFile.exists()) {
                System.out.println("No file or directory copied!");
                return;
            }
            File newFile = new File(path.toString(), potentialFile.getName());
            try {
                if (newFile.exists()) {
                    boolean confirm = MainUtility.confirm("File or directory already exists", "This file is already exists, overwriting it?");
                    if (confirm) {
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
                    }
                } else {
                    Files.move(potentialFile.toPath(), newFile.toPath());
                    logger.info("{} moved to {}", potentialFile.getName(), newFile.getName());
                }
            } catch (IOException e) {
                logger.error(e);
            }

        }
        ProjectManager.openProject(openProjectPath.get(0));

    }

    public static void renameDirectory(Path path) {

        FileManager.renameFile(path);
    }

    public static void recursiveDelete(Path path) throws IOException {

        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
                for (Path item : directoryStream) {
                    recursiveDelete(item);
                }
            }
        }
        Files.delete(path);

    }

    public static void setDirectoryChooser(DirectoryChooser directoryChooser) {

        DirectoryManager.directoryChooser = directoryChooser;
    }

    public static void setTabPane(TabPane tabPane) {

        DirectoryManager.tabPane = tabPane;
    }

    public static void setOpenProjectPath(ArrayList<Path> openProjectPath) {

        DirectoryManager.openProjectPath = openProjectPath;
    }

    public static void setProjectView(VBox projectView) {

        DirectoryManager.projectView = projectView;
    }

    public static void setClipboard(Clipboard clipboard) {

        DirectoryManager.clipboard = clipboard;
    }

    public static void setShouldCut(ArrayList<Boolean> shouldCut) {

        DirectoryManager.shouldCut = shouldCut;
    }
}
