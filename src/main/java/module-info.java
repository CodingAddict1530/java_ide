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
    opens com.project.utility to com.google.gson;
    exports com.project.javaeditor;
    exports com.project.utility;
    exports com.project.managers;
    exports com.project.custom_classes;
    opens com.project.managers to com.google.gson, org.eclipse.lsp4j.jsonrpc;
    opens com.project.custom_classes to com.google.gson;
}