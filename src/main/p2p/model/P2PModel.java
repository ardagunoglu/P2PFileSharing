package main.p2p.model;

import java.util.HashSet;
import java.util.Set;

public class P2PModel {
    private String sharedFolderPath = "C:\\My Shared Folder\\";
    private String destinationPath = "C:\\P2P Downloads\\";
    private boolean isConnected = false;
    private final Set<Peer> peers = new HashSet<>();

    public String getSharedFolderPath() {
        return sharedFolderPath;
    }

    public void setSharedFolderPath(String path) {
        this.sharedFolderPath = path;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String path) {
        this.destinationPath = path;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public Set<Peer> getPeers() {
        return new HashSet<>(peers);
    }

    public void addPeer(Peer peer) {
        if (peers.add(peer)) {
            System.out.println("New peer added: " + peer);
        }
    }

    public void clearPeers() {
        peers.clear();
    }

    public void setPeers(Set<Peer> newPeers) {
        peers.clear();
        peers.addAll(newPeers);
    }

    public void removePeer(Peer peer) {
        synchronized (peers) {
            peers.remove(peer);
            System.out.println("Removed peer from model: " + peer);
        }
    }

    @Override
    public String toString() {
        return String.format("P2PModel{sharedFolderPath='%s', destinationPath='%s', isConnected=%s, peers=%s}",
                sharedFolderPath, destinationPath, isConnected, peers);
    }
}
