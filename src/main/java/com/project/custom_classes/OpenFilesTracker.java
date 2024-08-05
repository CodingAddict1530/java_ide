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
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Tracks open files in the editor.
 */
public class OpenFilesTracker {

    /**
     * A list containing all open files.
     */
    private static final ArrayList<OpenFile> openFiles = new ArrayList<>();

    /**
     * Adds an OpenFile to the list.
     *
     * @param file The OpenFile.
     */
    public static void addOpenFile(OpenFile file) {

        openFiles.add(file);
    }

    /**
     * Removes an OpenFile from the list.
     *
     * @param file the OpenFile.
     */
    public static void removeOpenFile(OpenFile file) {

        openFiles.remove(file);
    }

    /**
     * Removes an OpenFile from the list.
     *
     * @param index The index of the file.
     */
    public static void removeOpenFile(int index) {

        openFiles.remove(index);
    }

    /**
     * Returns all openFiles.
     *
     * @return All OpenFiles.
     */
    public static ArrayList<OpenFile> getOpenFiles() {

        return openFiles;
    }

    /**
     * Retrieves a particular OpenFile determined by the index.
     *
     * @param index The index of the file in the list.
     * @return The OpenFile.
     */
    public static OpenFile getOpenFile(int index) {

        return openFiles.get(index);
    }

    /**
     * Retrieves a particular OpenFile determined by the tab.
     *
     * @param tab The tab of the file in the list.
     * @return The OpenFile.
     */
    public static OpenFile getOpenFile(Tab tab) {

        return find(tab);
    }

    /**
     * Retrieves a particular OpenFile determined by the file itself.
     *
     * @param file The file the list.
     * @return The OpenFile.
     */
    public static OpenFile getOpenFile(CustomFile file) {

        return find(file);
    }

    /**
     * Retrieves a particular OpenFile determined by the path.
     *
     * @param path The path of the file in the list.
     * @return The OpenFile.
     */
    public static OpenFile getOpenFile(Path path) {

        return find(path);
    }

    /**
     * Checks whether a particular file is saved by the index.
     *
     * @param index the index of the file in the list.
     * @return Whether its saved or not.
     */
    public static Boolean isSaved(int index) {

        return openFiles.get(index).getIsSaved();
    }

    /**
     * Checks whether a particular file is saved by the tab.
     *
     * @param tab the tab of the file in the list.
     * @return Whether its saved or not.
     */
    public static Boolean isSaved(Tab tab) {

        OpenFile result = find(tab);
        return result == null ? null : result.getIsSaved();
    }

    /**
     * Checks whether a particular file is saved by the file itself.
     *
     * @param file the file in the list.
     * @return Whether its saved or not.
     */
    public static Boolean isSaved(CustomFile file) {

        OpenFile result = find(file);
        return result == null ? null : result.getIsSaved();
    }

    /**
     * Checks whether a particular file is saved by the path.
     *
     * @param path the path of the file in the list.
     * @return Whether its saved or not.
     */
    public static Boolean isSaved(Path path) {

        OpenFile result = find(path);
        return result == null ? null : result.getIsSaved();
    }

    /**
     * Retrieves a list of all saved files.
     *
     * @return A list of all saved files.
     */
    public static ArrayList<OpenFile> getAllSaved() {

        ArrayList<OpenFile> result = new ArrayList<>();
        for (OpenFile file : openFiles) {
            if (file.getIsSaved()) {
                result.add(file);
            }
        }

        return result;
    }

    /**
     * Retrieves a list of all unsaved files.
     *
     * @return A list of all unsaved files.
     */
    public static ArrayList<OpenFile> getAllUnSaved() {

        ArrayList<OpenFile> result = new ArrayList<>();
        for (OpenFile file : openFiles) {
            if (!file.getIsSaved()) {
                result.add(file);
            }
        }

        return result;
    }

    /**
     * Finds a file in openFiles List by the tab, file or path
     *
     * @param obj the object to use to find the file.
     * @return The OpenFile.
     */
    private static OpenFile find(Object obj) {

        for (OpenFile o : openFiles) {
            if (obj instanceof CustomFile && o.getFile().equals(obj)) {
                return o;
            } else if (obj instanceof Path && o.getFile().toPath().equals(obj)) {
                return o;
            } else if (obj instanceof Tab && o.getTab().equals(obj)) {
                return o;
            }
        }
        return null;

    }

}
