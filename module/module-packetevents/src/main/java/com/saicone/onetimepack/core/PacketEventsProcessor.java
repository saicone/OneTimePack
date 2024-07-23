package com.saicone.onetimepack.core;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.saicone.onetimepack.OneTimePack;
import com.saicone.onetimepack.core.packet.ResourcePackPop;
import com.saicone.onetimepack.core.packet.ResourcePackPush;
import com.saicone.onetimepack.core.packet.ResourcePackStatus;
import com.saicone.onetimepack.util.ValueComparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PacketEventsProcessor extends Processor<User, ResourcePackPush, ConnectionState> implements PacketListener {

    @Override
    public void onEnable() {
        // IDK why is this enabled by default
        PacketEvents.getAPI().getSettings().debug(false);
        PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.LOWEST);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.RESOURCE_PACK_STATUS || event.getPacketType() == PacketType.Configuration.Client.RESOURCE_PACK_STATUS) {
            final ResourcePackStatus packet = new ResourcePackStatus(event);
            onPackStatus(event.getUser(), packet.getUniqueId(), packet.getResult());
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.CONFIGURATION_START) {
            if (!isSendCached1_20_2() || event.getUser().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_3)) {
                return;
            }
            final UUID uuid = event.getUser().getUUID();
            if (getUsers().containsKey(uuid)) {
                OneTimePack.log(4, "The cached pack will be send for player due it's on configuration state");
                event.getPostTasks().add(() -> {
                    if (event.isCancelled()) return;
                    for (Map.Entry<UUID, ResourcePackPush> entry : getUsers().get(uuid).getPacks().entrySet()) {
                        final ResourcePackPush packet = entry.getValue().as(event.getUser().getConnectionState());
                        packet.setServerVersion(event.getUser().getClientVersion().toServerVersion());
                        event.getUser().sendPacket(packet);
                    }
                    OneTimePack.log(4, "Sent!");
                });
            }
        } else if (event.getPacketType() == PacketType.Configuration.Server.RESOURCE_PACK_SEND) {
            onPackPush(event, ConnectionState.CONFIGURATION);
        } else if (event.getPacketType() == PacketType.Play.Server.RESOURCE_PACK_SEND) {
            onPackPush(event, ConnectionState.PLAY);
        } else if (event.getPacketType() == PacketType.Configuration.Server.RESOURCE_PACK_REMOVE) {
            final ResourcePackPop packet = new ResourcePackPop(event);
            event.setCancelled(onPackPop(event.getUser(), ConnectionState.CONFIGURATION, packet, packet.getUniqueId()));
        } else if (event.getPacketType() == PacketType.Play.Server.RESOURCE_PACK_REMOVE) {
            final ResourcePackPop packet = new ResourcePackPop(event);
            event.setCancelled(onPackPop(event.getUser(), ConnectionState.PLAY, packet, packet.getUniqueId()));
        }
    }

    protected void onPackPush(@NotNull PacketSendEvent event, @NotNull ConnectionState state) {
        final ResourcePackPush packet = new ResourcePackPush(event);
        final Optional<PackResult> optional = onPackPush(event.getUser(), state, packet, packet.getUniqueId(), packet.getHash());
        if (optional == null) return;

        event.setCancelled(true);

        final PackResult result = optional.orElse(null);
        if (result == null || true) return;

        final ResourcePackStatus cached = event.getUser().getClientVersion().isOlderThan(ClientVersion.V_1_20_3)
                ? new ResourcePackStatus(packet.getHash(), result)
                : new ResourcePackStatus(packet.getState(), packet.getUniqueId(), result);
        cached.setServerVersion(packet.getServerVersion());
        event.getUser().writePacket(cached);
        OneTimePack.log(4, () -> "Sent cached result " + cached + " from user " + event.getUser().getUUID());
    }

    @Override
    public @NotNull ProtocolOptions<ResourcePackPush> getOptions(@NotNull ConnectionState state) {
        if (state == ConnectionState.CONFIGURATION) {
            return getConfigurationOptions();
        } else {
            return getPlayOptions();
        }
    }

    @Override
    protected @NotNull PacketUser<ResourcePackPush> getPacketUser(@NotNull User user) {
        PacketUser<ResourcePackPush> packetUser = getUsers().get(user.getUUID());
        if (packetUser == null) {
            packetUser = new PacketUser<>(user.getUUID(), user.getClientVersion().isOlderThan(ClientVersion.V_1_20_3));
            getUsers().put(user.getUUID(), packetUser);
        }
        return packetUser;
    }

    @Override
    protected @Nullable ValueComparator<ResourcePackPush> getPackValue(@NotNull String name) {
        switch (name) {
            case "UUID":
                return ResourcePackPush::getUniqueId;
            case "URL":
                return ResourcePackPush::getUrl;
            case "HASH":
                return ResourcePackPush::getHash;
            case "PROMPT":
                return ResourcePackPush::getPrompt;
            case "ALL":
                return pack -> pack;
            case "ANY":
                return pack -> true;
            default:
                return null;
        }
    }

    @Override
    public void clearPackets(@NotNull User user, @NotNull ConnectionState state) {
        user.sendPacket(new ResourcePackPop(state, false, null));
    }
}
