package main.p2p.networking;

import main.p2p.controller.P2PController;
import main.p2p.file.FileManager;
import main.p2p.file.FileSearchManager;
import main.p2p.model.P2PModel;
import main.p2p.model.Peer;
import main.p2p.util.NetworkUtils;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JList;
import javax.swing.SwingUtilities;

public class PeerConnection {
    private static final int CONNECTION_PORT = 4000;
    private final P2PModel model;
    private final Set<Peer> foundPeers = new HashSet<>();
    private final List<Map.Entry<Peer, String>> matchFoundPeers = new ArrayList<>();
    private final Map<String, Map.Entry<String, String>> nearestFile = new HashMap<>();
    private final BlockingQueue<DatagramPacket> packetQueue = new LinkedBlockingQueue<>();
    private final Map<String, byte[]> downloadedFiles = new HashMap<>();
    private final Map<String, Integer> chunkCountMap = new HashMap<>();
    private boolean running = true;
    private DatagramSocket socket;
    private P2PController controller;
    private final JList<String> excludeFilesMasksList;
    private final JList<String> excludeFoldersList;
    private boolean rootOnly;
    
    private final CountDownLatch disconnectLatch = new CountDownLatch(1);

    public PeerConnection(P2PModel model, P2PController controller, JList<String> excludeFilesMasksList, JList<String> excludeFoldersList, boolean rootOnly) {
        this.model = model;
        try {
            this.socket = new DatagramSocket(CONNECTION_PORT);
            this.socket.setBroadcast(true);
        } catch (SocketException e) {
            throw new RuntimeException("Error initializing socket: " + e.getMessage(), e);
        }
        this.controller = controller;
        this.excludeFilesMasksList = excludeFilesMasksList;
        this.excludeFoldersList = excludeFoldersList;
        this.rootOnly = rootOnly;
    }

    public void connectPeers() {
        if (socket == null || socket.isClosed()) {
            try {
                socket = new DatagramSocket(CONNECTION_PORT);
                socket.setBroadcast(true);
                System.out.println("Socket reinitialized for connection.");
            } catch (SocketException e) {
                throw new RuntimeException("Error reinitializing socket: " + e.getMessage(), e);
            }
        }

        sendConnectMessage();

        if (running) {
            listenForResponses();
        }
    }

    private void sendConnectMessage() {
        new Thread(() -> {
            try {
                String connectMessage = MessageType.P2P_CONNECT_ME.getValue();
                DatagramPacket packet = new DatagramPacket(
                        connectMessage.getBytes(),
                        connectMessage.length(),
                        InetAddress.getByName("192.168.1.255"),
                        CONNECTION_PORT
                );

                socket.send(packet);
                System.out.println("Connection message sent to broadcast address.");

            } catch (Exception e) {
                System.err.println("Error sending connection message: " + e.getMessage());
            }
        }).start();
    }
    
    public void sendDisconnectMessage() {
        new Thread(() -> {
            try {
                String disconnectMessage = MessageType.P2P_DISCONNECT.getValue();
                DatagramPacket packet = new DatagramPacket(
                        disconnectMessage.getBytes(),
                        disconnectMessage.length(),
                        InetAddress.getByName("192.168.1.255"),
                        CONNECTION_PORT
                );

                socket.send(packet);
                System.out.println("Disconnect message sent to all peers.");

                synchronized (foundPeers) {
                    foundPeers.clear();
                    model.getPeerGraph().getAllPeers().forEach(model::removePeer);
                    System.out.println("Cleared local peers and graph.");
                }
            } catch (Exception e) {
                System.err.println("Error sending disconnect message: " + e.getMessage());
            } finally {
                disconnectLatch.countDown();
            }
        }).start();
    }

