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
 * A custom TreeNode that represents a file.
 */
public class FileTreeNode extends TreeNode {

    /**
     * The parent TreeNode.
     */
    private TreeNode parent;

    /**
     * Instantiates a new FileTreeNode Object.
     *
     * @param path The Path of the file it represents.
     * @param parent The parent.
     */
    public FileTreeNode(Path path, TreeNode parent) {

        super(path);
        this.parent = parent;
    }

    /**
     * Retrieves the parent of the TreeNode.
     *
     * @return The parent.
     */
    @Override
    public TreeNode getParent() {

        return parent;
    }

    /**
     * Retrieves the children of the TreeNode.
     *
     * @return null since a file has no children.
     */
    @Override
    public ArrayList<TreeNode> getChildren() {

        return null;
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
     * Returns a String representation of the TreeNode.
     *
     * @return the String representation.
     */
    @Override
    public String toString() {

        return String.format("Name: %s\n\t- ", path.getFileName());

    }

}
