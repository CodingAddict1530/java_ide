package com.project.utility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

public class JLSManager {

    private static final Logger logger = LogManager.getLogger(JLSManager.class);

    private static Process process;
    private static LanguageServer languageServer;
    private static final LanguageClient languageClient = new LanguageClient() {


        @Override
        public void telemetryEvent(Object o) {

            System.out.println("Telemetry event: " + o);
        }

        @Override
        public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {

            System.out.println("Diagnostics: " + diagnostics);
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
        public void logMessage(MessageParams messageParams) {

            logger.info("Message: {}", messageParams);
        }
    };

    public static void startServer() {

        try {

            ProcessBuilder processBuilder = new ProcessBuilder("gradle", "runLanguageServer");
            processBuilder.redirectErrorStream(true);

            process = processBuilder.start();
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

        if (process != null) {
            process.destroy();
        }

    }

}
