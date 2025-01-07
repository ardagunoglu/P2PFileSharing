package main.p2p.networking;

import main.p2p.controller.P2PController;
import main.p2p.file.FileManager;
import main.p2p.file.FileSearchManager;
import main.p2p.model.P2PModel;
import main.p2p.model.Peer;
import main.p2p.util.NetworkUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JList;
import javax.swing.SwingUtilities;

public class PeerConnection {
    private static final int CONNECTION_PORT = 4000;
    private final P2PModel model;
    private final Set<Peer> foundPeers = new HashSet<>();
    private final List<Map.Entry<Peer, String>> matchFoundPeers = new ArrayList<>();
    private final Map<String, Map.Entry<String, String>> nearestFile = new HashMap<>();
    private final BlockingQueue<DatagramPacket> packetQueue = new LinkedBlockingQueue<>();
    private final Map<String, ScheduledExecutorService> activeSchedulers = new HashMap<>();
    private final Map<String, Map<Integer, byte[]>> receivedChunksMap = new HashMap<>();
    private final Map<String, Integer> totalChunksMap = new HashMap<>();
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
        } else if(receivedData.startsWith("REQUEST_CHUNK:")) {
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
    
    private void handleChunkResponse(String responseData) {
        try {
            System.out.println("Raw response data: " + responseData);
            String[] parts = responseData.split("\\|", 4);
            if (parts.length == 4) {
                String filePath = parts[0];
                int chunkIndex = Integer.parseInt(parts[1]);
                int totalChunks = Integer.parseInt(parts[2]);
                String rawChunk = parts[3];

                byte[] rawBytes = rawChunk.getBytes();
                System.out.println("Raw byte values for chunk:");
                for (byte b : rawBytes) {
                    System.out.printf("%02X ", b);
                }
                System.out.println();

                System.out.println("Received raw chunk " + chunkIndex + " for file " + filePath);
            } else {
                System.err.println("Invalid RESPONSE_CHUNK format. Received parts: " + parts.length);
            }
        } catch (Exception e) {
            System.err.println("Error handling RESPONSE_CHUNK: " + e.getMessage());
            e.printStackTrace();
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

    private void handleChunkRequest(String requestData, InetAddress senderAddress, int senderPort) {
        try {
            String[] parts = requestData.split("\\|");
            if (parts.length == 2) {
                String filePath = parts[0];
                int chunkIndex = Integer.parseInt(parts[1]);

                String rootPath = model.getSharedFolderPath();
                File fullPath = new File(rootPath, filePath);

                FileManager fileManager = new FileManager(fullPath.getAbsolutePath());
                int totalChunks = fileManager.getTotalChunks();
                byte[] chunkData = fileManager.getChunk(chunkIndex);
                String chunkDataEncoded = Base64.getEncoder().encodeToString(chunkData);

                String responseMessage = "RESPONSE_CHUNK:" + filePath + "|" + chunkIndex + "|" + totalChunks + "|" + chunkDataEncoded;
                DatagramPacket responsePacket = new DatagramPacket(
                        responseMessage.getBytes(),
                        responseMessage.length(),
                        senderAddress,
                        senderPort
                );

                socket.send(responsePacket);
                System.out.println("Sent RESPONSE_CHUNK for file: " + filePath + ", chunk index: " + chunkIndex);
            } else {
                System.err.println("Invalid REQUEST_CHUNK format.");
            }
        } catch (Exception e) {
            System.err.println("Error handling chunk request: " + e.getMessage());
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

                startMatchMonitoringForFile(filePath);
            }
        } catch (Exception e) {
            System.err.println("Error handling MATCH_FOUND: " + e.getMessage());
        }
    }
    
    private void startMatchMonitoringForFile(String filePath) {
        synchronized (activeSchedulers) {
            if (activeSchedulers.containsKey(filePath)) {
                System.out.println("Monitoring already active for file: " + filePath);
                return;
            }

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            activeSchedulers.put(filePath, scheduler);

            scheduler.scheduleAtFixedRate(new Runnable() {
                private int lastMatchCount = 0;

                @Override
                public void run() {
                    synchronized (matchFoundPeers) {
                        int currentMatchCount = (int) matchFoundPeers.stream()
                                .filter(entry -> entry.getValue().equals(filePath))
                                .count();

                        if (currentMatchCount > 0 && currentMatchCount == lastMatchCount) {
                            System.out.println("All peers have sent match data for file: " + filePath);
                            processMatchFoundPeersForFile(filePath);
                            stopMonitoringForFile(filePath);
                        } else {
                            lastMatchCount = currentMatchCount;
                        }
                    }
                }
            }, 0, 2, TimeUnit.SECONDS);
        }
    }
    
    private void stopMonitoringForFile(String filePath) {
        synchronized (activeSchedulers) {
            ScheduledExecutorService scheduler = activeSchedulers.remove(filePath);
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
                System.out.println("Monitoring stopped for file: " + filePath);
            }
        }
    }
    
    private void processMatchFoundPeersForFile(String filePath) {
        synchronized (matchFoundPeers) {
            System.out.println("Processing matched peers for file: " + filePath);

            for (Map.Entry<Peer, String> entry : matchFoundPeers) {
                Peer peer = entry.getKey();
                String matchedFilePath = entry.getValue();

                if (filePath.equals(matchedFilePath)) {
                    try {
                        requestChunksFromPeer(peer, filePath);
                    } catch (Exception e) {
                        System.err.println("Error requesting chunks from peer: " + peer + " (" + e.getMessage() + ")");
                    }
                }
            }
        }
    }
    
    private void requestChunksFromPeer(Peer peer, String filePath) {
        new Thread(() -> {
            int chunkIndex = 0;

            synchronized (totalChunksMap) {
                totalChunksMap.putIfAbsent(filePath, Integer.MAX_VALUE);
            }

            while (true) {
                try {
                    synchronized (receivedChunksMap) {
                        Map<Integer, byte[]> chunks = receivedChunksMap.getOrDefault(filePath, new HashMap<>());
                        Integer totalChunks = totalChunksMap.get(filePath);

                        if (chunks.size() == totalChunks) {
                            System.out.println("All chunks received for file: " + filePath);
                            break;
                        }

                        chunkIndex++;
                    }

                    String requestMessage = "REQUEST_CHUNK:" + filePath + "|" + chunkIndex;
                    DatagramPacket requestPacket = new DatagramPacket(
                            requestMessage.getBytes(),
                            requestMessage.length(),
                            InetAddress.getByName(peer.getIpAddress()),
                            peer.getPort()
                    );

                    socket.send(requestPacket);
                    System.out.println("Sent REQUEST_CHUNK to peer: " + peer.getIpAddress() + " for chunk index: " + chunkIndex);

                    Thread.sleep(500);
                } catch (Exception e) {
                    System.err.println("Error requesting chunk: " + e.getMessage());
                    break;
                }
            }
        }).start();
    }


    
    private void reassembleFile(String filePath) {
        String outputPath = "received_" + new File(filePath).getName();
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            Map<Integer, byte[]> chunks = receivedChunksMap.get(filePath);
            for (int i = 0; i < chunks.size(); i++) {
                fos.write(chunks.get(i));
            }
            System.out.println("File reassembled successfully: " + outputPath);
        } catch (IOException e) {
            System.err.println("Error reassembling file: " + e.getMessage());
        } finally {
            // Cleanup
            receivedChunksMap.remove(filePath);
            totalChunksMap.remove(filePath);
        }
    }
    
    private DatagramPacket receiveResponse() throws IOException {
        byte[] buffer = new byte[4096];
        DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(responsePacket);
        return responsePacket;
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