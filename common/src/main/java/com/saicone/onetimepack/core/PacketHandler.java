package com.saicone.onetimepack.core;

import com.saicone.onetimepack.OneTimePack;
import com.saicone.onetimepack.core.packet.ResourcePackRemove;
import com.saicone.onetimepack.module.listener.PacketListener;
import com.saicone.onetimepack.module.Mappings;
import com.saicone.onetimepack.core.packet.ResourcePackSend;
import com.saicone.onetimepack.core.packet.ResourcePackStatus;
import dev.simplix.protocolize.api.Direction;
import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.Protocol;
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.listener.PacketReceiveEvent;
import dev.simplix.protocolize.api.mapping.ProtocolIdMapping;
import dev.simplix.protocolize.api.packet.AbstractPacket;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

public class PacketHandler {

    private static final Class<?> START_CONFIGURATION;

    static {
        Class<?> startConfiguration = null;
        try {
            startConfiguration = Class.forName("net.md_5.bungee.protocol.packet.StartConfiguration");
        } catch (ClassNotFoundException e) {
            try {
                startConfiguration = Class.forName("com.velocitypowered.proxy.protocol.packet.config.StartUpdate");
            } catch (ClassNotFoundException ignored) { }
        }
        START_CONFIGURATION = startConfiguration;
    }

    private final OneTimePack plugin;

    private PacketListener packetListener;
    private BiPredicate<ResourcePackSend, ResourcePackSend> comparator;
    private ResourcePackStatus.Result defaultStatus = null;

    private final Map<UUID, PacketPlayer> players = new HashMap<>();

    public PacketHandler(@NotNull OneTimePack plugin) {
        this.plugin = plugin;
    }

    public void onLoad() {
        final Mappings mappings = new Mappings(plugin.getProvider().getPluginFolder(), "mappings.json");
        mappings.load();
        register(mappings, ResourcePackSend.class, ResourcePackSend::register);
        register(mappings, ResourcePackRemove.class, ResourcePackRemove::register);
        register(mappings, ResourcePackStatus.class, ResourcePackStatus::register);
        onReload();
    }

    public void onReload() {
        final String packComparator = OneTimePack.SETTINGS.getString("Pack.Comparator", "!UUID OR HASH").toUpperCase();
        final List<BiPredicate<ResourcePackSend, ResourcePackSend>> predicates = new ArrayList<>();
        for (String block : packComparator.split(" AND ")) {
            if (block.contains(" OR ")) {
                final List<BiPredicate<ResourcePackSend, ResourcePackSend>> optionals = new ArrayList<>();
                for (String optional : block.split(" OR ")) {
                    final BiPredicate<ResourcePackSend, ResourcePackSend> predicate = ResourcePackSend.comparator(optional);
                    if (predicate != null) {
                        optionals.add(predicate);
                    }
                }
                if (optionals.size() == 1) {
                    predicates.add(optionals.get(0));
                } else if (!optionals.isEmpty()) {
                    predicates.add((pack1, pack2) -> {
                        for (BiPredicate<ResourcePackSend, ResourcePackSend> optional : optionals) {
                            if (optional.test(pack1, pack2)) {
                                return true;
                            }
                        }
                        return false;
                    });
                }
            } else {
                final BiPredicate<ResourcePackSend, ResourcePackSend> predicate = ResourcePackSend.comparator(block);
                if (predicate != null) {
                    predicates.add(predicate);
                }
            }
        }

        if (predicates.isEmpty()) {
            comparator = (pack1, pack2) -> true;
        } else if (predicates.size() == 1) {
            comparator = predicates.get(0);
        } else {
            comparator = (pack1, pack2) -> {
                for (BiPredicate<ResourcePackSend, ResourcePackSend> predicate : predicates) {
                    if (!predicate.test(pack1, pack2)) {
                        return false;
                    }
                }
                return true;
            };
        }

        final Object status = OneTimePack.SETTINGS.get("Pack.Default-Status");
        if (status instanceof Number) {
            final int num = ((Number) status).intValue();
            if (num > 7) {
                defaultStatus = null;
                OneTimePack.log(2, "Default pack status cannot be a number upper than 7, so will be set as 'none'");
            } else {
                defaultStatus = num < 0 ? null : ResourcePackStatus.Result.VALUES[num];
            }
            return;
        } else if (status != null) {
            final String name = String.valueOf(status).trim().toUpperCase().replace(' ', '_');
            if (name.equals("NONE")) {
                defaultStatus = null;
                return;
            }
            for (ResourcePackStatus.Result result : ResourcePackStatus.Result.VALUES) {
                if (result.name().equals(name)) {
                    defaultStatus = result;
                    return;
                }
            }
            OneTimePack.log(2, "The pack status '" + status + "' is not a valid state");
        }
        defaultStatus = null;
    }

