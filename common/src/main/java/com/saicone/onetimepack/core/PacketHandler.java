package com.saicone.onetimepack.core;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Configuration;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.saicone.onetimepack.OneTimePack;
import com.saicone.onetimepack.core.packet.ResourcePackPop;
import com.saicone.onetimepack.core.packet.ResourcePackPush;
import com.saicone.onetimepack.core.packet.ResourcePackStatus;
import com.saicone.onetimepack.util.ValueComparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PacketHandler implements PacketListener {

    private ProtocolOptions playOptions;
    private ProtocolOptions configurationOptions;

    private boolean sendCached1_20_2 = false;
    private boolean sendInvalid = false;

    private final Map<UUID, PacketUser> users = new HashMap<>();

    public void onEnable() {
        PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.LOWEST);
        onReload();
    }

    public void onDisable() {
        clear();
    }

    public void onReload() {
        playOptions = ProtocolOptions.of(ConnectionState.PLAY);
        if (playOptions.allowClear()) {
            OneTimePack.log(2, "The resource pack clear was allowed to be used on PLAY protocol, " +
                    "take in count this option may generate problems with < 1.20.3 servers using ViaVersion");
        }
        if (playOptions.allowRemove()) {
            OneTimePack.log(2, "The resource pack remove was allowed to be used on PLAY protocol, " +
                    "take in count this option may generate problems with servers using ItemsAdder");
        }
        configurationOptions = ProtocolOptions.of(ConnectionState.CONFIGURATION);
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

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == Play.Client.RESOURCE_PACK_STATUS || event.getPacketType() == Configuration.Client.RESOURCE_PACK_STATUS) {
            final ResourcePackStatus packet = new ResourcePackStatus(event);
            getPacketUser(event.getUser()).add(packet);
            OneTimePack.log(4, "Saved cached result " + packet + " from user " + event.getUser().getUUID());
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == Play.Server.CONFIGURATION_START) {
            if (!sendCached1_20_2 || event.getUser().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_3)) {
                return;
            }
            final UUID uuid = event.getUser().getUUID();
            if (users.containsKey(uuid)) {
                OneTimePack.log(4, "The cached pack will be send for player due it's on configuration state");
                event.getPostTasks().add(() -> {
                    if (event.isCancelled()) return;
                    for (Map.Entry<UUID, ResourcePackPush> entry : users.get(uuid).getPacks().entrySet()) {
                        event.getUser().sendPacket(entry.getValue().as(PacketEvents.getAPI().getProtocolManager().getUser(event.getChannel()).getConnectionState()));
                    }
                    OneTimePack.log(4, "Sent!");
                });
            }
        } else if (event.getPacketType() == Configuration.Server.RESOURCE_PACK_SEND) {
            onPackPush(event, new ResourcePackPush(event), configurationOptions);
        } else if (event.getPacketType() == Play.Server.RESOURCE_PACK_SEND) {
            onPackPush(event, new ResourcePackPush(event), playOptions);
        } else if (event.getPacketType() == Configuration.Server.RESOURCE_PACK_REMOVE) {
            onPackPop(event, new ResourcePackPop(event), configurationOptions);
        } else if (event.getPacketType() == Play.Server.RESOURCE_PACK_REMOVE) {
            onPackPop(event, new ResourcePackPop(event), playOptions);
        }
    }

    private void onPackPush(@NotNull PacketSendEvent event, @NotNull ResourcePackPush packet, @NotNull ProtocolOptions options) {
        if (OneTimePack.getLogLevel() >= 4) {
            OneTimePack.log(4, "Received ResourcePackPush: " + packet);
        }

        final String hash = packet.getHash();
        // Avoid invalid resource pack sending
        if (String.valueOf(hash).equalsIgnoreCase("null")) {
            if (sendInvalid) {
                OneTimePack.log(4, "The packet doesn't contains HASH, but invalid packs are allowed");
            } else {
                OneTimePack.log(4, "Invalid packet HASH received, so will be cancelled");
                event.setCancelled(true);
                return;
            }
        }

        final PacketUser user = getPacketUser(event.getUser());
        if (!options.sendDuplicated() && user.contains(packet, options)) {
            OneTimePack.log(4, "Same resource pack received for user: " + user.getUniqueId());
            // Async operation
            new Thread(() -> {
                final ResourcePackStatus cached = user.getResult(packet, options);
                if (cached == null) {
                    return;
                }
                // TODO: Test packet sending to server
                PacketEvents.getAPI().getProtocolManager().sendPacket(event.getChannel(), cached.as(packet.getState()));
                if (OneTimePack.getLogLevel() >= 4) {
                    OneTimePack.log(4, "Sent cached result " + cached + " from user " + user.getUniqueId());
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

        user.add(packet);
        OneTimePack.log(4, "Save packet on " + packet.getState().name() + " protocol for user " + user.getUniqueId());
    }

    private void onPackPop(@NotNull PacketSendEvent event, @NotNull ResourcePackPop packet, @NotNull ProtocolOptions options) {
        if (!options.allowClear() && !packet.hasUniqueId()) {
            OneTimePack.log(4, "Cancelling packs clear from " + packet.getState().name() + " protocol for user " + event.getUser().getUUID());
            event.setCancelled(users.containsKey(event.getUser().getUUID()));
            return;
        }
        if (!options.allowRemove()) {
            OneTimePack.log(4, "Cancelling pack remove from " + packet.getState().name() + " protocol for user " + event.getUser().getUUID());
            event.setCancelled(users.containsKey(event.getUser().getUUID()));
            return;
        }
        getPacketUser(event.getUser()).remove(packet);
        OneTimePack.log(4, "Remove cached packet using " + packet + " from user " + event.getUser().getUUID());
    }

    @NotNull
    public Map<UUID, PacketUser> getUsers() {
        return users;
    }

    @NotNull
    public PacketUser getPacketUser(@NotNull User user) {
        PacketUser packetUser = users.get(user.getUUID());
        if (packetUser == null) {
            packetUser = new PacketUser(user);
            users.put(user.getUUID(), packetUser);
        }
        return packetUser;
    }

    @Nullable
    public static ValueComparator<ResourcePackPush> getPackComparator(@NotNull String name) {
        final boolean nonNull = name.charAt(0) == '!';
        final ValueComparator<ResourcePackPush> comparator;
        switch ((nonNull ? name.substring(1) : name).toUpperCase()) {
            case "UUID":
                comparator = ResourcePackPush::getUniqueId;
                break;
            case "URL":
                comparator = ResourcePackPush::getUrl;
                break;
            case "HASH":
                comparator = ResourcePackPush::getHash;
                break;
            case "PROMPT":
                comparator = ResourcePackPush::getPrompt;
                break;
            case "ALL":
                comparator = pack -> pack;
                break;
            case "ANY":
                return pack -> true;
            default:
                OneTimePack.log(2, "The pack comparator '" + name + "' is not valid");
                return null;
        }
        return nonNull ? comparator.nonNull() : comparator;
    }

    public void clear() {
        OneTimePack.log(4, "The data from packet handler was cleared");
        for (Map.Entry<UUID, PacketUser> entry : users.entrySet()) {
            entry.getValue().clear();
        }
        users.clear();
    }

    public void clear(@NotNull UUID uuid) {
        OneTimePack.log(4, "Removing data from user " + uuid);
        final PacketUser player = users.remove(uuid);
        if (player != null) {
            player.clear();
        }
    }
}
