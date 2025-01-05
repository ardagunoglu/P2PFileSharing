package main.p2p.networking;

import main.p2p.model.P2PModel;
import main.p2p.model.Peer;

import java.net.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PeerDiscovery {
    private static final int DISCOVERY_PORT = 4000; // UDP port
    private final P2PModel model; // P2PModel referansı
    private final Set<Peer> discoveredPeers = new HashSet<>();
    private boolean running = true;

    public PeerDiscovery(P2PModel model) {
        this.model = model;
    }

    public void discoverPeers() {
        sendDiscoveryMessage();

        listenForResponses();
    }

    private void sendDiscoveryMessage() {
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
                System.err.println("Error sending discovery message: " + e.getMessage());
            }
        }).start();
    }

    private void listenForResponses() {
        new Thread(() -> {
            try (DatagramSocket listenerSocket = new DatagramSocket(DISCOVERY_PORT)) {
                System.out.println("Listening for peers on port " + DISCOVERY_PORT);

                InetAddress localAddress = getLocalAddress();
                System.out.println("Local address detected: " + localAddress);

                while (running) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
                    listenerSocket.receive(receivedPacket);

                    String receivedData = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
                    InetAddress senderAddress = receivedPacket.getAddress();

                    if (senderAddress.equals(localAddress)) {
                        System.out.println("Ignored message from self: " + senderAddress);
                        continue;
                    }

                    System.out.println("Received data: " + receivedData + " from " + senderAddress);

                    if (receivedData.equals("P2P_DISCOVERY")) {
                        sendResponse(senderAddress, receivedPacket.getPort());
                    } else if (receivedData.startsWith("P2P_RESPONSE")) {
                        processResponse(receivedData, senderAddress);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error in listener socket: " + e.getMessage());
            }
        }).start();
    }


    private void sendResponse(InetAddress requesterAddress, int requesterPort) {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                String responseMessage = "P2P_RESPONSE:peer_id:" + DISCOVERY_PORT;
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
    
    private InetAddress getLocalAddress() throws SocketException {
        InetAddress localAddress = null;

        for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                continue; // Skip loopback, virtual, or inactive interfaces
            }

            for (InetAddress inetAddress : Collections.list(networkInterface.getInetAddresses())) {
                if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                    localAddress = inetAddress;
                    break;
                }
            }
            if (localAddress != null) break;
        }
        return localAddress;
    }

    private void processResponse(String responseData, InetAddress senderAddress) {
        String[] parts = responseData.split(":");
        if (parts.length == 3) {
            String peerId = parts[1];
            String peerAddress = senderAddress.getHostAddress();
            int peerPort = Integer.parseInt(parts[2]);

            Peer peer = new Peer(peerId, peerAddress, peerPort);
            if (discoveredPeers.add(peer)) {
                model.addPeer(peer);
                System.out.println("Discovered peer: " + peer);
            }
        }
    }

    public void stopDiscovery() {
        running = false;
    }

    public Set<Peer> getDiscoveredPeers() {
        return discoveredPeers;
    }
}
