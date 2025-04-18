package com.saicone.onetimepack.core;

import com.saicone.onetimepack.OneTimePack;
import com.saicone.onetimepack.module.TinySettings;
import com.saicone.onetimepack.util.ValueComparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProtocolOptions<PackT> {

    private final boolean enabled;
    private final ValueComparator<PackT> comparator;
    private final PackResult defaultStatus;
    private final PackBehavior behavior;
    private final boolean send;
    private final boolean clear;
    private final boolean remove;
    private final int minProtocol;

    @NotNull
    public static <T> ProtocolOptions<T> valueOf(@NotNull ProtocolState state, @NotNull ValueComparator.Provider<T> provider) {
        return valueOf(new String[] {
                "protocol." + state.name().toLowerCase() + ".",
                "protocol.default."
        }, provider);
    }

    @NotNull
    public static <T> ProtocolOptions<T> valueOf(@NotNull String group, @NotNull ProtocolState state, @NotNull ValueComparator.Provider<T> provider) {
        return valueOf(new String[] {
                "group." + group + ".protocol." + state.name().toLowerCase() + ".",
                "group." + group + ".protocol.default.",
                "protocol." + state.name().toLowerCase() + ".",
                "protocol.default."
        }, provider);
    }

    @NotNull
    public static <T> ProtocolOptions<T> valueOf(@NotNull String[] paths, @NotNull ValueComparator.Provider<T> provider) {
        final TinySettings config = OneTimePack.SETTINGS;
        return new ProtocolOptions<>(
                config.getRecursively(paths, (section, path) -> section.getBoolean(path + "enabled"), true),
                ValueComparator.read(config.getRecursively(paths, (section, path) -> section.getString(path + "comparator"), "!UUID OR !HASH OR URL"), provider),
                PackResult.of(config.getRecursively(paths, (section, path) -> section.getString(path + "default-status"), "none"), null),
                PackBehavior.of(config.getRecursively(paths, (section, path) -> section.getString(path + "behavior"), "OVERRIDE")),
                config.getRecursively(paths, (section, path) -> section.getBoolean(path + "send"), false),
                config.getRecursively(paths, (section, path) -> section.getBoolean(path + "clear"), false),
                config.getRecursively(paths, (section, path) -> section.getBoolean(path + "remove"), false),
                config.getRecursively(paths, (section, path) -> section.getInt(path + "min-protocol"), -1)
        );
    }

    public ProtocolOptions(boolean enabled, @NotNull ValueComparator<PackT> comparator, @Nullable PackResult defaultStatus, @NotNull PackBehavior behavior, boolean send, boolean clear, boolean remove, int minProtocol) {
        this.enabled = enabled;
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

    public boolean isEnabled() {
        return enabled;
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