package main.p2p.networking;

import main.p2p.model.Peer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class FileSearchHandler {

    private static final int TIMEOUT_MS = 3000;

    public Map<String, Map.Entry<String, String>> searchFilesFromPeer(String searchQuery, Peer peer) {
        Map<String, Map.Entry<String, String>> foundFiles = new HashMap<>();
        try (DatagramSocket socket = new DatagramSocket()) {
            String message = "SEARCH:" + searchQuery;

            DatagramPacket requestPacket = new DatagramPacket(
                    message.getBytes(),
                    message.length(),
                    InetAddress.getByName(peer.getIpAddress()),
                    peer.getPort()
            );
            socket.send(requestPacket);

            byte[] buffer = new byte[2048];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
            socket.setSoTimeout(TIMEOUT_MS);
            socket.receive(responsePacket);

            String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
            if (!response.equals("NO_FILES_FOUND")) {
                for (String fileInfo : response.split(",")) {
                    String[] parts = fileInfo.split("\\|"); // Assuming "filePath|hash" format
                    if (parts.length == 2) {
                        String filePath = parts[0].trim();
                        String fileHash = parts[1].trim();
                        foundFiles.put(filePath, new AbstractMap.SimpleEntry<>(filePath, fileHash));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error searching on peer " + peer.getIpAddress() + ": " + e.getMessage());
        }
        return foundFiles;
    }
}
