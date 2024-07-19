package managers;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.LineNumberFactory;
import utility.EditAreaUtility;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.function.IntFunction;

public class FileManager {

    private static TabPane tabPane;
    private static ArrayList<Path> openFilesPaths;
    private static ArrayList<Tab> tabs;
    private static ArrayList<Path> filePaths;
    private static ArrayList<Boolean> saved;
    private static FileChooser fileChooser;

    public static void newFile(String path, String text, boolean isColored) {

        Tab newTab = new Tab();
        tabs.add(newTab);
        filePaths.add((path == null) ? null : Paths.get(path));
        saved.add(path != null);
        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER);
        Label headerLabel = new Label((path == null) ? "* Untitled.java     " :
                new File(path).getName() + "     ");
        Button closeBtn = new Button("x");
        closeBtn.getStyleClass().add("close-button");
        closeBtn.setOnAction(event -> closeFile(newTab));
        header.getChildren().addAll(headerLabel, closeBtn);
        newTab.setGraphic(header);

        InlineCssTextArea textArea = new InlineCssTextArea();
        textArea.getStyleClass().add("inline-css-text-area");

        IntFunction<Node> lineNumberFactory = LineNumberFactory.get(textArea);
        IntFunction<Node> customLineNumberFactory = line -> {
            Node node = lineNumberFactory.apply(line);
            if (node instanceof Label label) {
                label.setFont(Font.font("Roboto", FontWeight.BOLD, 13));
                label.setAlignment(Pos.CENTER_RIGHT);
                label.setStyle("-fx-padding: 0 5 0 0; -fx-background-color: white;");
            }
            return node;
        };

        textArea.setParagraphGraphicFactory(customLineNumberFactory);
        if (isColored) {
            EditAreaUtility.addEventHandlers(textArea, newTab);
        }
        textArea.replaceText((text == null) ? "" : text);
        ContextMenu contextMenu = EditAreaUtility.getContextMenu(
                new Object[]{"Cut", KeyCode.X, 1},
                new Object[]{"Copy", KeyCode.C, 2},
                new Object[]{"Paste", KeyCode.V, 3}
        );
        textArea.setContextMenu(contextMenu);
        textArea.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(textArea, event.getScreenX(), event.getScreenY());
            }
        });
        EditAreaUtility.color(textArea);

        newTab.setContent(textArea);
        tabPane.getTabs().add(newTab);

        //Focus new tab.
        tabPane.getSelectionModel().select(newTab);
    }

    public static void closeFile(Tab tab) {

        if (tab == null) {
            tab = tabPane.getSelectionModel().getSelectedItem();
        }
        if (tab != null) {
            saved.remove(tabs.indexOf(tab));
            openFilesPaths.remove(filePaths.get(tabs.indexOf(tab)));
            filePaths.remove(tabs.indexOf(tab));
            tabs.remove(tab);
            tabPane.getTabs().remove(tab);
        }
    }

    public static void saveFile() {

        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        if (tab == null) {
            return;
        }

        InlineCssTextArea textArea = (InlineCssTextArea) tab.getContent();
        File file;
        if (tabs.contains(tab) && filePaths.get(tabs.indexOf(tab)) != null) {
            file = filePaths.get(tabs.indexOf(tab)).toFile();
        } else {
            fileChooser.setTitle("Save File");
            fileChooser.getExtensionFilters().removeAll();
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Java files", "*.java")
            );
            file = fileChooser.showSaveDialog(tabPane.getScene().getWindow());
            if (file != null) {
                openFilesPaths.add(file.toPath());
            }
        }
        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(textArea.getText());
                filePaths.set(tabs.indexOf(tab), file.toPath());
                saved.set(tabs.indexOf(tab), true);
                String fileName = file.getName();
                HBox header = (HBox) tab.getGraphic();
                header.getChildren().remove(0);
                header.getChildren().add(0, new Label(fileName + "     "));
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        } else {
            System.out.println("No File Selected");
        }
    }

    public static void openFile(Path path) {

        File file;
        if (path == null) {
            fileChooser.setTitle("Open File");
            fileChooser.getExtensionFilters().removeAll();
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Java files", "*.java")
            );
            file = fileChooser.showOpenDialog(tabPane.getScene().getWindow());
        } else {
            file = path.toFile();
        }

        if (filePaths.contains(file.toPath())) {
            tabPane.getSelectionModel().select(tabs.get(filePaths.indexOf(file.toPath())));
            return;
        }

        String[] splitName;
        if (file != null) {
            splitName = file.getName().split("\\.");
            if (!file.exists() || !file.isFile() || !file.canRead() ||
                    !file.canWrite() || (!splitName[splitName.length - 1].equals("java") &&
                    !splitName[splitName.length - 1].equals("txt"))) {
                return;
            }
        } else {
            return;
        }

        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line);
                text.append("\n");
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        openFilesPaths.add(file.toPath());
        newFile(file.getPath(), text.toString(),splitName[splitName.length - 1].equals("java"));

    }

    public static void setTabPane(TabPane tabPane) {

        FileManager.tabPane = tabPane;
    }

    public static void setOpenFilesPaths(ArrayList<Path> openFilesPaths) {

        FileManager.openFilesPaths = openFilesPaths;
    }

    public static void setTabs(ArrayList<Tab> tabs) {

        FileManager.tabs = tabs;
    }

    public static void setFilePaths(ArrayList<Path> filePaths) {

        FileManager.filePaths = filePaths;
    }

    public static void setSaved(ArrayList<Boolean> saved) {

        FileManager.saved = saved;
    }

    public static void setFileChooser(FileChooser fileChooser) {

        FileManager.fileChooser = fileChooser;
    }

}
