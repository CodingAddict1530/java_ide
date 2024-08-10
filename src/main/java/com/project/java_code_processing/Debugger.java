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

package com.project.java_code_processing;

import com.project.custom_classes.BreakPoint;
import com.project.managers.EditAreaManager;
import com.project.managers.FileManager;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.StackFrame;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.StepRequest;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * Attaches to JVM and debugs.
 */
public class Debugger {

    /**
     * The logger for the class.
     */
    private static final Logger logger = LogManager.getLogger(Debugger.class);

    /**
     * The address the JVM is running on (127.0.0.1).
     */
    public static final String HOSTNAME = "localhost";

    /**
     * The port number the JVM is listening on.
     */
    private final int port;

    /**
     * A Virtual Machine for debugging.
     */
    private VirtualMachine vm;

    /**
     * A Map storing all breakpoints.
     */
    private final Map<Label, BreakPoint> bpMap = FileManager.getBpMap();

    /**
     * Whether the debugger has finished debugging or not.
     */
    private boolean hasFinished = false;

    /**
     * The Node that displays current variable values while debugging.
     */
    private final ScrollPane variableArea;

    /**
     * Used for synchronization.
     */
    private CountDownLatch latch;

    /**
     * Stores the notification sent to the debugger.
     */
    private String debugNotification;

    /**
     * Stores the current StepRequest.
     */
    private StepRequest stepRequest;

    /**
     * Stores thread specific information from the VirtualMachine.
     */
    private ThreadReference thread;

    /**
     * Holds the debugger events.
     */
    private EventQueue queue;

    /**
     * Instantiates a new Debugger.
     *
     * @param port The port number the JVM is listening on.
     * @param variableArea The Node that displays current variable values while debugging.
     */
    public Debugger(int port, ScrollPane variableArea) {

        this.port = port;
        this.variableArea = variableArea;
    }

    /**
     * Carries out the whole process.
     */
    public void run() {

        // Run on a new thread to avoid blocking the main thread.
        new Thread(() -> {
            try {

                // Attach to the JVM.
                vm = attach();
                if (vm == null) {
                    logger.error("Failed to attach virtual machine");
                    System.err.println("Failed to attach virtual machine");
                    return;
                }

                // Collect all classes that have breakpoints.
                Set<String> classSet = new HashSet<>();
                for (BreakPoint bp : bpMap.values()) {
                    classSet.add(bp.className());
                }

                // Set ClassPrepareRequests for each class.
                // This makes the JVM notify the debugger that it has loaded the class.
                for (String className : classSet) {
                    setClassPrepareRequest(vm, className);
                }

                // Start debugging.
                queue = vm.eventQueue();
                debug();

                // Clean up
                vm.dispose();

                // Clear the variableArea and remove highlightings.
                Platform.runLater(() -> {
                    ((VBox) variableArea.getContent()).getChildren().clear();
                    EditAreaManager.clearDebugCanvases();
                });

                // Indicate that debugging is done.
                hasFinished = true;
            } catch (Exception e) {
                logger.error(e);
            }
        }).start();

    }

