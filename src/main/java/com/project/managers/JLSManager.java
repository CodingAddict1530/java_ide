package com.project.managers;

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

    private static Process process;
    private static LanguageServer languageServer;
    private static InputStream inputStream;
    private static OutputStream outputStream;
    private static ArrayList<WorkspaceFolder> workspaceFolders = new ArrayList<>();
    private static final LanguageClient languageClient = new LanguageClient() {


        @Override
        public void telemetryEvent(Object o) {

            System.out.println("Telemetry event: " + o);
        }

        @Override
        public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {

            handleDiagnostics(diagnostics);
        }

        @Override
        public void showMessage(MessageParams messageParams) {

            System.out.println("Message: " + messageParams);
        }

        @Override
        public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {

            System.out.println("Message request: " + requestParams.getMessage());
            return null;
        }

        @Override
        public CompletableFuture<Void> registerCapability(RegistrationParams params) {

            System.out.println("Registered Capabilities: " + params);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> unregisterCapability(UnregistrationParams params) {

            System.out.println("Unregistered Capabilities: " + params);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<java.util.List<WorkspaceFolder>> workspaceFolders() {

            return CompletableFuture.completedFuture(workspaceFolders);
        }

        @Override
        public void logMessage(MessageParams messageParams) {

            logger.info("Message: {}", messageParams);
        }

        @JsonNotification("language/status")
        public void languageStatus(LanguageStatusParams params) {
            System.out.println("Language status: " + params);
        }

        @JsonNotification("language/eventNotification")
        public void languageEventNotification(Object params) {
            System.out.println("Language event notif: " + params);
        }

    };

    public static void startServer() {

        try {

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
                    "-configuration", "lib/jdt-language-server-1.37.0-202406271335/config_win",
                    "-data", "jdt_data"
            };
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            process = processBuilder.start();
            inputStream = process.getInputStream();
            outputStream = process.getOutputStream();

            Launcher<LanguageServer> launcher = LSPLauncher.createClientLauncher(languageClient, inputStream, outputStream);
            launcher.startListening();
            languageServer = launcher.getRemoteProxy();
        } catch (IOException e) {
            logger.error(e);
        }

    }

    public static void stopServer() {

        try {
            if (languageServer != null) {
                System.out.println("KK: " + languageServer.shutdown().get());
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
        Path root = Paths.get("C:\\Users\\hp\\NotAnIDE_Projects\\First");
        workspaceFolders.add(new WorkspaceFolder(root.toUri().toString(), "First"));
        initializeParams.setWorkspaceFolders(workspaceFolders);

        // Define client capabilities
        ClientCapabilities clientCapabilities = new ClientCapabilities();
        clientCapabilities.setTextDocument(getTDCCapabilities());
        clientCapabilities.setWorkspace(getWCCapabilities());

        initializeParams.setCapabilities(clientCapabilities);
        CompletableFuture<InitializeResult> initializeFuture = languageServer.initialize(initializeParams);
        initializeFuture.thenAccept(result -> {
            logger.info("Initialization successful. Capabilities: {}", result.getCapabilities());
            languageServer.initialized(new InitializedParams());
            /*
            languageClient.workspaceFolders().thenAccept(folders -> {
                System.out.println("Workspace folders: " + folders);
            });
            */
            sendDCWFN(languageServer, workspaceFolders);
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

    public static void sendDCWFN(LanguageServer languageServer, List<WorkspaceFolder> workspaceFolders) {

        DidChangeWorkspaceFoldersParams params = new DidChangeWorkspaceFoldersParams();
        WorkspaceFoldersChangeEvent event = new WorkspaceFoldersChangeEvent();
        event.setAdded(workspaceFolders);
        event.setRemoved(Collections.emptyList());
        params.setEvent(event);

        languageServer.getWorkspaceService().didChangeWorkspaceFolders(params);

    }

    private static void handleDiagnostics(PublishDiagnosticsParams diagnostics) {

        List<Diagnostic> diagnosticList = diagnostics.getDiagnostics();
        if (diagnosticList.isEmpty()) {
            return;
        }
        Diagnostic firstDiagnostic = diagnosticList.get(0);
        System.out.println(firstDiagnostic);
        diagnosticList.remove(0);
        for (Diagnostic diagnostic : diagnosticList) {
            Platform.runLater(() ->
                    {
                        try {
                            EditAreaUtility.processDiagnostic(diagnostic, Paths.get(new URI(diagnostics.getUri())));
                        } catch (URISyntaxException e) {
                            logger.error(e);
                            System.out.println(e.getMessage());
                        }
                    }
                    );
        }

    }

    private static TextDocumentClientCapabilities getTDCCapabilities() {

        // Text Document Capabilities
        TextDocumentClientCapabilities textDocumentCapabilities = new TextDocumentClientCapabilities();

        // Synchronization
        SynchronizationCapabilities syncCapabilities = new SynchronizationCapabilities(true, true, true, true);
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
}
