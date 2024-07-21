package managers;

import custom_classes.RootTreeNode;

import java.io.File;
import java.nio.file.Path;

public class ProjectManager {

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
            } else {
                System.out.println("Failed to create project directory");
            }
        } else {
            System.out.println("Project directory already exists");
        }

        for (String dir: new String[] {"src", "bin", "lib"}) {
            File directory = new File(projectDir, dir);
            if (!directory.exists()) {
                if (directory.mkdir()) {
                    System.out.println("Project directory: " + dir + " created");
                } else {
                    System.out.println("Failed to create project directory: " + dir);
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

    public static RootTreeNode getCurrentProject() {

        return currentProject;
    }

}
