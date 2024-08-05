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

import com.project.custom_classes.OpenFile;
import com.project.custom_classes.OpenFilesTracker;
import com.project.custom_classes.LanguageStatusParams;
import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.RegistrationParams;
import org.eclipse.lsp4j.UnregistrationParams;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams;
import org.eclipse.lsp4j.WorkspaceFoldersChangeEvent;
import org.eclipse.lsp4j.WillSaveTextDocumentParams;
import org.eclipse.lsp4j.TextDocumentSaveReason;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.SaveOptions;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.HoverOptions;
import org.eclipse.lsp4j.SignatureHelpOptions;
import org.eclipse.lsp4j.DiagnosticRegistrationOptions;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles language server related operations.
 */
public class JLSManager {

    /**
     * The logger for the class.
     */
    private static final Logger logger = LogManager.getLogger(JLSManager.class);

    /**
     * The language server interface.
     */
    private static LanguageServer languageServer;

    /**
     * Contains open projects.
     */
    private static final ArrayList<WorkspaceFolder> workspaceFolders = new ArrayList<>();

    /**
     * Stores the current number of resets the server has made.
     */
    private static final AtomicInteger numberOfResets = new AtomicInteger(0);

    /**
     * The language client.
     */
    private static final LanguageClient languageClient = new LanguageClient() {

        /**
         * Receives telemetry events from server.
         *
         * @param o The telemetry event.
         */
        @Override
        public void telemetryEvent(Object o) {
            try {
                System.out.println("Telemetry event: " + o);
            } catch (Exception e) {
                logger.error(e);
                resetServer();
                incrementNOR();
            }
        }

        /**
         * Receives diagnostics from server.
         *
         * @param diagnostics The diagnostics.
         */
        @Override
        public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
            try {
                handleDiagnostics(diagnostics);
            } catch (Exception e) {
                logger.error(e);
                resetServer();
                incrementNOR();
            }
        }

        /**
         * Receives messages from the server.
         *
         * @param messageParams The messages.
         */
        @Override
        public void showMessage(MessageParams messageParams) {
            try {
                System.out.println("Message: " + messageParams);
            } catch (Exception e) {
                logger.error(e);
                resetServer();
                incrementNOR();
            }
        }