    /**
     * Attaches to the running JVM.
     *
     * @return A VirtualMachine for debugging.
     * @throws Exception when it fails to attach for a particular reason.
     */
    private VirtualMachine attach() throws Exception {

        VirtualMachineManager vmManager = Bootstrap.virtualMachineManager();

        // Find the socket connector.
        AttachingConnector connector = vmManager.attachingConnectors().stream()
                .filter(c -> c.name().equals("com.sun.jdi.SocketAttach"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No socket attaching connector found"));

        Map<String, Connector.Argument> arguments = connector.defaultArguments();
        arguments.get("hostname").setValue(HOSTNAME);
        arguments.get("port").setValue(String.valueOf(port));

        return connector.attach(arguments);
    }

    /**
     * Sets a ClassPrepareRequest.
     * This makes the JVM notify the debugger when a class has been loaded.
     *
     * @param vm The VirtualMachine.
     * @param className The class.
     */
    public void setClassPrepareRequest(VirtualMachine vm, String className) {

        ClassPrepareRequest classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest();
        classPrepareRequest.addClassFilter(className);
        classPrepareRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        classPrepareRequest.enable();

    }

    /**
     * Sets all the breakpoints in the particular class.
     *
     * @param className The class.
     * @throws Exception when it fails to set breakpoints for a particular reason.
     */
    private void setBreakpoints(String className) throws Exception {

        for (BreakPoint bp : bpMap.values()) {

            // Filter out for only the breakpoints for the class.
            if (!bp.className().equals(className)) {
                continue;
            }

            // Try to find the loaded class.
            List<ReferenceType> classes = vm.classesByName(className);
            if (classes.isEmpty()) {
                logger.error("No classes found for {}", bp.className());
                continue;
            }

            ReferenceType classRef = classes.get(0);
            Location location = classRef.locationsOfLine(bp.lineNumber()).get(0);
            BreakpointRequest bpRequest = vm.eventRequestManager().createBreakpointRequest(location);
            bpRequest.enable();
            logger.info("Breakpoint set for class {}, lineNumber {}", bp.className(), bp.lineNumber());

        }

    }

    /**
     * Main method that carries out the debugging.
     *
     * @throws Exception when something goes wrong.
     */
    private void debug() throws Exception {

        latch = new CountDownLatch(1);
        while (true) {
            EventSet eventSet = queue.remove();
            for (Event event : eventSet) {
                if (event instanceof BreakpointEvent || event instanceof StepEvent) {

                    if (event instanceof BreakpointEvent) {
                        thread = ((BreakpointEvent) event).thread();

                        // Highlight the current line in the source file.
                        if (!EditAreaManager.highlightDebugLine(((BreakpointEvent) event).location())) {
                            logger.error("Class not recognized");
                        }
                    } else {
                        thread = ((StepEvent) event).thread();

                        // Highlight the current line in the source file.
                        if (!EditAreaManager.highlightDebugLine(((StepEvent) event).location())) {
                            logger.error("Class not recognized");
                        }
                    }
                    handleStop();

                    // Pause execution until debugger receives a notification.
                    latch.await();

                    // Reset the CountDownLatch.
                    latch = new CountDownLatch(1);
                    if (debugNotification == null || debugNotification.isEmpty()) {
                        continue;
                    }
                    switch (debugNotification) {
                        case "step-into":
                            createStepRequest(StepRequest.STEP_INTO);
                            break;
                        case "step-out":
                            createStepRequest(StepRequest.STEP_OUT);
                            break;
                        case "step-over":
                            createStepRequest(StepRequest.STEP_OVER);
                            break;
                        case "continue":
                            if (vm != null && stepRequest != null) {

                                // Remove the existing StepRequest if any.
                                vm.eventRequestManager().deleteEventRequest(stepRequest);
                                Platform.runLater(EditAreaManager::clearDebugCanvases);
                            }

                            // Clear the variableArea.
                            Platform.runLater(() -> ((VBox) variableArea.getContent()).getChildren().clear());
                            break;
                    }
                    debugNotification = null;
                } else if (event instanceof ClassPrepareEvent cpe) {

                    // Set breakpoints for the class that has been loaded.
                    setBreakpoints(cpe.referenceType().name());
                } else if (event instanceof VMDeathEvent || event instanceof VMDisconnectEvent) {

                    // JVM has terminated, so end the debugging session.
                    logger.info("JVM terminated or disconnected due to {}", event);
                    return;
                }
            }
            eventSet.resume();
        }

    }

    /**
     * Updates the variableArea.
     *
     * @throws Exception when something goes wrong.
     */
    public void handleStop() throws Exception {


        // Get the current stack frame
        StackFrame frame = thread.frame(0);

        // Get local variables from the current stack frame
        List<LocalVariable> variables = frame.visibleVariables();

        // Update variable Area on the JavaFX Thread.
        Platform.runLater(() -> {
            ((VBox) variableArea.getContent()).getChildren().clear();
            for (LocalVariable variable : variables) {
                Label label = new Label(variable.name() + " : " + frame.getValue(variable).toString());
                label.getStyleClass().add("debug-variable");
                ((VBox) variableArea.getContent()).getChildren().add(label);
            }
        });

    }

    /**
     * Creates a Step Request.
     *
     * @param stepSize Refers to the kind of step (Step_INTO, STEP_OVER...).
     */
    public void createStepRequest(int stepSize) {

        // Remove previous Step Requests since there can only be one at a time.
        if (stepRequest != null) {
            vm.eventRequestManager().deleteEventRequest(stepRequest);
        }
        stepRequest = vm.eventRequestManager().createStepRequest(thread, StepRequest.STEP_LINE, stepSize);
        stepRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        stepRequest.enable();

    }

    /**
     * Returns a tooltip with the call stack inside.
     *
     * @return The ToolTip.
     * @throws Exception When something goes wrong.
     */
    public Tooltip getCallStack() throws Exception {

        Tooltip tooltip = new Tooltip();
        GridPane gridPane = new GridPane();
        gridPane.setStyle("-fx-background: #424746;-fx-fill: white");
        for (int i = 0; i < thread.frames().size(); i++) {
            Label lineLabel = new Label(thread.frames().get(i).toString());
            lineLabel.getStyleClass().add("debug-variable");

            gridPane.add(lineLabel, 0, i);

        }

        ScrollPane scrollPane = new ScrollPane(gridPane);
        scrollPane.setMaxWidth(300); // Set the preferred width for the scrollable area
        scrollPane.setMaxHeight(300); // Set the preferred height for the scrollable area

        // Customize ScrollPane appearance
        scrollPane.setStyle("-fx-background: #424746;"); // Make background transparent
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Hide horizontal scrollbar
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        tooltip.setGraphic(scrollPane);
        return tooltip;

    }

    /**
     * Receives notifications and releases the execution of code.
     *
     * @param notification The notification (step-into, step-out, step-over, continue).
     */
    public void getNotification(String notification) {

        debugNotification = notification;
        latch.countDown();
    }

    /**
     * Returns whether the debug session has finished or not.
     *
     * @return The state.
     */
    public boolean hasFinished() {

        return hasFinished;
    }

}
