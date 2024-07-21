package custom_classes;


import javafx.scene.control.Label;

import java.nio.file.Path;

public class CustomTreeLabel extends Label {

    private Path path;

    public CustomTreeLabel(String s, Path path) {

        super(s);
        this.path = path;
    }

    public Path getPath() {

        return this.path;
    }

    public void setPath(Path path) {

        this.path = path;
    }

}
