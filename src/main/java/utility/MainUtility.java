package utility;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class MainUtility {

    private static ArrayList<Path> openProjectPath;
    private static ArrayList<Path> openFilesPaths;

    // 1 Failed to write
    // 2 Failed to make readonly
    // 3 Failed to write and make readonly
    public static int writeOpenData(Path path, boolean readOnly) {

        File file = path.toFile();
        if (file.exists() && !file.setWritable(true)) {
            return 1;
        }

        int returnValue = 0;

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(path.toString()))) {
            if (!openFilesPaths.isEmpty() && openProjectPath.get(0) != null) {
                out.writeUTF(openProjectPath.get(0).toString());
                out.writeChar('\n');
            }
            for (Path p : openFilesPaths) {
                out.writeUTF(p.toString());
                out.writeChar('\n');
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            returnValue = 1;
        }


        if (readOnly) {

            if (!file.setReadOnly()) {
                if (returnValue == 1) {
                    returnValue = 3;
                } else {
                    returnValue = 2;
                }
            }
        }

        return returnValue;
    }

    public static ArrayList<Path> read(Path path) {

        ArrayList<Path> returnValue = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(new FileInputStream(path.toString()))) {
            while (in.available() > 0) {
                returnValue.add(Paths.get(in.readUTF()));
                in.readChar();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return (returnValue.isEmpty()) ? null : returnValue;

    }

    public static void setOpenProjectPath(ArrayList<Path> openProjectPath) {

        MainUtility.openProjectPath = openProjectPath;
    }

    public static void setOpenFilesPaths(ArrayList<Path> openFilesPaths) {

        MainUtility.openFilesPaths = openFilesPaths;
    }

}
