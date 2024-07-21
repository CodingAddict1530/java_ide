package com.project.javaeditor;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utility.MainUtility;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;


public class Application extends javafx.application.Application {

    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("editor.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 1000, 600);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("style.css")).toExternalForm());
        stage.setTitle("Not An IDE");
        stage.setScene(scene);
        ArrayList<Path> previousContent = null;
        try {
            previousContent = MainUtility.readOpenData(Paths.get("src/main/files/records.dat"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        Controller controller = fxmlLoader.getController();
        controller.addPreviousContent(previousContent);
        MainUtility.checkAndFix();
        stage.show();
    }

    @Override
    public void stop() {

        try {
            System.out.println(MainUtility.writeOpenData(
                    Paths.get("src/main/files/records.dat"),
                    true
            ));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    public static void main(String[] args) {

        launch();
    }
}