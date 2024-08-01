module com.project.javaeditor {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires org.fxmisc.richtext;

    requires java.compiler;
    requires org.apache.logging.log4j.core;
    requires org.eclipse.lsp4j;
    requires org.eclipse.lsp4j.jsonrpc;
    requires com.google.gson;
    requires org.fxmisc.undo;

    opens com.project.javaeditor to javafx.fxml;
    opens com.project.managers to org.eclipse.lsp4j.jsonrpc;
    opens com.project.utility to com.google.gson;
    exports com.project.javaeditor;
    exports com.project.utility;
    exports com.project.managers;
    exports com.project.custom_classes;
}