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

import javafx.scene.control.Tab;

/**
 * Represents an open File in the editor.
 */
public class OpenFile {

    /**
     * The file.
     */
    private CustomFile file;

    /**
     * The tab showing the contents of the file.
     */
    private Tab tab;

    /**
     * Whether the file is saved or not.
     */
    private boolean isSaved;

    /**
     * Instantiates a new OpenFile Object.
     *
     * @param file The file.
     * @param tab The tab.
     * @param isSaved Whether is it saved.
     */
    public OpenFile(CustomFile file, Tab tab, boolean isSaved) {

        this.file = file;
        this.tab = tab;
        this.isSaved = isSaved;
    }

    /**
     * Retrieves the file.
     *
     * @return The file.
     */
    public CustomFile getFile() {

        return file;
    }

    /**
     * Updates the file.
     *
     * @param file The new file.
     */
    public void setFile(CustomFile file) {

        this.file = file;
    }

    /**
     * Retrieves the tab for that file.
     *
     * @return The tab.
     */
    public Tab getTab() {

        return tab;
    }

    /**
     * Updates the tab for that file.
     *
     * @param tab The new tab.
     */
    public void setTab(Tab tab) {

        this.tab = tab;
    }

    /**
     * Retrieves whether the file is saved or not.
     *
     * @return Whether the file is saved or not.
     */
    public boolean getIsSaved() {

        return isSaved;
    }

    /**
     * Updates the save status of the file.
     *
     * @param isSaved The new save status.
     */
    public void setIsSaved(boolean isSaved) {

        this.isSaved = isSaved;
    }

}
