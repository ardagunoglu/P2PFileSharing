package main.p2p.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class FileSearchManager {

    private final File rootDirectory;
    private final JList<String> excludeFilesMasksList; // Reference to exclusion list

    public FileSearchManager(File rootDirectory, JList<String> excludeFilesMasksList) {
        this.rootDirectory = rootDirectory;
        this.excludeFilesMasksList = excludeFilesMasksList;
    }

    public List<String> searchFiles(String query) {
        List<String> result = new ArrayList<>();
        if (rootDirectory != null && rootDirectory.isDirectory()) {
            searchInDirectory(rootDirectory, query, result);
        }
        return result;
    }

    private void searchInDirectory(File directory, String query, List<String> result) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    searchInDirectory(file, query, result);
                } else if (shouldIncludeFile(file, query)) {
                    result.add(file.getName());
                }
            }
        }
    }

    private boolean shouldIncludeFile(File file, String query) {
        String fileName = file.getName();
        ListModel<String> exclusionListModel = excludeFilesMasksList.getModel();

        for (int i = 0; i < exclusionListModel.getSize(); i++) {
            String exclusionPattern = exclusionListModel.getElementAt(i);

            if (fileName.equalsIgnoreCase(exclusionPattern)) {
                return false;
            }

            if (exclusionPattern.startsWith("*.") && fileName.toLowerCase().endsWith(exclusionPattern.substring(1).toLowerCase())) {
                return false;
            }
        }

        return fileName.contains(query);
    }
}
