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

package com.project.javaeditor;

import com.project.custom_classes.SettingsResult;
import com.project.custom_classes.ConsoleTextArea;
import com.project.custom_classes.OpenFilesTracker;
import com.project.custom_classes.CustomFile;
import com.project.custom_classes.CustomTextArea;
import com.project.gradle.GradleWrapper;
import com.project.java_code_processing.JavaCodeExecutor;
import com.project.managers.DirectoryManager;
import com.project.managers.FileManager;
import com.project.managers.ProjectManager;
import com.project.managers.TextManager;
import com.project.managers.EditAreaManager;
import com.project.utility.MainUtility;
import com.project.utility.SettingsUtility;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TabPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static com.project.managers.EditAreaManager.addAccelerator;

/**
 * The controller of the main Scene.
 */
public class Controller implements Initializable {

    /**
     * TabPane containing edit areas.
     */
    @FXML
    private TabPane tabPane;

    /**
     * Creates new file.
     */
    @FXML
    private MenuItem newFile;

    /**
     * Opens a file.
     */
    @FXML
    private MenuItem openFile;

    /**
     * Saves current file.
     */
    @FXML
    private MenuItem saveFile;

    /**
     * Closes current file.
     */
    @FXML
    private MenuItem closeFile;

    /**
     * Cuts selected text or item.
     */
    @FXML
    private MenuItem cut;

    /**
     * Copies selected text or item.
     */
    @FXML
    private MenuItem copy;

    /**
     * Paste copied text or item.
     */
    @FXML
    private MenuItem paste;

    /**
     * Undoes an action in the code area.
     */
    @FXML
    private MenuItem undo;

    /**
     * Redoes an action in the code area.
     */
    @FXML
    private MenuItem redo;

    /**
     * Creates a new project.
     */
    @FXML
    private MenuItem newProject;

    /**
     * Opens a new project.
     */
    @FXML
    private MenuItem openProject;

    /**
     * Deletes the current project.
     */
    @FXML
    private MenuItem deleteProject;

    /**
     * Contains the tree view of the current project.
     */
    @FXML
    private VBox projectView;

    /**
     * Contains the projectView and the TabPane.
     */
    @FXML
    private SplitPane splitPane;

    /**
     * Contains the splitPane and the console.
     */
    @FXML
    private SplitPane verticalSplitPane;

    /**
     * The footer of the application.
     */
    @FXML
    private HBox footer;

    /**
     * Contains the ConsoleTextArea.
     */
    @FXML
    private HBox console;

    /**
     * Contains the menu options.
     */
    @FXML
    private MenuBar menuBar;

    /**
     * The part of the title bar with other buttons except close, minimize and maximize.
     */
    @FXML
    private HBox titleBarOptions;

    /**
     * The part of the title bar with close, minimize and maximize buttons.
     */
    @FXML
    private HBox titleBarButtons;

    /**
     * HBox used to create padding.
     */
    @FXML
    private HBox padHBox;

    /**
     * Displays the current project.
     */
    @FXML
    private Label projectName;

    /**
     * Contains the buttons related to running a program.
     */
    @FXML
    private HBox runOptions;

    /**
     * Contains the buttons related to settings.
     */
    @FXML
    private HBox settingsOptions;

    /**
     * Displays the path to the current file being edited.
     */
    @FXML
    private Label filePath;

    /**
     * The logger for the class.
     */
    private static final Logger logger = LogManager.getLogger(Controller.class);

    /**
     * A FileChooser Object to select files from device.
     */
    private static final FileChooser fileChooser = new FileChooser();

    /**
     * A DirectoryChooser Object to select directories from device.
     */
    private static final DirectoryChooser directoryChooser = new DirectoryChooser();

    /**
     * ArrayList containing the path of the currently open project.
     */
    private static final ArrayList<Path> openProjectPath = new ArrayList<>();

    /**
     * The system clipboard for copy, cut, and paste operations.
     */
    private static final Clipboard clipboard = Clipboard.getSystemClipboard();

    /**
     * Determines whether an action is a cut or copy.
     */
    private static final ArrayList<Boolean> shouldCut = new ArrayList<>();

    /**
     * Thread that checks what the current file being edited is and updates the filePath Label.
     */
    private static Thread filePathThread = null;

    /**
     * Contains the result from the settings Dialog.
     */
    private static SettingsResult settingsResult;

    /**
     * Determines whether filePathThread should keep running.
     */
    private static final AtomicReference<Boolean> keepRunning = new AtomicReference<>(false);

    /**
     * Initializes the scene.
     *
     * @param url The URL.
     * @param rb The ResourceBundle.
     */
    @FXML
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Setups
        setUpHeader();
        setUpFooter();
        setUpConsole();
        initializeManagers();

        // Add accelerators to menu items.
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

    /**
     * Creates a new file under src/main/java.
     * Does nothing if there is no current project.
     */
    @FXML
    public void newFile() {

        FileManager.newJavaClass(null);
    }

    /**
     * Closes the current file.
     */
    @FXML
    public void closeFile() {

        FileManager.closeFile(null);
    }

    /**
     * Saves the current file.
     */
    @FXML
    public void saveFile() {

        FileManager.saveFile(null);
    }

    /**
     * Opens a file.
     */
    @FXML
    public void openFile() {

        FileManager.openFile(null);
    }

