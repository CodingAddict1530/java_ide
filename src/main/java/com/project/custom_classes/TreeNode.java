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
 * Represents a file on a tree.
 */
public abstract class TreeNode implements Hierarchicalable {

    /**
     * The path of the file it represents.
     */
    protected Path path;

    /**
     * Instantiates a new TreeNode.
     *
     * @param path The Path of the file it represents.
     */
    public TreeNode(Path path) {

        this.path = path;
    }

    /**
     * Retrieves the Path of the file it represents.
     *
     * @return The Path.
     */
    public Path getPath() {

        return this.path;
    }

    /**
     * Updates the Path of the file it represents.
     *
     * @param path The new Path.
     */
    public void setPath(Path path) {

        this.path = path;
    }

    /**
     * Retrieves the name of the File it represents.
     *
     * @return The name.
     */
    public String getName() {

        return path.getFileName().toString();
    }

    /**
     * Retrieves the parent of the TreeNode.
     *
     * @return The parent.
     */
    @Override
    public abstract TreeNode getParent();

    /**
     * Retrieves the children of the TreeNode.
     *
     * @return The children.
     */
    @Override
    public abstract ArrayList<TreeNode> getChildren();

}
