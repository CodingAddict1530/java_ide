package com.project.javaeditor;

import com.project.custom_classes.CustomFile;
import com.project.custom_classes.OpenFilesTracker;
import com.project.custom_classes.SettingsResult;
import com.project.gradle.GradleWrapper;
import com.project.java_code_processing.JavaCodeExecutor;
import com.project.managers.*;
import com.project.utility.EditAreaUtility;
import com.project.utility.MainUtility;
import com.project.utility.SettingsUtility;
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
import com.project.custom_classes.ConsoleTextArea;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

import static com.project.utility.EditAreaUtility.addAccelerator;

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
    private MenuItem newProject;
    @FXML
    private MenuItem openProject;
    @FXML
    private MenuItem deleteProject;
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

    private static final Logger logger = LogManager.getLogger(Controller.class);

    private static final FileChooser fileChooser = new FileChooser();
    private static final DirectoryChooser directoryChooser = new DirectoryChooser();
    private static final ArrayList<Path> openProjectPath = new ArrayList<>();
    private static final Clipboard clipboard = Clipboard.getSystemClipboard();
    private static final ArrayList<Boolean> shouldCut = new ArrayList<>();
    private static SettingsResult settingsResult;

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        setUpConsole();
        initializeManagers();
        logger.info("Manager shared attributes set!");

        addAccelerator(newFile, KeyCode.N);
        addAccelerator(openFile, KeyCode.O);
        addAccelerator(saveFile, KeyCode.S);
        addAccelerator(closeFile, KeyCode.Q);
        addAccelerator(cut, KeyCode.X);
        addAccelerator(copy, KeyCode.C);
        addAccelerator(paste, KeyCode.V);
        addAccelerator(newProject, KeyCode.N, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
        addAccelerator(openProject, KeyCode.O, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
        addAccelerator(deleteProject, KeyCode.Q, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
        logger.info("Accelerators added");

        footer.setAlignment(Pos.CENTER);
        FontIcon runIcon = new FontIcon(FontAwesomeSolid.PLAY);
        runIcon.setIconSize(20);
        runIcon.setIconColor(Color.GREEN);
        Button runBtn = new Button();
        runBtn.setGraphic(runIcon);
        runBtn.setOnAction(event -> run());
        FontIcon settingsIcon = new FontIcon(FontAwesomeSolid.COG);
        settingsIcon.setIconSize(20);

        Button settingsBtn = new Button();
        settingsBtn.setGraphic(settingsIcon);
        settingsBtn.setOnAction(event -> showSettings());
        footer.getChildren().add(runBtn);
        footer.getChildren().add(settingsBtn);

        splitPane.setDividerPositions(0.3);
        verticalSplitPane.setDividerPositions(1);

    }

    @FXML
    public void newFile() {

        Application.fadeStage();
        //FileManager.newFile(null, null, true);
    }

    @FXML
    public void closeFile() {

        FileManager.closeFile(null);
    }

    @FXML
    public void saveFile() {

        FileManager.saveFile(null);
    }

    @FXML
    public void openFile() {

        FileManager.openFile(null);
    }

    @FXML
    public void newProject() {

        String output = MainUtility.quickDialog("New Project", "Enter project name");
        if (output == null) {
            return;
        }

        FileManager.closeAll();
        ProjectManager.createProject(output);

    }

    @FXML
    public void openProject() {

        FileManager.closeAll();
        ProjectManager.openProject(null);
    }

    @FXML
    public void deleteProject() {

        ProjectManager.deleteProject();
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

        try {
            if (paths != null) {
                ProjectManager.openProject(paths.get(0));
                if (paths.size() > 1) {
                    paths.remove(0);
                    for (Path file : paths) {
                        FileManager.openFile(file);
                    }
                }
                logger.info("Previous content added");
            } else {
                logger.info("No previous content");
                FileManager.newFile(null, null, true);
            }
        } catch (Exception e) {
            logger.error(e);
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
        logger.info("Console setup complete");

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
        if (tab == null) {
            return;
        }
        if (Boolean.FALSE.equals(OpenFilesTracker.isSaved(tab))) {
            saveFile();
        }
        CustomFile file = OpenFilesTracker.getOpenFile(tab).getFile();
        ConsoleTextArea consoleTextArea = (ConsoleTextArea) console.getChildren().get(0);
        consoleTextArea.unprotectText();
        consoleTextArea.replaceText("");
        consoleTextArea.protectText();
        GradleWrapper gradleWrapper = new GradleWrapper(new File("lib/gradle-8.9"), ProjectManager.getCurrentProject().getPath().toFile()
            , consoleTextArea);
        gradleWrapper.runBuild();
        gradleWrapper.run(file.getPackageName());
        /*
        switch (JavaCodeExecutor.run(file, ProjectManager.getCurrentProject())) {
            case 1:
                logger.info("Couldn't read {} to execute", file.getPath());
                break;
            case 2:
                logger.info("Compilation of {} failed", file.getPath());
                break;
            case 3:
                logger.info("Couldn't delete .class file of {}", file.getPath());
                break;
        }

         */

    }

    public void initializeManagers() {

        FileManager.setFileChooser(fileChooser);
        FileManager.setTabPane(tabPane);
        FileManager.setConsole(console);
        FileManager.setVerticalSplitPane(verticalSplitPane);
        FileManager.setOpenProjectPath(openProjectPath);
        FileManager.setClipboard(clipboard);
        FileManager.setShouldCut(shouldCut);

        DirectoryManager.setDirectoryChooser(directoryChooser);
        DirectoryManager.setOpenProjectPath(openProjectPath);
        DirectoryManager.setProjectView(projectView);
        DirectoryManager.setTabPane(tabPane);
        DirectoryManager.setClipboard(clipboard);
        DirectoryManager.setShouldCut(shouldCut);

        TextManager.setTabPane(tabPane);
        TextManager.setClipboard(clipboard);

        ProjectManager.setTextArea((ConsoleTextArea) console.getChildren().get(0));

        SettingsUtility.setDirectoryChooser(directoryChooser);
        SettingsUtility.setTabPane(tabPane);

        MainUtility.setOpenProjectPath(openProjectPath);

        JavaCodeExecutor.setConsoleTextArea((ConsoleTextArea) console.getChildren().get(0));

    }

}