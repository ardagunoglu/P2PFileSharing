package main.p2p.controller;

import main.p2p.model.FileChunk;
import main.p2p.model.P2PModel;
import main.p2p.model.Peer;
import main.p2p.networking.FileSearchHandler;
import main.p2p.networking.FileTransferManager;
import main.p2p.networking.PeerConnection;
import main.p2p.util.NetworkUtils;
import main.p2p.view.P2PView;

import javax.swing.*;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class P2PController {
    private final P2PModel model;
    private final P2PView view;
    private final FileTransferManager fileTransferManager;
    private PeerConnection peerConnection;
    private final ExecutorService executorService;

    public P2PController() {
        model = new P2PModel();
        view = new P2PView();
        fileTransferManager = new FileTransferManager();
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
    	    	peerConnection = new PeerConnection(
    	    			model,
    	    			this,
    	    			view.getSettingsPanel().getExcludeFilesMasksPanel().getExcludeFilesMasksList(),
    	    			view.getSettingsPanel().getFolderExclusionPanel().getExcludeFoldersPanel().getExcludeFoldersList(),
    	    			view.getSettingsPanel().getFolderExclusionPanel().getRootOnlyCheckBox().isSelected()
    	    		);
    	    	 if (!model.getPeers().stream().anyMatch(peer -> peer.getPeerId().equals("local_peer"))) {
    	             Peer localPeer = new Peer("local_peer", NetworkUtils.getLocalAddress().getHostAddress(), 4000);
    	             model.addPeer(localPeer);
    	         }
    	        peerConnection.connectPeers();
    	        JOptionPane.showMessageDialog(view, "Connected P2P network");
    	    } catch (Exception ex) {
    	        JOptionPane.showMessageDialog(view, "Failed to connect: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    	    }
    	});

        view.getDisconnectMenuItem().addActionListener(e -> {
            model.setConnected(false);
            view.getConnectMenuItem().setEnabled(true);
            view.getDisconnectMenuItem().setEnabled(false);

            peerConnection.sendDisconnectMessage();
            model.getPeers().forEach(model::removePeer);
            peerConnection.stopConnection();
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
            
            peerConnection.setRootOnly(selected);

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
        
        view.getFoundFilesPanel().getList().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    JList<String> list = (JList<String>) e.getSource();
                    int index = list.locationToIndex(e.getPoint());
                    if (index != -1) {
                        String selectedValue = list.getModel().getElementAt(index);

                        if (selectedValue != null) {
                            String[] parts = selectedValue.split(" from ");
                            if (parts.length == 2) {
                                String fileNameWithCount = parts[0];
                                String peerIp = parts[1];

                                String fileName = fileNameWithCount.replaceAll(" \\(\\d+\\)$", "");
                                String uniqueKey = fileName + "@" + peerIp;

                                Map.Entry<String, Peer> fileInfo = view.getFoundFilesPanel().getFilePeerMap().get(uniqueKey);
                                if (fileInfo != null) {
                                    String hash = fileInfo.getKey();
                                    System.out.println("Sending hash query to peers: " + hash);

                                    peerConnection.sendHashQuery(hash);

                                    SwingUtilities.invokeLater(() -> {
                                        view.getDownloadingFilesPanel().addDownloadingFile(uniqueKey, fileInfo);
                                        System.out.println("Added file to Downloading Files panel: " + uniqueKey);

                                        ((DefaultListModel<String>) list.getModel()).remove(index);
                                        view.getFoundFilesPanel().getFilePeerMap().remove(uniqueKey);
                                        System.out.println("Removed file from Found Files panel: " + selectedValue);
                                    });
                                }
                            }
                        }
                    }
                }
            }
        });

        view.getSearchPanel().getSearchButton().addActionListener(e -> {
            if (!model.isConnected()) {
                JOptionPane.showMessageDialog(view, "You need to connect to the network first!");
                return;
            }

            String searchQuery = view.getSearchPanel().getSearchField().getText().trim();
            if (searchQuery.isEmpty()) {
                JOptionPane.showMessageDialog(view, "Search field cannot be empty!");
                return;
            }

            view.getFoundFilesPanel().clearList();

            Map<String, Map.Entry<String, Peer>> foundFiles = searchFilesFromPeers(searchQuery);

            System.out.println("Found files count: " + foundFiles.size());

            if (foundFiles.isEmpty()) {
                JOptionPane.showMessageDialog(view, "No files found matching the query: " + searchQuery);
            } else {
                JOptionPane.showMessageDialog(view, "Search completed. Files found and listed.");
                view.getFoundFilesPanel().updateFoundFilesList(foundFiles);
            }
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
    
    public P2PView getView() {
        return view;
    }
    
    private Map<String, Map.Entry<String, Peer>> searchFilesFromPeers(String searchQuery) {
        Map<String, Map.Entry<String, Peer>> allFoundFiles = new HashMap<>();
        FileSearchHandler fileSearchHandler = new FileSearchHandler();

        for (Peer peer : model.getPeerGraph().getAllPeers()) {
            if (!peer.getPeerId().equals("local_peer")) {
                Map<String, Map.Entry<String, Peer>> peerFiles = fileSearchHandler.searchFilesFromPeer(searchQuery, peer);

                if (!peerFiles.isEmpty()) {
                    allFoundFiles.putAll(peerFiles);
                } else {
                    System.out.println("No files found for query '" + searchQuery + "' on peer: " + peer.getIpAddress());
                }
            }
        }

        return allFoundFiles;
    }
}