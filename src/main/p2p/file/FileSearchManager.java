package main.p2p.file;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;

public class FileSearchManager {

    private final File rootDirectory;
    private final JList<String> excludeFilesMasksList;
    private final JList<String> excludeFoldersList;
    private final boolean rootOnly;

    public FileSearchManager(File rootDirectory, JList<String> excludeFilesMasksList, JList<String> excludeFoldersList, boolean rootOnly) {
        this.rootDirectory = rootDirectory;
        this.excludeFilesMasksList = excludeFilesMasksList;
        this.excludeFoldersList = excludeFoldersList;
        this.rootOnly = rootOnly;
    }

    public Map<String, String> searchFiles(String query) {
        Map<String, String> result = new HashMap<>(); // Map of relativePath -> fileName
        if (rootDirectory != null && rootDirectory.isDirectory()) {
            if (rootOnly) {
                searchInRootOnly(rootDirectory, query, result);
            } else {
                searchInDirectory(rootDirectory, query, result, "");
            }
        }
        return result;
    }

    private void searchInDirectory(File directory, String query, Map<String, String> result, String relativePath) {
        if (shouldExcludeDirectory(directory)) {
            System.out.println("Skipping excluded directory: " + directory.getName());
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                String newRelativePath = relativePath + "/" + file.getName();
                if (file.isDirectory()) {
                    searchInDirectory(file, query, result, newRelativePath);
                } else if (shouldIncludeFile(file, query)) {
                    result.put(newRelativePath, file.getName());
                    System.out.println("File matched: " + newRelativePath);
                }
            }
        }
    }
    
    private void searchInRootOnly(File directory, String query, Map<String, String> result) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory() && shouldIncludeFile(file, query)) {
                    result.put("/" + file.getName(), file.getName());
                    System.out.println("File matched in root only: " + file.getName());
                }
            }
        }
    }
    
    private boolean shouldExcludeDirectory(File directory) {
        String dirName = directory.getName().toLowerCase();
        ListModel<String> exclusionListModel = excludeFoldersList.getModel();

        for (int i = 0; i < exclusionListModel.getSize(); i++) {
            String excludedDir = exclusionListModel.getElementAt(i).toLowerCase();
            if (dirName.equals(excludedDir)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldIncludeFile(File file, String query) {
        String fileName = file.getName().toLowerCase();
        ListModel<String> exclusionListModel = excludeFilesMasksList.getModel();

        for (int i = 0; i < exclusionListModel.getSize(); i++) {
            String exclusionPattern = exclusionListModel.getElementAt(i).toLowerCase();

            if (fileName.equals(exclusionPattern)) {
                return false;
            }

            if (exclusionPattern.startsWith("*.") && fileName.endsWith(exclusionPattern.substring(1))) {
                return false;
            }
        }

        return fileName.contains(query.toLowerCase());
    }
}
