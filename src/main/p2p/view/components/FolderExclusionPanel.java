package main.p2p.view.components;

import javax.swing.*;
import java.awt.*;

public class FolderExclusionPanel extends AbstractSettingsSection {
    private JCheckBox rootOnlyCheckBox;
    private ExcludeFoldersPanel excludeFoldersPanel;

    public FolderExclusionPanel(JCheckBox rootOnlyCheckBox, ExcludeFoldersPanel excludeFoldersPanel) {
        super("Folder exclusion");
        this.rootOnlyCheckBox = rootOnlyCheckBox;
        this.excludeFoldersPanel = excludeFoldersPanel;
        setupComponents();
    }

    @Override
    protected void setupComponents() {
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(rootOnlyCheckBox, BorderLayout.NORTH);

        this.setLayout(new BorderLayout(5, 5));
        this.add(northPanel, BorderLayout.NORTH);
        this.add(excludeFoldersPanel, BorderLayout.CENTER);
    }

    public ExcludeFoldersPanel getExcludeFoldersPanel() {
        return excludeFoldersPanel;
    }

    public JCheckBox getRootOnlyCheckBox() {
        return rootOnlyCheckBox;
    }
}
