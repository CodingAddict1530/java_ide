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

package com.project.custom_classes;

import java.nio.file.Path;
import java.util.ArrayList;

/**
 * A custom TreeNode that represents a directory.
 */
public class DirectoryTreeNode extends TreeNode {

    /**
     * The parent TreeNode.
     */
    private TreeNode parent;

    /**
     * A list of its children.
     */
    private final ArrayList<TreeNode> children;

    /**
     * Instantiates a new DirectoryTreeNode.
     *
     * @param path The Path to the file it represents.
     * @param parent The parent TreeNode.
     */
    public DirectoryTreeNode(Path path, TreeNode parent) {

        super(path);
        this.parent = parent;
        this.children = new ArrayList<>();
    }

    /**
     * Retrieves the parent of the TreeNode.
     *
     * @return The parent.
     */
    @Override
    public TreeNode getParent() {

        return this.parent;
    }

    /**
     * Retrieves the children of the TreeNode.
     *
     * @return The children.
     */
    @Override
    public ArrayList<TreeNode> getChildren() {

        return this.children;
    }

    /**
     * Updates the parent of the TreeNode.
     *
     * @param parent The parent.
     */
    public void setParent(TreeNode parent) {

        this.parent = parent;
    }

    /**
     * Adds a child to the TreeNode.
     *
     * @param child the child.
     */
    public void addChild(TreeNode child) {

        this.children.add(child);
    }

    /**
     * Removes a child from the TreeNode.
     *
     * @param child The child.
     */
    public void removeChild(TreeNode child) {

        this.children.remove(child);
    }

    /**
     * Returns a String representation of the TreeNode.
     *
     * @return the String representation.
     */
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
