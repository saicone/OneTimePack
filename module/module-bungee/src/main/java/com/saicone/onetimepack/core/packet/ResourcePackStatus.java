package com.saicone.onetimepack.core.packet;

import com.saicone.onetimepack.OneTimePack;
import com.saicone.onetimepack.core.BungeeProcessor;
import com.saicone.onetimepack.core.PackResult;
import io.netty.buffer.ByteBuf;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.ProtocolConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class ResourcePackStatus extends DefinedPacket {

    private UUID uniqueId; // Added in 1.20.3
    private String hash; // Removed in 1.10
    // 0: successfully loaded
    // 1: declined
    // 2: failed download
    // 3: accepted
    // 4: downloaded
    // 5: invalid URL
    // 6: failed to reload
    // 7: discarded
    private PackResult result;

    private transient Protocol protocol;

    public ResourcePackStatus() {
    }

    @Deprecated(since = "1.20.3")
    public ResourcePackStatus(@NotNull PackResult result) {
        this.result = result;
    }

    public ResourcePackStatus(@Nullable UUID uniqueId, @NotNull PackResult result) {
        this.uniqueId = uniqueId;
        this.result = result;
    }

    @Deprecated(since = "1.10")
    public ResourcePackStatus(@Nullable String hash, @NotNull PackResult result) {
        this.hash = hash;
        this.result = result;
    }

    @Nullable
    public UUID getUniqueId() {
        return uniqueId;
    }

    @Nullable
    public String getHash() {
        return hash;
    }

    @NotNull
    public PackResult getResult() {
        return result;
    }

    public int getResultOrdinal(int protocol) {
        if (result.ordinal() >= 4 && protocol < ProtocolConstants.MINECRAFT_1_20_3) {
            return result.getFallback();
        }
        return result.ordinal();
    }

    @NotNull
    public Protocol getProtocol() {
        return protocol == null ? Protocol.GAME : protocol;
    }

    public void setUniqueId(@Nullable UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public void setHash(@Nullable String hash) {
        this.hash = hash;
    }

    public void setResult(@NotNull PackResult result) {
        this.result = result;
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception {
        if (OneTimePack.get().getPacketHandler() instanceof BungeeProcessor processor) {
            processor.onPackStatus(this, handler);
        }
    }

    @Override
    public void read(ByteBuf buf, Protocol protocol, ProtocolConstants.Direction direction, int protocolVersion) {
        this.protocol = protocol;

        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_20_3) {
            uniqueId = readUUID(buf);
        }
        if (protocolVersion <= ProtocolConstants.MINECRAFT_1_9_4) {
            hash = readString(buf);
        }
        result = PackResult.of(readVarInt(buf));
    }

    @Override
    public void write(ByteBuf buf, Protocol protocol, ProtocolConstants.Direction direction, int protocolVersion) {
        this.protocol = protocol;

        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_20_3) {
            writeUUID(uniqueId, buf);
        }
        if (protocolVersion <= ProtocolConstants.MINECRAFT_1_9_4) {
            writeString(hash, buf);
        }
        writeVarInt(getResultOrdinal(protocolVersion), buf);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ResourcePackStatus that = (ResourcePackStatus) obj;

        if (result != that.result) return false;
        if (!Objects.equals(uniqueId, that.uniqueId)) return false;
        return Objects.equals(hash, that.hash);
    }

    @Override
    public int hashCode() {
        int result1 = uniqueId != null ? uniqueId.hashCode() : 0;
        result1 = 31 * result1 + (hash != null ? hash.hashCode() : 0);
        result1 = 31 * result1 + (result != null ? result.ordinal() : -1);
        return result1;
    }

    @Override
    public String toString() {
        return "ServerboundResourcePackPacket{" +
                (uniqueId != null ? "uniqueId='" + uniqueId + "', " : "") +
                (hash != null ? "hash='" + hash + "', " : "") +
                "result=" + (result != null ? result.ordinal() : -1) +
                '}';
    }
}
