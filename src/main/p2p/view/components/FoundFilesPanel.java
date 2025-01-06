package main.p2p.view.components;

import main.p2p.model.Peer;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class FoundFilesPanel extends AbstractListPanel {

    private Map<String, Map.Entry<String, String>> fileHashMap;

    public FoundFilesPanel() {
        super("Found files");
        fileHashMap = new HashMap<>();
    }

    @Override
    public void onElementAdded(String element) {
        System.out.println("Found file added: " + element);
    }

    @Override
    public void onElementRemoved(String element) {
        System.out.println("Found file removed: " + element);
    }

    public void updateFoundFilesList(Map<String, Map.Entry<String, String>> files) {
        DefaultListModel<String> model = new DefaultListModel<>();
        Map<String, Integer> displayNameCounts = new HashMap<>();
        Map<String, Integer> fileOccurrences = new HashMap<>();

        fileHashMap.clear();

        for (Map.Entry<String, Map.Entry<String, String>> entry : files.entrySet()) {
            String fullPath = entry.getKey();
            String fileName = fullPath.substring(fullPath.lastIndexOf("/") + 1);
            String fileHash = entry.getValue().getValue();

            fileHashMap.put(fullPath, entry.getValue());

            fileOccurrences.put(fileName, fileOccurrences.getOrDefault(fileName, 0) + 1);
        }

        Map<String, Integer> currentDisplayCounts = new HashMap<>();
        for (Map.Entry<String, Map.Entry<String, String>> entry : files.entrySet()) {
            String fullPath = entry.getKey();
            String fileName = fullPath.substring(fullPath.lastIndexOf("/") + 1);
            int occurrenceCount = fileOccurrences.get(fileName);

            currentDisplayCounts.put(fileName, currentDisplayCounts.getOrDefault(fileName, 0) + 1);
            int currentCount = currentDisplayCounts.get(fileName);

            String displayName = (occurrenceCount > 1)
                ? fileName + " (" + currentCount + ")"
                : fileName;

            model.addElement(displayName);
        }

        getList().setModel(model);
    }

    public void clearList() {
        DefaultListModel<String> model = (DefaultListModel<String>) getList().getModel();
        model.clear();
        fileHashMap.clear();
    }

    public Map<String, Map.Entry<String, String>> getFileHashMap() {
        return fileHashMap;
    }
}
