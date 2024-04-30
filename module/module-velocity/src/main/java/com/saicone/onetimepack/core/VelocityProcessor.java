package com.saicone.onetimepack.core;

import com.saicone.onetimepack.OneTimePack;
import com.saicone.onetimepack.util.ValueComparator;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.player.ServerResourcePackSendEvent;
import com.velocitypowered.api.network.ProtocolState;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class VelocityProcessor extends Processor<Player, ResourcePackInfo> {

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
    public void onPackSend(ServerResourcePackSendEvent event) {
        final ResourcePackInfo info = event.getProvidedResourcePack();

        final byte[] hash = info.getHash();
        if (hash == null) {
            if (isSendInvalid()) {
                OneTimePack.log(4, "The packet doesn't contains HASH, but invalid packs are allowed");
            } else {
                OneTimePack.log(4, "Invalid packet HASH received, so will be cancelled");
                event.setResult(ResultedEvent.GenericResult.denied());
                return;
            }
        }

        final Player player = event.getServerConnection().getPlayer();
        final ProtocolState state = player.getProtocolState();
        final ProtocolOptions<ResourcePackInfo> options = getOptions(state);

        final var user = getPacketUser(player);
        final UUID packId;
        if (!options.sendDuplicated() && (packId = user.contains(info, options)) != null) {
            OneTimePack.log(4, "Same resource pack received for user: " + user.getUniqueId());
            // Async operation
            proxy.getScheduler().buildTask(plugin, () -> {
                final var result = user.getResult(packId, options);
                if (result != null) {
                    proxy.getEventManager().fireAndForget(new PlayerResourcePackStatusEvent(player, info.getId(), VALUES[result.ordinal()], info));
                    if (OneTimePack.getLogLevel() >= 4) {
                        OneTimePack.log(4, "Sent cached result " + result + " from user " + user.getUniqueId());
                    }
                } else {
                    OneTimePack.log(2, "The user " + user.getUniqueId() + " doesn't have any cached resource pack status");
                }
            }).schedule();
            event.setResult(ResultedEvent.GenericResult.denied());
            return;
        }

        // Apply pack behavior for +1.20.3 client
        if (!user.isUniquePack() && !user.getPacks().isEmpty()) {
            OneTimePack.log(4, "Applying " + options.getBehavior().name() + " behavior...");
            if (options.getBehavior() == PackBehavior.OVERRIDE) {
                user.clear();
                player.clearResourcePacks();
            }
        }

        user.putPack(info.getId(), info);
        OneTimePack.log(4, "Save packet on " + state.name() + " protocol for user " + user.getUniqueId());
    }

    @Subscribe
    public void onPackStatus(PlayerResourcePackStatusEvent event) {
        getPacketUser(event.getPlayer()).putResult(event.getPackId(), event.getStatus());
        OneTimePack.log(4, "Saved cached result " + event.getStatus() + " from user " + event.getPlayer().getUniqueId());
    }

    private ProtocolOptions<ResourcePackInfo> getOptions(@NotNull ProtocolState state) {
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
}
