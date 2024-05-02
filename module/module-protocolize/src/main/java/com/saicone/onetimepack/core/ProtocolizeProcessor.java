package com.saicone.onetimepack.core;

import com.saicone.onetimepack.OneTimePack;
import com.saicone.onetimepack.module.listener.PacketListener;
import dev.simplix.protocolize.api.*;
import dev.simplix.protocolize.api.listener.PacketReceiveEvent;
import dev.simplix.protocolize.api.packet.AbstractPacket;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import dev.simplix.protocolize.api.util.ProtocolVersions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public abstract class ProtocolizeProcessor<StartT, PushT, StatusT> extends Processor<ProtocolizePlayer, PushT, Protocol> {

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

    protected void onPackPush(@NotNull PacketReceiveEvent<? extends PushT> event, @NotNull Protocol protocol, @Nullable UUID id, @Nullable Object hash) {
        final ProtocolizePlayer player = event.player();
        final Optional<PackResult> optional = onPackPush(player, protocol, event.packet(), id, hash);
        if (optional == null) return;

        event.cancelled(true);

        final PackResult result = optional.orElse(null);
        if (result == null) return;

        final StatusT cached = getStatusPacket(protocol, event.packet(), result);
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
        OneTimePack.log(4, () -> "Sent cached result " + result.name() + " from user " + player.uniqueId());
    }

    @NotNull
    public PacketListener getPacketListener() {
        return packetListener;
    }

    @Override
    public @NotNull ProtocolOptions<PushT> getOptions(@NotNull Protocol protocol) {
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
