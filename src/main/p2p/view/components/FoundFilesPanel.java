package main.p2p.view.components;

import javax.swing.*;

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

    public JList<String> getFileList() {
        return super.getList();
    }
}
