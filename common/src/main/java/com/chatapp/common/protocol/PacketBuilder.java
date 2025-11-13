package com.chatapp.common.protocol;

public class PacketBuilder {
    private Packet packet;

    private PacketBuilder(MessageType type) {
        this.packet = new Packet(type);
    }

    public static PacketBuilder create(MessageType type) {
        return new PacketBuilder(type);
    }

    public PacketBuilder put(String key, Object value) {
        packet.put(key, value);
        return this;
    }

    public PacketBuilder success(boolean success) {
        packet.setSuccess(success);
        return this;
    }

    public PacketBuilder error(String error) {
        packet.setError(error);
        return this;
    }

    public Packet build() {
        return packet;
    }
}