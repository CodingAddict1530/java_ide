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

import javafx.scene.canvas.Canvas;

/**
 * A Custom Canvas that stores its purpose (type).
 */
public class CustomCanvas extends Canvas {

    /**
     * The purpose, or type of canvas this is.
     */
    private String type;

    /**
     * Instantiates a new CustomCanvas.
     *
     * @param type The type of Canvas it is.
     */
    public CustomCanvas(String type) {

        super();
        this.type = type;
    }

    /**
     * Instantiates a new CustomCanvas.
     *
     * @param width The width of the Canvas.
     * @param height The height of the Canvas.
     * @param type The type of Canvas it is.
     */
    public CustomCanvas(double width, double height, String type) {

        super(width, height);
        this.type = type;
    }

    /**
     * Retrieves the type of Canvas this is.
     *
     * @return The type.
     */
    public String getType() {

        return this.type;
    }

    /**
     * Updates the type of Canvas this is.
     *
     * @param type The new type.
     */
    public void setType(String type) {

        this.type = type;
    }

}
