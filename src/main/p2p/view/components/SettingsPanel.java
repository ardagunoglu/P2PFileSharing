package main.p2p.view.components;

import javax.swing.*;
import java.awt.*;

public class SettingsPanel extends AbstractSettingsSection {
    private FolderExclusionPanel folderExclusionPanel;
    private ExcludeFilesMasksPanel excludeFilesMasksPanel;

    public SettingsPanel(FolderExclusionPanel folderExclusionPanel, ExcludeFilesMasksPanel excludeFilesMasksPanel) {
        super("Settings");
        this.folderExclusionPanel = folderExclusionPanel;
        this.excludeFilesMasksPanel = excludeFilesMasksPanel;
        setupComponents();
    }

    @Override
    protected void setupComponents() {
        JPanel settingsContent = new JPanel(new GridLayout(1, 2, 5, 5));
        settingsContent.add(folderExclusionPanel);
        settingsContent.add(excludeFilesMasksPanel);

        this.setLayout(new BorderLayout(5, 5));
        this.add(settingsContent, BorderLayout.CENTER);
    }

    public FolderExclusionPanel getFolderExclusionPanel() {
        return folderExclusionPanel;
    }

    public ExcludeFilesMasksPanel getExcludeFilesMasksPanel() {
        return excludeFilesMasksPanel;
    }
}
