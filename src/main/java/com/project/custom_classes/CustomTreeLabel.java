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

import javafx.scene.control.Label;
import java.nio.file.Path;

/**
 * A custom Label for TreeItems that has the path to the file it represents.
 */
public class CustomTreeLabel extends Label {

    /**
     * The Path to the file it represents.
     */
    private Path path;

    /**
     * Instantiates a new CustomTreeLabel Object.
     *
     * @param s The text of the label.
     * @param path The Path to the file it represents.
     */
    public CustomTreeLabel(String s, Path path) {

        super(s);
        this.path = path;
    }

    /**
     * Retrieves the Path to the file it represents.
     *
     * @return The Path.
     */
    public Path getPath() {

        return this.path;
    }

    /**
     * Updates the Path to the file it represents.
     *
     * @param path The new Path.
     */
    public void setPath(Path path) {

        this.path = path;
    }

}
