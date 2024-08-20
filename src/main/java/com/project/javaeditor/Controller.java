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
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
     * Contains the console components.
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
     * Divides the variableArea and the consoleTextArea.
     */
    @FXML
    private SplitPane consoleSplitPane;

    /**
     * Contains the buttons to control the debug session.
     */
    @FXML
    private HBox debugHeader;

    /**
     * The Node that displays current variable values while debugging.
     */
    @FXML
    private ScrollPane variableArea;

    /**
     * The logger for the class.
     */
    private static final Logger logger = LoggerFactory.getLogger(Controller.class);

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
     * The InlineCssTextArea used for the console.
     */
    private static ConsoleTextArea cTextArea;

    /**
     * A GradleWrapper to execute gradle tasks.
     */
    private static GradleWrapper gradleWrapper;

    /**
     * An Event Filter to hide call stack Tooltip.
     */
    private static EventHandler<MouseEvent> clickEF = null;

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
        setUpDebugArea();
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
        verticalSplitPane.getStyleClass().add("vertical-split-pane1");
        verticalSplitPane.setDividerPositions(1);

        debugHeader.setStyle("-fx-background-color: #424746");
        variableArea.getStyleClass().add("variable-area");
        startFilePathThread();

        MainUtility.installTooltip("Name of current project", projectName);

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
        ProjectManager.openProject(null, null);
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

        EditAreaManager.undoOrRedo((CustomTextArea) ((StackPane) tabPane.getSelectionModel().getSelectedItem().getContent()).getChildren().get(0), true);
    }

    /**
     * Redoes an action in the textArea.
     */
    @FXML
    public void redo() {

        EditAreaManager.undoOrRedo((CustomTextArea) ((StackPane) tabPane.getSelectionModel().getSelectedItem().getContent()).getChildren().get(0), false);
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
            logger.error(e.getMessage());
        }
    }

    /**
     * Opens previous project and files.
     *
     * @param paths An array with the Paths.
     */
    public void addPreviousContent(ArrayList<Path> paths) {

        if (paths != null && !paths.isEmpty()) {
            ProjectManager.openProject(paths.get(0), null);
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

    }

    /**
     * Sets up the console.
     */
    public void setUpConsole() {

        cTextArea = new ConsoleTextArea();
        cTextArea.getStyleClass().add("inline-css-text-area-console");
        cTextArea.protectText();
        cTextArea.setEditable(false);
        HBox.setHgrow(cTextArea, Priority.ALWAYS);
        console.getChildren().clear();
        console.getChildren().add(cTextArea);
        logger.info("Console setup complete");

    }

    /**
     * Sets up the Debug zone.
     */
    public void setUpDebugArea() {

        ImageView callStackIcon = new ImageView(
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/call-stack.png"))));
        MainUtility.sizeImage(callStackIcon, 20, 20);
        Button callStackBtn = new Button();
        callStackBtn.getStyleClass().add("runOptionBtn");
        callStackBtn.setGraphic(callStackIcon);

        // Listen for when the button is clicked.
        callStackBtn.setOnAction(event -> {
            Tooltip callStack;

            // Try getting the call stack from either of the executors.
            if ((callStack = gradleWrapper.getCallStack()) != null || (callStack = JavaCodeExecutor.getCallStack()) != null) {
                Tooltip finalCallStack = callStack;
                finalCallStack.show(
                        callStackBtn.getScene().getWindow(),
                        callStackBtn.localToScreen(callStackBtn.getLayoutBounds().getMaxX(), callStackBtn.getLayoutBounds().getMinY()).getX(),
                        callStackBtn.localToScreen(callStackBtn.getLayoutBounds().getMaxX(), callStackBtn.getLayoutBounds().getMinY()).getY()
                );
                clickEF = event2 -> {
                    if (event2.getTarget() == callStackBtn) {
                        return;
                    }
                    finalCallStack.hide();
                    callStackBtn.getScene().removeEventFilter(MouseEvent.MOUSE_CLICKED, clickEF);
                };
                callStackBtn.getScene().addEventFilter(MouseEvent.MOUSE_CLICKED, clickEF);
            }

        });

        // Install an informational tooltip.
        MainUtility.installTooltip("Show Call Stack", callStackBtn);

        ImageView stepIntoIcon = new ImageView(
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/step-into.png"))));
        MainUtility.sizeImage(stepIntoIcon, 20, 20);
        Button stepIntoBtn = new Button();
        stepIntoBtn.getStyleClass().add("runOptionBtn");
        stepIntoBtn.setGraphic(stepIntoIcon);

        // Listen for when the button is clicked.
        stepIntoBtn.setOnAction(event -> {
            gradleWrapper.notifyDebugger("step-into");
            JavaCodeExecutor.notifyDebugger("step-into");
        });

        // Install an informational tooltip.
        MainUtility.installTooltip("Step Into", stepIntoBtn);

        ImageView stepOverIcon = new ImageView(
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/step-over.png"))));
        MainUtility.sizeImage(stepOverIcon, 20, 20);
        Button stepOverBtn = new Button();
        stepOverBtn.getStyleClass().add("runOptionBtn");
        stepOverBtn.setGraphic(stepOverIcon);

        // Listen for when the button is clicked.
        stepOverBtn.setOnAction(event -> {
            gradleWrapper.notifyDebugger("step-over");
            JavaCodeExecutor.notifyDebugger("step-over");
        });

        // Install an informational tooltip.
        MainUtility.installTooltip("Step Over", stepOverBtn);

        ImageView stepOutIcon = new ImageView(
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/step-out.png"))));
        MainUtility.sizeImage(stepOutIcon, 20, 20);
        Button stepOutBtn = new Button();
        stepOutBtn.getStyleClass().add("runOptionBtn");
        stepOutBtn.setGraphic(stepOutIcon);

        // Listen for when the button is clicked.
        stepOutBtn.setOnAction(event -> {
            gradleWrapper.notifyDebugger("step-out");
            JavaCodeExecutor.notifyDebugger("step-out");
        });

        // Install an informational tooltip.
        MainUtility.installTooltip("Step Out", stepOutBtn);

        ImageView continueIcon = new ImageView(
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/play.png"))));
        MainUtility.sizeImage(continueIcon, 20, 20);
        Button continueBtn = new Button();
        continueBtn.getStyleClass().add("runOptionBtn");
        continueBtn.setGraphic(continueIcon);

        // Listen for when the button is clicked.
        continueBtn.setOnAction(event -> {
            gradleWrapper.notifyDebugger("continue");
            JavaCodeExecutor.notifyDebugger("continue");
        });

        // Install an informational tooltip.
        MainUtility.installTooltip("Continue Execution", continueBtn);

        debugHeader.getChildren().addAll(callStackBtn, stepOverBtn, stepIntoBtn, stepOutBtn, continueBtn);

    }

    /**
     * Shows a Dialog for settings.
     */
    public void showSettings() {

        String javaPath = SettingsUtility.getJavaPath().toString();

        Dialog<SettingsResult> dialog = SettingsUtility.createSettingsDialog(javaPath);
        dialog.showAndWait().ifPresent(result -> settingsResult = result);

        //MainUtility.popup(new Label(settingsResult.toString()));

    }

    /**
     * Runs a file or the app.
     */
    public void run(String command) {

        // Adjust the console area depending on the task.
        if (command.equals("debug")) {
            console.getChildren().clear();
            HBox hBox = new HBox();
            hBox.getChildren().add(cTextArea);
            console.getChildren().clear();
            HBox.setHgrow(cTextArea, Priority.ALWAYS);
            if (consoleSplitPane.getItems().size() > 1) {
                consoleSplitPane.getItems().remove(1);
            }
            consoleSplitPane.getItems().add(hBox);
            console.getChildren().add(consoleSplitPane);
            consoleSplitPane.setDividerPositions(0.3);
        } else {
            console.getChildren().clear();
            console.getChildren().add(cTextArea);
        }

        // Display the Console.
        verticalSplitPane.setDividerPositions(0.7);
        verticalSplitPane.getStyleClass().remove("vertical-split-pane1");
        verticalSplitPane.getStyleClass().add("vertical-split-pane2");
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
        cTextArea.setEditable(true);
        cTextArea.unprotectText();
        cTextArea.replaceText("");
        cTextArea.protectText();

        // Check if file is part of the current project and is a java file.
        if (ProjectManager.getCurrentProject() != null && file.toPath().startsWith(ProjectManager.getCurrentProject().getPath()) &&
                (file.toPath().toString().endsWith(".java") || file.toPath().toString().endsWith("build.gradle"))) {

            // If so use gradle.
            gradleWrapper = new GradleWrapper(new File("lib/gradle-8.9"), ProjectManager.getCurrentProject().getPath().toFile(),
                    cTextArea);
            gradleWrapper.setVariableArea(variableArea);
            Thread runThread = new Thread(() -> {
                try {

                    // Check whether it is run or build command.
                    if (command.equals("build")) {
                        gradleWrapper.runBuild();
                    } else if (file.toPath().toString().endsWith("build.gradle")) {
                        MainUtility.popup(new Label("Can't run this file. Try build..."));
                    } else {
                        gradleWrapper.run(file.getPackageName(), command);
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            });
            runThread.setDaemon(true);
            runThread.start();
        } else {

            MainUtility.popup(new Label("Class not in project!"));
            // Otherwise use the JavaCodeExecutor.
            /*
            new Thread(() -> {
                switch (JavaCodeExecutor.run(file, null, command)) {
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
            }).start().setDaemon;
            */
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
        ProjectManager.setConsole(console);
        ProjectManager.setCTextArea(cTextArea);
        ProjectManager.setVerticalSplitPane(verticalSplitPane);


        SettingsUtility.setDirectoryChooser(directoryChooser);
        SettingsUtility.setTabPane(tabPane);

        MainUtility.setOpenProjectPath(openProjectPath);

        JavaCodeExecutor.setConsoleTextArea((ConsoleTextArea) console.getChildren().get(0));
        JavaCodeExecutor.setVariableArea(variableArea);

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
                    logger.error(e.getMessage());
                }
            }

        });
        filePathThread.setDaemon(true);
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

            // Listen for when the button is clicked.
            settingsBtn.setOnAction(event -> showSettings());

            // Install an informational tooltip.
            MainUtility.installTooltip("Settings", settingsBtn);

            ImageView buildIcon = new ImageView(
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/build.png"))));
            MainUtility.sizeImage(buildIcon, 20, 20);
            Button buildBtn = new Button();
            buildBtn.getStyleClass().add("runOptionBtn");
            buildBtn.setGraphic(buildIcon);

            // Listen for when the button is clicked.
            buildBtn.setOnAction(event -> run("build"));

            // Install an informational tooltip.
            MainUtility.installTooltip("Build", buildBtn);

            settingsOptions.getChildren().addAll(settingsBtn, buildBtn);

            ImageView runIcon = new ImageView(
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/play.png"))));
            MainUtility.sizeImage(runIcon, 20, 20);
            Button runBtn = new Button();
            runBtn.getStyleClass().add("runOptionBtn");
            runBtn.setGraphic(runIcon);

            // Listen for when the button is clicked.
            runBtn.setOnAction(event -> run("run"));

            // Install an informational tooltip.
            MainUtility.installTooltip("Run", runBtn);

            ImageView debugIcon = new ImageView(
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/debug.png"))));
            MainUtility.sizeImage(debugIcon, 20, 20);
            Button debugBtn = new Button();
            debugBtn.getStyleClass().add("runOptionBtn");
            debugBtn.setGraphic(debugIcon);

            // Listen for when the button is clicked.
            debugBtn.setOnAction(event -> run("debug"));

            // Install an informational tooltip.
            MainUtility.installTooltip("Debug", debugBtn);

            ImageView stopIcon = new ImageView(
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/stop.png"))));
            MainUtility.sizeImage(stopIcon, 20, 20);
            Button stopBtn = new Button();
            stopBtn.getStyleClass().add("runOptionBtn");
            stopBtn.setGraphic(stopIcon);

            // Listen for when the button is clicked.
            stopBtn.setOnAction(event -> {
                JavaCodeExecutor.terminate();
                gradleWrapper.terminate();
            });

            // Install an informational tooltip.
            MainUtility.installTooltip("Stop", stopBtn);

            ImageView toggleConsoleIcon = new ImageView(
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/console.png"))));
            MainUtility.sizeImage(toggleConsoleIcon, 20, 20);
            Button toggleConsoleBtn = new Button();
            toggleConsoleBtn.getStyleClass().add("runOptionBtn");
            toggleConsoleBtn.setGraphic(toggleConsoleIcon);

            // Listen for when the button is clicked.
            toggleConsoleBtn.setOnAction(event -> {

                // Adjust the visibility of the console.
                if (verticalSplitPane.getStyleClass().contains("vertical-split-pane1")) {
                    verticalSplitPane.getStyleClass().remove("vertical-split-pane1");
                    verticalSplitPane.getStyleClass().add("vertical-split-pane2");
                    verticalSplitPane.setDividerPositions(0.7);
                    console.setMaxHeight(HBox.USE_COMPUTED_SIZE);
                    if (console.getChildren().isEmpty()) {
                        console.getChildren().add(consoleSplitPane);
                    }
                } else {
                    verticalSplitPane.getStyleClass().remove("vertical-split-pane2");
                    verticalSplitPane.getStyleClass().add("vertical-split-pane1");
                    verticalSplitPane.setDividerPositions(1.0);
                    console.setMaxHeight(0);
                    if (console.getChildren().get(0) == consoleSplitPane) {
                        console.getChildren().remove(0);
                    }
                }
            });

            // Install an informational tooltip.
            MainUtility.installTooltip("Toggle Console", toggleConsoleBtn);

            runOptions.getChildren().addAll(runBtn, debugBtn, stopBtn, toggleConsoleBtn);

            Button btnClose = new Button();
            ImageView closeImage = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/close.png"))));
            MainUtility.sizeImage(closeImage, 10, 10);
            btnClose.setGraphic(closeImage);
            btnClose.getStyleClass().add("topBarBtn");
            btnClose.setStyle("-fx-background-color: #B82E2E;");
            btnClose.setOnAction(event -> {

                if (MainUtility.confirm("Application about to close!", "Are you sure you want to exit?")) {
                    ApplicationView.checkForUnsavedFiles();
                    stage.close();
                }

            });

            // Install an informational tooltip.
            MainUtility.installTooltip("Close", btnClose);

            Button btnMinimize = new Button();
            ImageView minImage =new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/min.png"))));
            MainUtility.sizeImage(minImage, 10, 10);
            btnMinimize.setGraphic(minImage);
            btnMinimize.getStyleClass().add("topBarBtn");
            btnMinimize.setStyle("-fx-background-color: #3CB542;");
            btnMinimize.setOnAction(event -> stage.setIconified(true));

            // Install an informational tooltip.
            MainUtility.installTooltip("Minimize", btnMinimize);

            Button btnMaximize = new Button();
            ImageView maxImage = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/max.png"))));
            MainUtility.sizeImage(maxImage, 10, 10);
            btnMaximize.setGraphic(maxImage);
            btnMaximize.getStyleClass().add("topBarBtn");
            btnMaximize.setStyle("-fx-background-color: #B89C2E;");
            btnMaximize.setOnAction(event -> stage.setMaximized(!stage.isMaximized()));

            // Install an informational tooltip.
            MainUtility.installTooltip("Maximize", btnMaximize);

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

            // Install an informational tooltip.
            MainUtility.installTooltip("Go To", goToBtn);
            Button lineSeparatorBtn = new Button("LF");
            lineSeparatorBtn.getStyleClass().add("footerBtn");

            // Install an informational tooltip.
            MainUtility.installTooltip("Line Separator", lineSeparatorBtn);
            Button charEncodingBtn = new Button("UTF-8");
            charEncodingBtn.getStyleClass().add("footerBtn");

            // Install an informational tooltip.
            MainUtility.installTooltip("Character Encoding", charEncodingBtn);
            Button indentSpaceBtn = new Button("4 Spaces");
            indentSpaceBtn.getStyleClass().add("footerBtn");

            // Install an informational tooltip.
            MainUtility.installTooltip("Number of indent spaces", indentSpaceBtn);
            Button readOnlyToggleBtn = new Button();
            readOnlyToggleBtn.getStyleClass().add("footerBtn");

            // Install an informational tooltip.
            MainUtility.installTooltip("Toggle readonly", readOnlyToggleBtn);
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
