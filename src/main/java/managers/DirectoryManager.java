package managers;

import custom_classes.*;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DirectoryManager {

    private static DirectoryChooser directoryChooser;
    private static TabPane tabPane;
    private static ArrayList<Path> openProjectPath;
    private static VBox projectView;

    public static void openProject(Path path) {

        File dir;
        if (path == null) {
            directoryChooser.setTitle("Select a Directory");
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
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

        }

    }

    public static RootTreeNode parseDirectory(Path path) {

        RootTreeNode rootDirectory = new RootTreeNode(path);
        System.out.println(!Files.exists(path));
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            return null;
        }
        Map<Path, DirectoryTreeNode> pathMap = new HashMap<>();
        pathMap.put(path, rootDirectory);

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs){

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
        } catch (IOException e) {

            System.out.println(e.getMessage());
        }

        return rootDirectory;

    }

    public static TreeView<HBox> createTree(RootTreeNode root) {

        HBox hBox = new HBox();
        FontIcon icon = new FontIcon(FontAwesomeRegular.FOLDER_OPEN);
        icon.setIconColor(Color.RED);
        hBox.getChildren().add(icon);
        hBox.getChildren().add(new Label("  " + root.getName()));
        hBox.setAlignment(Pos.CENTER_LEFT);
        CustomTreeItem<HBox> rootItem = new CustomTreeItem<>(hBox, root.getPath());
        rootItem.setExpanded(true);
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
        return treeView;

    }

    public static void addChildren(TreeNode parentNode, CustomTreeItem<HBox> parentItem) {

        for (TreeNode childNode : parentNode.getChildren()) {

            HBox hBox = new HBox();
            String[] parts = childNode.getName().split("\\.");
            FontIcon icon = new FontIcon((childNode instanceof DirectoryTreeNode) ?
                    FontAwesomeRegular.FOLDER_OPEN :
                    (Objects.equals(parts[parts.length - 1], "java")) ? FontAwesomeRegular.FILE_CODE :
                            FontAwesomeRegular.FILE);
            icon.setIconColor((childNode instanceof DirectoryTreeNode) ? Color.GREEN :
                    (Objects.equals(parts[parts.length - 1], "java")) ? Color.PURPLE : Color.GRAY);
            hBox.getChildren().add(icon);
            hBox.getChildren().add(new Label("  " + childNode.getName()));
            hBox.setAlignment(Pos.CENTER_LEFT);
            CustomTreeItem<HBox> childItem = new CustomTreeItem<>(hBox, childNode.getPath());
            childItem.setExpanded(true);
            parentItem.getChildren().add(childItem);
            if (childNode instanceof DirectoryTreeNode) {
                addChildren(childNode, childItem);
            }

        }
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
}
