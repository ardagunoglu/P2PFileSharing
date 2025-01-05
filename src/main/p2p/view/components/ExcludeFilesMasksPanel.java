package main.p2p.view.components;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.*;

public class ExcludeFilesMasksPanel extends AbstractSettingsSection {
    private JList<String> excludeFilesMasksList;
    private JButton addFileMasksButton;
    private JButton delFileMasksButton;

    public ExcludeFilesMasksPanel(JList<String> excludeFilesMasksList, JButton addFileButton, JButton delFileButton) {
        super("Exclude files matching these masks");
        this.excludeFilesMasksList = excludeFilesMasksList;
        this.addFileMasksButton = addFileButton;
        this.delFileMasksButton = delFileButton;
        setupComponents();
    }

    @Override
    protected void setupComponents() {
        this.setLayout(new BorderLayout(5, 5));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.add(addFileMasksButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(delFileMasksButton);

        JPanel listAndButtonsPanel = new JPanel(new BorderLayout(5, 5));
        listAndButtonsPanel.add(new JScrollPane(excludeFilesMasksList), BorderLayout.CENTER);
        listAndButtonsPanel.add(buttonPanel, BorderLayout.EAST);
        
        this.delFileMasksButton.setEnabled(false);
        
        addFileMasksButton.setPreferredSize(const_dimension_button);
        addFileMasksButton.setMinimumSize(const_dimension_button);
        addFileMasksButton.setMaximumSize(const_dimension_button);
        
        delFileMasksButton.setPreferredSize(const_dimension_button);
        delFileMasksButton.setMinimumSize(const_dimension_button);
        delFileMasksButton.setMaximumSize(const_dimension_button);

        this.add(listAndButtonsPanel, BorderLayout.CENTER);
    }

    public JList<String> getExcludeFilesMasksList() {
        return excludeFilesMasksList;
    }

    public JButton getAddFileMasksButton() {
        return addFileMasksButton;
    }

    public JButton getDelFileMasksButton() {
        return delFileMasksButton;
    }
}
