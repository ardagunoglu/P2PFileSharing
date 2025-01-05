package main.p2p.networking;

import main.p2p.model.P2PModel;
import main.p2p.model.Peer;

import java.net.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PeerDiscovery {
    private final P2PModel model;
    private final Set<Peer> discoveredPeers = new HashSet<>();
    private boolean running = true;
    private int localPort;

    public PeerDiscovery(P2PModel model) {
        this.model = model;
    }

    public void discoverPeers() {
        try {
            DatagramSocket socket = new DatagramSocket();
            localPort = socket.getLocalPort();
            socket.close();
            System.out.println("Using local port: " + localPort);
        } catch (Exception e) {
            System.err.println("Error assigning local port: " + e.getMessage());
            return;
        }

        sendDiscoveryMessage();
        listenForResponses();
    }

    private void sendDiscoveryMessage() {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(localPort)) {
                socket.setBroadcast(true);
                String discoveryMessage = "P2P_DISCOVERY:" + localPort;
                DatagramPacket packet = new DatagramPacket(
                        discoveryMessage.getBytes(),
                        discoveryMessage.length(),
                        InetAddress.getByName("192.168.1.255"),
                        localPort
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
            try (DatagramSocket listenerSocket = new DatagramSocket(localPort)) {
                System.out.println("Listening for peers on port " + localPort);

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

                    if (receivedData.startsWith("P2P_DISCOVERY")) {
                        processDiscovery(receivedData, senderAddress, receivedPacket.getPort());
                    } else if (receivedData.startsWith("P2P_RESPONSE")) {
                        processResponse(receivedData, senderAddress);
                    } else if (receivedData.equals("P2P_FINALIZED")) {
                        System.out.println("Connection finalized with " + senderAddress);
                        Peer finalizedPeer = new Peer("peer_id", senderAddress.getHostAddress(), localPort);
                        if (discoveredPeers.add(finalizedPeer)) {
                            model.addPeer(finalizedPeer);
                            System.out.println("Peer added from finalized message: " + finalizedPeer);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error in listener socket: " + e.getMessage());
            }
        }).start();
    }

    private void processDiscovery(String discoveryData, InetAddress senderAddress, int senderPort) {
        String[] parts = discoveryData.split(":");
        if (parts.length == 2) {
            int senderDiscoveryPort = Integer.parseInt(parts[1]);
            sendResponse(senderAddress, senderDiscoveryPort);
        }
    }

    private void sendResponse(InetAddress requesterAddress, int requesterPort) {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                String responseMessage = "P2P_RESPONSE:peer_id:" + localPort;
                DatagramPacket responsePacket = new DatagramPacket(
                        responseMessage.getBytes(),
                        responseMessage.length(),
                        requesterAddress,
                        requesterPort
                );

                socket.send(responsePacket);
                System.out.println("Response sent to " + requesterAddress + " on port " + requesterPort);
            } catch (Exception e) {
                System.err.println("Error sending response: " + e.getMessage());
            }
        }).start();
    }

    private InetAddress getLocalAddress() throws SocketException {
        for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                continue;
            }

            for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                    if (address.getHostAddress().startsWith("192.168.1.")) {
                        return address;
                    }
                }
            }
        }
        throw new SocketException("No suitable local address found");
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

                sendFinalizedMessage(peerAddress, peerPort);
            }
        }
    }

    private void sendFinalizedMessage(String peerAddress, int peerPort) {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
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

    public void stopDiscovery() {
        running = false;
    }

    public Set<Peer> getDiscoveredPeers() {
        return discoveredPeers;
    }
}
