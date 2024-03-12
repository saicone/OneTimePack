package com.saicone.onetimepack.core;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.saicone.onetimepack.core.packet.ResourcePackPop;
import com.saicone.onetimepack.core.packet.ResourcePackPush;
import com.saicone.onetimepack.core.packet.ResourcePackStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class PacketUser {

    private static final UUID DUMMY_ID = new UUID(0, 0);

    private final UUID uniqueId;
    private final boolean uniquePack;

    private final Map<UUID, ResourcePackPush> cachedPacks = new LinkedHashMap<>();
    private final Map<UUID, ResourcePackStatus> cachedResults = new HashMap<>();

    public PacketUser(@NotNull User user) {
        this(user.getUUID(), user.getClientVersion().isOlderThan(ClientVersion.V_1_20_3));
    }

    public PacketUser(@NotNull UUID uniqueId, boolean uniquePack) {
        this.uniqueId = uniqueId;
        this.uniquePack = uniquePack;
    }

    public boolean isUniquePack() {
        return uniquePack;
    }

    @NotNull
    public UUID getUniqueId() {
        return uniqueId;
    }

    @Nullable
    public ResourcePackPush getPack() {
        if (cachedPacks.isEmpty()) {
            return null;
        }
        if (uniquePack) {
            return cachedPacks.get(DUMMY_ID);
        }
        return cachedPacks.entrySet().iterator().next().getValue();
    }

    @NotNull
    public Map<UUID, ResourcePackPush> getPacks() {
        return cachedPacks;
    }

    @Nullable
    public ResourcePackStatus getResult() {
        if (cachedResults.isEmpty()) {
            return null;
        }
        if (uniquePack) {
            return cachedResults.get(DUMMY_ID);
        }
        return cachedResults.entrySet().iterator().next().getValue();
    }

    @Nullable
    public ResourcePackStatus getResult(@Nullable UUID uniqueId) {
        if (uniquePack) {
            return cachedResults.get(DUMMY_ID);
        }
        return cachedResults.get(uniqueId);
    }

    @Nullable
    public ResourcePackStatus getResult(@NotNull ResourcePackPush packet, @NotNull ProtocolOptions options) {
        ResourcePackStatus result;
        if (uniquePack) {
            result = cachedResults.get(DUMMY_ID);
        } else if (packet.getUniqueId() != null) {
            result = cachedResults.get(packet.getUniqueId());
        } else {
            result = null;
        }
        if (result == null && options.getDefaultStatus() != null) {
            if (uniquePack) {
                result = new ResourcePackStatus(packet.getHash(), options.getDefaultStatus());
            } else if (packet.getUniqueId() != null) {
                result = new ResourcePackStatus(packet.getState(), packet.getUniqueId(), options.getDefaultStatus());
            } else {
                return null;
            }
            add(result);
        }
        return result;
    }

    public boolean contains(@NotNull ResourcePackPush packet, @NotNull ProtocolOptions options) {
        for (Map.Entry<UUID, ResourcePackPush> entry : cachedPacks.entrySet()) {
            if (options.getComparator().matches(entry.getValue(), packet)) {
                return true;
            }
        }
        return false;
    }

    public void add(@NotNull ResourcePackPush packet) {
        if (packet.getUniqueId() == null || uniquePack) {
            cachedPacks.put(DUMMY_ID, packet.copy());
        } else {
            cachedPacks.put(packet.getUniqueId(), packet.copy());
        }
    }

    public void add(@NotNull ResourcePackStatus packet) {
        if (packet.getUniqueId() == null || uniquePack) {
            cachedResults.put(DUMMY_ID, packet.copy());
        } else {
            cachedResults.put(packet.getUniqueId(), packet.copy());
        }
    }

    public void remove() {
        cachedPacks.clear();
    }

    public void remove(@NotNull ResourcePackPush packet) {
        if (packet.getUniqueId() == null || uniquePack) {
            cachedPacks.remove(DUMMY_ID);
        } else {
            cachedPacks.remove(packet.getUniqueId());
        }
    }

    public void remove(@NotNull ResourcePackPop packet) {
        if (!packet.hasUniqueId() || uniquePack) {
            cachedPacks.clear();
        } else {
            cachedPacks.remove(packet.getUniqueId());
        }
    }

    public void clear() {
        cachedPacks.clear();
        cachedResults.clear();
    }
}