    private void listenForResponses() {
        new Thread(() -> {
            try {
                InetAddress localAddress = NetworkUtils.getLocalAddress();
                System.out.println("Local address detected: " + localAddress);

                while (running) {
                    byte[] buffer = new byte[4096];
                    DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(receivedPacket);
                    
                    if (!running) break;

                    String receivedData = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
                    InetAddress senderAddress = receivedPacket.getAddress();

                    if (senderAddress.equals(localAddress)) {
                        System.out.println("Ignored message from self: " + senderAddress);
                        continue;
                    }

                    System.out.println("Received data: " + receivedData + " from " + senderAddress);
                    
                    packetQueue.put(receivedPacket);
                }
            } catch (Exception e) {
                if (running) {
                    System.err.println("Error in listener socket: " + e.getMessage());
                }
            }
        }).start();
        
        new Thread(() -> {
            while (running) {
                try {
                    DatagramPacket packet = packetQueue.take();
                    processResponse(packet);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Processing thread interrupted: " + e.getMessage());
                }
            }
        }).start();
    }
    
    private void processResponse(DatagramPacket packet) {
    	
    	String receivedData = new String(packet.getData(), 0, packet.getLength());
        InetAddress senderAddress = packet.getAddress();
    	
    	if (receivedData.startsWith("SEARCH:")) {
            handleSearchRequest(receivedData, senderAddress, packet.getPort());
        } else if (receivedData.startsWith("QUERY_HASH:")) {
            handleHashQuery(receivedData.substring(11), senderAddress, packet.getPort());
        } else if (receivedData.startsWith("MATCH_FOUND:")) {
            handleMatchFound(receivedData, senderAddress);
        } else if (receivedData.startsWith("REQUEST_CHUNK:")) {
            handleChunkRequest(receivedData.substring(14), senderAddress, packet.getPort());
        } else if (receivedData.startsWith("RESPONSE_CHUNK:")) {
            handleChunkResponse(receivedData.substring(15));
        } else {
            try {
				handlePeerConnectionMessages(receivedData, packet, senderAddress);
			} catch (SocketException e) {
				e.printStackTrace();
			}
        }
    }
    
    private void handleChunkRequest(String data, InetAddress requesterAddress, int requesterPort) {
        try {
            String[] parts = data.split("\\|");
            if (parts.length != 2) return;

            String filePath = parts[0].trim();
            int chunkIndex = Integer.parseInt(parts[1].trim());

            FileManager fileManager = new FileManager(filePath);
            byte[] chunkData = fileManager.getChunk(chunkIndex);

            if (chunkData != null) {
                String response = "RESPONSE_CHUNK:" + filePath + "|" + chunkIndex + "|" + new String(chunkData);
                DatagramPacket responsePacket = new DatagramPacket(
                    response.getBytes(),
                    response.length(),
                    requesterAddress,
                    requesterPort
                );
                socket.send(responsePacket);
                System.out.println("Sent chunk " + chunkIndex + " of file " + filePath + " to " + requesterAddress);
            } else {
                System.err.println("Chunk " + chunkIndex + " not found for file " + filePath);
            }
        } catch (Exception e) {
            System.err.println("Error handling chunk request: " + e.getMessage());
        }
    }
    
    public void downloadFileFromPeers(String filePath, FileManager fileManager) {
        int totalChunks = fileManager.getTotalChunks();
        chunkCountMap.put(filePath, totalChunks);
        System.out.println("Total chunks to download: " + totalChunks);

        List<Peer> peersForFile = new ArrayList<>();
        synchronized (matchFoundPeers) {
            for (Map.Entry<Peer, String> entry : matchFoundPeers) {
                if (entry.getValue().equals(filePath)) {
                    peersForFile.add(entry.getKey());
                }
            }
        }

        if (peersForFile.isEmpty()) {
            System.out.println("No peers found for the file: " + filePath);
            return;
        }

        int currentChunkIndex = 0;
        while (currentChunkIndex < totalChunks) {
            Peer selectedPeer = peersForFile.get(currentChunkIndex % peersForFile.size());
            String request = "REQUEST_CHUNK:" + filePath + "|" + currentChunkIndex;

            try {
                DatagramPacket requestPacket = new DatagramPacket(
                    request.getBytes(),
                    request.length(),
                    InetAddress.getByName(selectedPeer.getIpAddress()),
                    selectedPeer.getPort()
                );
                socket.send(requestPacket);
                System.out.println("Requested chunk " + currentChunkIndex + " from peer: " + selectedPeer.getIpAddress());
            } catch (Exception e) {
                System.err.println("Error requesting chunk: " + e.getMessage());
            }

            currentChunkIndex++;
        }
    }

    
    private void handleChunkResponse(String data) {
        try {
            String[] parts = data.split("\\|");
            if (parts.length != 3) return;

            String filePath = parts[0].trim();
            int chunkIndex = Integer.parseInt(parts[1].trim());
            byte[] chunkData = parts[2].getBytes();

            downloadedFiles.putIfAbsent(filePath, new byte[chunkCountMap.getOrDefault(filePath, 0) * 256]);
            System.arraycopy(chunkData, 0, downloadedFiles.get(filePath), chunkIndex * 256, chunkData.length);

            System.out.println("Received chunk " + chunkIndex + " for file " + filePath);

            if (isDownloadComplete(filePath)) {
                System.out.println("Download complete for file: " + filePath);
                saveFile(filePath);
            }
        } catch (Exception e) {
            System.err.println("Error handling chunk response: " + e.getMessage());
        }
    }

