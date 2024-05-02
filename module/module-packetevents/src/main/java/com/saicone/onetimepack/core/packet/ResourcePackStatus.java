package com.saicone.onetimepack.core.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.saicone.onetimepack.OneTimePack;
import com.saicone.onetimepack.core.PackResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class ResourcePackStatus extends PacketWrapper<ResourcePackStatus> implements CommonPacketWrapper<ResourcePackStatus> {

    private final ConnectionState state;

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

    public ResourcePackStatus(@NotNull PacketReceiveEvent event) {
        super(event, false);
        this.state = event.getPacketType() == PacketType.Configuration.Client.RESOURCE_PACK_STATUS ? ConnectionState.CONFIGURATION : ConnectionState.PLAY;
        readEvent(event);
    }

    public ResourcePackStatus(@NotNull PackResult result) {
        this(ConnectionState.PLAY, null, null, result);
    }

    public ResourcePackStatus(@Nullable String hash, @NotNull PackResult result) {
        this(ConnectionState.PLAY, null, hash, result);
    }

    public ResourcePackStatus(@NotNull ConnectionState state, @Nullable UUID uniqueId, @NotNull PackResult result) {
        this(state, uniqueId, null, result);
    }

    ResourcePackStatus(@NotNull ConnectionState state, @Nullable UUID uniqueId, @Nullable String hash, @NotNull PackResult result) {
        super(state == ConnectionState.CONFIGURATION ? PacketType.Configuration.Client.RESOURCE_PACK_STATUS : PacketType.Play.Client.RESOURCE_PACK_STATUS);
        this.state = state;
        this.uniqueId = uniqueId;
        this.hash = hash;
        this.result = result;
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

    @Nullable
    public String getHash() {
        return hash;
    }

    @NotNull
    public PackResult getResult() {
        return result;
    }

    public int getResultOrdinal() {
        if (result.ordinal() >= 4 && serverVersion.isOlderThan(ServerVersion.V_1_20_3)) {
            return result.getFallback();
        }
        return result.ordinal();
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
    public void read() {
        if (getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_3)) {
            uniqueId = readUUID();
        }
        if (getClientVersion().isOlderThan(ClientVersion.V_1_10)) {
            hash = readString(ResourcePackPush.MAX_HASH_LENGTH);
        }
        result = PackResult.of(readVarInt());
        OneTimePack.log(4, () -> "[" + getState().name() + "] Packet#read() = " + this);
    }

    @Override
    public void write() {
        if (getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_3)) {
            writeUUID(uniqueId);
        }
        if (getClientVersion().isOlderThan(ClientVersion.V_1_10)) {
            writeString(hash, ResourcePackPush.MAX_HASH_LENGTH);
        }
        writeVarInt(getResultOrdinal());
    }

    @Override
    public @NotNull ResourcePackStatus copy() {
        return new ResourcePackStatus(state, uniqueId, hash, result);
    }

    @Override
    public void copy(ResourcePackStatus wrapper) {
        uniqueId = wrapper.uniqueId;
        hash = wrapper.hash;
        result = wrapper.result;
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
        result1 = 31 * result1 + (result != null ? result.ordinal() : -1);
        return result1;
    }

    @Override
    public String toString() {
        return "ServerboundResourcePack{" +
                (uniqueId != null ? "uniqueId='" + uniqueId + "', " : "") +
                (hash != null ? "hash='" + hash + "', " : "") +
                "result=" + (result != null ? result.ordinal() : -1) +
                '}';
    }
}
