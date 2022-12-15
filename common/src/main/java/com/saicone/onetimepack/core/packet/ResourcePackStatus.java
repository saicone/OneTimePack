package com.saicone.onetimepack.core.packet;

import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.Protocol;
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.mapping.AbstractProtocolMapping;
import dev.simplix.protocolize.api.mapping.ProtocolIdMapping;
import dev.simplix.protocolize.api.packet.AbstractPacket;
import dev.simplix.protocolize.api.util.ProtocolUtil;
import io.netty.buffer.ByteBuf;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static dev.simplix.protocolize.api.util.ProtocolVersions.*;

public class ResourcePackStatus extends AbstractPacket {

    public static final List<ProtocolIdMapping> DEFAULT_MAPPINGS = Arrays.asList(
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_8, MINECRAFT_1_8, 0x19),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_9, MINECRAFT_1_11_2, 0x16),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_12, MINECRAFT_1_12_2, 0x18),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_13, MINECRAFT_1_13_2, 0x1D),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_14, MINECRAFT_1_15_2, 0x1F),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_16, MINECRAFT_1_16_1, 0x20),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_16_2, MINECRAFT_1_18_2, 0x21),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19, MINECRAFT_1_19, 0x23),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19_1, MINECRAFT_LATEST, 0x24)
    );

    public static void register() {
        register(null);
    }

    public static void register(List<ProtocolIdMapping> mappings) {
        Protocolize.protocolRegistration().registerPacket(
                mappings == null || mappings.isEmpty() ? DEFAULT_MAPPINGS : mappings,
                Protocol.PLAY,
                PacketDirection.SERVERBOUND,
                ResourcePackStatus.class
        );
    }

    // Removed in 1.10
    private String hash;
    // 0: successfully loaded
    // 1: declined
    // 2: failed download
    // 3: accepted
    private int result;

    public ResourcePackStatus() {
    }

    public ResourcePackStatus(int result) {
        this.result = result;
    }

    public ResourcePackStatus(String hash, int result) {
        this.hash = hash;
        this.result = result;
    }

    public String getHash() {
        return hash;
    }

    public int getResult() {
        return result;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void setResult(int result) {
        this.result = result;
    }

    @Override
    public void read(ByteBuf buf, PacketDirection direction, int protocol) {
        if (protocol <= MINECRAFT_1_9_4) {
            hash = ProtocolUtil.readString(buf);
        }
        result = ProtocolUtil.readVarInt(buf);
    }

    @Override
    public void write(ByteBuf buf, PacketDirection direction, int protocol) {
        if (protocol <= MINECRAFT_1_9_4) {
            ProtocolUtil.writeString(buf, hash);
        }
        ProtocolUtil.writeVarInt(buf, result);
    }

    public ResourcePackStatus copy() {
        return new ResourcePackStatus(hash, result);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ResourcePackStatus packet = (ResourcePackStatus) obj;

        if (result != packet.result) return false;
        return Objects.equals(hash, packet.hash);
    }

    @Override
    public int hashCode() {
        int result1 = hash != null ? hash.hashCode() : 0;
        result1 = 31 * result1 + result;
        return result1;
    }

    @Override
    public String toString() {
        return "PlayInResourcePack{" +
                (hash != null ? "hash='" + hash + "', " : "") +
                "result=" + result +
                '}';
    }
}
