package main.p2p.model;

public class Peer {
    private final String ipAddress;
    private final int port;

    public Peer(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return ipAddress + ":" + port;
    }
}