        /**
         * Receives message requests from the server.
         *
         * @param requestParams The request parameters.
         * @return null.
         */
        @Override
        public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
            try {
                System.out.println("Message request: " + requestParams.getMessage());
            } catch (Exception e) {
                logger.error(e);
                resetServer();
                incrementNOR();
            }
            return null;
        }

        /**
         * Receives registered capabilities from the server.
         *
         * @param params The registration parameters.
         * @return null
         */
        @Override
        public CompletableFuture<Void> registerCapability(RegistrationParams params) {
            try {
                System.out.println("Registered Capabilities: " + params);
                return CompletableFuture.completedFuture(null);
            } catch (Exception e) {
                logger.error(e);
                resetServer();
                incrementNOR();
                return null;
            }
        }

        /**
         * Receives unregistered capabilities from the server.
         *
         * @param params The unregistration parameters.
         * @return null.
         */
        @Override
        public CompletableFuture<Void> unregisterCapability(UnregistrationParams params) {
            try {
                System.out.println("Unregistered Capabilities: " + params);
                return CompletableFuture.completedFuture(null);
            } catch (Exception e) {
                logger.error(e);
                resetServer();
                incrementNOR();
                return null;
            }
        }

        /**
         * Receives server request for workspace folders.
         *
         * @return The workspace folders.
         */
        @Override
        public CompletableFuture<java.util.List<WorkspaceFolder>> workspaceFolders() {
            try {
                return CompletableFuture.completedFuture(workspaceFolders);
            } catch (Exception e) {
                logger.error(e);
                resetServer();
                incrementNOR();
                return null;
            }
        }

        /**
         * Receives logs from the server.
         *
         * @param messageParams The message parameters.
         */
        @Override
        public void logMessage(MessageParams messageParams) {
            try {
                logger.info("Message: {}", messageParams);
            } catch (Exception e) {
                logger.error(e);
                resetServer();
                incrementNOR();
            }
        }

        /**
         * Receives status notifications from the server.
         *
         * @param params The status parameters.
         */
        @JsonNotification("language/status")
        public void languageStatus(LanguageStatusParams params) {
            try {
                System.out.println("Language status: " + params);
            } catch (Exception e) {
                logger.error(e);
                resetServer();
                incrementNOR();
            }
        }

        /**
         * Receives event notifications from the server.
         *
         * @param params The parameters.
         */
        @JsonNotification("language/eventNotification")
        public void languageEventNotification(Object params) {
            try {
                System.out.println("Language event notif: " + params);
            } catch (Exception e) {
                logger.error(e);
                resetServer();
                incrementNOR();
            }
        }

    };

    /**
     * Starts the server.
     */
    public static void startServer() {

        try {

            String config = "";

            // Determine what config file to use.
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                config = "config_win";
            } else if (System.getProperty("os.name").toLowerCase().contains("linux")) {
                if (System.getProperty("os.arch").toLowerCase().contains("arm")) {
                    config = "config_linux_arm";
                } else {
                    config = "config_linux";
                }
            } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                if (System.getProperty("os.arch").toLowerCase().contains("arm")) {
                    config = "config_mac_arm";
                } else {
                    config = "config_mac";
                }
            }
            if (config.isEmpty()) {
                logger.fatal("OS NOT DETERMINED!");
                return;
            }
            String[] command = {
                    "java",
                    "-Declipse.application=org.eclipse.jdt.ls.core.id1",
                    "-Dosgi.bundles.defaultStartLevel=4",
                    "-Declipse.product=org.eclipse.jdt.ls.core.product",
                    "-Dlog.level=ALL",
                    "-Xmx1G",
                    "--add-modules=ALL-SYSTEM",
                    "--add-opens", "java.base/java.util=ALL-UNNAMED",
                    "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                    "-jar", "lib/jdt-language-server-1.37.0-202406271335/plugins/org.eclipse.equinox.launcher_1.6.900.v20240613-2009.jar",
                    "-configuration", "lib/jdt-language-server-1.37.0-202406271335/" + config,
                    "-data", "jdt_data"
            };
            ProcessBuilder processBuilder = new ProcessBuilder(command);

            // Redirect error stream to output.
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            InputStream inputStream = process.getInputStream();
            OutputStream outputStream = process.getOutputStream();

            // Launch the server.
            Launcher<LanguageServer> launcher = LSPLauncher.createClientLauncher(languageClient, inputStream, outputStream);
            launcher.startListening();
            languageServer = launcher.getRemoteProxy();
        } catch (Exception e) {
            logger.error(e);
        }

    }

    /**
     * Stops the server.
     */
    public static void stopServer() {

        // Send didClose notification for each file closed.
        for (OpenFile o : OpenFilesTracker.getOpenFiles()) {
            JLSManager.didClose(o.getFile().toPath());
        }
        try {
            if (languageServer != null) {
                languageServer.shutdown().get();
                languageServer.exit();
            }
        }catch (Exception e) {
            System.out.println(e.getMessage());
            logger.error(e);
        }

    }

    /**
     * Initializes the server.
     */
    public static void initializeServer() {

        InitializeParams initializeParams = new InitializeParams();
        long pid = ProcessHandle.current().pid();

        // Check if process id can be safely downcast.
        if (pid > Integer.MAX_VALUE || pid < Integer.MIN_VALUE) {
            System.out.println("YIKES! PID: " + pid);
            logger.fatal("PID: {} cant be safely converted to int", pid);
            return;
        }
        initializeParams.setProcessId((int) pid);

        // Send initialize request.
        CompletableFuture<InitializeResult> initializeFuture = languageServer.initialize(initializeParams);
        initializeFuture.thenAccept(result -> {
            logger.info("Initialization successful. Capabilities: {}", result.getCapabilities());

            // Register capabilities.
            registerSync();
            registerCompletion();
            registerHover();
            registerSignatureHelp();
            registerDiagnostic();

            languageServer.initialized(new InitializedParams());
        }).exceptionally(throwable -> {
            logger.fatal("Initialization failed: {}", throwable.getMessage());
            return null;
        });

    }

    /**
     * Notifies the server that a file has been opened.
     *
     * @param path The Path to the file.
     * @param content The current contents of the file.
     */
    public static void didOpen(Path path, String content) {

        TextDocumentItem tdi = new TextDocumentItem();
        tdi.setUri(path.toUri().toString());
        tdi.setLanguageId("java");
        tdi.setVersion(1);
        tdi.setText(content == null ? "" : content);

        languageServer.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(tdi));

    }

    /**
     * Notifies the server that a file has changed.
     *
     * @param path The Path to the file.
     * @param content The contents of the file.
     * @param version The version of the file (Increment the previous version).
     */
    public static void didChange(Path path, String content, int version) {

        TextDocumentItem tdi = new TextDocumentItem();
        tdi.setUri(path.toUri().toString());
        tdi.setLanguageId("java");
        tdi.setVersion(version);
        tdi.setText(content == null ? "" : content);

        VersionedTextDocumentIdentifier textDocumentIdentifier = new VersionedTextDocumentIdentifier(path.toUri().toString(), version);

        TextDocumentContentChangeEvent changeEvent = new TextDocumentContentChangeEvent();
        changeEvent.setText(content);

        DidChangeTextDocumentParams params = new DidChangeTextDocumentParams();
        params.setTextDocument(textDocumentIdentifier);
        params.setContentChanges(Collections.singletonList(changeEvent));

        languageServer.getTextDocumentService().didChange(params);

    }

    /**
     * Notifies the server that a file was closed.
     *
     * @param path The Path to the file.
     */
    public static void didClose(Path path) {

        TextDocumentIdentifier textDocumentIdentifier = new TextDocumentIdentifier(path.toUri().toString());
        DidCloseTextDocumentParams params = new DidCloseTextDocumentParams(textDocumentIdentifier);
        languageServer.getTextDocumentService().didClose(params);

    }

    /**
     * Requests the server to complete code.
     *
     * @param path The Path to the file.
     * @param position The position in the file.
     * @return A list completion items.
     */
    public static List<CompletionItem> complete(Path path, Position position) {

        CountDownLatch latch = new CountDownLatch(1);

        CompletionParams completionParams = new CompletionParams();
        completionParams.setTextDocument(new TextDocumentIdentifier(path.toUri().toString()));
        completionParams.setPosition(position);

        final List<CompletionItem>[] returnValue = new List[]{null};

        // Request completion.
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> futureCompletion = languageServer.getTextDocumentService().completion(completionParams);
        futureCompletion.thenAccept(completionItems -> {
            if (completionItems != null) {

                // Process the completion items
                returnValue[0] = completionItems.isLeft() ? completionItems.getLeft() : completionItems.getRight().getItems();
            } else {
                System.out.println("No completion items found.");
            }

            // Release the thread.
            latch.countDown();
        }).exceptionally(ex -> {
            System.err.println("Error while fetching completion items: " + ex.getMessage());

            // Release the thread.
            latch.countDown();
            return null;
        });
        try {

            // Wait for the completion items to be returned by the server.
            latch.await();
        } catch (Exception e) {
            logger.error(e);
        }
        return returnValue[0];

    }

    /**
     * Requests the server for signature helps.
     *
     * @param path The Path to the file.
     * @param position The position in the file.
     * @return A SignatureHelp Object.
     */
    public static SignatureHelp getSignatureHelp(Path path, Position position) {

        CountDownLatch latch = new CountDownLatch(1);

        SignatureHelpParams params = new SignatureHelpParams();
        params.setTextDocument(new TextDocumentIdentifier(path.toUri().toString()));
        params.setPosition(position);

        final SignatureHelp[] returnValue = new SignatureHelp[1];

        // Send request to server.
        CompletableFuture<SignatureHelp> futureSignatureHelp = languageServer.getTextDocumentService().signatureHelp(params);
        futureSignatureHelp.thenAccept(signatureHelp -> {
            if (signatureHelp != null) {

                // Process the signature help
                returnValue[0] = signatureHelp;
            } else {
                System.out.println("No signature help available.");
            }

            // Release the thread.
            latch.countDown();
        }).exceptionally(ex -> {
            System.err.println("Error while fetching signature help: " + ex.getMessage());

            // Release the thread.
            latch.countDown();
            return null;
        });

        try {

            // Wait for completion items to be returned by the server.
            latch.await();
        } catch (InterruptedException e) {
            logger.error(e);
        }

        return returnValue[0];

    }

    /**
     * Requests the server do details about a token hovered upon.
     *
     * @param path The Path to the file.
     * @param position The position in the file.
     * @return A Hover Object with the details.
     */
    public static Hover getHover(Path path, Position position) {

        CountDownLatch latch = new CountDownLatch(1);

        HoverParams params = new HoverParams();
        params.setTextDocument(new TextDocumentIdentifier(path.toUri().toString()));
        params.setPosition(position);

        final Hover[] returnValue = new Hover[1];

        // Send the request.
        CompletableFuture<Hover> futureHover = languageServer.getTextDocumentService().hover(params);
        futureHover.thenAccept(hover -> {
            if (hover != null) {

                // Process the hover information
                returnValue[0] = hover;
            } else {
                System.out.println("No hover information available.");
            }

            // Release the thread.
            latch.countDown();
        }).exceptionally(ex -> {
            System.err.println("Error while fetching hover information: " + ex.getMessage());

            // Release the thread.
            latch.countDown();
            return null;
        });

        try {

            // Wait for the server to return the information.
            latch.await();
        } catch (InterruptedException e) {
            logger.error(e);
        }

        return returnValue[0];

    }

    /**
     * Request server to change the workspace folder.
     *
     * @param uri The URI to the folder.
     * @param name The name to identify the workspace folder.
     * @param add Whether to add or remove the folder.
     */
    public static void changeWorkSpaceFolder(String uri, String name, boolean add) {

        boolean found = false;

        // Check whether

        for (WorkspaceFolder workspaceFolder : workspaceFolders) {
            if (add && workspaceFolder.getUri().equals(uri)) {
                return;
            } else if (!add && workspaceFolder.getUri().equals(uri)) {
                found = true;
                break;
            }
        }
        if (!found && !add) {
            return;
        }
        if (add) {
            workspaceFolders.add(new WorkspaceFolder(uri, name));
        } else {
            for (WorkspaceFolder workspaceFolder : workspaceFolders) {
                if (workspaceFolder.getUri().equals(uri)) {
                    workspaceFolders.remove(new WorkspaceFolder(uri, name));
                    break;
                }
            }

        }
        sendDCWFN(add);

    }

    /**
     * Sends a did change workspace folder notification to the server.
     *
     * @param add Whether a workspace folder was added or removed.
     */
    public static void sendDCWFN(boolean add) {

        DidChangeWorkspaceFoldersParams params = new DidChangeWorkspaceFoldersParams();
        WorkspaceFoldersChangeEvent event = new WorkspaceFoldersChangeEvent();
        if (add) {
            event.setAdded(workspaceFolders);
            event.setRemoved(Collections.emptyList());
        } else {
            event.setAdded(Collections.emptyList());
            event.setRemoved(workspaceFolders);
        }
        params.setEvent(event);

        languageServer.getWorkspaceService().didChangeWorkspaceFolders(params);

    }

    /**
     * Sends a willSave notification to the server.
     *
     * @param uri The URI of the file.
     */
    public static void sendWillSave(String uri) {

        WillSaveTextDocumentParams params = new WillSaveTextDocumentParams();
        params.setTextDocument(new TextDocumentIdentifier(uri));

        // Reason for the save event
        params.setReason(TextDocumentSaveReason.Manual);

        languageServer.getTextDocumentService().willSave(params);

    }

    /**
     * Sends a didSave notification to the server.
     *
     * @param uri The URI of the file.
     * @param content The contents of the file.
     */
    public static void sendDidSave(String uri, String content) {

        DidSaveTextDocumentParams params = new DidSaveTextDocumentParams();
        params.setText(content);
        params.setTextDocument(new TextDocumentIdentifier(uri));

        languageServer.getTextDocumentService().didSave(params);

    }

    /**
     * Notifies the server that a file has been deleted
     *
     * @param uri The URI of the file.
     */
    public static void sendDeletedFile(String uri) {

        FileEvent fileEvent = new FileEvent(uri, FileChangeType.Deleted);
        DidChangeWatchedFilesParams params = new DidChangeWatchedFilesParams(Collections.singletonList(fileEvent));

        languageServer.getWorkspaceService().didChangeWatchedFiles(params);

    }

    /**
     * Handles diagnostics from the server.
     *
     * @param diagnostics The Diagnostics.
     */
    private static void handleDiagnostics(PublishDiagnosticsParams diagnostics) {

        List<Diagnostic> diagnosticList = diagnostics.getDiagnostics();

        // Ignore empty diagnostics.
        if (diagnosticList.isEmpty()) {
            return;
        }
        for (Diagnostic diagnostic : diagnosticList) {

            // Use JavaFX thread since changes involve UI update.
            Platform.runLater(() -> {
                try {
                    EditAreaManager.processDiagnostic(diagnostic, Paths.get(new URI(diagnostics.getUri())));
                } catch (Exception e) {
                    logger.error(e);
                }
            });
        }

    }

    /**
     * Restarts the server.
     */
    private static void resetServer() {

        FileManager.saveFiles(OpenFilesTracker.getOpenFiles());
        stopServer();
        startServer();
        for (OpenFile o: OpenFilesTracker.getOpenFiles()) {
            StringBuilder text = new StringBuilder();

            // Read the contents of the file.
            ArrayList<String> lines = FileManager.readFile(o.getFile().toPath());
            if (lines != null) {
                for (String line : lines) {
                    text.append(line).append("\n");
                }
            }

            // Send didOpen notification to the server.
            didOpen(o.getFile().toPath(), text.toString());
        }

    }

    /**
     * Registers synchronization capabilities.
     */
    public static void registerSync() {

        TextDocumentSyncOptions syncOptions = new TextDocumentSyncOptions();
        syncOptions.setOpenClose(true);
        syncOptions.setChange(TextDocumentSyncKind.Incremental);
        syncOptions.setWillSave(true);
        syncOptions.setWillSaveWaitUntil(false);
        syncOptions.setSave(new SaveOptions(true));

        // Define the registration parameters
        RegistrationParams params = new RegistrationParams();
        params.setRegistrations(Collections.singletonList(
                new Registration(
                        "syncCapabilities", // Unique ID for the capability
                        "textDocumentSync",
                        syncOptions
                )
        ));

        languageClient.registerCapability(params);

    }

    /**
     * Registers completion capabilities.
     */
    public static void registerCompletion() {

        CompletionOptions completionOptions = new CompletionOptions();
        completionOptions.setResolveProvider(true);
        completionOptions.setTriggerCharacters(getCompletionCharacters());

        // Define the registration parameters
        RegistrationParams params = new RegistrationParams();
        params.setRegistrations(Collections.singletonList(
                new Registration(
                        "completionCapability", // Unique ID for the capability
                        "textDocument/completion",
                        completionOptions
                )
        ));

        languageClient.registerCapability(params);

    }

    /**
     * Registers hover capabilities.
     */
    public static void registerHover() {

        RegistrationParams params = new RegistrationParams();
        params.setRegistrations(Collections.singletonList(
                new Registration(
                        "hoverCapability", // Unique ID for the capability
                        "textDocument/hover",
                        new HoverOptions()
                )
        ));

        languageClient.registerCapability(params);

    }

    /**
     * Registers signature help capabilities.
     */
    public static void registerSignatureHelp() {

        SignatureHelpOptions signatureHelpOptions = new SignatureHelpOptions();
        signatureHelpOptions.setTriggerCharacters(Arrays.asList("(", ","));

        // Define the registration parameters
        RegistrationParams params = new RegistrationParams();
        params.setRegistrations(Collections.singletonList(
                new Registration(
                        "signatureHelpCapability", // Unique ID for the capability
                        "textDocument/signatureHelp",
                        signatureHelpOptions
                )
        ));

        languageClient.registerCapability(params);

    }

    /**
     * Registers diagnostic capabilities.
     */
    public static void registerDiagnostic() {

        RegistrationParams params = new RegistrationParams();
        params.setRegistrations(Collections.singletonList(
                new Registration(
                        "diagnosticCapability", // Unique ID for the capability
                        "textDocument/publishDiagnostics",
                        new DiagnosticRegistrationOptions()
                )
        ));

        languageClient.registerCapability(params);

    }

    /**
     * Increments numberOfResets.
     * Stops the server it the number of resets exceeds 5.
     */
    private static void incrementNOR() {

        if (numberOfResets.get() > 5) {
            stopServer();
            System.out.println("SERVER ERROR!");
        }

        numberOfResets.incrementAndGet();

    }

    /**
     * Generates a list of characters to trigger completions.
     *
     * @return The List.
     */
    private static List<String> getCompletionCharacters() {

        List<String> list = new ArrayList<>();

        // Add digits 0-9
        for (char c = '0'; c <= '9'; c++) {
            list.add(String.valueOf(c));
        }

        // Add uppercase letters A-Z
        for (char c = 'A'; c <= 'Z'; c++) {
            list.add(String.valueOf(c));
        }

        // Add lowercase letters a-z
        for (char c = 'a'; c <= 'z'; c++) {
            list.add(String.valueOf(c));
        }

        list.addAll(List.of(".", "(", "\"", "'", ":", "@", "[", "{", " "));

        return list;

    }

}
