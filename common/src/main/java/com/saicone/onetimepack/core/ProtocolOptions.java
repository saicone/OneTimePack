package com.saicone.onetimepack.core;

import com.saicone.onetimepack.OneTimePack;
import com.saicone.onetimepack.module.TinySettings;
import com.saicone.onetimepack.util.ValueComparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class ProtocolOptions<PackT> {

    private final ValueComparator<PackT> comparator;
    private final PackResult defaultStatus;
    private final PackBehavior behavior;
    private final boolean send;
    private final boolean clear;
    private final boolean remove;
    private final int minProtocol;

    @NotNull
    public static <T> ProtocolOptions<T> of(@NotNull ProtocolState state, @NotNull Function<String, ValueComparator<T>> provider) {
        final TinySettings config = OneTimePack.SETTINGS;
        final String path = "protocol." + state.name().toLowerCase() + ".";
        final String def = "protocol.default.";
        return new ProtocolOptions<T>(
                ValueComparator.read(config.getString(path + "comparator", config.getString(def + "comparator", "!UUID OR !HASH OR URL")), provider),
                PackResult.of(config.getString(path + "default-status", config.getString(def + "default-status", "none")), null),
                PackBehavior.of(config.getString(path + "behavior", config.getString(def + "behavior", "OVERRIDE"))),
                config.getBoolean(path + "send", config.getBoolean(def + "send", false)),
                config.getBoolean(path + "clear", config.getBoolean(def + "clear", false)),
                config.getBoolean(path + "remove", config.getBoolean(def + "remove", false)),
                config.getInt(path + "min-protocol", config.getInt(def + "min-protocol", -1))
        );
    }

    public ProtocolOptions(@NotNull ValueComparator<PackT> comparator, @Nullable PackResult defaultStatus, @NotNull PackBehavior behavior, boolean send, boolean clear, boolean remove, int minProtocol) {
        this.comparator = comparator;
        this.defaultStatus = defaultStatus;
        this.behavior = behavior;
        this.send = send;
        this.clear = clear;
        this.remove = remove;
        this.minProtocol = minProtocol;
    }

    @NotNull
    public ValueComparator<PackT> getComparator() {
        return comparator;
    }

    @Nullable
    public PackResult getDefaultStatus() {
        return defaultStatus;
    }

    @NotNull
    public PackBehavior getBehavior() {
        return behavior;
    }

    public int getMinProtocol() {
        return minProtocol;
    }

    public boolean sendDuplicated() {
        return send;
    }

    public boolean allowClear() {
        return clear;
    }

    public boolean allowRemove() {
        return remove;
    }
}