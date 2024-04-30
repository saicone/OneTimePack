package com.saicone.onetimepack.core;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
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
import java.util.UUID;

public class PacketEventsProcessor extends Processor<User, ResourcePackPush> implements PacketListener {

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
            getPacketUser(event.getUser()).putResult(packet.getUniqueId(), packet.getResult());
            OneTimePack.log(4, "Saved cached result " + packet + " from user " + event.getUser().getUUID());
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
                        packet.setClientVersion(event.getUser().getClientVersion());
                        event.getUser().sendPacket(packet);
                    }
                    OneTimePack.log(4, "Sent!");
                });
            }
        } else if (event.getPacketType() == PacketType.Configuration.Server.RESOURCE_PACK_SEND) {
            onPackPush(event, new ResourcePackPush(event), getConfigurationOptions());
        } else if (event.getPacketType() == PacketType.Play.Server.RESOURCE_PACK_SEND) {
            onPackPush(event, new ResourcePackPush(event), getPlayOptions());
        } else if (event.getPacketType() == PacketType.Configuration.Server.RESOURCE_PACK_REMOVE) {
            onPackPop(event, new ResourcePackPop(event), getConfigurationOptions());
        } else if (event.getPacketType() == PacketType.Play.Server.RESOURCE_PACK_REMOVE) {
            onPackPop(event, new ResourcePackPop(event), getPlayOptions());
        }
    }

    private void onPackPush(@NotNull PacketSendEvent event, @NotNull ResourcePackPush packet, @NotNull ProtocolOptions<ResourcePackPush> options) {
        if (OneTimePack.getLogLevel() >= 4) {
            OneTimePack.log(4, "Received ResourcePackPush: " + packet);
        }

        final String hash = packet.getHash();
        // Avoid invalid resource pack sending
        if (String.valueOf(hash).equalsIgnoreCase("null")) {
            if (isSendInvalid()) {
                OneTimePack.log(4, "The packet doesn't contains HASH, but invalid packs are allowed");
            } else {
                OneTimePack.log(4, "Invalid packet HASH received, so will be cancelled");
                event.setCancelled(true);
                return;
            }
        }

        final PacketUser<ResourcePackPush> user = getPacketUser(event.getUser());
        final UUID packId;
        if (!options.sendDuplicated() && (packId = user.contains(packet, options)) != null) {
            OneTimePack.log(4, "Same resource pack received for user: " + user.getUniqueId());
            // Async operation
            new Thread(() -> {
                final PackResult result = user.getResult(packId, options);
                if (result != null) {
                    final ResourcePackStatus cached = user.isUniquePack()
                            ? new ResourcePackStatus(packet.getHash(), result)
                            : new ResourcePackStatus(packet.getState(), packet.getUniqueId(), result);
                    cached.setClientVersion(packet.getClientVersion());
                    event.getUser().writePacket(cached);
                    if (OneTimePack.getLogLevel() >= 4) {
                        OneTimePack.log(4, "Sent cached result " + cached + " from user " + user.getUniqueId());
                    }
                } else {
                    OneTimePack.log(2, "The user " + user.getUniqueId() + " doesn't have any cached resource pack status");
                }
            }).start();
            event.setCancelled(true);
            return;
        }

        // Apply pack behavior for +1.20.3 client
        if (!user.isUniquePack() && !user.getPacks().isEmpty()) {
            OneTimePack.log(4, "Applying " + options.getBehavior().name() + " behavior...");
            if (options.getBehavior() == PackBehavior.OVERRIDE) {
                user.clear();
                event.getUser().sendPacket(new ResourcePackPop(packet.getState(), false, null));
            }
        }

        user.putPack(packet.getUniqueId(), packet);
        OneTimePack.log(4, "Save packet on " + packet.getState().name() + " protocol for user " + user.getUniqueId());
    }

    private void onPackPop(@NotNull PacketSendEvent event, @NotNull ResourcePackPop packet, @NotNull ProtocolOptions<ResourcePackPush> options) {
        if (!options.allowClear() && !packet.hasUniqueId()) {
            OneTimePack.log(4, "Cancelling packs clear from " + packet.getState().name() + " protocol for user " + event.getUser().getUUID());
            event.setCancelled(true);
            return;
        }
        if (!options.allowRemove()) {
            OneTimePack.log(4, "Cancelling pack remove from " + packet.getState().name() + " protocol for user " + event.getUser().getUUID());
            event.setCancelled(true);
            return;
        }
        getPacketUser(event.getUser()).removePack(packet.getUniqueId());
        OneTimePack.log(4, "Remove cached packet using " + packet + " from user " + event.getUser().getUUID());
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
}
