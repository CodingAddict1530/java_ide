package utility;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import managers.FileManager;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;

public class MainUtility {

    private static ArrayList<Path> openProjectPath;
    private static ArrayList<Path> openFilesPaths;

    // 1 Failed to write
    // 2 Failed to make readonly
    public static int writeOpenData(Path path, boolean readOnly) {

        File file = path.toFile();
        if (file.exists() && !file.setWritable(true)) {
            return 1;
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(openProjectPath.get(0).toString()).append("\n");
        for (Path filePath : openFilesPaths) {
            stringBuilder.append(filePath.toString()).append("\n");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        boolean result = FileManager.writeToFile(path, stringBuilder.toString(), true, false);
        if (result) {
            if (readOnly) {
                if (!file.setReadOnly()) {
                    return 2;
                }
            }
            return 0;
        } else {
            return 1;
        }

    }

    public static ArrayList<Path> readOpenData(Path path) {

        ArrayList<String> lines = FileManager.readFile(path);
        ArrayList<Path> returnValue = new ArrayList<>();
        boolean valid = true;
        if (lines != null) {
            for (String s : lines) {
                if (new File(s).exists()) {
                    returnValue.add(Paths.get(s));
                } else {
                    valid = false;
                }
            }
        } else {
            valid = false;
        }

        return (valid ? returnValue : null);

    }

    public static void setOpenProjectPath(ArrayList<Path> openProjectPath) {

        MainUtility.openProjectPath = openProjectPath;
    }

    public static void setOpenFilesPaths(ArrayList<Path> openFilesPaths) {

        MainUtility.openFilesPaths = openFilesPaths;
    }

    public static boolean checkAndFix() {

        String home = System.getProperty("user.home");
        File appHome = new File(home, "NotAnIDE_Projects");
        if (!appHome.exists()) {
            return appHome.mkdir();
        }

        return true;

    }

    public static String quickDialog(String title, String text) {

        final String[] output = new String[1];
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(text);
        dialog.setGraphic(null);
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(userInput -> output[0] = userInput);
        return output[0];

    }

    public static boolean confirm(String title, String text) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        Optional<ButtonType> result = alert.showAndWait();
        return (result.isPresent() && result.get() == ButtonType.OK);

    }

}
