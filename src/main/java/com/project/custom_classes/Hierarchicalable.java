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

import java.util.ArrayList;

/**
 * Represents an object that belongs to a hierarchy.
 */
public interface Hierarchicalable {

    /**
     * Retrieves the parent of the TreeNode.
     *
     * @return The parent.
     */
    TreeNode getParent();

    /**
     * Retrieves the children of the TreeNode.
     *
     * @return The children.
     */
    ArrayList<TreeNode> getChildren();

}
