package main.p2p.networking;

import java.net.*;
import java.util.*;
import main.p2p.model.Peer;
import main.p2p.model.P2PModel;

public class PeerDiscovery {
    private static final int DISCOVERY_PORT = 9876;
    private final P2PModel model;
    private final Set<Peer> discoveredPeers = new HashSet<>();
    private volatile boolean isRunning = true;

    public PeerDiscovery(P2PModel model) {
        this.model = model;
    }

    public void discoverPeers() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);

            String discoveryMessage = "P2P_DISCOVERY";
            DatagramPacket packet = new DatagramPacket(
                discoveryMessage.getBytes(),
                discoveryMessage.length(),
                InetAddress.getByName("255.255.255.255"),
                DISCOVERY_PORT
            );
            socket.send(packet);

            System.out.println("Discovery message sent. Waiting for peers...");

            // Receive responses
            byte[] buffer = new byte[1024];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);

            while (isRunning) {
                try {
                    socket.receive(response);
                    String peerAddress = response.getAddress().getHostAddress();
                    int peerPort = response.getPort();

                    Peer peer = new Peer(peerAddress, peerPort);
                    if (discoveredPeers.add(peer)) {
                        System.out.println("Discovered peer: " + peer);
                        updateModelPeers();
                    }
                } catch (SocketTimeoutException e) {
                    // Optional: Handle timeouts if needed
                }
            }
        } catch (Exception e) {
            System.err.println("Error during peer discovery: " + e.getMessage());
        }
    }

    public void stopDiscovery() {
        isRunning = false;
    }

    private void updateModelPeers() {
        synchronized (model) {
            model.setPeers(new HashSet<>(discoveredPeers));
        }
    }

    public Set<Peer> getDiscoveredPeers() {
        return Collections.unmodifiableSet(discoveredPeers);
    }
}
