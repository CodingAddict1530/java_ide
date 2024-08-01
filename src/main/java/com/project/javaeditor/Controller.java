package com.project.javaeditor;

import com.project.custom_classes.*;
import com.project.gradle.GradleWrapper;
import com.project.java_code_processing.JavaCodeExecutor;
import com.project.managers.*;
import com.project.utility.EditAreaUtility;
import com.project.utility.MainUtility;
import com.project.utility.ProjectWatcher;
import com.project.utility.SettingsUtility;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;

import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

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
    private MenuItem undo;
    @FXML
    private MenuItem redo;
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
    @FXML
    private MenuBar menuBar;
    @FXML
    private HBox titleBarOptions;
    @FXML
    private HBox titleBarButtons;
    @FXML
    private HBox padHBox;
    @FXML
    private Label projectName;
    @FXML
    private HBox runOptions;
    @FXML
    private HBox settingsOptions;
    @FXML
    private Label filePath;

    private static final Logger logger = LogManager.getLogger(Controller.class);

    private static final FileChooser fileChooser = new FileChooser();
    private static final DirectoryChooser directoryChooser = new DirectoryChooser();
    private static final ArrayList<Path> openProjectPath = new ArrayList<>();
    private static final Clipboard clipboard = Clipboard.getSystemClipboard();
    private static final ArrayList<Boolean> shouldCut = new ArrayList<>();
    private static Thread filePathThread = null;
    private static SettingsResult settingsResult;
    private static final AtomicReference<Boolean> keepRunning = new AtomicReference<>(false);

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        setUpHeader();
        setUpFooter();

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
        addAccelerator(undo, KeyCode.Z);
        addAccelerator(redo,  KeyCode.Z, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
        addAccelerator(newProject, KeyCode.N, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
        addAccelerator(openProject, KeyCode.O, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
        addAccelerator(deleteProject, KeyCode.Q, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
        logger.info("Accelerators added");

        footer.setAlignment(Pos.CENTER);

        projectView.getStyleClass().add("tab-pane");

        splitPane.setDividerPositions(0.3);
        verticalSplitPane.getStyleClass().add("vertical-split-pane");
        verticalSplitPane.setDividerPositions(1);

        startFilePathThread();

    }

    @FXML
    public void newFile() {

        FileManager.newJavaClass(null);
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

    @FXML
    public void undo() {

        EditAreaUtility.undo((CustomTextArea) tabPane.getSelectionModel().getSelectedItem().getContent(), true);
    }

    @FXML
    public void redo() {

        EditAreaUtility.undo((CustomTextArea) tabPane.getSelectionModel().getSelectedItem().getContent(), false);
    }

    @FXML
    public void openIcons8Link() {

        try {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(new URI("https://icons8.com/"));
            }
        } catch (Exception e) {
            logger.error(e);
        }
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
        textArea.setEditable(false);
        console.setMaxHeight(0);

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
        verticalSplitPane.getStyleClass().remove("vertical-split-pane");
        console.setMaxHeight(HBox.USE_COMPUTED_SIZE);
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        if (tab == null) {
            return;
        }
        if (Boolean.FALSE.equals(OpenFilesTracker.isSaved(tab))) {
            saveFile();
        }
        CustomFile file = OpenFilesTracker.getOpenFile(tab).getFile();
        ConsoleTextArea consoleTextArea = (ConsoleTextArea) console.getChildren().get(0);
        consoleTextArea.setEditable(true);
        consoleTextArea.unprotectText();
        consoleTextArea.replaceText("");
        consoleTextArea.protectText();
        if (file.toPath().startsWith(ProjectManager.getCurrentProject().getPath())) {
            GradleWrapper gradleWrapper = new GradleWrapper(new File("lib/gradle-8.9"), ProjectManager.getCurrentProject().getPath().toFile()
                    , consoleTextArea);
            gradleWrapper.runBuild();
            gradleWrapper.run(file.getPackageName());
        } else {
            switch (JavaCodeExecutor.run(file, null)) {
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
        }

    }

    public void initializeManagers() {

        FileManager.setFileChooser(fileChooser);
        FileManager.setTabPane(tabPane);
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
        ProjectManager.setProjectName(projectName);

        SettingsUtility.setDirectoryChooser(directoryChooser);
        SettingsUtility.setTabPane(tabPane);

        MainUtility.setOpenProjectPath(openProjectPath);

        JavaCodeExecutor.setConsoleTextArea((ConsoleTextArea) console.getChildren().get(0));

    }

    public void startFilePathThread() {

        if (filePathThread != null) {
            keepRunning.set(false);
        }
        keepRunning.set(true);
        filePathThread = new Thread(() -> {

            while (keepRunning.get()) {
                try {
                    if (tabPane.getSelectionModel().getSelectedItem() != null) {
                        String path = OpenFilesTracker.getOpenFile(tabPane.getSelectionModel().getSelectedItem()).getFile().getPath();
                        String[] parts = path.split("\\\\");
                        StringBuilder sb = new StringBuilder();
                        for (int i = parts.length - 1; i >= 0; i--) {
                            if (!parts[i].equals("FusionProjects")) {
                                sb.append(parts[i]).append("  <  ");
                            } else {
                                break;
                            }
                        }
                        if (filePath.getText().isEmpty()) {
                            for (int i = parts.length - 1; i >= 0; i--) {
                                sb.append(parts[i]).append("  <  ");
                            }
                        }
                        for (int i = 0; i < 5; i++) {
                            sb.deleteCharAt(sb.length() - 1);
                        }
                        Platform.runLater(() -> filePath.setText(sb.toString()));
                    } else {
                        Platform.runLater(() -> filePath.setText(ProjectManager.APP_HOME.toPath().toFile().getName()));
                    }
                    Thread.sleep(2000);
                } catch (Exception e) {
                    logger.error(e);
                }
            }

        });
        filePathThread.start();

    }

    public void stopFilePathThread() {

        if (filePathThread != null) {
            keepRunning.set(false);
        }
    }

    private void setUpHeader() {

        Platform.runLater(() -> {
            Stage stage = (Stage) tabPane.getScene().getWindow();

            titleBarOptions.getStyleClass().add("menu-bar");
            titleBarButtons.getStyleClass().add("menu-bar");
            padHBox.getStyleClass().add("menu-bar");
            titleBarOptions.setAlignment(Pos.CENTER);
            titleBarButtons.setAlignment(Pos.CENTER);
            settingsOptions.setAlignment(Pos.CENTER);
            runOptions.setAlignment(Pos.CENTER);
            projectName.setStyle("-fx-text-fill: white;-fx-font-size: 16px;-fx-font-weight: bold;");

            ImageView settingsIcon = new ImageView(
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/settings.png"))));
            MainUtility.sizeImage(settingsIcon, 20, 20);
            Button settingsBtn = new Button();
            settingsBtn.getStyleClass().add("runOptionBtn");
            settingsBtn.setGraphic(settingsIcon);
            settingsBtn.setOnAction(event -> showSettings());

            settingsOptions.getChildren().add(settingsBtn);

            ImageView runIcon = new ImageView(
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/play.png"))));
            MainUtility.sizeImage(runIcon, 20, 20);
            Button runBtn = new Button();
            runBtn.getStyleClass().add("runOptionBtn");
            runBtn.setGraphic(runIcon);
            runBtn.setOnAction(event -> run());

            ImageView debugIcon = new ImageView(
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/debug.png"))));
            MainUtility.sizeImage(debugIcon, 20, 20);
            Button debugBtn = new Button();
            debugBtn.getStyleClass().add("runOptionBtn");
            debugBtn.setGraphic(debugIcon);
            debugBtn.setOnAction(event -> run());

            ImageView stopIcon = new ImageView(
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/stop.png"))));
            MainUtility.sizeImage(stopIcon, 20, 20);
            Button stopBtn = new Button();
            stopBtn.getStyleClass().add("runOptionBtn");
            stopBtn.setGraphic(stopIcon);
            stopBtn.setOnAction(event -> run());

            runOptions.getChildren().addAll(runBtn, debugBtn, stopBtn);

            Button btnClose = new Button();
            ImageView closeImage = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/close.png"))));
            MainUtility.sizeImage(closeImage, 10, 10);
            btnClose.setGraphic(closeImage);
            btnClose.getStyleClass().add("topBarBtn");
            btnClose.setStyle("-fx-background-color: #B82E2E;");
            btnClose.setOnAction(event -> {
                EventHandler<WindowEvent> closeRequestHandler = stage.getOnCloseRequest();
                if (closeRequestHandler != null) {
                    // Create a new WindowEvent
                    WindowEvent windowEvent = new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST);
                    // Fire the close request event
                    closeRequestHandler.handle(windowEvent);

                    // If the event is not consumed, close the stage
                    if (!windowEvent.isConsumed()) {
                        stage.close();
                    }
                } else {
                    stage.close();
                }
            });

            Button btnMinimize = new Button();
            ImageView minImage =new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/min.png"))));
            MainUtility.sizeImage(minImage, 10, 10);
            btnMinimize.setGraphic(minImage);
            btnMinimize.getStyleClass().add("topBarBtn");
            btnMinimize.setStyle("-fx-background-color: #3CB542;");
            btnMinimize.setOnAction(event -> stage.setIconified(true));

            Button btnMaximize = new Button();
            ImageView maxImage = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/max.png"))));
            MainUtility.sizeImage(maxImage, 10, 10);
            btnMaximize.setGraphic(maxImage);
            btnMaximize.getStyleClass().add("topBarBtn");
            btnMaximize.setStyle("-fx-background-color: #B89C2E;");
            btnMaximize.setOnAction(event -> stage.setMaximized(!stage.isMaximized()));

            HBox spacing1 = new HBox();
            spacing1.setPrefWidth(5);
            HBox spacing2 = new HBox();
            spacing2.setPrefWidth(5);
            titleBarButtons.getChildren().addAll(btnMinimize, spacing1, btnMaximize, spacing2, btnClose);

            // Allow dragging the window
            setUpTopBar(menuBar, stage);
            setUpTopBar(titleBarOptions, stage);
            setUpTopBar(titleBarButtons, stage);
        });

    }

    private void setUpFooter() {

        Platform.runLater(() -> {
            footer.getStyleClass().add("menu-bar");
            filePath.setStyle("-fx-text-fill: #f0f0f0;-fx-font-size: 15px;");
            filePath.setText(ProjectManager.APP_HOME.toPath().toFile().getName());
            Button goToBtn = new Button("433:33");
            goToBtn.getStyleClass().add("footerBtn");
            Button lineSeparatorBtn = new Button("LF");
            lineSeparatorBtn.getStyleClass().add("footerBtn");
            Button charEncodingBtn = new Button("UTF-8");
            charEncodingBtn.getStyleClass().add("footerBtn");
            Button indentSpaceBtn = new Button("4 Spaces");
            indentSpaceBtn.getStyleClass().add("footerBtn");
            Button readOnlyToggleBtn = new Button();
            readOnlyToggleBtn.getStyleClass().add("footerBtn");
            ImageView padlockIcon = new ImageView(
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/padlock-open.png"))));
            MainUtility.sizeImage(padlockIcon, 15, 15);
            readOnlyToggleBtn.setGraphic(padlockIcon);
            footer.getChildren().addAll(goToBtn, lineSeparatorBtn, charEncodingBtn, indentSpaceBtn, readOnlyToggleBtn);
            EditAreaUtility.setGoTo(goToBtn);
        });

    }

    private void setUpTopBar(Node node, Stage stage) {

        AtomicReference<Double> xOffset = new AtomicReference<>();
        AtomicReference<Double> yOffset = new AtomicReference<>();
        node.setOnMousePressed(event -> {
            xOffset.set(event.getSceneX());
            yOffset.set(event.getSceneY());
        });
        node.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset.get());
            stage.setY(event.getScreenY() - yOffset.get());
        });

    }

}