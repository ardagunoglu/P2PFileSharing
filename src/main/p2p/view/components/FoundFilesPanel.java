package main.p2p.view.components;

import main.p2p.model.Peer;

import javax.swing.*;
import java.util.Set;

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

    public void updatePeersList(Set<Peer> peers) {
        DefaultListModel<String> model = new DefaultListModel<>();
        for (Peer peer : peers) {
            model.addElement(peer.toString());
        }
        getList().setModel(model);
    }
}
