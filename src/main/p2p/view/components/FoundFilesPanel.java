package main.p2p.view.components;

import main.p2p.model.Peer;

import javax.swing.*;
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
        for (Map.Entry<String, Peer> entry : files.entrySet()) {
            String fileDisplay = entry.getKey() + " (from " + entry.getValue().getIpAddress() + ")";
            model.addElement(fileDisplay);
        }
        getList().setModel(model);
    }
    
    public void clearList() {
        DefaultListModel<String> model = (DefaultListModel<String>) getList().getModel();
        model.clear();
    }
}
