package com.saicone.onetimepack.core;

import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.saicone.onetimepack.OneTimePack;
import com.saicone.onetimepack.core.packet.ResourcePackPush;
import com.saicone.onetimepack.core.packet.ResourcePackStatus;
import com.saicone.onetimepack.module.TinySettings;
import com.saicone.onetimepack.util.ValueComparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProtocolOptions {

    private final ValueComparator<ResourcePackPush> comparator;
    private final ResourcePackStatus.Result defaultStatus;
    private final PackBehavior behavior;
    private final boolean send;
    private final boolean clear;
    private final boolean remove;

    @NotNull
    public static ProtocolOptions of(@NotNull ConnectionState state) {
        final TinySettings config = OneTimePack.SETTINGS;
        final String path = "protocol." + state.name().toLowerCase() + ".";
        final String def = "protocol.default.";
        return new ProtocolOptions(
                ValueComparator.read(config.getString(path + "comparator", config.getString(def + "comparator", "!UUID OR !HASH OR URL")), PacketHandler::getPackComparator),
                ResourcePackStatus.Result.of(config.getString(path + "default-status", config.getString(def + "default-status", "none")), null),
                PackBehavior.of(config.getString(path + "behavior", config.getString(def + "behavior", "OVERRIDE"))),
                config.getBoolean(path + "send", config.getBoolean(def + "send", false)),
                config.getBoolean(path + "clear", config.getBoolean(def + "clear", false)),
                config.getBoolean(path + "remove", config.getBoolean(def + "remove", false))
        );
    }

    public ProtocolOptions(@NotNull ValueComparator<ResourcePackPush> comparator, @Nullable ResourcePackStatus.Result defaultStatus, @NotNull PackBehavior behavior, boolean send, boolean clear, boolean remove) {
        this.comparator = comparator;
        this.defaultStatus = defaultStatus;
        this.behavior = behavior;
        this.send = send;
        this.clear = clear;
        this.remove = remove;
    }

    @NotNull
    public ValueComparator<ResourcePackPush> getComparator() {
        return comparator;
    }

    @Nullable
    public ResourcePackStatus.Result getDefaultStatus() {
        return defaultStatus;
    }

    @NotNull
    public PackBehavior getBehavior() {
        return behavior;
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
