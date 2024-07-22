package com.project.managers;

import com.project.custom_classes.RootTreeNode;
import com.project.utility.MainUtility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;

public class ProjectManager {

    private static final Logger logger = LogManager.getLogger(ProjectManager.class);

    public static final File APP_HOME = new File(System.getProperty("user.home"), "NotAnIDE_Projects");
    private static RootTreeNode currentProject;

    public static RootTreeNode createProject(String name) {

        if (name == null || name.isEmpty()) {
            return null;
        }
        RootTreeNode projectRoot = null;
        File projectDir = new File(APP_HOME, name);
        if (!projectDir.exists()) {
            if (projectDir.mkdir()) {
                projectRoot = new RootTreeNode(projectDir.toPath());
                logger.info("Project directory created: {}", projectDir.getAbsolutePath());
            } else {
                logger.warn("Failed to create project directory: {}", projectDir.getAbsolutePath());
            }
        } else {
            System.out.println("Project directory already exists");
        }

        for (String dir: new String[] {"src", "bin", "lib"}) {
            File directory = new File(projectDir, dir);
            if (!directory.exists()) {
                if (directory.mkdir()) {
                    logger.info("Created {} in {}", dir, projectDir.getAbsolutePath());
                } else {
                    logger.warn("Failed to create {} in {}", dir, projectDir.getAbsolutePath());
                }
            }
        }

        currentProject = projectRoot;
        return projectRoot;
    }

    public static void openProject(Path path) {

        path = DirectoryManager.openProject(path);
        if (path == null) {
            return;
        }
        boolean isProject = true;
        for (String s: new String[] {"bin", "src", "lib"}) {
            File file = new File(path.toString(), s);
            if (!file.exists()) {
                isProject = false;
                break;
            }
        }
        if (isProject) {
            currentProject = new RootTreeNode(path);
        } else {
            currentProject = null;
        }

    }

    public static void deleteProject() {

        Path path = currentProject.getPath();
        if (MainUtility.confirm("Delete "+ path.getFileName(), "This entire project will be deleted")) {
            currentProject = null;
            DirectoryManager.deleteDirectory(path);
        }
        openProject(APP_HOME.toPath());

    }

    public static RootTreeNode getCurrentProject() {

        return currentProject;
    }

}