    /**
     * Creates a new project.
     */
    @FXML
    public void newProject() {

        String output = MainUtility.quickDialog("New Project", "Enter project name");
        if (output == null) {
            return;
        }

        FileManager.closeAll();
        ProjectManager.createProject(output);

    }

    /**
     * Opens a project.
     */
    @FXML
    public void openProject() {

        FileManager.closeAll();
        ProjectManager.openProject(null);
    }

    /**
     * Deletes a project.
     */
    @FXML
    public void deleteProject() {

        ProjectManager.deleteProject();
    }

    /**
     * Copies the selected item or text.
     */
    @FXML
    public void copy() {

        TextManager.copy();
    }

    /**
     * Cuts the selected item or text.
     */
    @FXML
    public void cut() {

        TextManager.cut();
    }

    /**
     * Pastes the item or text in the clipboard.
     */
    @FXML
    public void paste() {

        TextManager.paste();
    }

    /**
     * Undoes an action in the textArea.
     */
    @FXML
    public void undo() {

        EditAreaManager.undoOrRedo((CustomTextArea) tabPane.getSelectionModel().getSelectedItem().getContent(), true);
    }

    /**
     * Redoes an action in the textArea.
     */
    @FXML
    public void redo() {

        EditAreaManager.undoOrRedo((CustomTextArea) tabPane.getSelectionModel().getSelectedItem().getContent(), false);
    }

    /**
     * Opens a link to Icons8 in a browser.
     */
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

    /**
     * Opens previous project and files.
     *
     * @param paths An array with the Paths.
     */
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
            }
        } catch (Exception e) {
            logger.error(e);
        }

    }

    /**
     * Sets up the console.
     */
    public void setUpConsole() {

        ConsoleTextArea textArea = new ConsoleTextArea();
        textArea.getStyleClass().add("inline-css-text-area-console");

        // Style user input with color green.
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

    /**
     * Shows a Dialog for settings.
     */
    public void showSettings() {

        String javaPath = SettingsUtility.getJavaPath();

        Dialog<SettingsResult> dialog = SettingsUtility.createSettingsDialog(javaPath);
        dialog.showAndWait().ifPresent(result -> settingsResult = result);

        System.out.println(settingsResult);

    }

    /**
     * Runs a file or the app.
     */
    public void run() {

        // Display the Console.
        verticalSplitPane.setDividerPositions(0.7);
        verticalSplitPane.getStyleClass().remove("vertical-split-pane");
        console.setMaxHeight(HBox.USE_COMPUTED_SIZE);

        // Get current tab.
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        if (tab == null) {
            return;
        }
        if (Boolean.FALSE.equals(OpenFilesTracker.isSaved(tab))) {

            // Save the file if it is not.
            saveFile();
        }

        // Get the file.
        CustomFile file = OpenFilesTracker.getOpenFile(tab).getFile();

        // Unprotect the ConsoleTextArea to add text to it, then re-protect it.
        ConsoleTextArea consoleTextArea = (ConsoleTextArea) console.getChildren().get(0);
        consoleTextArea.setEditable(true);
        consoleTextArea.unprotectText();
        consoleTextArea.replaceText("");
        consoleTextArea.protectText();

        // Check if file is part of the current project.
        if (file.toPath().startsWith(ProjectManager.getCurrentProject().getPath())) {

            // If so use gradle.
            GradleWrapper gradleWrapper = new GradleWrapper(new File("lib/gradle-8.9"), ProjectManager.getCurrentProject().getPath().toFile()
                    , consoleTextArea);
            gradleWrapper.runBuild();
            gradleWrapper.run(file.getPackageName());
        } else {

            // Otherwise use the JavaCodeExecutor.
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

    /**
     * Initialize managers with all the necessary fields.
     */
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

        ProjectManager.setConsoleTextArea((ConsoleTextArea) console.getChildren().get(0));
        ProjectManager.setProjectName(projectName);

        SettingsUtility.setDirectoryChooser(directoryChooser);
        SettingsUtility.setTabPane(tabPane);

        MainUtility.setOpenProjectPath(openProjectPath);

        JavaCodeExecutor.setConsoleTextArea((ConsoleTextArea) console.getChildren().get(0));

    }

    /**
     * Starts checking and updating filePath.
     */
    public void startFilePathThread() {

        // If already running, restart it.
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

                        // Use Javafx Thread for UI updates.
                        Platform.runLater(() -> filePath.setText(sb.toString()));
                    } else {

                        // Use Javafx Thread for UI updates.
                        Platform.runLater(() -> filePath.setText(ProjectManager.APP_HOME.toPath().toFile().getName()));
                    }

                    // Wait 2 seconds before checking again.
                    Thread.sleep(2000);
                } catch (Exception e) {
                    logger.error(e);
                }
            }

        });
        filePathThread.start();

    }

    /**
     * Stops the filePathThread.
     */
    public void stopFilePathThread() {

        if (filePathThread != null) {
            keepRunning.set(false);
        }
    }

    /**
     * Sets up the header of the application.
     */
    private void setUpHeader() {

        // Use JavaFX Thread to ensure Node being accessed have been initialized.
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

    /**
     * Sets up the footer of the application.
     */
    private void setUpFooter() {

        // Use JavaFX Thread to ensure Node being accessed have been initialized.
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
            EditAreaManager.setGoTo(goToBtn);
        });

    }

    /**
     * Enable the header to be used to drag the window.
     *
     * @param node A particular Node that is part of the header.
     * @param stage The primary stage.
     */
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
