package main.p2p.model;

import java.util.Set;

public class P2PModel {
    private String sharedFolderPath = "C:\\My Shared Folder\\";
    private String destinationPath = "C:\\P2P Downloads\\";
    private boolean isConnected = false;
    private final PeerGraph peerGraph = new PeerGraph();

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

    public void addPeer(Peer peer) {
        peerGraph.addPeer(peer);
        System.out.println("New peer added: " + peer);
    }

    public void addConnection(Peer peer1, Peer peer2) {
        peerGraph.addConnection(peer1, peer2);
        System.out.println("Connection added between: " + peer1 + " and " + peer2);
    }

    public void removePeer(Peer peer) {
        peerGraph.removePeer(peer);
        System.out.println("Peer removed: " + peer);
    }

    public void removeConnection(Peer peer1, Peer peer2) {
        peerGraph.removeConnection(peer1, peer2);
        System.out.println("Connection removed between: " + peer1 + " and " + peer2);
    }

    public PeerGraph getPeerGraph() {
        return peerGraph;
    }
    
    public Set<Peer> getPeers() {
        return peerGraph.getAllPeers();
    }

    @Override
    public String toString() {
        return String.format(
            "P2PModel{sharedFolderPath='%s', destinationPath='%s', isConnected=%s, graph=%s}",
            sharedFolderPath, destinationPath, isConnected, peerGraph
        );
    }
}
