package com.saicone.onetimepack.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum ProtocolState {

    HANDSHAKING,
    STATUS,
    LOGIN,
    PLAY,
    CONFIGURATION;

    private static final ProtocolState[] VALUES = values();

    @Nullable
    public static ProtocolState of(@NotNull String s) {
        for (ProtocolState state : VALUES) {
            if (state.name().equalsIgnoreCase(s)) {
                return state;
            }
        }
        return null;
    }

    @NotNull
    public static ProtocolState of(@NotNull Enum<?> e) {
        return VALUES[e.ordinal()];
    }
}
