package com.project.managers;

import com.project.custom_classes.OpenFile;
import com.project.custom_classes.OpenFilesTracker;
import com.project.utility.EditAreaUtility;
import com.project.utility.LanguageStatusParams;
import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class JLSManager {

    private static final Logger logger = LogManager.getLogger(JLSManager.class);

    private static LanguageServer languageServer;
    private static final ArrayList<WorkspaceFolder> workspaceFolders = new ArrayList<>();
    private static final LanguageClient languageClient = new LanguageClient() {

        @Override
        public void telemetryEvent(Object o) {
            try {
                System.out.println("Telemetry event: " + o);
            } catch (Exception e) {
                logger.error(e);
                resetServer();
            }
        }

        @Override
        public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
            try {
                handleDiagnostics(diagnostics);
            } catch (Exception e) {
                logger.error(e);
                resetServer();
            }
        }

        @Override
        public void showMessage(MessageParams messageParams) {
            try {
                System.out.println("Message: " + messageParams);
            } catch (Exception e) {
                logger.error(e);
                resetServer();
            }
        }

        @Override
        public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
            try {
                System.out.println("Message request: " + requestParams.getMessage());
            } catch (Exception e) {
                logger.error(e);
                resetServer();
            }
            return null;
        }

        @Override
        public CompletableFuture<Void> registerCapability(RegistrationParams params) {
            try {
                System.out.println("Registered Capabilities: " + params);
                return CompletableFuture.completedFuture(null);
            } catch (Exception e) {
                logger.error(e);
                resetServer();
                return null;
            }
        }

        @Override
        public CompletableFuture<Void> unregisterCapability(UnregistrationParams params) {
            try {
                System.out.println("Unregistered Capabilities: " + params);
                return CompletableFuture.completedFuture(null);
            } catch (Exception e) {
                logger.error(e);
                resetServer();
                return null;
            }
        }

        @Override
        public CompletableFuture<java.util.List<WorkspaceFolder>> workspaceFolders() {
            try {
                return CompletableFuture.completedFuture(workspaceFolders);
            } catch (Exception e) {
                logger.error(e);
                resetServer();
                return null;
            }
        }

        @Override
        public void logMessage(MessageParams messageParams) {
            try {
                logger.info("Message: {}", messageParams);
            } catch (Exception e) {
                logger.error(e);
                resetServer();
            }
        }

        @JsonNotification("language/status")
        public void languageStatus(LanguageStatusParams params) {
            try {
                System.out.println("Language status: " + params);
            } catch (Exception e) {
                logger.error(e);
                resetServer();
            }
        }

        @JsonNotification("language/eventNotification")
        public void languageEventNotification(Object params) {
            try {
                System.out.println("Language event notif: " + params);
            } catch (Exception e) {
                logger.error(e);
                resetServer();
            }
        }

    };

    public static void startServer() {

        try {

            String config = "";
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
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            InputStream inputStream = process.getInputStream();
            OutputStream outputStream = process.getOutputStream();

            Launcher<LanguageServer> launcher = LSPLauncher.createClientLauncher(languageClient, inputStream, outputStream);
            launcher.startListening();
            languageServer = launcher.getRemoteProxy();
        } catch (IOException e) {
            logger.error(e);
        }

    }

    public static void stopServer() {

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

    public static void initializeServer() {

        InitializeParams initializeParams = new InitializeParams();
        long pid = ProcessHandle.current().pid();
        if (pid > Integer.MAX_VALUE || pid < Integer.MIN_VALUE) {
            System.out.println("YIKES! PID: " + pid);
            logger.fatal("PID: {} cant be safely converted to int", pid);
            return;
        }
        initializeParams.setProcessId((int) pid);

        CompletableFuture<InitializeResult> initializeFuture = languageServer.initialize(initializeParams);
        initializeFuture.thenAccept(result -> {
            logger.info("Initialization successful. Capabilities: {}", result.getCapabilities());

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

    public static void didOpen(Path path, String content) {

        TextDocumentItem tdi = new TextDocumentItem();
        tdi.setUri(path.toUri().toString());
        tdi.setLanguageId("java");
        tdi.setVersion(1);
        tdi.setText(content == null ? "" : content);

        languageServer.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(tdi));

    }

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

    public static void didClose(Path path) {

        TextDocumentIdentifier textDocumentIdentifier = new TextDocumentIdentifier(path.toUri().toString());
        DidCloseTextDocumentParams params = new DidCloseTextDocumentParams(textDocumentIdentifier);
        languageServer.getTextDocumentService().didClose(params);

    }

    public static List<CompletionItem> complete(Path path, Position position) {

        CountDownLatch latch = new CountDownLatch(1);

        CompletionParams completionParams = new CompletionParams();
        completionParams.setTextDocument(new TextDocumentIdentifier(path.toUri().toString()));
        completionParams.setPosition(position);

        final List<CompletionItem>[] returnValue = new List[]{null};
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> futureCompletion = languageServer.getTextDocumentService().completion(completionParams);
        // Handle the response
        futureCompletion.thenAccept(completionItems -> {
            if (completionItems != null) {
                // Process the completion items
                returnValue[0] = completionItems.isLeft() ? completionItems.getLeft() : completionItems.getRight().getItems();
                latch.countDown();
            } else {
                System.out.println("No completion items found.");
                latch.countDown();
            }
        }).exceptionally(ex -> {
            System.err.println("Error while fetching completion items: " + ex.getMessage());
            latch.countDown();
            return null;
        });
        try {
            latch.await();
        } catch (Exception e) {
            logger.error(e);
        }
        return returnValue[0];

    }

    public static SignatureHelp getSignatureHelp(Path path, Position position) {

        CountDownLatch latch = new CountDownLatch(1);

        SignatureHelpParams params = new SignatureHelpParams();
        params.setTextDocument(new TextDocumentIdentifier(path.toUri().toString()));
        params.setPosition(position);

        final SignatureHelp[] returnValue = new SignatureHelp[1];
        CompletableFuture<SignatureHelp> futureSignatureHelp = languageServer.getTextDocumentService().signatureHelp(params);

        // Handle the response
        futureSignatureHelp.thenAccept(signatureHelp -> {
            if (signatureHelp != null) {
                // Process the signature help
                returnValue[0] = signatureHelp;
            } else {
                System.out.println("No signature help available.");
            }
            latch.countDown();
        }).exceptionally(ex -> {
            System.err.println("Error while fetching signature help: " + ex.getMessage());
            latch.countDown();
            return null;
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error(e);
        }

        return returnValue[0];

    }

    public static Hover getHover(Path path, Position position) {

        CountDownLatch latch = new CountDownLatch(1);

        HoverParams params = new HoverParams();
        params.setTextDocument(new TextDocumentIdentifier(path.toUri().toString()));
        params.setPosition(position);

        final Hover[] returnValue = new Hover[1];
        CompletableFuture<Hover> futureHover = languageServer.getTextDocumentService().hover(params);

        // Handle the response
        futureHover.thenAccept(hover -> {
            if (hover != null) {
                // Process the hover information
                returnValue[0] = hover;
            } else {
                System.out.println("No hover information available.");
            }
            latch.countDown();
        }).exceptionally(ex -> {
            System.err.println("Error while fetching hover information: " + ex.getMessage());
            latch.countDown();
            return null;
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error(e);
        }

        return returnValue[0];

    }

    public static void changeWorkSpaceFolder(String uri, String name, boolean add) {

        for (WorkspaceFolder workspaceFolder : workspaceFolders) {
            if (workspaceFolder.getUri().equals(uri)) {
                return;
            }
        }
        workspaceFolders.add(new WorkspaceFolder(uri, name));
        sendDCWFN(add);

    }

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

    public static void sendWillSave(String uri) {

        // Create parameters for the willSave notification
        WillSaveTextDocumentParams params = new WillSaveTextDocumentParams();
        params.setTextDocument(new TextDocumentIdentifier(uri));
        params.setReason(TextDocumentSaveReason.Manual); // Reason for the save event, e.g., TextDocumentSaveReason.Manual

        // Send the willSave notification
        languageServer.getTextDocumentService().willSave(params);

    }

    public static void sendDidSave(String uri, String content) {

        // Create parameters for the didSave notification
        DidSaveTextDocumentParams params = new DidSaveTextDocumentParams();
        params.setText(content); // Optionally provide the document text if needed

        // Set the URI of the document that was saved
        params.setTextDocument(new TextDocumentIdentifier(uri));

        // Send the didSave notification
        languageServer.getTextDocumentService().didSave(params);

    }

    public static void sendDeletedFile(String uri) {

        FileEvent fileEvent = new FileEvent(uri, FileChangeType.Deleted);

        // Create DidChangeWatchedFilesParams with the file event
        DidChangeWatchedFilesParams params = new DidChangeWatchedFilesParams(Collections.singletonList(fileEvent));

        // Send the notification to the language server
        languageServer.getWorkspaceService().didChangeWatchedFiles(params);

    }

    private static void handleDiagnostics(PublishDiagnosticsParams diagnostics) {

        List<Diagnostic> diagnosticList = diagnostics.getDiagnostics();
        if (diagnosticList.isEmpty()) {
            return;
        }
        for (Diagnostic diagnostic : diagnosticList) {
            Platform.runLater(() ->
                    {
                        try {
                            EditAreaUtility.processDiagnostic(diagnostic, Paths.get(new URI(diagnostics.getUri())));
                        } catch (Exception e) {
                            logger.error(e);
                        }
                    }
                    );
        }

    }

    private static TextDocumentClientCapabilities getTDCCapabilities() {

        // Text Document Capabilities
        TextDocumentClientCapabilities textDocumentCapabilities = new TextDocumentClientCapabilities();

        // Synchronization
        SynchronizationCapabilities syncCapabilities = new SynchronizationCapabilities(true, false, true, true);
        textDocumentCapabilities.setSynchronization(syncCapabilities);

        // Completion
        CompletionCapabilities completionCapabilities = new CompletionCapabilities();
        completionCapabilities.setDynamicRegistration(true); // Enable dynamic registration
        CompletionItemCapabilities completionItemCapabilities = new CompletionItemCapabilities();
        completionItemCapabilities.setSnippetSupport(true); // Support snippets
        completionItemCapabilities.setCommitCharactersSupport(true); // Support commit characters
        completionCapabilities.setCompletionItem(completionItemCapabilities);
        textDocumentCapabilities.setCompletion(completionCapabilities);

        // Hover
        HoverCapabilities hoverCapabilities = new HoverCapabilities(Collections.singletonList(MarkupKind.PLAINTEXT),true);
        textDocumentCapabilities.setHover(hoverCapabilities);

        // Signature Help
        SignatureInformationCapabilities signatureInformationCapabilities = new SignatureInformationCapabilities(
                Arrays.asList(MarkupKind.MARKDOWN, MarkupKind.PLAINTEXT)
        );
        signatureInformationCapabilities.setParameterInformation(new ParameterInformationCapabilities(true));
        signatureInformationCapabilities.setActiveParameterSupport(true);
        SignatureHelpCapabilities signatureHelpCapabilities = new SignatureHelpCapabilities(
                signatureInformationCapabilities, true
        );
        signatureHelpCapabilities.setContextSupport(true);
        textDocumentCapabilities.setSignatureHelp(signatureHelpCapabilities);

        // Diagnostics
        DiagnosticCapabilities diagnosticCapabilities = new DiagnosticCapabilities(true, true);
        textDocumentCapabilities.setDiagnostic(diagnosticCapabilities);

        return textDocumentCapabilities;

    }

    private static WorkspaceClientCapabilities getWCCapabilities() {

        // Workspace-related capabilities
        WorkspaceClientCapabilities workspaceClientCapabilities = new WorkspaceClientCapabilities();
        workspaceClientCapabilities.setWorkspaceEdit(new WorkspaceEditCapabilities()); // Example configuration
        workspaceClientCapabilities.setDidChangeConfiguration(new DidChangeConfigurationCapabilities()); // Example configuration
        workspaceClientCapabilities.setDidChangeWatchedFiles(new DidChangeWatchedFilesCapabilities()); // Example configuration
        workspaceClientCapabilities.setSymbol(new SymbolCapabilities());
        workspaceClientCapabilities.setExecuteCommand(new ExecuteCommandCapabilities());
        workspaceClientCapabilities.setWorkspaceFolders(true);
        workspaceClientCapabilities.setConfiguration(true);

        return workspaceClientCapabilities;
    }

    private static void resetServer() {

        FileManager.saveFiles(OpenFilesTracker.getOpenFiles());
        stopServer();
        startServer();
        for (OpenFile o: OpenFilesTracker.getOpenFiles()) {
            StringBuilder text = new StringBuilder();
            ArrayList<String> lines = FileManager.readFile(o.getFile().toPath());
            if (lines != null) {
                for (String line : lines) {
                    text.append(line).append("\n");
                }
            }
            JLSManager.didOpen(o.getFile().toPath(), text.toString());
        }

    }

    public static void registerSync() {

        TextDocumentSyncOptions syncOptions = new TextDocumentSyncOptions();
        syncOptions.setOpenClose(true);
        syncOptions.setChange(TextDocumentSyncKind.Incremental); // or Full
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

    public static void registerCompletion() {

        CompletionOptions completionOptions = new CompletionOptions();
        completionOptions.setResolveProvider(true); // Whether the server supports resolving completion items
        completionOptions.setTriggerCharacters(List.of(".", "(", "\"", "'", ":", "@", "[", "{", " "));

        // Define the registration parameters
        RegistrationParams params = new RegistrationParams();
        params.setRegistrations(Collections.singletonList(
                new Registration(
                        "completionCapability", // Unique ID for the capability
                        "textDocument/completion", // The capability to register
                        completionOptions // Typically used with completion capabilities
                )
        ));
        languageClient.registerCapability(params);

    }

    public static void registerHover() {

        RegistrationParams params = new RegistrationParams();
        params.setRegistrations(Collections.singletonList(
                new Registration(
                        "hoverCapability", // Unique ID for the capability
                        "textDocument/hover", // The capability to register
                        new HoverOptions() // Registration options for text documents
                )
        ));

        languageClient.registerCapability(params);

    }

    public static void registerSignatureHelp() {

        SignatureHelpOptions signatureHelpOptions = new SignatureHelpOptions();
        // Optionally configure the SignatureHelpOptions if needed
        signatureHelpOptions.setTriggerCharacters(Arrays.asList("(", ","));

        // Define the registration parameters
        RegistrationParams params = new RegistrationParams();
        params.setRegistrations(Collections.singletonList(
                new Registration(
                        "signatureHelpCapability", // Unique ID for the capability
                        "textDocument/signatureHelp", // The capability to register
                        signatureHelpOptions // Registration options for text documents
                )
        ));
        languageClient.registerCapability(params);

    }

    public static void registerDiagnostic() {

        RegistrationParams params = new RegistrationParams();
        params.setRegistrations(Collections.singletonList(
                new Registration(
                        "diagnosticCapability", // Unique ID for the capability
                        "textDocument/publishDiagnostics", // The capability to register
                        new DiagnosticRegistrationOptions() // Registration options for text documents
                )
        ));
        languageClient.registerCapability(params);

    }
}
