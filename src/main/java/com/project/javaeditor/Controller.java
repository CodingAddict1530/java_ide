package com.project.javaeditor;

import custom_classes.SettingsResult;
import java_code_processing.JavaCodeExecutor;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;

import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import custom_classes.ConsoleTextArea;
import managers.DirectoryManager;
import managers.FileManager;
import managers.TextManager;
import org.fxmisc.richtext.InlineCssTextArea;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import utility.*;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

import static utility.EditAreaUtility.addAccelerator;

public class Controller implements Initializable {

    @FXML
    private TabPane tabPane;
    @FXML
    private MenuItem newFile;
    @FXML
    private MenuItem openFile;
    @FXML
    private MenuItem saveFile;
    @FXML
    private MenuItem closeFile;
    @FXML
    private MenuItem cut;
    @FXML
    private MenuItem copy;
    @FXML
    private MenuItem paste;
    @FXML
    private MenuItem openProject;
    @FXML
    private VBox projectView;
    @FXML
    private SplitPane splitPane;
    @FXML
    private SplitPane verticalSplitPane;
    @FXML
    private HBox footer;
    @FXML
    private HBox console;

    private final ArrayList<Tab> tabs = new ArrayList<>();
    private final ArrayList<Path> filePaths = new ArrayList<>();
    private final ArrayList<Boolean> saved = new ArrayList<>();
    private final FileChooser fileChooser = new FileChooser();
    private final DirectoryChooser directoryChooser = new DirectoryChooser();
    private final ArrayList<Path> openProjectPath = new ArrayList<>();
    private final ArrayList<Path> openFilesPaths = new ArrayList<>();
    private SettingsResult settingsResult;

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        initializeManagers();

        addAccelerator(newFile, KeyCode.N);
        addAccelerator(openFile, KeyCode.O);
        addAccelerator(saveFile, KeyCode.S);
        addAccelerator(closeFile, KeyCode.Q);
        addAccelerator(cut, KeyCode.X);
        addAccelerator(copy, KeyCode.C);
        addAccelerator(paste, KeyCode.V);
        addAccelerator(openProject, KeyCode.P);

        footer.setAlignment(Pos.CENTER);
        FontIcon runIcon = new FontIcon(FontAwesomeSolid.PLAY);
        runIcon.setIconSize(20);
        runIcon.setIconColor(Color.GREEN);
        Button runBtn = new Button();
        runBtn.setGraphic(runIcon);
        runBtn.setOnAction(event -> run());
        FontIcon settingsIcon = new FontIcon(FontAwesomeSolid.COG);
        settingsIcon.setIconSize(20);
        //settingsIcon.setIconColor(Color.GREEN);
        Button settingsBtn = new Button();
        settingsBtn.setGraphic(settingsIcon);
        settingsBtn.setOnAction(event -> showSettings());
        footer.getChildren().add(runBtn);
        footer.getChildren().add(settingsBtn);

        splitPane.setDividerPositions(0.3);
        verticalSplitPane.setDividerPositions(1);
        setUpConsole();

    }

    @FXML
    public void newFile() {

        FileManager.newFile(null, null, true);
    }

    @FXML
    public void closeFile() {

        FileManager.closeFile(null);
    }

    @FXML
    public void saveFile() {

        FileManager.saveFile();
    }

    @FXML
    public void openFile() {

        FileManager.openFile(null);
    }

    @FXML
    public void openProject() {

        DirectoryManager.openProject(null);
    }

    @FXML
    public void copy() {

        TextManager.copy();
    }

    @FXML
    public void cut() {

        TextManager.cut();
    }

    @FXML
    public void paste() {

        TextManager.paste();
    }

    public void addPreviousContent(ArrayList<Path> paths) {

        if (paths != null) {
            DirectoryManager.openProject(paths.get(0));
            if (paths.size() > 1) {
                paths.remove(0);
                for (Path file : paths) {
                    FileManager.openFile(file);
                }
            }
        } else {
            FileManager.newFile(null, null, true);
        }

    }

    public void setUpConsole() {

        ConsoleTextArea textArea = new ConsoleTextArea();
        textArea.getStyleClass().add("inline-css-text-area-console");
        textArea.addEventFilter(KeyEvent.KEY_TYPED, event -> {

            int caretPos = textArea.getCaretPosition();
            String character = event.getCharacter();
            event.consume();
            if (!Objects.equals(character, "\b")) {
                textArea.insertText(caretPos, character);
                textArea.moveTo(caretPos + 1);
                if (character.matches("\\S")) {
                    textArea.setStyle(caretPos,
                            caretPos + 1, "-fx-fill: green;");
                }
            }
        });

        textArea.protectText();

        HBox.setHgrow(textArea, Priority.ALWAYS);
        console.getChildren().add(textArea);


    }

    public void showSettings() {

        String javaPath = SettingsUtility.getJavaPath();

        Dialog<SettingsResult> dialog = SettingsUtility.createSettingsDialog(javaPath);
        dialog.showAndWait().ifPresent(result -> settingsResult = result);

        System.out.println(settingsResult);

    }

    public void run() {

        verticalSplitPane.setDividerPositions(0.7);
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        InlineCssTextArea textArea = (InlineCssTextArea) tab.getContent();
        File file;
        if (filePaths.get(tabs.indexOf(tab)) == null) {
            file = new File("src/main/dummy/dummy.java");
            try {
                if (file.createNewFile()) {
                    try (FileWriter writer = new FileWriter(file)) {
                        writer.write(textArea.getText());
                    }
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return;
            }
        } else {
            file = filePaths.get(tabs.indexOf(tab)).toFile();
        }
        ConsoleTextArea consoleTextArea = (ConsoleTextArea) console.getChildren().get(0);
        consoleTextArea.unprotectText();
        consoleTextArea.replaceText("");
        consoleTextArea.protectText();
        System.out.println(JavaCodeExecutor.run(file, consoleTextArea));

    }

    public void initializeManagers() {

        FileManager.setFileChooser(fileChooser);
        FileManager.setTabs(tabs);
        FileManager.setFilePaths(filePaths);
        FileManager.setSaved(saved);
        FileManager.setOpenFilesPaths(openFilesPaths);
        FileManager.setTabPane(tabPane);

        DirectoryManager.setDirectoryChooser(directoryChooser);
        DirectoryManager.setOpenProjectPath(openProjectPath);
        DirectoryManager.setProjectView(projectView);
        DirectoryManager.setTabPane(tabPane);

        TextManager.setTabPane(tabPane);

        SettingsUtility.setDirectoryChooser(directoryChooser);
        SettingsUtility.setTabPane(tabPane);

        EditAreaUtility.setTabs(tabs);
        EditAreaUtility.setFilePaths(filePaths);
        EditAreaUtility.setSaved(saved);

        MainUtility.setOpenFilesPaths(openFilesPaths);
        MainUtility.setOpenProjectPath(openProjectPath);

    }

}