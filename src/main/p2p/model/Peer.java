package main.p2p.model;

import java.util.Objects;

public class Peer {
    private final String peerId;
    private final String ipAddress;
    private final int port;

    public Peer(String peerId, String ipAddress, int port) {
        this.peerId = peerId;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String getPeerId() {
        return peerId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Peer peer = (Peer) o;
        return peerId.equals(peer.peerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(peerId);
    }

    @Override
    public String toString() {
        return String.format("Peer{id='%s', ip='%s', port=%d}", peerId, ipAddress, port);
    }
}
