package com.project.custom_classes;

import java.nio.file.Path;
import java.util.ArrayList;

public class FileTreeNode extends TreeNode {

    private TreeNode parent;

    public FileTreeNode(Path path, TreeNode parent) {

        super(path);
        this.parent = parent;
    }

    @Override
    public TreeNode getParent() {

        return parent;
    }

    @Override
    public ArrayList<TreeNode> getChildren() {

        return null;
    }

    public void setParent(TreeNode parent) {

        this.parent = parent;
    }

    @Override
    public String toString() {

        return String.format("Name: %s\n\t- ", path.getFileName());

    }

}
