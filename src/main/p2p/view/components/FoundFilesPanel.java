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

    public void updateFoundFilesList(Map<String, Map.Entry<String, Peer>> files) {
        DefaultListModel<String> model = new DefaultListModel<>();
        Map<String, Integer> fileOccurrences = new HashMap<>();
        Map<String, Integer> currentDisplayCounts = new HashMap<>();

        fileHashMap.clear();

        for (Map.Entry<String, Map.Entry<String, Peer>> entry : files.entrySet()) {
            String fullPath = entry.getKey();
            String fileName = fullPath.substring(fullPath.lastIndexOf("/") + 1);
            String peerIp = entry.getValue().getValue().getIpAddress();

            String uniqueKey = fileName + "@" + peerIp;
            fileOccurrences.put(uniqueKey, fileOccurrences.getOrDefault(uniqueKey, 0) + 1);

            fileHashMap.put(fullPath, Map.entry(fileName, peerIp));
        }

        for (Map.Entry<String, Map.Entry<String, Peer>> entry : files.entrySet()) {
            String fullPath = entry.getKey();
            String fileName = fullPath.substring(fullPath.lastIndexOf("/") + 1);
            String peerIp = entry.getValue().getValue().getIpAddress();

            String uniqueKey = fileName + "@" + peerIp;

            currentDisplayCounts.put(uniqueKey, currentDisplayCounts.getOrDefault(uniqueKey, 0) + 1);
            int currentCount = currentDisplayCounts.get(uniqueKey);

            String displayName = fileOccurrences.get(uniqueKey) > 1
                ? fileName + " (" + currentCount + ")"
                : fileName;

            model.addElement(displayName + " from " + peerIp);
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
