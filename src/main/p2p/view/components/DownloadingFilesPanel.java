package main.p2p.view.components;

import main.p2p.model.Peer;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class DownloadingFilesPanel extends AbstractListPanel {
    private final Map<String, Map.Entry<String, Peer>> downloadingFilePeerMap;

    public DownloadingFilesPanel() {
        super("Downloading files");
        downloadingFilePeerMap = new HashMap<>();
    }

    @Override
    public void onElementAdded(String element) {
        System.out.println("Downloading file added: " + element);
    }

    @Override
    public void onElementRemoved(String element) {
        System.out.println("Downloading file removed: " + element);
    }

    public void addDownloadingFile(String uniqueKey, Map.Entry<String, Peer> fileInfo) {
        downloadingFilePeerMap.put(uniqueKey, fileInfo);
        addElement(uniqueKey);
    }

    public void removeDownloadingFile(String uniqueKey) {
        downloadingFilePeerMap.remove(uniqueKey);
        DefaultListModel<String> model = (DefaultListModel<String>) getList().getModel();
        model.removeElement(uniqueKey);
    }

    public Map<String, Map.Entry<String, Peer>> getDownloadingFilePeerMap() {
        return downloadingFilePeerMap;
    }
}
