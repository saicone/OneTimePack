package com.saicone.onetimepack.core;

import com.saicone.onetimepack.core.packet.ResourcePackRemove;
import com.saicone.onetimepack.core.packet.ResourcePackSend;
import com.saicone.onetimepack.core.packet.ResourcePackStatus;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiPredicate;

import static dev.simplix.protocolize.api.util.ProtocolVersions.MINECRAFT_1_20_3;

public class PacketPlayer {

    private static final UUID DUMMY_ID = new UUID(0, 0);

    private final UUID uniqueId;
    private final int protocol;

    private final transient boolean uniquePack;

    private final Map<UUID, ResourcePackSend> cachedPacks = new LinkedHashMap<>();
    private final Map<UUID, ResourcePackStatus> cachedResults = new HashMap<>();

    public PacketPlayer(UUID uniqueId, int protocol) {
        this.uniqueId = uniqueId;
        this.protocol = protocol;
        this.uniquePack = protocol < MINECRAFT_1_20_3;
    }

    public boolean isUniquePack() {
        return uniquePack;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public int getProtocol() {
        return protocol;
    }

    public ResourcePackSend getPack() {
        if (cachedPacks.isEmpty()) {
            return null;
        }
        if (uniquePack) {
            return cachedPacks.get(DUMMY_ID);
        }
        return cachedPacks.entrySet().iterator().next().getValue();
    }

    public Map<UUID, ResourcePackSend> getPacks() {
        return cachedPacks;
    }

    public ResourcePackStatus getResult() {
        if (cachedResults.isEmpty()) {
            return null;
        }
        if (uniquePack) {
            return cachedResults.get(DUMMY_ID);
        }
        return cachedResults.entrySet().iterator().next().getValue();
    }

    public ResourcePackStatus getResult(UUID uniqueId) {
        if (uniquePack) {
            return cachedResults.get(DUMMY_ID);
        }
        return cachedResults.get(uniqueId);
    }

    public ResourcePackStatus getResult(ResourcePackSend packet, ResourcePackStatus.Result defaultResult) {
        ResourcePackStatus result;
        if (uniquePack) {
            result = cachedResults.get(DUMMY_ID);
        } else if (packet.getUniqueId() != null) {
            result = cachedResults.get(packet.getUniqueId());
        } else {
            result = null;
        }
        if (result == null && defaultResult != null) {
            if (uniquePack) {
                result = new ResourcePackStatus(packet.getHash(), defaultResult);
            } else if (packet.getUniqueId() != null) {
                result = new ResourcePackStatus(packet.getUniqueId(), defaultResult);
            } else {
                return null;
            }
            add(result);
        }
        return result;
    }

    public boolean contains(ResourcePackSend packet, BiPredicate<ResourcePackSend, ResourcePackSend> comparator) {
        for (Map.Entry<UUID, ResourcePackSend> entry : cachedPacks.entrySet()) {
            if (comparator.test(entry.getValue(), packet)) {
                return true;
            }
        }
        return false;
    }

    public void add(ResourcePackSend packet) {
        if (packet.getUniqueId() == null || uniquePack) {
            cachedPacks.put(DUMMY_ID, packet.copy());
        } else {
            cachedPacks.put(packet.getUniqueId(), packet.copy());
        }
    }

    public void add(ResourcePackStatus packet) {
        if (packet.getUniqueId() == null || uniquePack) {
            cachedResults.put(DUMMY_ID, packet.copy());
        } else {
            cachedResults.put(packet.getUniqueId(), packet.copy());
        }
    }

    public void remove() {
        cachedPacks.clear();
    }

    public void remove(ResourcePackSend packet) {
        if (packet.getUniqueId() == null || uniquePack) {
            cachedPacks.remove(DUMMY_ID);
        } else {
            cachedPacks.remove(packet.getUniqueId());
        }
    }

    public void remove(ResourcePackRemove packet) {
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
