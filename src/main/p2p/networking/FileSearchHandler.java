package main.p2p.networking;

import main.p2p.model.Peer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class FileSearchHandler {

    private static final int TIMEOUT_MS = 3000;

    public Map<String, Peer> searchFilesFromPeer(String searchQuery, Peer peer) {
        Map<String, Peer> foundFiles = new HashMap<>();
        try (DatagramSocket socket = new DatagramSocket()) {
            String message = "SEARCH:" + searchQuery;

            DatagramPacket requestPacket = new DatagramPacket(
                    message.getBytes(),
                    message.length(),
                    InetAddress.getByName(peer.getIpAddress()),
                    peer.getPort()
            );
            socket.send(requestPacket);

            byte[] buffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
            socket.setSoTimeout(TIMEOUT_MS);
            socket.receive(responsePacket);

            String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
            for (String file : response.split(",")) {
                foundFiles.put(file.trim(), peer);
            }
        } catch (Exception e) {
            System.err.println("Error searching on peer " + peer.getIpAddress() + ": " + e.getMessage());
        }
        return foundFiles;
    }
}
