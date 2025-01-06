package main.p2p.model;

import java.util.*;

public class PeerGraph {
    private final Map<Peer, Set<Peer>> adjacencyList = new HashMap<>();

    public void addPeer(Peer peer) {
        adjacencyList.putIfAbsent(peer, new HashSet<>());
    }

    public void addConnection(Peer peer1, Peer peer2) {
        addPeer(peer1);
        addPeer(peer2);
        adjacencyList.get(peer1).add(peer2);
        adjacencyList.get(peer2).add(peer1);
    }

    public void removePeer(Peer peer) {
        if (adjacencyList.containsKey(peer)) {
            for (Peer connectedPeer : adjacencyList.get(peer)) {
                adjacencyList.get(connectedPeer).remove(peer);
            }
            adjacencyList.remove(peer);
        }
    }

    public void removeConnection(Peer peer1, Peer peer2) {
        if (adjacencyList.containsKey(peer1)) {
            adjacencyList.get(peer1).remove(peer2);
        }
        if (adjacencyList.containsKey(peer2)) {
            adjacencyList.get(peer2).remove(peer1);
        }
    }

    public Set<Peer> getConnections(Peer peer) {
        return adjacencyList.getOrDefault(peer, Collections.emptySet());
    }

    public boolean hasConnection(Peer peer1, Peer peer2) {
        return adjacencyList.containsKey(peer1) && adjacencyList.get(peer1).contains(peer2);
    }

    public Set<Peer> getAllPeers() {
        return new HashSet<>(adjacencyList.keySet());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Peer, Set<Peer>> entry : adjacencyList.entrySet()) {
            sb.append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }
}
