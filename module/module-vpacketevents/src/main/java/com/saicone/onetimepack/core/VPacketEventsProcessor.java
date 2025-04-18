package com.saicone.onetimepack.core;

import com.saicone.onetimepack.OneTimePack;
import com.saicone.onetimepack.util.ValueComparator;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.network.ProtocolState;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.RemoveResourcePackPacket;
import com.velocitypowered.proxy.protocol.packet.ResourcePackRequestPacket;
import com.velocitypowered.proxy.protocol.packet.ResourcePackResponsePacket;
import com.velocitypowered.proxy.protocol.packet.config.StartUpdatePacket;
import io.github._4drian3d.vpacketevents.api.event.PacketReceiveEvent;
import io.github._4drian3d.vpacketevents.api.event.PacketSendEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class VPacketEventsProcessor extends Processor<Player, ResourcePackRequestPacket, ProtocolState> {

    private final ProxyServer proxy;
    private final Object plugin;

    public VPacketEventsProcessor(@NotNull ProxyServer proxy, @NotNull Object plugin) {
        this.proxy = proxy;
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        proxy.getEventManager().register(plugin, this);
    }

    @Subscribe
    public void onPacketReceive(PacketReceiveEvent event) {
        final MinecraftPacket packet = event.getPacket();
        if (packet instanceof ResourcePackResponsePacket response) {
            onPackStatus(event.getPlayer(), response.getId(), response.getStatus());
        }
    }

    @Subscribe
    public void onPacketSend(PacketSendEvent event) {
        final MinecraftPacket packet = event.getPacket();
        if (packet instanceof StartUpdatePacket) {
            if (!isSendCached1_20_2() || event.getPlayer().getProtocolVersion().greaterThan(ProtocolVersion.MINECRAFT_1_20_2)) {
                return;
            }
            final UUID uuid = event.getPlayer().getUniqueId();
            if (getUsers().containsKey(uuid)) {
                OneTimePack.log(4, "The cached pack will be send for player due it's on configuration state");
                // Send with new thread due player still on PLAY protocol
                new Thread(() -> {
                    if (!event.getResult().isAllowed()) return;
                    for (var entry : getUsers().get(uuid).getPacks().entrySet()) {
                        ((ConnectedPlayer) event.getPlayer()).getConnection().write(entry.getValue());
                    }
                    OneTimePack.log(4, "Sent!");
                }).start();
            }
        } else if (packet instanceof ResourcePackRequestPacket request) {
            onPackPush(event, request);
        } else if (packet instanceof RemoveResourcePackPacket remove) {
            if (onPackPop(event.getPlayer(), event.getPlayer().getProtocolState(), remove, remove.getId())) {
                event.setResult(ResultedEvent.GenericResult.denied());
            }
        }
    }

    private void onPackPush(@NotNull PacketSendEvent event, @NotNull ResourcePackRequestPacket packet) {
        final Optional<PackResult> optional = onPackPush(event.getPlayer(), event.getPlayer().getProtocolState(), packet, packet.getId(), packet.getHash());
        if (optional == null) return;

        event.setResult(ResultedEvent.GenericResult.denied());

        final PackResult result = optional.orElse(null);
        if (result == null) return;

        event.getPlayer().getCurrentServer().ifPresent(server -> {
            final ResourcePackResponsePacket cached = new ResourcePackResponsePacket(packet.getId(), packet.getHash(), PlayerResourcePackStatusEvent.Status.values()[result.ordinal()]);
            ((VelocityServerConnection) server).getConnection().write(cached);
            OneTimePack.log(4, () -> "Sent cached result " + cached + " from user " + event.getPlayer().getUniqueId());
        });
    }

    @Override
    protected @NotNull UUID getUserId(@NotNull Player player) {
        return player.getUniqueId();
    }

    @Override
    protected @Nullable ValueComparator<ResourcePackRequestPacket> getPackValue(@NotNull String name) {
        return switch (name) {
            case "UUID" -> ResourcePackRequestPacket::getId;
            case "URL" -> ResourcePackRequestPacket::getUrl;
            case "HASH" -> ResourcePackRequestPacket::getHash;
            case "PROMPT" -> pack -> pack.getPrompt() == null ? null : pack.getPrompt().getComponent();
            case "ALL" -> pack -> pack;
            case "ANY" -> pack -> true;
            default -> null;
        };
    }

    @Override
    public void clearPackets(@NotNull Player player, @NotNull ProtocolState state) {
        player.clearResourcePacks();
    }
}
