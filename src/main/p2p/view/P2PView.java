package main.p2p.view;

import main.p2p.view.components.DestinationFolderPanel;
import main.p2p.view.components.DownloadingFilesPanel;
import main.p2p.view.components.ExcludeFilesMasksPanel;
import main.p2p.view.components.ExcludeFoldersPanel;
import main.p2p.view.components.FolderExclusionPanel;
import main.p2p.view.components.FoundFilesPanel;
import main.p2p.view.components.SearchPanel;
import main.p2p.view.components.SettingsPanel;
import main.p2p.view.components.SharedFolderPanel;
import main.p2p.view.menu.MenuBar;

import javax.swing.*;
import java.awt.*;

public class P2PView extends JFrame {
	private SharedFolderPanel sharedFolderPanel;
    private DestinationFolderPanel destinationFolderPanel;
    private DownloadingFilesPanel downloadingFilesPanel;
    private FoundFilesPanel foundFilesPanel;
    private SettingsPanel settingsPanel;
    private SearchPanel searchPanel;
    private MenuBar menuBar;

    public P2PView() {
        initializeComponents();
        setupLayout();
        setupMenuBar();
    }

    private void initializeComponents() {
    	sharedFolderPanel = new SharedFolderPanel();
        destinationFolderPanel = new DestinationFolderPanel();

    	JCheckBox rootOnlyCheckBox = new JCheckBox("Check new files only in the root");
        JList<String> excludeFoldersList = new JList<>(new DefaultListModel<>());
        JButton addFolderButton = new JButton("Add");
        JButton delFolderButton = new JButton("Del");
        ExcludeFoldersPanel excludeFoldersPanel = new ExcludeFoldersPanel(excludeFoldersList, addFolderButton, delFolderButton);

        FolderExclusionPanel folderExclusionPanel = new FolderExclusionPanel(rootOnlyCheckBox, excludeFoldersPanel);

        JList<String> excludeFilesMasksList = new JList<>(new DefaultListModel<>());
        JButton addFileMasksButton = new JButton("Add");
        JButton delFileMasksButton = new JButton("Del");
        ExcludeFilesMasksPanel excludeFilesMasksPanel = new ExcludeFilesMasksPanel(excludeFilesMasksList, addFileMasksButton, delFileMasksButton);

        settingsPanel = new SettingsPanel(folderExclusionPanel, excludeFilesMasksPanel);

        downloadingFilesPanel = new DownloadingFilesPanel();
        foundFilesPanel = new FoundFilesPanel();

        JButton searchButton = new JButton("Search");
        JTextField searchField = new JTextField();
        searchPanel = new SearchPanel(searchButton, searchField);

        setTitle("P2P File Sharing");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(740, 790);
        setResizable(false);
    }

    private void setupLayout() {
        setLayout(new BorderLayout(5, 5));
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;

        mainPanel.add(sharedFolderPanel, gbc);

        gbc.gridy = 1;
        mainPanel.add(destinationFolderPanel, gbc);

        gbc.gridy = 2;
        mainPanel.add(settingsPanel, gbc);

        gbc.gridy = 3;
        mainPanel.add(downloadingFilesPanel, gbc);

        gbc.gridy = 4;
        mainPanel.add(foundFilesPanel, gbc);

        gbc.gridy = 5;
        mainPanel.add(searchPanel, gbc);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.add(new JScrollPane(mainPanel), BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);
    }

    private void setupMenuBar() {
        menuBar = new MenuBar(
                e -> System.exit(0),
                e -> JOptionPane.showMessageDialog(this,
                        "P2P File Sharing Application\nDeveloped by: Kadir Arda Günoğlu",
                        "About",
                        JOptionPane.INFORMATION_MESSAGE)
        );
        setJMenuBar(menuBar);
    }

    public JMenuItem getConnectMenuItem() { return menuBar.getConnectMenuItem(); }
    public JMenuItem getDisconnectMenuItem() { return menuBar.getDisconnectMenuItem(); }
    public SharedFolderPanel getSharedFolderPanel() { return sharedFolderPanel; }
    public DestinationFolderPanel getDestinationPanel() { return destinationFolderPanel; }
    public DownloadingFilesPanel getDownloadingFilesPanel() { return downloadingFilesPanel; }
    public FoundFilesPanel getFoundFilesPanel() { return foundFilesPanel; }
    public SettingsPanel getSettingsPanel() { return settingsPanel; }
    public SearchPanel getSearchPanel() { return searchPanel; }
}
