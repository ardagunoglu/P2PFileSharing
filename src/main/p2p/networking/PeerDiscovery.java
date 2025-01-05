package main.p2p.networking;

import main.p2p.model.P2PModel;
import main.p2p.model.Peer;

import java.net.*;
import java.util.HashSet;
import java.util.Set;

public class PeerDiscovery {
    private static final int DISCOVERY_PORT = 4000;
    private final Set<Peer> discoveredPeers = new HashSet<>();
    private final P2PModel model; // Reference to P2PModel
    private boolean running = true;

    public PeerDiscovery(P2PModel model) {
        this.model = model;
    }

    public void discoverPeers() throws Exception {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
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
                System.err.println("Error broadcasting discovery message: " + e.getMessage());
            }
        }).start();

        new Thread(() -> {
            try (DatagramSocket listenerSocket = new DatagramSocket(DISCOVERY_PORT)) {
                System.out.println("Listening for peers on port " + DISCOVERY_PORT);
                while (running) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
                    listenerSocket.receive(responsePacket);

                    String receivedData = new String(responsePacket.getData(), 0, responsePacket.getLength());
                    if (receivedData.startsWith("P2P_RESPONSE")) {
                        String[] parts = receivedData.split(":");
                        if (parts.length == 3) {
                            String peerId = parts[1];
                            String peerAddress = responsePacket.getAddress().getHostAddress();
                            int peerPort = Integer.parseInt(parts[2]);

                            Peer peer = new Peer(peerId, peerAddress, peerPort);
                            if (discoveredPeers.add(peer)) {
                                model.addPeer(peer); // Add peer to P2PModel
                                System.out.println("Discovered peer: " + peer);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error in discovery listener: " + e.getMessage());
            }
        }).start();
    }

    public void sendResponse(InetAddress requesterAddress, int requesterPort) {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                String responseMessage = "P2P_RESPONSE:peer_id:5000";
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

    public void stopDiscovery() {
        running = false;
    }

    public Set<Peer> getDiscoveredPeers() {
        return discoveredPeers;
    }
}
