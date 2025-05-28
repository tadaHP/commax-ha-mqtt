package com.hyeonpyo.wallpadcontroller.parser.commax.type;

public enum PacketKind {
    COMMAND,
    STATE,
    STATE_REQUEST,
    ACK;

    public static PacketKind fromKey(String key) {
        return switch (key.toLowerCase()) {
            case "command" -> COMMAND;
            case "state" -> STATE;
            case "state_request" -> STATE_REQUEST;
            case "ack" -> ACK;
            default -> throw new IllegalArgumentException("Unknown packet kind: " + key);
        };
    }
}