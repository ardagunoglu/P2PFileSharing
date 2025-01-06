package main.p2p.view.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.*;

public class ExcludeFoldersPanel extends AbstractSettingsSection {
    private JList<String> excludeFoldersList;
    private JButton addFolderButton;
    private JButton delFolderButton;

    public ExcludeFoldersPanel(JList<String> excludeFoldersList, JButton addFolderButton, JButton delFolderButton) {
        super("Exclude files under these folders");
        this.excludeFoldersList = excludeFoldersList;
        this.addFolderButton = addFolderButton;
        this.delFolderButton = delFolderButton;
        setupComponents();
    }

    @Override
    protected void setupComponents() {
        this.setLayout(new BorderLayout(5, 5));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.add(addFolderButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(delFolderButton);

        JScrollPane scrollPane = new JScrollPane(excludeFoldersList);
        scrollPane.setPreferredSize(new Dimension(150, 125));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        JPanel listAndButtonsPanel = new JPanel(new BorderLayout(5, 5));
        listAndButtonsPanel.add(scrollPane, BorderLayout.CENTER);
        listAndButtonsPanel.add(buttonPanel, BorderLayout.EAST);

        this.delFolderButton.setEnabled(false);

        addFolderButton.setPreferredSize(const_dimension_button);
        addFolderButton.setMinimumSize(const_dimension_button);
        addFolderButton.setMaximumSize(const_dimension_button);

        delFolderButton.setPreferredSize(const_dimension_button);
        delFolderButton.setMinimumSize(const_dimension_button);
        delFolderButton.setMaximumSize(const_dimension_button);

        this.add(listAndButtonsPanel, BorderLayout.CENTER);
    }

    public JList<String> getExcludeFoldersList() {
        return excludeFoldersList;
    }

    public JButton getAddFolderButton() {
        return addFolderButton;
    }

    public JButton getDelFolderButton() {
        return delFolderButton;
    }
}
