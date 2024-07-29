package com.project.custom_classes;

import javafx.scene.control.Tab;

public class OpenFile {

    private CustomFile file;
    private Tab tab;
    private boolean isSaved;

    public OpenFile(CustomFile file, Tab tab, boolean isSaved) {

        this.file = file;
        this.tab = tab;
        this.isSaved = isSaved;
    }

    public CustomFile getFile() {

        return file;
    }

    public void setFile(CustomFile file) {

        this.file = file;
    }

    public Tab getTab() {

        return tab;
    }

    public void setTab(Tab tab) {

        this.tab = tab;
    }

    public boolean getIsSaved() {

        return isSaved;
    }

    public void setIsSaved(boolean isSaved) {

        this.isSaved = isSaved;
    }

}
