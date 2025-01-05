package main.p2p.model;

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
    public String toString() {
        return String.format("Peer{id='%s', ip='%s', port=%d}", peerId, ipAddress, port);
    }
}
