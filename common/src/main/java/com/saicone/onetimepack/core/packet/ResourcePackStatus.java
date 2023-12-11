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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

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
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19_1, MINECRAFT_1_20_1, 0x24),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_20_2, MINECRAFT_1_20_2, 0x27),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x28)
    );

    public static final List<ProtocolIdMapping> DEFAULT_MAPPINGS_CONFIGURATION = Arrays.asList(
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_20_1, MINECRAFT_1_20_3, 0x05)
    );

    public static void register() {
        register(Protocol.PLAY, DEFAULT_MAPPINGS, ResourcePackStatus.Play.class);
        register(Protocol.CONFIGURATION, DEFAULT_MAPPINGS_CONFIGURATION, ResourcePackStatus.Configuration.class);
    }

    public static void register(Function<String, List<ProtocolIdMapping>> provider) {
        if (provider == null) {
            register();
            return;
        }
        final List<ProtocolIdMapping> play = provider.apply("play");
        register(Protocol.PLAY, play == null ? DEFAULT_MAPPINGS : play, ResourcePackStatus.Play.class);
        final List<ProtocolIdMapping> configuration = provider.apply("configuration");
        register(Protocol.CONFIGURATION, configuration == null ? DEFAULT_MAPPINGS_CONFIGURATION : configuration, ResourcePackStatus.Configuration.class);
    }

    public static void register(Protocol protocol, List<ProtocolIdMapping> mappings, Class<? extends ResourcePackStatus> clazz) {
        if (mappings.isEmpty()) {
            return;
        }
        Protocolize.protocolRegistration().registerPacket(
                mappings,
                protocol,
                PacketDirection.SERVERBOUND,
                clazz
        );
    }

    // Added in 1.20.3
    private UUID uniqueId;
    // Removed in 1.10
    private String hash;
    // 0: successfully loaded
    // 1: declined
    // 2: failed download
    // 3: accepted
    // (configuration)
    // 4: downloaded
    // 5: invalid URL
    // 6: failed to reload
    // 7: discarded
    // (play)
    // 4: invalid URL
    // 5: failed to reload
    // 6: discarded
    private int result;

    public ResourcePackStatus() {
    }

    public ResourcePackStatus(int result) {
        this.result = result;
    }

    public ResourcePackStatus(UUID uniqueId, int result) {
        this.uniqueId = uniqueId;
        this.result = result;
    }

    public ResourcePackStatus(String hash, int result) {
        this.hash = hash;
        this.result = result;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getHash() {
        return hash;
    }

    public int getResult() {
        return result;
    }

    public int getResult(int protocol) {
        if (result >= 4 && protocol < MINECRAFT_1_20_3) {
            return getResultEnum().getFallback();
        }
        return result;
    }

    @NotNull
    public Result getResultEnum() {
        return Result.VALUES[result];
    }

    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public void setResult(@NotNull Result result) {
        this.result = result.ordinal();
    }

    @Override
    public void read(ByteBuf buf, PacketDirection direction, int protocol) {
        if (protocol >= MINECRAFT_1_20_3) {
            uniqueId = ProtocolUtil.readUniqueId(buf);
        }
        if (protocol <= MINECRAFT_1_9_4) {
            hash = ProtocolUtil.readString(buf);
        }
        result = ProtocolUtil.readVarInt(buf);
        if (OneTimePack.getLogLevel() >= 4) {
            OneTimePack.log(4, "Packet#read() = " + this);
        }
    }

    @Override
    public void write(ByteBuf buf, PacketDirection direction, int protocol) {
        if (protocol >= MINECRAFT_1_20_3) {
            ProtocolUtil.writeUniqueId(buf, uniqueId);
        }
        if (protocol <= MINECRAFT_1_9_4) {
            ProtocolUtil.writeString(buf, hash);
        }
        ProtocolUtil.writeVarInt(buf, getResult(protocol));
    }

    public ResourcePackStatus copy() {
        return uniqueId != null ? new ResourcePackStatus(uniqueId, result) : new ResourcePackStatus(hash, result);
    }

    public ResourcePackStatus.Play asPlay() {
        if (this instanceof ResourcePackStatus.Play) {
            return (ResourcePackStatus.Play) this;
        }
        return uniqueId != null ? new ResourcePackStatus.Play(uniqueId, result) : new ResourcePackStatus.Play(hash, result);
    }

    public ResourcePackStatus.Configuration asConfiguration() {
        if (this instanceof ResourcePackStatus.Configuration) {
            return (ResourcePackStatus.Configuration) this;
        }
        return uniqueId != null ? new ResourcePackStatus.Configuration(uniqueId, result) : new ResourcePackStatus.Configuration(hash, result);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourcePackStatus that = (ResourcePackStatus) o;

        if (result != that.result) return false;
        if (!Objects.equals(uniqueId, that.uniqueId)) return false;
        return Objects.equals(hash, that.hash);
    }

    @Override
    public int hashCode() {
        int result1 = uniqueId != null ? uniqueId.hashCode() : 0;
        result1 = 31 * result1 + (hash != null ? hash.hashCode() : 0);
        result1 = 31 * result1 + result;
        return result1;
    }

    @Override
    public String toString() {
        return "ServerboundResourcePack{" +
                (uniqueId != null ? "uniqueId='" + uniqueId + "', " : "") +
                (hash != null ? "hash='" + hash + "', " : "") +
                "result=" + result +
                '}';
    }

    public static class Play extends ResourcePackStatus {
        public Play() {
        }

        public Play(int result) {
            super(result);
        }

        public Play(UUID uniqueId, int result) {
            super(uniqueId, result);
        }

        public Play(String hash, int result) {
            super(hash, result);
        }

        @Override
        public @NotNull Result getResultEnum() {
            if (getResult() >= 4) {
                return Result.VALUES[getResult() + 1];
            }
            return super.getResultEnum();
        }

        @Override
        public void setResult(@NotNull Result result) {
            super.setResult(result == Result.DOWNLOADED ? Result.SUCCESS_DOWNLOAD : result);
        }
    }

    public static class Configuration extends ResourcePackStatus {
        public Configuration() {
        }

        public Configuration(int result) {
            super(result);
        }

        public Configuration(UUID uniqueId, int result) {
            super(uniqueId, result);
        }

        public Configuration(String hash, int result) {
            super(hash, result);
        }
    }

    public enum Result {
        SUCCESS_DOWNLOAD,
        DECLINED,
        FAILED_DOWNLOAD,
        ACCEPTED,
        DOWNLOADED(0),
        INVALID_URL(2),
        FAILED_RELOAD(2),
        DISCARDED(1);

        public static final Result[] VALUES = values();

        private final int fallback;

        Result() {
            this.fallback = 0;
        }

        Result(int fallback) {
            this.fallback = fallback;
        }

        public int getFallback() {
            return fallback;
        }
    }
}
