package com.saicone.onetimepack.core;

import com.saicone.onetimepack.OneTimePack;
import com.saicone.onetimepack.util.ValueComparator;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.player.ServerResourcePackRemoveEvent;
import com.velocitypowered.api.event.player.ServerResourcePackSendEvent;
import com.velocitypowered.api.event.player.configuration.PlayerEnteredConfigurationEvent;
import com.velocitypowered.api.network.ProtocolState;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class VelocityProcessor extends Processor<Player, ResourcePackInfo, ProtocolState> {

    private static final PlayerResourcePackStatusEvent.Status[] VALUES = PlayerResourcePackStatusEvent.Status.values();

    private final ProxyServer proxy;
    private final Object plugin;

    public VelocityProcessor(@NotNull ProxyServer proxy, @NotNull Object plugin) {
        this.proxy = proxy;
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        proxy.getEventManager().register(plugin, this);
    }

    @Subscribe
    public void onEnterConfiguration(PlayerEnteredConfigurationEvent event) {
        if (!isSendCached1_20_2() || event.player().getProtocolVersion().greaterThan(ProtocolVersion.MINECRAFT_1_20_2)) {
            return;
        }
        final UUID uuid = event.player().getUniqueId();
        if (getUsers().containsKey(uuid)) {
            OneTimePack.log(4, "The cached pack will be send for player due it's on configuration state");
            for (var entry : getUsers().get(uuid).getPacks().entrySet()) {
                event.player().sendResourcePackOffer(entry.getValue());
            }
            OneTimePack.log(4, "Sent!");
        }
    }

    @Subscribe
    public void onPackSend(ServerResourcePackSendEvent event) {
        final Player player = event.getServerConnection().getPlayer();
        final ResourcePackInfo info = event.getProvidedResourcePack();

        final Optional<PackResult> optional = onPackPush(player, player.getProtocolState(), info, info.getId(), info.getHash());
        if (optional == null) return;

        event.setResult(ResultedEvent.GenericResult.denied());

        final PackResult result = optional.orElse(null);
        if (result == null) return;

        // Async operation
        proxy.getScheduler().buildTask(plugin, () -> {
            proxy.getEventManager().fireAndForget(new PlayerResourcePackStatusEvent(player, info.getId(), VALUES[result.ordinal()], info));
            OneTimePack.log(4, () -> "Sent cached result " + result.name() + " from user " + player.getUniqueId());
        }).schedule();
    }

    @Subscribe
    public void onPackRemove(ServerResourcePackRemoveEvent event) {
        final Player player = event.getServerConnection().getPlayer();
        if (onPackPop(player, player.getProtocolState(), event, event.getPackId())) {
            event.setResult(ResultedEvent.GenericResult.denied());
        }
    }

    @Subscribe
    public void onPackStatus(PlayerResourcePackStatusEvent event) {
        onPackStatus(event.getPlayer(), event.getPackId(), event.getStatus());
    }

    @Override
    public @NotNull ProtocolOptions<ResourcePackInfo> getOptions(@NotNull ProtocolState state) {
        if (state == ProtocolState.CONFIGURATION) {
            return getConfigurationOptions();
        } else {
            return getPlayOptions();
        }
    }

    @Override
    protected @NotNull PacketUser<ResourcePackInfo> getPacketUser(@NotNull Player player) {
        var packetUser = getUsers().get(player.getUniqueId());
        if (packetUser == null) {
            packetUser = new PacketUser<>(player.getUniqueId(), player.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_20_3));
            getUsers().put(player.getUniqueId(), packetUser);
        }
        return packetUser;
    }

    @Override
    protected @Nullable ValueComparator<ResourcePackInfo> getPackValue(@NotNull String name) {
        return switch (name) {
            case "UUID" -> ResourcePackInfo::getId;
            case "URL" -> ResourcePackInfo::getUrl;
            case "HASH" -> ResourcePackInfo::getHash;
            case "PROMPT" -> ResourcePackInfo::getPrompt;
            case "ALL" -> pack -> pack;
            case "ANY" -> pack -> true;
            default -> null;
        };
    }

    @Override
    public void clearPackets(@NotNull Player player, @NotNull ProtocolState state) {
        player.clearResourcePacks();
    }
}
