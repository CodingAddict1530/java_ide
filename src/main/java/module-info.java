module com.project.javaeditor {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing;
    requires org.fxmisc.richtext;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;
    requires java.compiler;
    requires org.apache.logging.log4j.core;
    requires org.eclipse.lsp4j;
    requires org.eclipse.lsp4j.jsonrpc;

    opens com.project.javaeditor to javafx.fxml;
    exports com.project.javaeditor;
    exports com.project.utility;
    exports com.project.managers;
    exports com.project.custom_classes;
}