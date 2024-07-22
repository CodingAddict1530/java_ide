package com.project.custom_classes;

import java.nio.file.Path;
import java.util.ArrayList;

public class DirectoryTreeNode extends TreeNode {

    private TreeNode parent;
    private ArrayList<TreeNode> children;

    public DirectoryTreeNode(Path path, TreeNode parent) {

        super(path);
        this.parent = parent;
        this.children = new ArrayList<>();
    }

    @Override
    public TreeNode getParent() {

        return parent;
    }

    @Override
    public ArrayList<TreeNode> getChildren() {

        return children;
    }

    public void setParent(TreeNode parent) {

        this.parent = parent;
    }

    public boolean addChild(TreeNode child) {

        return children.add(child);
    }

    public boolean removeChild(TreeNode child) {

        return children.remove(child);
    }

    @Override
    public String toString() {

        StringBuilder str = new StringBuilder();
        str.append(String.format("Name: %s\nChildren: \n\t- ", path.getFileName()));
        if (!children.isEmpty()) {
            for (TreeNode child : children) {
                str.append(child.toString());
            }
        } else {
            str.append("No children");
        }


        return str.toString();

    }

}
