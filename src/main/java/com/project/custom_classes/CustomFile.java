package com.project.custom_classes;

import java.io.File;

public class CustomFile extends File {

    private String packageName;

    public CustomFile(String path, String packageName) {

        super(path);
        this.packageName = packageName;
    }

    public CustomFile(String parent, String child, String packageName) {

        super(parent, child);
        this.packageName = packageName;
    }

    public CustomFile(CustomFile parent, String child, String packageName) {

        super(parent, child);
        this.packageName = packageName;
    }

    public String getPackageName() {

        return this.packageName;
    }

    public void setPackageName(String packageName) {

        this.packageName = packageName;
    }
}
