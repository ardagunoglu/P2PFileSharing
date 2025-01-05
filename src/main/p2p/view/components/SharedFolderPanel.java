package main.p2p.view.components;

import javax.swing.*;

public class SharedFolderPanel extends AbstractFolderPanel {

    public SharedFolderPanel() {
        super("Root of the P2P shared folder", "C:\\My Shared Folder\\");
    }

    @Override
    public void onFolderSelected(String path) {
        System.out.println("Shared folder selected: " + path);
    }

    public JTextField getFolderField() {
        return super.getFolderField();
    }

    public JButton getSetFolderButton() {
        return super.getSetFolderButton();
    }
}
