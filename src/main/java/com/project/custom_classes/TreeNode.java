package com.project.custom_classes;

import java.nio.file.Path;
import java.util.ArrayList;

public abstract class TreeNode implements Hierarchicalable {

    protected Path path;

    public TreeNode(Path path) {

        this.path = path;
    }

    public Path getPath() {

        return path;
    }

    public void setPath(Path path) {

        this.path = path;
    }

    public String getName() {

        return path.getFileName().toString();
    }

    @Override
    public abstract TreeNode getParent();

    @Override
    public abstract ArrayList<TreeNode> getChildren();

}
