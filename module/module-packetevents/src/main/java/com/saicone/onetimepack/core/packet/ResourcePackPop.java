package com.saicone.onetimepack.core.packet;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.saicone.onetimepack.OneTimePack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class ResourcePackPop extends PacketWrapper<ResourcePackPop> implements CommonPacketWrapper<ResourcePackPop> {

    private final ConnectionState state;

    private boolean hasUniqueId;
    private UUID uniqueId;

    public ResourcePackPop(@NotNull PacketSendEvent event) {
        super(event, false);
        this.state = event.getPacketType() == PacketType.Configuration.Server.RESOURCE_PACK_REMOVE ? ConnectionState.CONFIGURATION : ConnectionState.PLAY;
        readEvent(event);
    }

    public ResourcePackPop(@NotNull ConnectionState state, boolean hasUniqueId, @Nullable UUID uniqueId) {
        super(state == ConnectionState.CONFIGURATION ? PacketType.Configuration.Server.RESOURCE_PACK_REMOVE : PacketType.Play.Server.RESOURCE_PACK_REMOVE);
        this.state = state;
        this.hasUniqueId = hasUniqueId;
        this.uniqueId = uniqueId;
    }

    @NotNull
    @Override
    public ConnectionState getState() {
        return state;
    }

    @Nullable
    public UUID getUniqueId() {
        return uniqueId;
    }

    public boolean hasUniqueId() {
        return hasUniqueId;
    }

    public void setHasUniqueId(boolean hasUniqueId) {
        this.hasUniqueId = hasUniqueId;
    }

    public void setUniqueId(@Nullable UUID uniqueId) {
        this.uniqueId = uniqueId;
        this.hasUniqueId = uniqueId != null;
    }

    @Override
    public void read() {
        hasUniqueId = readBoolean();
        if (hasUniqueId) {
            uniqueId = readUUID();
        }
        if (OneTimePack.getLogLevel() >= 4) {
            OneTimePack.log(4, "[" + getState().name() + "] Packet#read() = " + this);
        }
    }

    @Override
    public void write() {
        writeBoolean(hasUniqueId);
        if (hasUniqueId) {
            writeUUID(uniqueId);
        }
    }

    @Override
    public @NotNull ResourcePackPop copy() {
        return new ResourcePackPop(state, hasUniqueId, uniqueId);
    }

    @Override
    public void copy(ResourcePackPop wrapper) {
        hasUniqueId = wrapper.hasUniqueId;
        uniqueId = wrapper.uniqueId;
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
}
