package com.saicone.onetimepack.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class PacketUser<PackT> {

    private static final UUID DUMMY_ID = new UUID(0, 0);
    private static final int MINECRAFT_1_20_3 = 765;

    private final UUID uniqueId;
    private final int protocolVersion;

    private final transient boolean uniquePack;

    private final Map<UUID, PackT> cachedPacks = new LinkedHashMap<>();
    private final Map<UUID, PackResult> cachedResults = new HashMap<>();

    public PacketUser(@NotNull UUID uniqueId, int protocolVersion) {
        this.uniqueId = uniqueId;
        this.protocolVersion = protocolVersion;
        this.uniquePack = protocolVersion < MINECRAFT_1_20_3;
    }

    public boolean isUniquePack() {
        return uniquePack;
    }

    @NotNull
    public UUID getUniqueId() {
        return uniqueId;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    @Nullable
    public PackT getPack() {
        if (cachedPacks.isEmpty()) {
            return null;
        }
        if (uniquePack) {
            return cachedPacks.get(DUMMY_ID);
        }
        return cachedPacks.entrySet().iterator().next().getValue();
    }

    @NotNull
    public Map<UUID, PackT> getPacks() {
        return cachedPacks;
    }

    @Nullable
    public PackResult getResult() {
        if (cachedResults.isEmpty()) {
            return null;
        }
        if (uniquePack) {
            return cachedResults.get(DUMMY_ID);
        }
        return cachedResults.entrySet().iterator().next().getValue();
    }

    @Nullable
    public PackResult getResult(@Nullable UUID uniqueId) {
        if (uniquePack) {
            return cachedResults.get(DUMMY_ID);
        }
        return cachedResults.get(uniqueId);
    }

    @Nullable
    public PackResult getResult(@NotNull UUID id, @NotNull ProtocolOptions<PackT> options) {
        return cachedResults.getOrDefault(id, options.getDefaultStatus());
    }

    @NotNull
    public Map<UUID, PackResult> getResults() {
        return cachedResults;
    }

    @Nullable
    public UUID contains(@NotNull PackT packet, @NotNull ProtocolOptions<PackT> options) {
        for (Map.Entry<UUID, PackT> entry : cachedPacks.entrySet()) {
            if (options.getComparator().matches(entry.getValue(), packet)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void putPack(@Nullable UUID id, @NotNull PackT packet) {
        if (id == null || uniquePack) {
            cachedPacks.put(DUMMY_ID, packet);
        } else {
            cachedPacks.put(id, packet);
        }
    }

    public <E extends Enum<E>> void putResult(@Nullable UUID id, @NotNull E result) {
        if (id == null || uniquePack) {
            cachedResults.put(DUMMY_ID, PackResult.from(result));
        } else {
            cachedResults.put(id, PackResult.from(result));
        }
    }

    public void putResult(@Nullable UUID id, @NotNull PackResult result) {
        if (id == null || uniquePack) {
            cachedResults.put(DUMMY_ID, result);
        } else {
            cachedResults.put(id, result);
        }
    }

    public void removePack() {
        cachedPacks.clear();
    }

    public void removePack(@Nullable UUID id) {
        if (id == null || uniquePack) {
            cachedPacks.remove(DUMMY_ID);
        } else {
            cachedPacks.remove(id);
        }
    }

    public void handleResult(@Nullable UUID id) {
        if (id == null || uniquePack) {
            cachedPacks.clear();
        } else {
            cachedPacks.remove(id);
        }
    }

    public void clear() {
        cachedPacks.clear();
        cachedResults.clear();
    }
}
