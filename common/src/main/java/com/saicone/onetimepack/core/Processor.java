package com.saicone.onetimepack.core;

import com.saicone.onetimepack.OneTimePack;
import com.saicone.onetimepack.util.ValueComparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class Processor<UserT, PackT> {

    private ProtocolOptions<PackT> playOptions;
    private ProtocolOptions<PackT> configurationOptions;

    private boolean sendCached1_20_2 = false;
    private boolean sendInvalid = false;

    private final Map<UUID, PacketUser<PackT>> users = new HashMap<>();

    public void onLoad() {
        // empty default method
    }

    public void onEnable() {
        // empty default method
    }

    public void onDisable() {
        // empty default method
    }

    public void load() {
        onLoad();
        reload();
    }

    public void enable() {
        onEnable();
    }

    public void disable() {
        onDisable();
        clear();
    }

    public void reload() {
        playOptions = ProtocolOptions.of(ProtocolState.PLAY, this::getPackComparator);
        if (playOptions.allowClear()) {
            OneTimePack.log(2, "The resource pack clear was allowed to be used on PLAY protocol, " +
                    "take in count this option may generate problems with < 1.20.3 servers using ViaVersion");
        }
        if (playOptions.allowRemove()) {
            OneTimePack.log(2, "The resource pack remove was allowed to be used on PLAY protocol, " +
                    "take in count this option may generate problems with servers using ItemsAdder");
        }
        configurationOptions = ProtocolOptions.of(ProtocolState.CONFIGURATION, this::getPackComparator);
        sendCached1_20_2 = OneTimePack.SETTINGS.getBoolean("experimental.send-cached-1-20-2", false);
        if (sendCached1_20_2) {
            OneTimePack.log(2, "The cached resource pack was allowed to be re-sended to 1.20.2 clients, " +
                    "take in count this option will make 1.20.2 players to re-download resource pack on server switch");
        }
        sendInvalid = OneTimePack.SETTINGS.getBoolean("experimental.send-invalid", false);
        if (sendInvalid) {
            OneTimePack.log(3, "Invalid packs will be send to players");
        }
    }

    public boolean isSendCached1_20_2() {
        return sendCached1_20_2;
    }

    public boolean isSendInvalid() {
        return sendInvalid;
    }

    @NotNull
    public ProtocolOptions<PackT> getOptions(@NotNull ProtocolState protocol) {
        return protocol == ProtocolState.CONFIGURATION ? getConfigurationOptions() : getPlayOptions();
    }

    @NotNull
    public ProtocolOptions<PackT> getPlayOptions() {
        return playOptions;
    }

    @NotNull
    public ProtocolOptions<PackT> getConfigurationOptions() {
        return configurationOptions;
    }

    @NotNull
    public Map<UUID, PacketUser<PackT>> getUsers() {
        return users;
    }

    @NotNull
    protected abstract PacketUser<PackT> getPacketUser(@NotNull UserT user);

    @Nullable
    protected ValueComparator<PackT> getPackComparator(@NotNull String input) {
        final boolean nonNull = input.charAt(0) == '!';
        final ValueComparator<PackT> comparator = getPackValue((nonNull ? input.substring(1) : input).toUpperCase());
        if (comparator == null) {
            OneTimePack.log(2, "The pack comparator '" + input + "' is not valid");
            return null;
        }
        return nonNull ? comparator.nonNull() : comparator;
    }

    @Nullable
    protected abstract ValueComparator<PackT> getPackValue(@NotNull String name);

    public void clear() {
        OneTimePack.log(4, "The data from packet handler was cleared");
        for (Map.Entry<UUID, PacketUser<PackT>> entry : users.entrySet()) {
            entry.getValue().clear();
        }
        users.clear();
    }

    public void clear(@NotNull UUID uuid) {
        OneTimePack.log(4, "Removing data from user " + uuid);
        final PacketUser<PackT> player = users.remove(uuid);
        if (player != null) {
            player.clear();
        }
    }
}
