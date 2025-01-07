package main.p2p.view.components;

import main.p2p.model.Peer;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class FoundFilesPanel extends AbstractListPanel {

    private Map<String, Map.Entry<String, Peer>> filePeerMap;

    public FoundFilesPanel() {
        super("Found files");
        filePeerMap = new HashMap<>();
        initializeSelectionListener();
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
        Map<String, Integer> displayCounts = new HashMap<>();

        filePeerMap.clear();

        for (Map.Entry<String, Map.Entry<String, Peer>> entry : files.entrySet()) {
            String fullPath = entry.getKey();
            String fileName = fullPath.substring(fullPath.lastIndexOf("/") + 1);
            String peerIp = entry.getValue().getValue().getIpAddress();

            String uniqueKey = fileName + "@" + peerIp;
            fileOccurrences.put(uniqueKey, fileOccurrences.getOrDefault(uniqueKey, 0) + 1);
            filePeerMap.put(fileName + "@" + peerIp, entry.getValue());
        }

        for (Map.Entry<String, Map.Entry<String, Peer>> entry : files.entrySet()) {
            String fullPath = entry.getKey();
            String fileName = fullPath.substring(fullPath.lastIndexOf("/") + 1);
            String peerIp = entry.getValue().getValue().getIpAddress();

            String uniqueKey = fileName + "@" + peerIp;

            displayCounts.put(uniqueKey, displayCounts.getOrDefault(uniqueKey, 0) + 1);
            int displayCount = displayCounts.get(uniqueKey);

            String displayName = fileOccurrences.get(uniqueKey) > 1
                    ? fileName + " (" + displayCount + ")"
                    : fileName;

            model.addElement(displayName + " from " + peerIp);
        }

        getList().setModel(model);
    }

    public void clearList() {
        DefaultListModel<String> model = (DefaultListModel<String>) getList().getModel();
        model.clear();
        filePeerMap.clear();
    }

    public Map<String, Map.Entry<String, Peer>> getFilePeerMap() {
        return filePeerMap;
    }

    private void initializeSelectionListener() {
        getList().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                JList<String> list = (JList<String>) e.getSource();
                String selectedValue = list.getSelectedValue();

                if (selectedValue != null) {
                    String[] parts = selectedValue.split(" from ");
                    if (parts.length == 2) {
                        String fileNameWithCount = parts[0];
                        String peerIp = parts[1];

                        String fileName = fileNameWithCount.replaceAll(" \\(\\d+\\)$", "");
                        String uniqueKey = fileName + "@" + peerIp;

                        Map.Entry<String, Peer> fileInfo = filePeerMap.get(uniqueKey);
                        if (fileInfo != null) {
                            String hash = fileInfo.getKey();
                            System.out.println("Selected file hash: " + hash);
                        }
                    }
                }
            }
        });
    }
}
