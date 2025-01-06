package main.p2p.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileSearchManager {

    private final File rootDirectory;

    public FileSearchManager(File rootDirectory) {
        this.rootDirectory = rootDirectory;
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
                } else if (file.getName().contains(query)) {
                    result.add(file.getName());
                }
            }
        }
    }
}