    private boolean isDownloadComplete(String filePath) {
        return downloadedFiles.get(filePath) != null &&
               downloadedFiles.get(filePath).length == chunkCountMap.getOrDefault(filePath, 0) * 256;
    }

    private void saveFile(String filePath) {
        try {
        	String destinationFolder = controller.getView().getDestinationPanel().getFolderField().getText();
            if (destinationFolder == null || destinationFolder.isEmpty()) {
                System.err.println("Destination folder is not set!");
                return;
            }

            File outputFile = new File(destinationFolder, new File(filePath).getName());
            outputFile.getParentFile().mkdirs();

            java.nio.file.Files.write(outputFile.toPath(), downloadedFiles.get(filePath));
            System.out.println("File saved to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error saving file: " + e.getMessage());
        }
    }

    private void handleHashQuery(String hash, InetAddress senderAddress, int senderPort) {
        System.out.println("Hash query received: " + hash);

        FileSearchManager fileSearchManager = new FileSearchManager(
            new File(model.getSharedFolderPath()),
            excludeFilesMasksList,
            excludeFoldersList,
            rootOnly
        );

        Map<String, String> foundFiles = fileSearchManager.searchFilesByHash(hash);

        if (!foundFiles.isEmpty()) {
            String selectedFilePath = null;
            int selectedDepth = Integer.MAX_VALUE;

            for (Map.Entry<String, String> entry : foundFiles.entrySet()) {
                String filePath = entry.getKey();
                String fileHash = entry.getValue();

                int depth = calculatePathDepth(filePath);

                if (depth < selectedDepth || (depth == selectedDepth && selectedFilePath == null)) {
                    selectedFilePath = filePath;
                    selectedDepth = depth;
                }
            }

            if (selectedFilePath != null) {
                synchronized (nearestFile) {
                    nearestFile.put(hash, Map.entry(selectedFilePath, foundFiles.get(selectedFilePath)));
                }

                String rootPath = model.getSharedFolderPath();
                File fullPath = new File(rootPath, selectedFilePath);

                try {
                    FileManager fileManager = new FileManager(fullPath.getAbsolutePath());
                    System.out.println("File split into " + fileManager.getTotalChunks() + " chunks.");

                    fileManager.printChunks();

                } catch (IOException e) {
                    System.err.println("Error splitting file into chunks: " + fullPath.getAbsolutePath() + " (" + e.getMessage() + ")");
                    return;
                }

                sendFileMatchResponse(selectedFilePath, senderAddress.getHostAddress(), senderAddress, senderPort);
                System.out.println("Selected file match found and response sent: " + selectedFilePath + " | Hash: " + hash);
            }
        } else {
            System.out.println("No matching files found for hash: " + hash);
        }
    }

    
    private int calculatePathDepth(String filePath) {
        String normalizedPath = filePath.replaceAll("^/|/$", "");

        return normalizedPath.split("/").length;
    }
    
