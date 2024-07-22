package com.project.custom_classes;

import javafx.scene.control.TreeItem;

import java.nio.file.Path;

public class CustomTreeItem<T> extends TreeItem<T> {

    private Path path;

    public CustomTreeItem(T value, Path path) {

        super(value);
        this.path = path;
    }

    public Path getPath() {

        return path;
    }

    public void setPath(Path path) {

        this.path = path;
    }

}
