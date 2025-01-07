package main.p2p.file;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
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

    public Map<String, Map.Entry<String, String>> searchFiles(String query) {
        Map<String, Map.Entry<String, String>> result = new HashMap<>();
        if (rootDirectory != null && rootDirectory.isDirectory()) {
            if (rootOnly) {
                searchInRootOnly(rootDirectory, query, result);
            } else {
                searchInDirectory(rootDirectory, query, result, "");
            }
        }
        return result;
    }
    
    public Map<String, String> searchFilesByHash(String hash) {
        Map<String, String> result = new HashMap<>();
        if (rootDirectory != null && rootDirectory.isDirectory()) {
            if (rootOnly) {
                searchHashInRootOnly(rootDirectory, hash, result);
            } else {
                searchHashInDirectory(rootDirectory, hash, result, "");
            }
        }
        return result;
    }
    
    private void searchHashInDirectory(File directory, String hash, Map<String, String> result, String relativePath) {
        if (shouldExcludeDirectory(directory)) {
            System.out.println("Skipping excluded directory: " + directory.getName());
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                String newRelativePath = relativePath + "/" + file.getName();
                if (file.isDirectory()) {
                    searchHashInDirectory(file, hash, result, newRelativePath);
                } else if (shouldIncludeFileByHash(file)) {
                    String fileHash = computeFileHash(file);
                    if (fileHash.equals(hash)) {
                        result.put(newRelativePath, fileHash);
                        System.out.println("Hash matched: " + newRelativePath + " | Hash: " + fileHash);
                    }
                }
            }
        }
    }

    private void searchHashInRootOnly(File directory, String hash, Map<String, String> result) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory() && shouldIncludeFileByHash(file)) {
                    String fileHash = computeFileHash(file);
                    if (fileHash.equals(hash)) {
                        result.put("/" + file.getName(), fileHash);
                        System.out.println("Hash matched in root only: " + file.getName() + " | Hash: " + fileHash);
                    }
                }
            }
        }
    }

    private void searchInDirectory(File directory, String query, Map<String, Map.Entry<String, String>> result, String relativePath) {
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
                    String hash = computeFileHash(file);
                    result.put(newRelativePath, Map.entry(file.getName(), hash));
                    System.out.println("File matched: " + newRelativePath + " | Hash: " + hash);
                }
            }
        }
    }
    
    private void searchInRootOnly(File directory, String query, Map<String, Map.Entry<String, String>> result) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory() && shouldIncludeFile(file, query)) {
                    String hash = computeFileHash(file);
                    result.put("/" + file.getName(), Map.entry(file.getName(), hash));
                    System.out.println("File matched in root only: " + file.getName() + " | Hash: " + hash);
                }
            }
        }
    }
    
    private String computeFileHash(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] byteArray = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesRead);
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            System.err.println("Error calculating hash for file: " + file.getName() + " - " + e.getMessage());
            return "";
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
    
    private boolean shouldIncludeFileByHash(File file) {
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

        return true;
    }
}
