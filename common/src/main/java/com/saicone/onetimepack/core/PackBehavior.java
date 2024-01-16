package com.saicone.onetimepack.core;

import org.jetbrains.annotations.NotNull;

public enum PackBehavior {

    STACK,
    OVERRIDE;

    @NotNull
    public static PackBehavior of(@NotNull String s) {
        if (s.equalsIgnoreCase("OVERRIDE")) {
            return OVERRIDE;
        } else {
            return STACK;
        }
    }
}
