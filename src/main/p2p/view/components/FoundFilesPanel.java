package main.p2p.view.components;

import main.p2p.model.Peer;

import javax.swing.*;

import java.util.HashMap;
import java.util.Map;

public class FoundFilesPanel extends AbstractListPanel {

    public FoundFilesPanel() {
        super("Found files");
    }

    @Override
    public void onElementAdded(String element) {
        System.out.println("Found file added: " + element);
    }

    @Override
    public void onElementRemoved(String element) {
        System.out.println("Found file removed: " + element);
    }

    public void updateFoundFilesList(Map<String, Peer> files) {
        DefaultListModel<String> model = new DefaultListModel<>();
        Map<String, Integer> fileNameCounts = new HashMap<>();

        for (Map.Entry<String, Peer> entry : files.entrySet()) {
            String relativePath = entry.getKey();
            String fileName = relativePath.substring(relativePath.lastIndexOf("/") + 1);

            fileNameCounts.put(fileName, fileNameCounts.getOrDefault(fileName, 0) + 1);
            int count = fileNameCounts.get(fileName);

            String fileDisplay = count > 1
                ? fileName + " (" + count + ")"
                : fileName;

            model.addElement(fileDisplay + " (from " + entry.getValue().getIpAddress() + ")");
        }

        getList().setModel(model);
    }

    public void clearList() {
        DefaultListModel<String> model = (DefaultListModel<String>) getList().getModel();
        model.clear();
    }
}
