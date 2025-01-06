package main.p2p.networking;

import main.p2p.controller.P2PController;
import main.p2p.file.FileSearchManager;
import main.p2p.model.P2PModel;
import main.p2p.model.Peer;
import main.p2p.util.NetworkUtils;

import java.io.File;
import java.net.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.swing.JList;
import javax.swing.SwingUtilities;

public class PeerConnection {
    private static final int CONNECTION_PORT = 4000;
    private final P2PModel model;
    private final Set<Peer> foundPeers = new HashSet<>();
    private boolean running = true;
    private DatagramSocket socket;
    private P2PController controller;
    private final JList<String> excludeFilesMasksList;
    
    private final CountDownLatch disconnectLatch = new CountDownLatch(1);

    public PeerConnection(P2PModel model, P2PController controller, JList<String> excludeFilesMasksList) {
        this.model = model;
        try {
            this.socket = new DatagramSocket(CONNECTION_PORT);
            this.socket.setBroadcast(true);
        } catch (SocketException e) {
            throw new RuntimeException("Error initializing socket: " + e.getMessage(), e);
        }
        this.controller = controller;
        this.excludeFilesMasksList = excludeFilesMasksList;
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
                    byte[] buffer = new byte[1024];
                    DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(receivedPacket);

                    String receivedData = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
                    InetAddress senderAddress = receivedPacket.getAddress();

                    if (senderAddress.equals(localAddress)) {
                        System.out.println("Ignored message from self: " + senderAddress);
                        continue;
                    }

                    System.out.println("Received data: " + receivedData + " from " + senderAddress);

                    if (receivedData.startsWith("SEARCH:")) {
                        handleSearchRequest(receivedData, senderAddress, receivedPacket.getPort());
                    } else {
                        handlePeerConnectionMessages(receivedData, receivedPacket, senderAddress);
                    }
                }
            } catch (Exception e) {
                if (running) {
                    System.err.println("Error in listener socket: " + e.getMessage());
                }
            }
        }).start();
    }
    
    private void handleSearchRequest(String receivedData, InetAddress senderAddress, int senderPort) {
        String query = receivedData.substring(7).trim();
        System.out.println("Search query received: " + query);

        FileSearchManager fileSearchManager = new FileSearchManager(
                new File(model.getSharedFolderPath()),
                excludeFilesMasksList
            );

        List<String> foundFiles = fileSearchManager.searchFiles(query);

        String response = String.join(",", foundFiles);
        if (response.isEmpty()) {
            response = "NO_FILES_FOUND";
        }

        try {
            DatagramPacket responsePacket = new DatagramPacket(
                response.getBytes(),
                response.length(),
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