    private void handleMatchFound(String receivedData, InetAddress senderAddress) {
        try {
            String[] parts = receivedData.substring(12).split("\\|");
            if (parts.length == 2) {
                String filePath = parts[0];
                String requesterIp = parts[1];

                Peer matchingPeer = new Peer(senderAddress.getHostAddress(), senderAddress.getHostAddress(), CONNECTION_PORT);

                synchronized (matchFoundPeers) {
                    matchFoundPeers.add(Map.entry(matchingPeer, filePath));
                }

                System.out.println("MATCH_FOUND: File " + filePath + " available from peer " + matchingPeer.getIpAddress());

                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        synchronized (matchFoundPeers) {
                            if (!matchFoundPeers.isEmpty()) {
                            	System.out.println("filepath: " + filePath);
                                downloadFileFromPeers(filePath, new FileManager(filePath));
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error starting download: " + e.getMessage());
                    }
                }).start();
            }
        } catch (Exception e) {
            System.err.println("Error handling MATCH_FOUND: " + e.getMessage());
        }
    }

    
    public List<Map.Entry<Peer, String>> getMatchFoundPeers() {
        synchronized (matchFoundPeers) {
            return new ArrayList<>(matchFoundPeers);
        }
    }

    private void sendFileMatchResponse(String filePath, String requesterIp, InetAddress senderAddress, int senderPort) {
        try {
            String response = "MATCH_FOUND:" + filePath + "|" + requesterIp;
            DatagramPacket responsePacket = new DatagramPacket(
                response.getBytes(),
                response.length(),
                senderAddress,
                senderPort
            );

            socket.send(responsePacket);
            System.out.println("MATCH_FOUND response sent for file: " + filePath + " to requester IP: " + requesterIp);
        } catch (Exception e) {
            System.err.println("Error sending MATCH_FOUND response: " + e.getMessage());
        }
    }

    
    public Map<String, Map.Entry<String, String>> getNearestFile() {
        synchronized (nearestFile) {
            return new HashMap<>(nearestFile);
        }
    }
    
    public void sendHashQuery(String hash) {
        for (Peer peer : model.getPeers()) {
            if (!peer.getPeerId().equals("local_peer")) {
                new Thread(() -> {
                    try {
                        String queryMessage = "QUERY_HASH:" + hash;
                        DatagramPacket packet = new DatagramPacket(
                            queryMessage.getBytes(),
                            queryMessage.length(),
                            InetAddress.getByName(peer.getIpAddress()),
                            peer.getPort()
                        );
                        socket.send(packet);
                        System.out.println("Sent hash query to: " + peer.getIpAddress());
                    } catch (Exception e) {
                        System.err.println("Error sending hash query: " + e.getMessage());
                    }
                }).start();
            }
        }
    }
    
    private void handleSearchRequest(String receivedData, InetAddress senderAddress, int senderPort) {
        String query = receivedData.substring(7).trim();
        System.out.println("Search query received: " + query);

        FileSearchManager fileSearchManager = new FileSearchManager(
                new File(model.getSharedFolderPath()),
                excludeFilesMasksList,
                excludeFoldersList,
                rootOnly
        );

        Map<String, Map.Entry<String, String>> foundFiles = fileSearchManager.searchFiles(query);

        StringBuilder response = new StringBuilder();
        for (Map.Entry<String, Map.Entry<String, String>> entry : foundFiles.entrySet()) {
            response.append(entry.getKey()).append("|").append(entry.getValue().getValue()).append(",");
        }

        String responseString = response.length() > 0
                ? response.substring(0, response.length() - 1)
                : "NO_FILES_FOUND";

        try {
            DatagramPacket responsePacket = new DatagramPacket(
                    responseString.getBytes(),
                    responseString.length(),
                    senderAddress,
                    senderPort
            );
            socket.send(responsePacket);
            System.out.println("Search response sent to " + senderAddress + ":" + senderPort);
        } catch (Exception e) {
            System.err.println("Error sending search response: " + e.getMessage());
        }
    }


    private void handleDisconnectMessage(String message, InetAddress senderAddress) {
        Peer disconnectingPeer = new Peer(senderAddress.getHostAddress(), senderAddress.getHostAddress(), CONNECTION_PORT);

        synchronized (foundPeers) {
            if (foundPeers.remove(disconnectingPeer)) {
                model.removePeer(disconnectingPeer);
                model.getPeerGraph().removePeer(disconnectingPeer);
                System.out.println("Peer disconnected: " + disconnectingPeer);
            }
        }
    }
    
    public void setRootOnly(boolean rootOnly) {
    	this.rootOnly = rootOnly;
    }
    
    private void sendFinalizedMessage(String peerAddress, int peerPort) {
        new Thread(() -> {
            try {
                String finalizedMessage = MessageType.P2P_FINALIZED.getValue();
                DatagramPacket packet = new DatagramPacket(
                        finalizedMessage.getBytes(),
                        finalizedMessage.length(),
                        InetAddress.getByName(peerAddress),
                        peerPort
                );
                socket.send(packet);
                System.out.println("Finalized message sent to " + peerAddress + ":" + peerPort);
            } catch (Exception e) {
                System.err.println("Error sending finalized message: " + e.getMessage());
            }
        }).start();
    }
    
    private void handlePeerConnectionMessages(String receivedData, DatagramPacket receivedPacket, InetAddress senderAddress) throws SocketException {
    	try {
            MessageType messageType = MessageType.fromString(receivedData);
            switch (messageType) {
                case P2P_CONNECT_ME:
                    sendResponse(senderAddress, receivedPacket.getPort());
                    break;
                case P2P_DISCONNECT:
                    handleDisconnectMessage(receivedData, senderAddress);
                    break;
                case P2P_FINALIZED:
                    handleFinalizedMessage(senderAddress);
                    break;
                case P2P_RESPONSE:
                    processResponse(receivedData, senderAddress);
                    break;
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Unknown message type received: " + receivedData);
        }
    }

    private void sendResponse(InetAddress requesterAddress, int requesterPort) {
        new Thread(() -> {
            try {
                String responseMessage = MessageType.P2P_RESPONSE.getValue();
                DatagramPacket responsePacket = new DatagramPacket(
                        responseMessage.getBytes(),
                        responseMessage.length(),
                        requesterAddress,
                        requesterPort
                );

                socket.send(responsePacket);
                System.out.println("Response sent to " + requesterAddress);
            } catch (Exception e) {
                System.err.println("Error sending response: " + e.getMessage());
            }
        }).start();
    }

    private void processResponse(String responseData, InetAddress senderAddress) throws SocketException {
        Peer newPeer = new Peer(senderAddress.getHostAddress(), senderAddress.getHostAddress(), CONNECTION_PORT);

        synchronized (foundPeers) {
            if (!foundPeers.contains(newPeer)) {
                foundPeers.add(newPeer);
                model.addPeer(newPeer);

                Peer localPeer = model.getPeers().stream()
                    .filter(peer -> peer.getPeerId().equals("local_peer"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("local_peer not found in the graph"));

                model.addConnection(localPeer, newPeer);

                System.out.println("Connected, connected peer: " + newPeer);

                sendFinalizedMessage(newPeer.getIpAddress(), newPeer.getPort());
            } else {
                System.out.println("Peer already exists: " + newPeer);
            }
        }
    }

    private void handleFinalizedMessage(InetAddress senderAddress) {
        synchronized (foundPeers) {
            Peer finalizedPeer = new Peer(senderAddress.getHostAddress(), senderAddress.getHostAddress(), CONNECTION_PORT);

            if (!foundPeers.contains(finalizedPeer)) {
                foundPeers.add(finalizedPeer);
                model.addPeer(finalizedPeer);

                Peer localPeer = model.getPeers().stream()
                    .filter(peer -> peer.getPeerId().equals("local_peer"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("local_peer not found in the graph"));

                model.addConnection(localPeer, finalizedPeer);

                System.out.println(MessageType.P2P_FINALIZED.getValue() + " received and peer re-added: " + finalizedPeer);
            } else {
                System.out.println(MessageType.P2P_FINALIZED.getValue() + " received from already known peer: " + finalizedPeer);
            }
        }
    }

    public void stopConnection() {
        running = false;
        try {
            disconnectLatch.await();

            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Socket closed during stopConnection.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupted while waiting for disconnect to complete: " + e.getMessage());
        }
    }

    public Set<Peer> getfoundPeers() {
        return foundPeers;
    }
}
