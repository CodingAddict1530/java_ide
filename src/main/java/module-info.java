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
    requires eu.hansolo.tilesfx;
    requires java.compiler;

    opens com.project.javaeditor to javafx.fxml;
    exports com.project.javaeditor;
    exports utility;
    exports managers;
    exports custom_classes;
}