    public void onEnable() {
        packetListener = new PacketListener();
        packetListener.registerSend(START_CONFIGURATION, Direction.UPSTREAM, event -> {
            final UUID uuid = event.player().uniqueId();
            if (players.containsKey(uuid)) {
                OneTimePack.log(4, "The cached pack will be send for player due it's on configuration state");
                new Thread(() -> {
                    for (Map.Entry<UUID, ResourcePackSend> entry : players.get(uuid).getPacks().entrySet()) {
                        event.player().sendPacket(getWrappedPacket(entry.getValue().asConfiguration(), Protocol.CONFIGURATION, PacketDirection.CLIENTBOUND, event.player().protocolVersion()));
                    }
                    OneTimePack.log(4, "Sent!");
                }).start();
            }
        });
        packetListener.registerReceive(ResourcePackSend.Configuration.class, Direction.DOWNSTREAM, event -> {
            final ResourcePackSend.Configuration packet = event.packet();
            if (onPackSend(event, packet)) {
                getPacketPlayer(event.player()).add(packet);
                OneTimePack.log(4, "Save packet on config for player " + event.player().uniqueId());
            }
        });
        packetListener.registerReceive(ResourcePackSend.Play.class, Direction.DOWNSTREAM, event -> {
            final ResourcePackSend.Play packet = event.packet();
            if (!onPackSend(event, packet)) {
                return;
            }

            final PacketPlayer player = getPacketPlayer(event.player());
            final UUID uuid = event.player().uniqueId();
            // Cancel resource pack re-sending to player
            if (player.contains(packet, comparator)) {
                OneTimePack.log(4, "Same resource pack received for player: " + player.getUniqueId());
                event.cancelled(true);
                // Re-send to server the actual resource pack status from player
                plugin.getProvider().run(() -> {
                    final ResourcePackStatus cached = player.getResult(packet, defaultStatus);
                    if (cached == null) {
                        return;
                    }
                    event.player().sendPacketToServer(cached.asPlay());
                    if (OneTimePack.getLogLevel() >= 4) {
                        OneTimePack.log(4, "Sent cached result " + cached + " from player " + uuid);
                    }
                }, true);
                return;
            }

            player.add(packet);
            OneTimePack.log(4, "Save packet on play for player " + uuid);
        });
        packetListener.registerReceive(ResourcePackRemove.Configuration.class, Direction.UPSTREAM, event -> onPackRemove(event.player(), event.packet()));
        packetListener.registerReceive(ResourcePackRemove.Play.class, Direction.UPSTREAM, event -> onPackRemove(event.player(), event.packet()));
        packetListener.registerReceive(ResourcePackStatus.Configuration.class, Direction.UPSTREAM, event -> onPackStatus(event.player(), event.packet()));
        packetListener.registerReceive(ResourcePackStatus.Play.class, Direction.UPSTREAM, event -> onPackStatus(event.player(), event.packet()));
    }

    public void onDisable() {
        if (packetListener != null) {
            packetListener.unregister();
        }
        clear();
    }

    private boolean onPackSend(@NotNull PacketReceiveEvent<?> event, @Nullable ResourcePackSend packet) {
        if (packet == null) {
            OneTimePack.log(4, "The packet ResourcePackSend was null");
            return false;
        }
        if (OneTimePack.getLogLevel() >= 4) {
            OneTimePack.log(4, "Received ResourcePackSend: " + packet);
        }

        final String hash = packet.getHash();
        // Avoid invalid resource pack sending
        if (String.valueOf(hash).equalsIgnoreCase("null") || hash.trim().isEmpty()) {
            OneTimePack.log(4, "Invalid packet received, so will be cancelled");
            event.cancelled(true);
            return false;
        }
        return true;
    }

    private void onPackRemove(@NotNull ProtocolizePlayer player, @Nullable ResourcePackRemove packet) {
        if (packet == null) {
            OneTimePack.log(4, "The packet ResourcePackRemove was null");
            return;
        }
        getPacketPlayer(player).remove(packet);
        OneTimePack.log(4, "Remove cached packet using: " + packet + " from player " + player.uniqueId());
    }

    private void onPackStatus(@NotNull ProtocolizePlayer player, @Nullable ResourcePackStatus packet) {
        if (packet == null) {
            OneTimePack.log(4, "The packet ResourcePackStatus was null");
            return;
        }
        getPacketPlayer(player).add(packet);
        OneTimePack.log(4, "Saved cached result: " + packet + " from player " + player.uniqueId());
    }

    @NotNull
    public Map<UUID, PacketPlayer> getPlayers() {
        return players;
    }

    @NotNull
    public PacketPlayer getPacketPlayer(@NotNull ProtocolizePlayer player) {
        PacketPlayer packetPlayer = players.get(player.uniqueId());
        if (packetPlayer == null) {
            packetPlayer = new PacketPlayer(player.uniqueId(), player.protocolVersion());
            players.put(player.uniqueId(), packetPlayer);
        }
        return packetPlayer;
    }

    @NotNull
    public static Object getWrappedPacket(@NotNull AbstractPacket packet, @NotNull Protocol protocol, @NotNull PacketDirection direction, int protocolVersion) {
        final Object wrapped = Protocolize.protocolRegistration().createPacket(packet.getClass(), protocol, direction, protocolVersion);
        try {
            wrapped.getClass().getDeclaredMethod("wrapper", AbstractPacket.class).invoke(wrapped, packet);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return wrapped;
    }

    private <T extends AbstractPacket> void register(@NotNull Mappings mappings, @NotNull Class<T> clazz, @NotNull Consumer<Function<String, List<ProtocolIdMapping>>> consumer) {
        if (!mappings.contains(clazz.getSimpleName())) {
            consumer.accept(null);
        } else {
            consumer.accept(protocol -> mappings.getMappings(clazz.getSimpleName(), protocol));
        }
    }

    public void clear() {
        OneTimePack.log(4, "The data from packet handler was cleared");
        for (Map.Entry<UUID, PacketPlayer> entry : players.entrySet()) {
            entry.getValue().clear();
        }
        players.clear();
    }

    public void clear(@NotNull UUID uuid) {
        OneTimePack.log(4, "Removing data from player " + uuid);
        final PacketPlayer player = players.remove(uuid);
        if (player != null) {
            player.clear();
        }
    }
}
