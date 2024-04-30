package com.saicone.onetimepack.core;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum PackResult {

    SUCCESS_DOWNLOAD,
    DECLINED,
    FAILED_DOWNLOAD,
    ACCEPTED,
    DOWNLOADED(0),
    INVALID_URL(2),
    FAILED_RELOAD(2),
    DISCARDED(1);

    public static final PackResult[] VALUES = values();

    private final int fallback;

    PackResult() {
        this.fallback = 0;
    }

    PackResult(int fallback) {
        this.fallback = fallback;
    }

    public int getFallback() {
        return fallback;
    }

    @NotNull
    public static <E extends Enum<E>> PackResult from(@NotNull E value) {
        return from(value, SUCCESS_DOWNLOAD);
    }

    @Nullable
    @Contract("_, !null -> !null")
    public static <E extends Enum<E>> PackResult from(@NotNull E value, @Nullable PackResult def) {
        return of(value.ordinal(), def);
    }

    @NotNull
    public static PackResult of(int ordinal) {
        return of(ordinal, SUCCESS_DOWNLOAD);
    }

    @Nullable
    @Contract("_, !null -> !null")
    public static PackResult of(int ordinal, @Nullable PackResult def) {
        if (ordinal < VALUES.length) {
            return VALUES[ordinal];
        }
        return def;
    }

    @NotNull
    public static PackResult of(@NotNull String s) {
        return of(s, SUCCESS_DOWNLOAD);
    }

    @Nullable
    @Contract("_, !null -> !null")
    public static PackResult of(@NotNull String s, @Nullable PackResult def) {
        if (s.equalsIgnoreCase("none")) {
            return def;
        }
        for (PackResult value : VALUES) {
            if (value.name().equalsIgnoreCase(s)) {
                return value;
            }
        }
        return def;
    }
}
