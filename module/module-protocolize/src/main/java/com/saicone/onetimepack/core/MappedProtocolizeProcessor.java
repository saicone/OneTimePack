package com.saicone.onetimepack.core;

import com.saicone.onetimepack.OneTimePack;
import com.saicone.onetimepack.core.packet.ResourcePackPop;
import com.saicone.onetimepack.core.packet.ResourcePackPush;
import com.saicone.onetimepack.core.packet.ResourcePackStatus;
import com.saicone.onetimepack.module.Mappings;
import com.saicone.onetimepack.util.ValueComparator;
import dev.simplix.protocolize.api.Direction;
import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.Protocol;
import dev.simplix.protocolize.api.mapping.ProtocolIdMapping;
import dev.simplix.protocolize.api.packet.AbstractPacket;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class MappedProtocolizeProcessor<StartT> extends ProtocolizeProcessor<StartT, ResourcePackPush, ResourcePackStatus> {

    public MappedProtocolizeProcessor(@NotNull Class<StartT> startConfigurationClass) {
        super(startConfigurationClass);
    }

    @Override
    public void onLoad() {
        final Mappings mappings = new Mappings(OneTimePack.get().getProvider().getPluginFolder(), "mappings.json");
        mappings.load();
        register(mappings, ResourcePackPush.class, ResourcePackPush::register);
        register(mappings, ResourcePackPop.class, ResourcePackPop::register);
        register(mappings, ResourcePackStatus.class, ResourcePackStatus::register);
    }

    @Override
    protected void registerListeners() {
        getPacketListener().registerReceive(ResourcePackPush.Configuration.class, Direction.DOWNSTREAM, event -> {
            final ResourcePackPush packet = event.packet();
            onPackPush(event, Protocol.CONFIGURATION, packet.getUniqueId(), packet.getHash());
        });
        getPacketListener().registerReceive(ResourcePackPush.Play.class, Direction.DOWNSTREAM, event -> {
            final ResourcePackPush packet = event.packet();
            onPackPush(event, Protocol.PLAY, packet.getUniqueId(), packet.getHash());
        });
        getPacketListener().registerReceive(ResourcePackPop.Configuration.class, Direction.DOWNSTREAM, event -> {
            final ResourcePackPop packet = event.packet();
            event.cancelled(onPackPop(event.player(), Protocol.CONFIGURATION, packet, packet.getUniqueId()));
        });
        getPacketListener().registerReceive(ResourcePackPop.Play.class, Direction.DOWNSTREAM, event -> {
            final ResourcePackPop packet = event.packet();
            event.cancelled(onPackPop(event.player(), Protocol.PLAY, packet, packet.getUniqueId()));
        });
        getPacketListener().registerReceive(ResourcePackStatus.Configuration.class, Direction.UPSTREAM, event -> {
            final ResourcePackStatus packet = event.packet();
            onPackStatus(event.player(), packet.getUniqueId(), packet.getResult());
        });
        getPacketListener().registerReceive(ResourcePackStatus.Play.class, Direction.UPSTREAM, event -> {
            final ResourcePackStatus packet = event.packet();
            onPackStatus(event.player(), packet.getUniqueId(), packet.getResult());
        });
    }

    @Override
    public @NotNull ProtocolOptions<ResourcePackPush> getOptions(@NotNull Protocol protocol) {
        if (protocol == Protocol.CONFIGURATION) {
            return getConfigurationOptions();
        } else {
            return getPlayOptions();
        }
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
    protected @NotNull ResourcePackPush getPushPacket(@NotNull ResourcePackPush packet) {
        return packet.asConfiguration();
    }

    @Override
    protected @NotNull ResourcePackStatus getStatusPacket(@NotNull Protocol protocol, @NotNull ResourcePackPush packet, @NotNull PackResult result) {
        if (protocol == Protocol.CONFIGURATION) {
            return new ResourcePackStatus.Configuration(packet.getUniqueId(), result);
        } else {
            if (packet.getUniqueId() == null) {
                return new ResourcePackStatus.Play(packet.getHash(), result);
            } else {
                return new ResourcePackStatus.Play(packet.getUniqueId(), result);
            }
        }
    }

    private <T extends AbstractPacket> void register(@NotNull Mappings mappings, @NotNull Class<T> clazz, @NotNull Consumer<Function<String, List<ProtocolIdMapping>>> consumer) {
        if (!mappings.contains(clazz.getSimpleName())) {
            consumer.accept(null);
        } else {
            consumer.accept(protocol -> mappings.getMappings(clazz.getSimpleName(), protocol));
        }
    }

    @Override
    public void clearPackets(@NotNull ProtocolizePlayer player, @NotNull Protocol protocol) {
        final ResourcePackPop clearPacket = protocol == Protocol.CONFIGURATION ? new ResourcePackPop.Configuration() : new ResourcePackPop.Play();
        if (protocol == Protocol.CONFIGURATION) {
            player.sendPacket(getWrappedPacket(
                    clearPacket,
                    Protocol.CONFIGURATION,
                    PacketDirection.CLIENTBOUND,
                    player.protocolVersion()
            ));
        } else {
            player.sendPacket(clearPacket);
        }
    }
}
