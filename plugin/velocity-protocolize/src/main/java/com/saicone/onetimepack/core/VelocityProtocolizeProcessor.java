package com.saicone.onetimepack.core;

import com.saicone.onetimepack.OneTimePack;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VelocityProtocolizeProcessor extends ProtocolizeProcessor<StartUpdatePacket, ResourcePackRequestPacket, RemoveResourcePackPacket, ResourcePackResponsePacket> {

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
            case "PROMPT" -> ResourcePackRequestPacket::getPrompt;
            case "ALL" -> pack -> pack;
            case "ANY" -> pack -> true;
            default -> null;
        };
    }

    @Override
    protected void registerListeners() {
        getPacketListener().registerReceive(ResourcePackRequestPacket.class, Direction.DOWNSTREAM, event -> {
            final ResourcePackRequestPacket packet = event.packet();
            if (packet == null) {
                OneTimePack.log(4, "The packet ResourcePackPush was null");
                event.cancelled(!isSendInvalid());
                return;
            }
            proxy.getPlayer(event.player().uniqueId()).ifPresent(player -> {
                event.cancelled(onPackSend(event.player(), getProtocol(player), packet, packet.getId(), packet.getHash(), getConfigurationOptions()));
            });
        });
        getPacketListener().registerReceive(RemoveResourcePackPacket.class, Direction.DOWNSTREAM, event -> {
            final RemoveResourcePackPacket packet = event.packet();
            if (packet == null) {
                OneTimePack.log(4, "The packet ResourcePackPop was null");
                event.cancelled(true);
                return;
            }
            proxy.getPlayer(event.player().uniqueId()).ifPresent(player -> {
                event.cancelled(onPackRemove(event.player(), getProtocol(player), packet, packet.getId(), getConfigurationOptions()));
            });
        });
        getPacketListener().registerReceive(ResourcePackResponsePacket.class, Direction.UPSTREAM, event -> {
            final ResourcePackResponsePacket packet = event.packet();
            if (packet == null) {
                OneTimePack.log(4, "The packet ResourcePackStatus was null");
                return;
            }
            proxy.getPlayer(event.player().uniqueId()).ifPresent(player -> {
                onPackStatus(event.player(), packet, packet.getId(), PackResult.of(packet.getStatus().ordinal()));
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
    protected @NotNull RemoveResourcePackPacket getClearPacket(@NotNull Protocol protocol) {
        return new RemoveResourcePackPacket();
    }

    @Override
    protected @NotNull ResourcePackResponsePacket getStatusPacket(@NotNull Protocol protocol, @NotNull ResourcePackRequestPacket packet, @NotNull PackResult result) {
        if (packet.getId() == null) {
            return new ResourcePackResponsePacket(null, packet.getHash(), PlayerResourcePackStatusEvent.Status.values()[result.ordinal()]);
        } else {
            return new ResourcePackResponsePacket(packet.getId(), null, PlayerResourcePackStatusEvent.Status.values()[result.ordinal()]);
        }
    }
}
