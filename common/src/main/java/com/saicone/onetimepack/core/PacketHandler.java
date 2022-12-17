package com.saicone.onetimepack.core;

import com.saicone.onetimepack.OneTimePack;
import com.saicone.onetimepack.module.listener.PacketListener;
import com.saicone.onetimepack.module.Mappings;
import com.saicone.onetimepack.core.packet.ResourcePackSend;
import com.saicone.onetimepack.core.packet.ResourcePackStatus;
import dev.simplix.protocolize.api.Direction;
import dev.simplix.protocolize.api.mapping.ProtocolIdMapping;
import dev.simplix.protocolize.api.packet.AbstractPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class PacketHandler {

    private final OneTimePack plugin;

    private PacketListener packetListener;
    private BiFunction<ResourcePackSend, ResourcePackSend, Boolean> comparator;
    private int defaultStatus = -1;

    private final Map<UUID, ResourcePackSend> cachedPack = new HashMap<>();
    private final Map<UUID, ResourcePackStatus> cachedResult = new HashMap<>();

    public PacketHandler(@NotNull OneTimePack plugin) {
        this.plugin = plugin;
    }

    public void onLoad() {
        final Mappings mappings = new Mappings(plugin.getProvider().getPluginFolder(), "mappings.json");
        final Map<String, List<ProtocolIdMapping>> map = mappings.load();
        register(map, ResourcePackSend.class, ResourcePackSend::register);
        register(map, ResourcePackStatus.class, ResourcePackStatus::register);
        final String packComparator = OneTimePack.SETTINGS.getString("Pack.Comparator", "HASH");
        switch (packComparator.toUpperCase()) {
            case "URL":
                comparator = (pack1, pack2) -> Objects.equals(pack1.getUrl(), pack2.getUrl());
                OneTimePack.log(3, "URL comparator will be used");
                break;
            case "HASH":
                comparator = (pack1, pack2) -> Objects.equals(pack1.getHash(), pack2.getHash());
                OneTimePack.log(3, "HASH comparator will be used");
                break;
            case "PROMPT":
            case "MESSAGE":
            case "MSG":
            case "PROMPT_MESSAGE":
                comparator = (pack1, pack2) -> Objects.equals(pack1.getPromptMessage(), pack2.getPromptMessage());
                OneTimePack.log(3, "PROMPT_MESSAGE comparator will be used, take in count this only work for Minecraft 1.17 or upper");
                break;
            case "ALL":
                comparator = ResourcePackSend::equals;
                OneTimePack.log(3, "ALL comparator will be used");
                break;
            case "ANY":
                comparator = (pack1, pack2) -> true;
                OneTimePack.log(3, "ANY comparator will be used, if you have different resource packs in your network will be compared as equals");
                break;
            default:
                comparator = (pack1, pack2) -> Objects.equals(pack1.getHash(), pack2.getHash());
                OneTimePack.log(2, "The string '" + packComparator + "' is not a valid comparator, HASH comparator will be used by default");
                break;
        }
        defaultStatus = OneTimePack.SETTINGS.getInt("Pack.Default-Status", -1);
        if (defaultStatus > 3) {
            defaultStatus = -1;
            OneTimePack.log(2, "Default pack status cannot be a number upper than 3, so will be set as -1");
        }
    }

    public void onEnable() {
        packetListener = new PacketListener();
        packetListener.registerReceive(ResourcePackSend.class, Direction.DOWNSTREAM, event -> {
            final ResourcePackSend packet = event.packet();
            if (packet == null) {
                OneTimePack.log(4, "The packet ResourcePackSend was null");
                return;
            }
            if (OneTimePack.getLogLevel() >= 4) {
                OneTimePack.log(4, "Received ResourcePackSend: " + packet);
            }

            final String hash = packet.getHash();
            // Avoid invalid resource pack sending
            if (String.valueOf(hash).equalsIgnoreCase("null") || hash.trim().isEmpty()) {
                OneTimePack.log(4, "Invalid packet received, so will be cancelled");
                event.cancelled(true);
                return;
            }

            final UUID uuid = event.player().uniqueId();
            // Cancel resource pack re-sending to player
            if (cachedPack.containsKey(uuid) && comparator.apply(cachedPack.get(uuid), packet)) {
                OneTimePack.log(4, "Same resource pack received for player: " + uuid);
                event.cancelled(true);
                // Re-send to server the actual resource pack status from player
                plugin.getProvider().run(() -> {
                    if (!cachedResult.containsKey(uuid)) {
                        if (defaultStatus < 0) {
                            return;
                        }
                        cachedResult.put(uuid, new ResourcePackStatus(packet.getHash(), defaultStatus));
                    }
                    event.player().sendPacketToServer(cachedResult.get(uuid));
                    if (OneTimePack.getLogLevel() >= 4) {
                        OneTimePack.log(4, "Sent cached result " + cachedResult.get(uuid) + " from player " + uuid);
                    }
                }, true);
                return;
            }

            cachedPack.put(uuid, packet.copy());
            OneTimePack.log(4, "Save packet for player " + uuid);
        });
        packetListener.registerReceive(ResourcePackStatus.class, Direction.UPSTREAM, event -> {
            final ResourcePackStatus packet = event.packet();
            if (packet == null) {
                OneTimePack.log(4, "The packet ResourcePackStatus was null");
                return;
            }

            cachedResult.put(event.player().uniqueId(), packet.copy());
            OneTimePack.log(4, "Saved cached result: " + packet + " from player " + event.player().uniqueId());
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
        OneTimePack.log(4, "The data from packet handler was cleared");
        cachedPack.clear();
        cachedResult.clear();
    }

    public void clear(@NotNull UUID uuid) {
        OneTimePack.log(4, "Removing data from player " + uuid);
        cachedPack.remove(uuid);
        cachedResult.remove(uuid);
    }
}
