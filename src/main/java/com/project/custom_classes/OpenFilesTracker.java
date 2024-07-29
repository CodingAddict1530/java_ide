package com.project.custom_classes;

import javafx.scene.control.Tab;

import java.nio.file.Path;
import java.util.ArrayList;

public class OpenFilesTracker {

    private static final ArrayList<OpenFile> openFiles = new ArrayList<>();

    public static void addOpenFile(OpenFile file) {

        openFiles.add(file);
    }

    public static void removeOpenFile(OpenFile file) {

        openFiles.remove(file);
    }

    public static void removeOpenFile(int index) {

        openFiles.remove(index);
    }

    public static ArrayList<OpenFile> getOpenFiles() {

        return openFiles;
    }

    public static OpenFile getOpenFile(int index) {

        return openFiles.get(index);
    }

    public static OpenFile getOpenFile(Tab tab) {

        return find(tab);
    }

    public static OpenFile getOpenFile(CustomFile file) {

        return find(file);
    }

    public static OpenFile getOpenFile(Path path) {

        return find(path);
    }

    public static Boolean isSaved(int index) {

        return openFiles.get(index).getIsSaved();
    }

    public static Boolean isSaved(Tab tab) {

        OpenFile result = find(tab);
        return result == null ? null : result.getIsSaved();
    }

    public static Boolean isSaved(CustomFile file) {

        OpenFile result = find(file);
        return result == null ? null : result.getIsSaved();
    }

    public static Boolean isSaved(Path path) {

        OpenFile result = find(path);
        return result == null ? null : result.getIsSaved();
    }

    public static ArrayList<OpenFile> getAllSaved() {

        ArrayList<OpenFile> result = new ArrayList<>();
        for (OpenFile file : openFiles) {
            if (file.getIsSaved()) {
                result.add(file);
            }
        }

        return result;
    }

    public static ArrayList<OpenFile> getAllUnSaved() {

        ArrayList<OpenFile> result = new ArrayList<>();
        for (OpenFile file : openFiles) {
            if (!file.getIsSaved()) {
                result.add(file);
            }
        }

        return result;
    }

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
