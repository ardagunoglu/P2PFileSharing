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
                String responseMessage = "P2P_RESPONSE:" + generatePeerId(requesterAddress, DISCOVERY_PORT);
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
        String[] parts = responseData.split(":");
        if (parts.length == 3) {
            String peerId = parts[1];
            String peerAddress = senderAddress.getHostAddress();
            int peerPort = Integer.parseInt(parts[2]);

            Peer newPeer = new Peer(peerId, peerAddress, peerPort);

            synchronized (discoveredPeers) {
                if (!discoveredPeers.contains(newPeer)) {
                    discoveredPeers.add(newPeer);
                    model.addPeer(newPeer);

                    Peer localPeer = new Peer(peerId, NetworkUtils.getLocalAddress().getHostAddress(), DISCOVERY_PORT);
                    model.addConnection(localPeer, newPeer);

                    System.out.println("Discovered and connected peer: " + newPeer);

                    sendFinalizedMessage(peerAddress, peerPort);
                    
                    controller.updateUIList();
                } else {
                    System.out.println("Peer already exists: " + newPeer);
                }
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
