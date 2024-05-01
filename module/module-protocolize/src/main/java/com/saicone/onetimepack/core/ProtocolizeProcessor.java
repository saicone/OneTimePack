package com.saicone.onetimepack.core;

import com.saicone.onetimepack.OneTimePack;
import com.saicone.onetimepack.module.listener.PacketListener;
import dev.simplix.protocolize.api.*;
import dev.simplix.protocolize.api.packet.AbstractPacket;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import dev.simplix.protocolize.api.util.ProtocolVersions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.UUID;

public abstract class ProtocolizeProcessor<StartT, PushT, PopT, StatusT> extends Processor<ProtocolizePlayer, PushT> {

    private static final MethodHandle WRAPPER;

    static {
        final String name;
        if (Protocolize.platform() == Platform.BUNGEECORD) {
            name = "dev.simplix.protocolize.bungee.packet.BungeeCordProtocolizePacket";
        } else {
            name = "dev.simplix.protocolize.velocity.packet.VelocityProtocolizePacket";
        }
        MethodHandle wrapper = null;
        try {
            final Class<?> packetClass = Class.forName(name);
            wrapper = MethodHandles.lookup().unreflect(packetClass.getDeclaredMethod("wrapper", AbstractPacket.class));
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        WRAPPER = wrapper;
    }

    private final Class<StartT> startConfigurationClass;

    private PacketListener packetListener;

    public ProtocolizeProcessor(@NotNull Class<StartT> startConfigurationClass) {
        this.startConfigurationClass = startConfigurationClass;
    }

    @Override
    public void onEnable() {
        packetListener = new PacketListener();
        packetListener.registerSend(startConfigurationClass, Direction.UPSTREAM, event -> {
            if (!isSendCached1_20_2() || event.player().protocolVersion() >= ProtocolVersions.MINECRAFT_1_20_3) {
                return;
            }
            final UUID uuid = event.player().uniqueId();
            if (getUsers().containsKey(uuid)) {
                OneTimePack.log(4, "The cached pack will be send for player due it's on configuration state");
                // Send with new thread due Protocolize catch StartConfiguration packet before proxy itself
                new Thread(() -> {
                    for (Map.Entry<UUID, PushT> entry : getUsers().get(uuid).getPacks().entrySet()) {
                        final PushT packet = getPushPacket(entry.getValue());
                        if (packet instanceof AbstractPacket) {
                            event.player().sendPacket(getWrappedPacket(
                                    (AbstractPacket) packet,
                                    Protocol.CONFIGURATION,
                                    PacketDirection.CLIENTBOUND,
                                    event.player().protocolVersion()
                            ));
                        } else {
                            event.player().sendPacket(packet);
                        }
                    }
                    OneTimePack.log(4, "Sent!");
                }).start();
            }
        });
        registerListeners();
    }

    protected abstract void registerListeners();

    protected boolean onPackSend(@NotNull ProtocolizePlayer player, @NotNull Protocol protocol, @NotNull PushT packet, @Nullable UUID id, @Nullable String hash) {
        return onPackSend(player, protocol, packet, id, hash, getOptions(protocol));
    }

    protected boolean onPackSend(@NotNull ProtocolizePlayer player, @NotNull Protocol protocol, @NotNull PushT packet, @Nullable UUID id, @Nullable String hash, @NotNull ProtocolOptions<PushT> options) {
        if (OneTimePack.getLogLevel() >= 4) {
            OneTimePack.log(4, "Received ResourcePackPush: " + packet);
        }

        // Avoid invalid resource pack sending
        if (String.valueOf(hash).equalsIgnoreCase("null")) {
            if (isSendInvalid()) {
                OneTimePack.log(4, "The packet doesn't contains HASH, but invalid packs are allowed");
            } else {
                OneTimePack.log(4, "Invalid packet HASH received, so will be cancelled");
                return true;
            }
        }

        final PacketUser<PushT> user = getPacketUser(player);
        final UUID uuid = player.uniqueId();
        // Cancel resource pack re-sending to player
        final UUID packId;
        if (!options.sendDuplicated() && (packId = user.contains(packet, options)) != null) {
            OneTimePack.log(4, "Same resource pack received for player: " + user.getUniqueId());
            // Re-send to server the actual resource pack status from player
            new Thread(() -> {
                final PackResult result = user.getResult(packId, options);
                if (result == null) {
                    OneTimePack.log(2, "The user " + user.getUniqueId() + " doesn't have any cached resource pack status");
                    return;
                }
                final StatusT cached = getStatusPacket(protocol, packet, result);
                if (protocol == Protocol.CONFIGURATION && cached instanceof AbstractPacket) {
                    player.sendPacketToServer(getWrappedPacket(
                            (AbstractPacket) cached,
                            Protocol.CONFIGURATION,
                            PacketDirection.SERVERBOUND,
                            player.protocolVersion()
                    ));
                } else {
                    player.sendPacketToServer(cached);
                }
                if (OneTimePack.getLogLevel() >= 4) {
                    OneTimePack.log(4, "Sent cached result " + cached + " from player " + uuid);
                }
            }).start();
            return true;
        }

        // Apply pack behavior for +1.20.3 client
        if (!user.isUniquePack() && !user.getPacks().isEmpty()) {
            OneTimePack.log(4, "Applying " + options.getBehavior().name() + " behavior...");
            if (options.getBehavior() == PackBehavior.OVERRIDE) {
                user.clear();
                final PopT clearPacket = getClearPacket(protocol);
                if (protocol == Protocol.CONFIGURATION && clearPacket instanceof AbstractPacket) {
                    player.sendPacket(getWrappedPacket(
                            (AbstractPacket) clearPacket,
                            Protocol.CONFIGURATION,
                            PacketDirection.CLIENTBOUND,
                            player.protocolVersion()
                    ));
                } else {
                    player.sendPacket(clearPacket);
                }
            }
        }

        user.putPack(id, packet);
        OneTimePack.log(4, "Save packet on " + protocol.name() + " protocol for player " + uuid);
        return false;
    }

    protected boolean onPackRemove(@NotNull ProtocolizePlayer player, @NotNull Protocol protocol, @NotNull PopT packet, @Nullable UUID id) {
        return onPackRemove(player, protocol, packet, id, getOptions(protocol));
    }

    protected boolean onPackRemove(@NotNull ProtocolizePlayer player, @NotNull Protocol protocol, @NotNull PopT packet, @Nullable UUID id, @NotNull ProtocolOptions<PushT> options) {
        if (!options.allowClear() && id == null) {
            OneTimePack.log(4, "Cancelling packs clear from " + protocol.name() + " protocol for player " + player.uniqueId());
            return true;
        }
        if (!options.allowRemove()) {
            OneTimePack.log(4, "Cancelling pack remove from " + protocol.name() + " protocol for player " + player.uniqueId());
            return true;
        }
        getPacketUser(player).removePack(id);
        OneTimePack.log(4, "Remove cached packet using: " + packet + " from player " + player.uniqueId());
        return false;
    }

    protected void onPackStatus(@NotNull ProtocolizePlayer player, @NotNull StatusT packet, @Nullable UUID id, @NotNull PackResult result) {
        getPacketUser(player).putResult(id, result);
        OneTimePack.log(4, "Saved cached result: " + packet + " from player " + player.uniqueId());
    }

    @NotNull
    public PacketListener getPacketListener() {
        return packetListener;
    }

    @NotNull
    public ProtocolOptions<PushT> getOptions(@NotNull Protocol protocol) {
        if (protocol == Protocol.CONFIGURATION) {
            return getConfigurationOptions();
        } else {
            return getPlayOptions();
        }
    }

    @Override
    protected @NotNull PacketUser<PushT> getPacketUser(@NotNull ProtocolizePlayer player) {
        PacketUser<PushT> packetUser = getUsers().get(player.uniqueId());
        if (packetUser == null) {
            packetUser = new PacketUser<>(player.uniqueId(), player.protocolVersion() < ProtocolVersions.MINECRAFT_1_20_3);
            getUsers().put(player.uniqueId(), packetUser);
        }
        return packetUser;
    }

    @NotNull
    protected PushT getPushPacket(@NotNull PushT packet) {
        return packet;
    }

    @NotNull
    protected abstract PopT getClearPacket(@NotNull Protocol protocol);

    @NotNull
    protected abstract StatusT getStatusPacket(@NotNull Protocol protocol, @NotNull PushT packet, @NotNull PackResult result);

    @NotNull
    public static Object getWrappedPacket(@NotNull AbstractPacket packet, @NotNull Protocol protocol, @NotNull PacketDirection direction, int protocolVersion) {
        final Object wrapped = Protocolize.protocolRegistration().createPacket(packet.getClass(), protocol, direction, protocolVersion);
        try {
            WRAPPER.invoke(wrapped, packet);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return wrapped;
    }
}
