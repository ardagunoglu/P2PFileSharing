package main.p2p.networking;

public enum MessageType {
    P2P_CONNECT_ME("P2P_CONNECT_ME"),
    P2P_DISCONNECT("P2P_DISCONNECT"),
    P2P_FINALIZED("P2P_FINALIZED"),
    P2P_RESPONSE("P2P_RESPONSE");

    private final String value;

    MessageType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static MessageType fromString(String text) {
        for (MessageType type : MessageType.values()) {
            if (type.value.equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown message type: " + text);
    }
}
