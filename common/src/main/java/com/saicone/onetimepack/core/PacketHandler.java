package com.saicone.onetimepack.core;

import com.saicone.onetimepack.ProxyResourcePack;
import com.saicone.onetimepack.module.listener.PacketListener;
import com.saicone.onetimepack.module.Mappings;
import com.saicone.onetimepack.core.packet.ResourcePackSend;
import com.saicone.onetimepack.core.packet.ResourcePackStatus;
import dev.simplix.protocolize.api.Direction;
import dev.simplix.protocolize.api.mapping.ProtocolIdMapping;
import dev.simplix.protocolize.api.packet.AbstractPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class PacketHandler {

    private final ProxyResourcePack plugin;

    private PacketListener packetListener;

    private final Map<UUID, String> cachedPack = new HashMap<>();
    private final Map<UUID, ResourcePackStatus> cachedResult = new HashMap<>();

    public PacketHandler(@NotNull ProxyResourcePack plugin) {
        this.plugin = plugin;
    }

    public void onLoad() {
        final Mappings mappings = new Mappings(plugin.getProvider().getPluginFolder(), "mappings.json");
        final Map<String, List<ProtocolIdMapping>> map = mappings.load();
        register(map, ResourcePackSend.class, ResourcePackSend::register);
        register(map, ResourcePackStatus.class, ResourcePackStatus::register);
    }

    public void onEnable() {
        packetListener = new PacketListener();
        packetListener.registerReceive(ResourcePackSend.class, Direction.DOWNSTREAM, event -> {
            final ResourcePackSend packet = event.packet();
            if (packet == null) {
                return;
            }

            final String hash = packet.getHash();
            // Avoid invalid resource pack sending
            if (String.valueOf(hash).equalsIgnoreCase("null") || hash.trim().isEmpty()) {
                event.cancelled(true);
                return;
            }

            final UUID uuid = event.player().uniqueId();
            // Cancel resource pack re-sending to player
            if (cachedPack.containsKey(uuid) && cachedPack.get(uuid).equals(hash)) {
                event.cancelled(true);
                // Re-send to server the actual resource pack status from player
                plugin.getProvider().run(() -> {
                    if (cachedResult.containsKey(uuid)) {
                        event.player().sendPacketToServer(cachedResult.get(uuid));
                    }
                }, true);
                return;
            }

            cachedPack.put(uuid, packet.getHash());
        });
        packetListener.registerReceive(ResourcePackStatus.class, Direction.UPSTREAM, event -> {
            final ResourcePackStatus packet = event.packet();
            if (packet == null) {
                return;
            }

            cachedResult.put(event.player().uniqueId(), packet.copy());
        });
    }

    public void onDisable() {
        if (packetListener != null) {
            packetListener.unregister();
        }
        clear();
    }

    private <T extends AbstractPacket> void register(@Nullable Map<String, List<ProtocolIdMapping>> map, @NotNull Class<T> clazz, @NotNull Consumer<List<ProtocolIdMapping>> consumer) {
        if (map == null || !map.containsKey(clazz.getSimpleName())) {
            consumer.accept(null);
        } else {
            final List<ProtocolIdMapping> mappings = map.get(clazz.getSimpleName());
            if (!mappings.isEmpty()) {
                consumer.accept(mappings);
            }
        }
    }

    public void clear() {
        cachedPack.clear();
        cachedResult.clear();
    }

    public void clear(@NotNull UUID uuid) {
        cachedPack.remove(uuid);
        cachedResult.remove(uuid);
    }
}
