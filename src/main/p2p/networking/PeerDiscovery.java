package main.p2p.networking;

import main.p2p.controller.P2PController;
import main.p2p.model.P2PModel;
import main.p2p.model.Peer;
import main.p2p.util.NetworkUtils;

import java.net.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.SwingUtilities;

public class PeerDiscovery {
    private static final int DISCOVERY_PORT = 4000;
    private final P2PModel model;
    private final Set<Peer> discoveredPeers = new HashSet<>();
    private boolean running = true;
    private DatagramSocket socket;
    private P2PController controller;

    public PeerDiscovery(P2PModel model, P2PController controller) {
        this.model = model;
        try {
            this.socket = new DatagramSocket(DISCOVERY_PORT);
            this.socket.setBroadcast(true);
        } catch (SocketException e) {
            throw new RuntimeException("Error initializing socket: " + e.getMessage(), e);
        }
        this.controller = controller;
    }

    public void discoverPeers() {
        sendDiscoveryMessage();

        if (running) {
            listenForResponses();
        }
    }

    private void sendDiscoveryMessage() {
        new Thread(() -> {
            try {
                String discoveryMessage = "P2P_DISCOVERY";
                DatagramPacket packet = new DatagramPacket(
                        discoveryMessage.getBytes(),
                        discoveryMessage.length(),
                        InetAddress.getByName("192.168.1.255"),
                        DISCOVERY_PORT
                );

                socket.send(packet);
                System.out.println("Discovery message sent to broadcast address.");

            } catch (Exception e) {
                System.err.println("Error sending discovery message: " + e.getMessage());
            }
        }).start();
    }
    
    public void sendDisconnectMessage() {
        new Thread(() -> {
            try {
                String disconnectMessage = "P2P_DISCONNECT";
                DatagramPacket packet = new DatagramPacket(
                        disconnectMessage.getBytes(),
                        disconnectMessage.length(),
                        InetAddress.getByName("192.168.1.255"),
                        DISCOVERY_PORT
                );

                socket.send(packet);
                System.out.println("Disconnect message sent to all peers.");
                
                synchronized (discoveredPeers) {
                    discoveredPeers.clear();
                    model.getPeerGraph().getAllPeers().forEach(model::removePeer);
                    System.out.println("Cleared local peers and graph.");
                }
                controller.updateUIList();
            } catch (Exception e) {
                System.err.println("Error sending disconnect message: " + e.getMessage());
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

                    if (receivedData.equals("P2P_DISCOVERY")) {
                        sendResponse(senderAddress, receivedPacket.getPort());
                    } else if (receivedData.startsWith("P2P_DISCONNECT")) {
                        handleDisconnectMessage(receivedData, senderAddress);
                    } else if (receivedData.equals("P2P_FINALIZED")) {
                        handleFinalizedMessage(senderAddress);
                    } else {
                        processResponse(receivedData, senderAddress);
                    }
                }
            } catch (Exception e) {
                if (running) {
                    System.err.println("Error in listener socket: " + e.getMessage());
                }
            }
        }).start();
    }

    private void handleDisconnectMessage(String message, InetAddress senderAddress) {
        String[] parts = message.split(":");
        if (parts.length == 3) {
            String peerId = parts[1];
            Peer disconnectingPeer = new Peer(peerId, senderAddress.getHostAddress(), DISCOVERY_PORT);

            synchronized (discoveredPeers) {
                if (discoveredPeers.remove(disconnectingPeer)) {
                    model.removePeer(disconnectingPeer);
                    model.getPeerGraph().removePeer(disconnectingPeer);
                    System.out.println("Peer disconnected: " + disconnectingPeer);
                    controller.updateUIList();
                }
            }
        }
    }
    
    private void sendFinalizedMessage(String peerAddress, int peerPort) {
        new Thread(() -> {
            try {
                String finalizedMessage = "P2P_FINALIZED";
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

    private void sendResponse(InetAddress requesterAddress, int requesterPort) {
        new Thread(() -> {
            try {
                String responseMessage = "P2P_RESPONSE";
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
        if (!responseData.equals("P2P_RESPONSE")) {
            System.out.println("Invalid response message received: " + responseData);
            return;
        }

        Peer newPeer = new Peer(senderAddress.getHostAddress(), senderAddress.getHostAddress(), DISCOVERY_PORT);

        synchronized (discoveredPeers) {
            if (!discoveredPeers.contains(newPeer) && !newPeer.getPeerId().equals("local_peer")) {
                discoveredPeers.add(newPeer);
                model.addPeer(newPeer);

                Peer localPeer = model.getPeers().stream()
                    .filter(peer -> peer.getPeerId().equals("local_peer"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("local_peer not found in the graph"));

                model.addConnection(localPeer, newPeer);

                System.out.println("Discovered and connected peer: " + newPeer);

                sendFinalizedMessage(newPeer.getIpAddress(), newPeer.getPort());

                controller.updateUIList();
            } else {
                System.out.println("Peer already exists or is local_peer: " + newPeer);
            }
        }
    }

    private void handleFinalizedMessage(InetAddress senderAddress) {
        synchronized (discoveredPeers) {
            Peer finalizedPeer = new Peer(senderAddress.getHostAddress(), senderAddress.getHostAddress(), DISCOVERY_PORT);

            if (discoveredPeers.contains(finalizedPeer)) {
                System.out.println("P2P_FINALIZED received from peer: " + finalizedPeer);
            } else {
                System.out.println("P2P_FINALIZED received, but peer not found: " + finalizedPeer);
            }
        }
    }
    
    private String generatePeerId(InetAddress address, int port) {
        return address.getHostAddress() + ":" + port;
    }

    public void stopDiscovery() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public Set<Peer> getDiscoveredPeers() {
        return discoveredPeers;
    }
}
