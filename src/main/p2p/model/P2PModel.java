package main.p2p.model;

import java.util.HashSet;
import java.util.Set;

public class P2PModel {
    private String sharedFolderPath = "C:\\My Shared Folder\\";
    private String destinationPath = "C:\\P2P Downloads\\";
    private boolean isConnected = false;
    private Set<Peer> peers = new HashSet<>();

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
        return peers;
    }

    public void setPeers(Set<Peer> peers) {
        this.peers = peers;
    }
}
