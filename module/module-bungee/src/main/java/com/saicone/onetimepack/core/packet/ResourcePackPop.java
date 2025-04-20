package com.saicone.onetimepack.core.packet;

import com.saicone.onetimepack.OneTimePack;
import com.saicone.onetimepack.core.BungeeProcessor;
import io.netty.buffer.ByteBuf;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.ProtocolConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class ResourcePackPop extends DefinedPacket {

    private boolean hasUniqueId;
    private UUID uniqueId;

    private transient Protocol protocol;

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
        return protocol == null ? Protocol.GAME : protocol;
    }

    public void setHasUniqueId(boolean hasUniqueId) {
        this.hasUniqueId = hasUniqueId;
    }

    public void setUniqueId(@Nullable UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception {
        if (OneTimePack.get().getPacketHandler() instanceof BungeeProcessor processor) {
            processor.onPackPop(this, handler);
        }
    }

    @Override
    public void read(ByteBuf buf, Protocol protocol, ProtocolConstants.Direction direction, int protocolVersion) {
        this.protocol = protocol;

        hasUniqueId = buf.readBoolean();
        if (hasUniqueId) {
            uniqueId = readUUID(buf);
        }
    }

    @Override
    public void write(ByteBuf buf, Protocol protocol, ProtocolConstants.Direction direction, int protocolVersion) {
        this.protocol = protocol;

        buf.writeBoolean(hasUniqueId);
        if (hasUniqueId) {
            writeUUID(uniqueId, buf);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ResourcePackPop that = (ResourcePackPop) obj;

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
        return "ClientboundResourcePackPopPacket{" +
                "hasUniqueId=" + hasUniqueId +
                (hasUniqueId ? ", uniqueId=" + uniqueId : "") +
                '}';
    }
}
