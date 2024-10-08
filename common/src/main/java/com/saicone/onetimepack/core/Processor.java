package com.saicone.onetimepack.core;

import com.saicone.onetimepack.OneTimePack;
import com.saicone.onetimepack.util.ValueComparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public abstract class Processor<UserT, PackT, StateT extends Enum<StateT>> {

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

    @Nullable
    protected Optional<PackResult> onPackPush(@NotNull UserT userType, @NotNull StateT state, @NotNull PackT packet, @Nullable UUID id, @Nullable Object hash) {
        return onPackPush(userType, state, packet, id, hash, getOptions(state));
    }

    @Nullable
    protected Optional<PackResult> onPackPush(@NotNull UserT userType, @NotNull StateT state, @NotNull PackT packet, @Nullable UUID id, @Nullable Object hash, @NotNull ProtocolOptions<PackT> options) {
        OneTimePack.log(4, () -> "Received " + packet.getClass().getSimpleName() + ": " + packet);

        // Avoid invalid resource pack sending
        if (hash == null || String.valueOf(hash).equalsIgnoreCase("null")) {
            if (isSendInvalid()) {
                OneTimePack.log(4, "The packet doesn't contains HASH, but invalid packs are allowed");
            } else {
                OneTimePack.log(4, "Invalid packet HASH received, so will be cancelled");
                return Optional.empty();
            }
        }

        final PacketUser<PackT> user = getPacketUser(userType);
        // Check protocol restrictions
        if (user.getProtocolVersion() < options.getMinProtocol()) {
            OneTimePack.log(2, "The user " + user.getUniqueId() + " doesn't meet the minimum protocol requirement");
            return Optional.ofNullable(options.getDefaultStatus());
        }

        // Cancel resource pack re-sending to player
        final UUID packId;
        if (!options.sendDuplicated() && (packId = user.contains(packet, options)) != null) {
            OneTimePack.log(4, "Same resource pack received for user: " + user.getUniqueId());
            // Re-send to server the actual resource pack status from player
            final PackResult result = user.getResult(packId, options);
            if (result == null) {
                OneTimePack.log(2, "The user " + user.getUniqueId() + " doesn't have any cached resource pack status");
            }
            return Optional.ofNullable(result);
        }

        // Apply pack behavior for +1.20.3 client
        if (!user.isUniquePack() && !user.getPacks().isEmpty()) {
            OneTimePack.log(4, "Applying " + options.getBehavior().name() + " behavior...");
            if (options.getBehavior() == PackBehavior.OVERRIDE) {
                user.clear();
                clearPackets(userType, state);
            }
        }

        user.putPack(id, packet);
        OneTimePack.log(4, "Save packet on " + state.name() + " protocol for user " + user.getUniqueId());
        return null;
    }

    protected boolean onPackPop(@NotNull UserT userType, @NotNull StateT state, @NotNull Object packet, @Nullable UUID id) {
        return onPackPop(userType, state, packet, id, getOptions(state));
    }

    protected boolean onPackPop(@NotNull UserT userType, @NotNull StateT state, @NotNull Object packet, @Nullable UUID id, @NotNull ProtocolOptions<PackT> options) {
        if (!options.allowClear() && id == null) {
            OneTimePack.log(4, () -> "Cancelling packs clear from " + state.name() + " protocol for player " + getPacketUser(userType).getUniqueId());
            return true;
        }
        if (!options.allowRemove()) {
            OneTimePack.log(4, () -> "Cancelling pack remove from " + state.name() + " protocol for player " + getPacketUser(userType).getUniqueId());
            return true;
        }
        final PacketUser<PackT> user = getPacketUser(userType);
        user.removePack(id);
        OneTimePack.log(4, () -> "Remove cached packet using: " + packet + " from player " + user.getUniqueId());
        return false;
    }

    protected <E extends Enum<E>> void onPackStatus(@NotNull UserT userType, @Nullable UUID id, @NotNull E result) {
        final PacketUser<PackT> user = getPacketUser(userType);
        user.putResult(id, result);
        OneTimePack.log(4, () -> "Saved cached result " + result + " from player " + user.getUniqueId());
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
    public abstract ProtocolOptions<PackT> getOptions(@NotNull StateT state);

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

    public abstract void clearPackets(@NotNull UserT user, @NotNull StateT state);
}
