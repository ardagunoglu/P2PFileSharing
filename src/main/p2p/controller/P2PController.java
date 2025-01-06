package main.p2p.controller;

import main.p2p.model.FileChunk;
import main.p2p.model.P2PModel;
import main.p2p.model.Peer;
import main.p2p.networking.FileTransferManager;
import main.p2p.networking.PeerDiscovery;
import main.p2p.util.NetworkUtils;
import main.p2p.view.P2PView;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class P2PController {
    private final P2PModel model;
    private final P2PView view;
    private final FileTransferManager fileTransferManager;
    private PeerDiscovery peerDiscovery;
    private final ExecutorService executorService;

    public P2PController() {
        model = new P2PModel();
        view = new P2PView();
        fileTransferManager = new FileTransferManager();
        peerDiscovery = new PeerDiscovery(model, this);
        executorService = Executors.newCachedThreadPool();

        initializeListeners();
        initializeFileTransfer();
    }

    private void initializeListeners() {
    	view.getConnectMenuItem().addActionListener(e -> {
    	    model.setConnected(true);
    	    view.getConnectMenuItem().setEnabled(false);
    	    view.getDisconnectMenuItem().setEnabled(true);

    	    try {
    	        peerDiscovery.discoverPeers();
    	    } catch (Exception ex) {
    	        JOptionPane.showMessageDialog(view, "Failed to connect: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    	    }
    	});

        view.getDisconnectMenuItem().addActionListener(e -> {
            model.setConnected(false);
            view.getConnectMenuItem().setEnabled(true);
            view.getDisconnectMenuItem().setEnabled(false);

            peerDiscovery.stopDiscovery();
            JOptionPane.showMessageDialog(view, "Disconnected from P2P network");
        });

        view.getSharedFolderPanel().getSetFolderButton().addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fileChooser.showOpenDialog(view) == JFileChooser.APPROVE_OPTION) {
                File folder = fileChooser.getSelectedFile();
                view.getSharedFolderPanel().getFolderField().setText(folder.getAbsolutePath());
                model.setSharedFolderPath(folder.getAbsolutePath());
            }
        });

        view.getDestinationPanel().getSetFolderButton().addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fileChooser.showOpenDialog(view) == JFileChooser.APPROVE_OPTION) {
                File folder = fileChooser.getSelectedFile();
                view.getDestinationPanel().getFolderField().setText(folder.getAbsolutePath());
                model.setDestinationPath(folder.getAbsolutePath());
            }
        });

        view.getSettingsPanel().getFolderExclusionPanel().getRootOnlyCheckBox().addActionListener(e -> {
            boolean selected = view.getSettingsPanel().getFolderExclusionPanel().getRootOnlyCheckBox().isSelected();
            view.getSettingsPanel().getFolderExclusionPanel().getExcludeFoldersPanel().getExcludeFoldersList().setEnabled(!selected);
            view.getSettingsPanel().getFolderExclusionPanel().getExcludeFoldersPanel().getAddFolderButton().setEnabled(!selected);
            view.getSettingsPanel().getFolderExclusionPanel().getExcludeFoldersPanel().setEnabled(!selected);

            if (selected) {
                ((DefaultListModel<String>) view.getSettingsPanel().getFolderExclusionPanel().getExcludeFoldersPanel().getExcludeFoldersList().getModel()).clear();
                view.getSettingsPanel().getFolderExclusionPanel().getExcludeFoldersPanel().getDelFolderButton().setEnabled(false);
            } else {
                updateDelFolderButtonState();
            }
        });

        view.getSettingsPanel().getFolderExclusionPanel().getExcludeFoldersPanel().getExcludeFoldersList().addListSelectionListener(e -> updateDelFolderButtonState());
        view.getSettingsPanel().getExcludeFilesMasksPanel().getExcludeFilesMasksList().addListSelectionListener(e -> updateDelFileButtonState());

        view.getSettingsPanel().getFolderExclusionPanel().getExcludeFoldersPanel().getAddFolderButton().addActionListener(e -> {
            String folder = JOptionPane.showInputDialog(view, "Enter folder to exclude:");
            if (folder != null && !folder.trim().isEmpty()) {
                ((DefaultListModel<String>) view.getSettingsPanel().getFolderExclusionPanel().getExcludeFoldersPanel().getExcludeFoldersList().getModel()).addElement(folder.trim());
            }
        });

        view.getSettingsPanel().getExcludeFilesMasksPanel().getAddFileMasksButton().addActionListener(e -> {
            String file = JOptionPane.showInputDialog(view, "Enter file mask to exclude:");
            if (file != null && !file.trim().isEmpty()) {
                ((DefaultListModel<String>) view.getSettingsPanel().getExcludeFilesMasksPanel().getExcludeFilesMasksList().getModel()).addElement(file.trim());
            }
        });

        view.getSettingsPanel().getFolderExclusionPanel().getExcludeFoldersPanel().getDelFolderButton().addActionListener(e -> {
            int selectedIndex = view.getSettingsPanel().getFolderExclusionPanel().getExcludeFoldersPanel().getExcludeFoldersList().getSelectedIndex();
            if (selectedIndex != -1) {
                ((DefaultListModel<String>) view.getSettingsPanel().getFolderExclusionPanel().getExcludeFoldersPanel().getExcludeFoldersList().getModel()).remove(selectedIndex);
                updateDelFolderButtonState();
            }
        });

        view.getSettingsPanel().getExcludeFilesMasksPanel().getDelFileMasksButton().addActionListener(e -> {
            int selectedIndex = view.getSettingsPanel().getExcludeFilesMasksPanel().getExcludeFilesMasksList().getSelectedIndex();
            if (selectedIndex != -1) {
                ((DefaultListModel<String>) view.getSettingsPanel().getExcludeFilesMasksPanel().getExcludeFilesMasksList().getModel()).remove(selectedIndex);
                updateDelFileButtonState();
            }
        });

        view.getSearchPanel().getSearchButton().addActionListener(e -> {
            boolean isConnected = model.isConnected();
            if (!isConnected) {
                JOptionPane.showMessageDialog(view, "You should connect to the network first!");
                return;
            }

            JOptionPane.showMessageDialog(view, "Searching for files...");
        });
    }

    private void updateDelFolderButtonState() {
        boolean hasSelection = !view.getSettingsPanel().getFolderExclusionPanel().getExcludeFoldersPanel().getExcludeFoldersList().isSelectionEmpty();
        view.getSettingsPanel().getFolderExclusionPanel().getExcludeFoldersPanel().getDelFolderButton().setEnabled(hasSelection);
    }

    private void updateDelFileButtonState() {
        boolean hasSelection = !view.getSettingsPanel().getExcludeFilesMasksPanel().getExcludeFilesMasksList().isSelectionEmpty();
        view.getSettingsPanel().getExcludeFilesMasksPanel().getDelFileMasksButton().setEnabled(hasSelection);
    }

    private void initializeFileTransfer() {
        fileTransferManager.startListening(5000, chunk -> {
            System.out.println("Received chunk: " + chunk.getChunkIndex());
            // Handle received chunk (e.g., save to file, update progress, etc.)
        });
    }

    public void showGUI() {
        view.setVisible(true);
    }
    
    public void updateUIList() {
    	SwingUtilities.invokeLater(() -> {
            view.getFoundFilesPanel().updatePeersList(new HashSet<>(model.getPeers()));
        });
    }
}