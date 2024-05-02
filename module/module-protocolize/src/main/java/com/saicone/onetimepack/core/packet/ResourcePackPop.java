package com.saicone.onetimepack.core.packet;

import com.saicone.onetimepack.OneTimePack;
import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.Protocol;
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.mapping.AbstractProtocolMapping;
import dev.simplix.protocolize.api.mapping.ProtocolIdMapping;
import dev.simplix.protocolize.api.packet.AbstractPacket;
import dev.simplix.protocolize.api.util.ProtocolUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import static dev.simplix.protocolize.api.util.ProtocolVersions.*;

public class ResourcePackPop extends AbstractPacket {

    public static final List<ProtocolIdMapping> DEFAULT_MAPPINGS = Arrays.asList(
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x43)
    );

    public static final List<ProtocolIdMapping> DEFAULT_MAPPINGS_CONFIGURATION = Arrays.asList(
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x06)
    );

    public static void register() {
        register(Protocol.PLAY, DEFAULT_MAPPINGS, ResourcePackPop.Play.class);
        register(Protocol.CONFIGURATION, DEFAULT_MAPPINGS_CONFIGURATION, ResourcePackPop.Configuration.class);
    }

    public static void register(@Nullable Function<String, List<ProtocolIdMapping>> provider) {
        if (provider == null) {
            register();
            return;
        }
        final List<ProtocolIdMapping> play = provider.apply("play");
        register(Protocol.PLAY, play == null ? DEFAULT_MAPPINGS : play, ResourcePackPop.Play.class);
        final List<ProtocolIdMapping> configuration = provider.apply("configuration");
        register(Protocol.CONFIGURATION, configuration == null ? DEFAULT_MAPPINGS_CONFIGURATION : configuration, ResourcePackPop.Configuration.class);
    }

    public static void register(@NotNull Protocol protocol, @NotNull List<ProtocolIdMapping> mappings, Class<? extends ResourcePackPop> clazz) {
        if (mappings.isEmpty()) {
            return;
        }
        Protocolize.protocolRegistration().registerPacket(
                mappings,
                protocol,
                PacketDirection.CLIENTBOUND,
                clazz
        );
    }

    private boolean hasUniqueId;
    private UUID uniqueId;

    public ResourcePackPop() {
    }

    public ResourcePackPop(boolean hasUniqueId, @Nullable UUID uniqueId) {
        this.hasUniqueId = hasUniqueId;
        this.uniqueId = uniqueId;
    }

    public boolean hasUniqueId() {
        return hasUniqueId;
    }

    @Nullable
    public UUID getUniqueId() {
        return uniqueId;
    }

    @NotNull
    public Protocol getProtocol() {
        return this instanceof Play ? Protocol.PLAY : Protocol.CONFIGURATION;
    }

    public void setHasUniqueId(boolean hasUniqueId) {
        this.hasUniqueId = hasUniqueId;
    }

    public void setUniqueId(@Nullable UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    @Override
    public void read(ByteBuf buf, PacketDirection direction, int protocol) {
        hasUniqueId = buf.readBoolean();
        if (hasUniqueId) {
            uniqueId = ProtocolUtil.readUniqueId(buf);
        }
        OneTimePack.log(4, () -> "[" + getProtocol().name() + "] Packet#read() = " + this);
    }

    @Override
    public void write(ByteBuf buf, PacketDirection direction, int protocol) {
        buf.writeBoolean(hasUniqueId);
        if (hasUniqueId) {
            ProtocolUtil.writeUniqueId(buf, uniqueId);
        }
    }

    @NotNull
    public ResourcePackPop copy() {
        return new ResourcePackPop(hasUniqueId, uniqueId);
    }

    @NotNull
    public ResourcePackPop.Play asPlay() {
        if (this instanceof ResourcePackPop.Play) {
            return (ResourcePackPop.Play) this;
        }
        return new ResourcePackPop.Play(hasUniqueId, uniqueId);
    }

    @NotNull
    public ResourcePackPop.Configuration asConfiguration() {
        if (this instanceof ResourcePackPop.Configuration) {
            return (ResourcePackPop.Configuration) this;
        }
        return new ResourcePackPop.Configuration(hasUniqueId, uniqueId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourcePackPop that = (ResourcePackPop) o;

        if (hasUniqueId != that.hasUniqueId) return false;
        return Objects.equals(uniqueId, that.uniqueId);
    }

    @Override
    public int hashCode() {
        int result = (hasUniqueId ? 1 : 0);
        result = 31 * result + (uniqueId != null ? uniqueId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ClientboundResourcePackPop{" +
                "hasUniqueId=" + hasUniqueId +
                (hasUniqueId ? ", uniqueId=" + uniqueId : "") +
                '}';
    }

    public static class Play extends ResourcePackPop {
        public Play() {
        }

        public Play(boolean hasUniqueId, @Nullable UUID uniqueId) {
            super(hasUniqueId, uniqueId);
        }
    }

    public static class Configuration extends ResourcePackPop {
        public Configuration() {
        }

        public Configuration(boolean hasUniqueId, @Nullable UUID uniqueId) {
            super(hasUniqueId, uniqueId);
        }
    }
}