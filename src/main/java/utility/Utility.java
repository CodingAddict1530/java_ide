package utility;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Utility {

    public static final ArrayList<String> ORANGE_KEY_WORDS = new ArrayList<>(List.of(
            "abstract",
            "assert",
            "boolean",
            "break",
            "byte",
            "case",
            "catch",
            "char",
            "class",
            "const",
            "continue",
            "default",
            "do",
            "double",
            "else",
            "enum",
            "extends",
            "final",
            "finally",
            "float",
            "for",
            "goto",
            "if",
            "implements",
            "import",
            "instanceof",
            "int",
            "interface",
            "long",
            "native",
            "new",
            "null",
            "package",
            "private",
            "protected",
            "public",
            "return",
            "short",
            "static",
            "strictfp",
            "super",
            "switch",
            "synchronized",
            "this",
            "throw",
            "throws",
            "transient",
            "try",
            "void",
            "volatile",
            "while"
    ));

    // 1 Failed to write
    // 2 Failed to make readonly
    // 3 Failed to write and make readonly
    public static int write(Path path, boolean readOnly, Path openProject, ArrayList<Path> data) {

        File file = path.toFile();
        if (file.exists() && !file.setWritable(true)) {
            return 1;
        }

        int returnValue = 0;

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(path.toString()))) {
            if (openProject != null) {
                out.writeUTF(openProject.toString());
                out.writeChar('\n');
            }
            for (Object o : data) {
                out.writeUTF(o.toString());
                out.writeChar('\n');
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            returnValue = 1;
        }


        if (readOnly) {

            if (!file.setReadOnly()) {
                switch (returnValue) {
                    case 0: returnValue = 2;
                    case 1: returnValue = 3;
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

}
