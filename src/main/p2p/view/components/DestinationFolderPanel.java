package main.p2p.view.components;

import javax.swing.*;

public class DestinationFolderPanel extends AbstractFolderPanel {

    public DestinationFolderPanel() {
        super("Destination folder", "C:\\P2P Downloads\\");
    }

    @Override
    public void onFolderSelected(String path) {
        System.out.println("Destination folder selected: " + path);
    }

    public JTextField getFolderField() {
        return super.getFolderField();
    }

    public JButton getSetFolderButton() {
        return super.getSetFolderButton();
    }
}
