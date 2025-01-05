package main.p2p.view.components;

import javax.swing.*;

public class DownloadingFilesPanel extends AbstractListPanel {

    public DownloadingFilesPanel() {
        super("Downloading files");
    }

    @Override
    public void onElementAdded(String element) {
        System.out.println("Downloading file added: " + element);
    }

    @Override
    public void onElementRemoved(String element) {
        System.out.println("Downloading file removed: " + element);
    }

    public JList<String> getFileList() {
        return super.getList();
    }
}
