package com.saicone.onetimepack.core;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BungeePacketUser<PackT> extends PacketUser<PackT> {

    private final ProxiedPlayer player;

    public BungeePacketUser(@NotNull ProxiedPlayer player) {
        this.player = player;
    }

    @Override
    public @NotNull UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public int getProtocolVersion() {
        return player.getPendingConnection().getVersion();
    }

    @Override
    public @Nullable String getServer() {
        final Server server = player.getServer();
        return server == null ? null : server.getInfo().getName();
    }
}
