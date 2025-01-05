package main.p2p.view.components;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public abstract class AbstractFolderPanel extends JPanel {
    private final JTextField folderField;
    private final JButton setFolderButton;

    public AbstractFolderPanel(String title, String defaultPath) {
        this.setLayout(new BorderLayout(5, 5));
        this.setBorder(BorderFactory.createTitledBorder(title));

        folderField = new JTextField(defaultPath);
        setFolderButton = new JButton("Set");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.add(setFolderButton);

        this.add(folderField, BorderLayout.CENTER);
        this.add(buttonPanel, BorderLayout.EAST);
        
        setFolderButton.setPreferredSize(new Dimension(60, 25));
        setFolderButton.setMinimumSize(new Dimension(60, 25));
        setFolderButton.setMaximumSize(new Dimension(60, 25));
    }

    public String getFolderPath() {
        return folderField.getText();
    }

    public JTextField getFolderField() {
        return folderField;
    }

    public JButton getSetFolderButton() {
        return setFolderButton;
    }

    public abstract void onFolderSelected(String path);
}
