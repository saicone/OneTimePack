package com.saicone.onetimepack.core;

import com.saicone.onetimepack.util.ValueComparator;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.protocol.packet.RemoveResourcePackPacket;
import com.velocitypowered.proxy.protocol.packet.ResourcePackRequestPacket;
import com.velocitypowered.proxy.protocol.packet.ResourcePackResponsePacket;
import com.velocitypowered.proxy.protocol.packet.config.StartUpdatePacket;
import dev.simplix.protocolize.api.Direction;
import dev.simplix.protocolize.api.Protocol;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VelocityProtocolizeProcessor extends ProtocolizeProcessor<StartUpdatePacket, ResourcePackRequestPacket, ResourcePackResponsePacket> {

    private final ProxyServer proxy;

    public VelocityProtocolizeProcessor(@NotNull ProxyServer proxy) {
        super(StartUpdatePacket.class);
        this.proxy = proxy;
    }

    @Override
    protected @Nullable ValueComparator<ResourcePackRequestPacket> getPackValue(@NotNull String name) {
        return switch (name) {
            case "UUID" -> ResourcePackRequestPacket::getId;
            case "URL" -> ResourcePackRequestPacket::getUrl;
            case "HASH" -> ResourcePackRequestPacket::getHash;
            case "PROMPT" -> pack -> pack.getPrompt() == null ? null : pack.getPrompt().getBinaryTag();
            case "ALL" -> pack -> pack;
            case "ANY" -> pack -> true;
            default -> null;
        };
    }

    @Override
    protected void registerListeners() {
        getPacketListener().registerReceive(ResourcePackRequestPacket.class, Direction.DOWNSTREAM, event -> {
            final ResourcePackRequestPacket packet = event.packet();
            proxy.getPlayer(event.player().uniqueId()).ifPresent(player -> {
                onPackPush(event, getProtocol(player), packet.getId(), packet.getHash());
            });
        });
        getPacketListener().registerReceive(RemoveResourcePackPacket.class, Direction.DOWNSTREAM, event -> {
            final RemoveResourcePackPacket packet = event.packet();
            proxy.getPlayer(event.player().uniqueId()).ifPresent(player -> {
                event.cancelled(onPackPop(event.player(), getProtocol(player), packet, packet.getId()));
            });
        });
        getPacketListener().registerReceive(ResourcePackResponsePacket.class, Direction.UPSTREAM, event -> {
            final ResourcePackResponsePacket packet = event.packet();
            proxy.getPlayer(event.player().uniqueId()).ifPresent(player -> {
                onPackStatus(event.player(), packet.getId(), PackResult.of(packet.getStatus().ordinal()));
            });
        });
    }

    @NotNull
    private Protocol getProtocol(@NotNull Player player) {
        return switch (player.getProtocolState()) {
            case HANDSHAKE -> Protocol.HANDSHAKE;
            case STATUS -> Protocol.STATUS;
            case LOGIN -> Protocol.LOGIN;
            case CONFIGURATION -> Protocol.CONFIGURATION;
            case PLAY -> Protocol.PLAY;
        };
    }

    @Override
    protected @NotNull ResourcePackResponsePacket getStatusPacket(@NotNull Protocol protocol, @NotNull ResourcePackRequestPacket packet, @NotNull PackResult result) {
        if (packet.getId() == null) {
            return new ResourcePackResponsePacket(null, packet.getHash(), PlayerResourcePackStatusEvent.Status.values()[result.ordinal()]);
        } else {
            return new ResourcePackResponsePacket(packet.getId(), null, PlayerResourcePackStatusEvent.Status.values()[result.ordinal()]);
        }
    }

    @Override
    public void clearPackets(@NotNull ProtocolizePlayer player, @NotNull Protocol state) {
        proxy.getPlayer(player.uniqueId()).ifPresent(Audience::clearResourcePacks);
    }
}
