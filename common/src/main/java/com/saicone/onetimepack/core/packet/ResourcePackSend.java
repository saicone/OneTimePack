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

public class ResourcePackSend extends AbstractPacket {

    public static final List<ProtocolIdMapping> DEFAULT_MAPPINGS = Arrays.asList(
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_8, MINECRAFT_1_8, 0x48),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_9, MINECRAFT_1_11_2, 0x32),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_12, MINECRAFT_1_12, 0x33),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_12_1, MINECRAFT_1_12_2, 0x34),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_13, MINECRAFT_1_13_2, 0x37),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_14, MINECRAFT_1_14_4, 0x39),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_15, MINECRAFT_1_15_2, 0x3A),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_16, MINECRAFT_1_16_1, 0x39),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_16_2, MINECRAFT_1_16_5, 0x38),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_17, MINECRAFT_1_18_2, 0x3C),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19, MINECRAFT_1_19, 0x3A),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19_1, MINECRAFT_1_19_2, 0x3D),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19_3, MINECRAFT_LATEST, 0x3C)
    );

    public static void register() {
        register(null);
    }

    public static void register(List<ProtocolIdMapping> mappings) {
        Protocolize.protocolRegistration().registerPacket(
                mappings == null || mappings.isEmpty() ? DEFAULT_MAPPINGS : mappings,
                Protocol.PLAY,
                PacketDirection.CLIENTBOUND,
                ResourcePackSend.class
        );
    }

    private String url;
    private String hash;

    // Added in 1.17
    private boolean forced;
    private boolean hasPromptMessage;
    private String promptMessage;

    public ResourcePackSend() {
    }

    public ResourcePackSend(String url, String hash, boolean forced, boolean hasPromptMessage, String promptMessage) {
        this.url = url;
        this.hash = hash;
        this.forced = forced;
        this.hasPromptMessage = hasPromptMessage;
        this.promptMessage = promptMessage;
    }

    public String getUrl() {
        return url;
    }

    public String getHash() {
        return hash;
    }

    public String getPromptMessage() {
        return promptMessage;
    }

    public boolean isForced() {
        return forced;
    }

    public boolean hasPromptMessage() {
        return hasPromptMessage;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void setForced(boolean forced) {
        this.forced = forced;
    }

    public void setHasPromptMessage(boolean hasPromptMessage) {
        this.hasPromptMessage = hasPromptMessage;
    }

    public void setPromptMessage(String promptMessage) {
        this.promptMessage = promptMessage;
    }

    @Override
    public void read(ByteBuf buf, PacketDirection direction, int protocol) {
        url = ProtocolUtil.readString(buf);
        hash = ProtocolUtil.readString(buf);
        if (protocol >= MINECRAFT_1_17) {
            forced = buf.readBoolean();
            hasPromptMessage = buf.readBoolean();
            if (hasPromptMessage) {
                promptMessage = ProtocolUtil.readString(buf);
            }
        } else {
            forced = false;
            hasPromptMessage = false;
        }
    }

    @Override
    public void write(ByteBuf buf, PacketDirection direction, int protocol) {
        ProtocolUtil.writeString(buf, url);
        ProtocolUtil.writeString(buf, hash);
        if (protocol >= MINECRAFT_1_17) {
            buf.writeBoolean(forced);
            buf.writeBoolean(hasPromptMessage);
            if (hasPromptMessage) {
                ProtocolUtil.writeString(buf, promptMessage);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ResourcePackSend packet = (ResourcePackSend) obj;
        return Objects.equals(packet.url, url) &&
                Objects.equals(packet.hash, hash) &&
                packet.forced == forced &&
                packet.hasPromptMessage == hasPromptMessage &&
                Objects.equals(packet.promptMessage, promptMessage);
    }

    @Override
    public int hashCode() {
        int result = url.hashCode();
        result = 31 * result + hash.hashCode();
        result = 31 * result + (forced ? 1 : 0);
        result = 31 * result + (hasPromptMessage ? 1 : 0);
        result = 31 * result + (promptMessage != null ? promptMessage.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PlayOutResourcePack{" +
                "url='" + url + '\'' +
                ", hash='" + hash + '\'' +
                ", forced=" + forced +
                ", hasPromptMessage=" + hasPromptMessage +
                ", promptMessage='" + promptMessage + '\'' +
                '}';
    }
}
