package com.saicone.onetimepack.core;

import com.velocitypowered.api.proxy.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class VelocityPacketUser<PackT> extends PacketUser<PackT> {

    private final Player player;

    public VelocityPacketUser(@NotNull Player player) {
        this.player = player;
    }

    @Override
    public @NotNull UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public int getProtocolVersion() {
        return player.getProtocolVersion().getProtocol();
    }

    @Override
    public @Nullable String getServer() {
        return player.getCurrentServer().map(server -> server.getServerInfo().getName()).orElse(null);
    }
}
