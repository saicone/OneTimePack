package com.saicone.onetimepack.core.packet;

import com.github.retrooper.packetevents.protocol.ConnectionState;
import org.jetbrains.annotations.NotNull;

public interface CommonPacketWrapper<T extends CommonPacketWrapper<T>> {

    @NotNull
    ConnectionState getState();

    @NotNull
    T copy();

    @NotNull
    T as(@NotNull ConnectionState state);
}
