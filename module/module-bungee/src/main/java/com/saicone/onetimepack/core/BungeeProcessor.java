package com.saicone.onetimepack.core;

import com.saicone.onetimepack.OneTimePack;
import com.saicone.onetimepack.core.packet.ResourcePackPop;
import com.saicone.onetimepack.core.packet.ResourcePackPush;
import com.saicone.onetimepack.core.packet.ResourcePackStatus;
import com.saicone.onetimepack.module.Mappings;
import com.saicone.onetimepack.util.ProtocolVersion;
import com.saicone.onetimepack.util.ValueComparator;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.CancelSendSignal;
import net.md_5.bungee.connection.DownstreamBridge;
import net.md_5.bungee.connection.UpstreamBridge;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.ProtocolConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class BungeeProcessor extends Processor<ProxiedPlayer, ResourcePackPush, ProtocolState> {

    private static final MethodHandle DOWNSTREAM_CON;
    private static final MethodHandle UPSTREAM_CON;

    private static final MethodHandle NEW_PROTOCOL_MAPPING;
    private static final MethodHandle REGISTER_PACKET;

    static {
        MethodHandle downstream$con = null;
        MethodHandle upstream$con = null;

        MethodHandle new$$ProtocolMapping = null;
        MethodHandle registerPacket = null;

        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();

            final Field field$downstream = DownstreamBridge.class.getDeclaredField("con");
            field$downstream.setAccessible(true);
            downstream$con = lookup.unreflectGetter(field$downstream);

            final Field field$upstream = UpstreamBridge.class.getDeclaredField("con");
            field$upstream.setAccessible(true);
            upstream$con = lookup.unreflectGetter(field$upstream);


            final Class<?> ProtocolMapping = Class.forName(Protocol.class.getName() + "$ProtocolMapping");

            final Constructor<?> constructor$ProtocolMapping = ProtocolMapping.getDeclaredConstructor(int.class, int.class);
            constructor$ProtocolMapping.setAccessible(true);
            new$$ProtocolMapping = lookup.unreflectConstructor(constructor$ProtocolMapping);

            final Method method$registerPacket = Protocol.DirectionData.class.getDeclaredMethod("registerPacket", Class.class, Supplier.class, ProtocolMapping.arrayType());
            method$registerPacket.setAccessible(true);
            registerPacket = lookup.unreflect(method$registerPacket);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        DOWNSTREAM_CON = downstream$con;
        UPSTREAM_CON = upstream$con;

        NEW_PROTOCOL_MAPPING = new$$ProtocolMapping;
        REGISTER_PACKET = registerPacket;
    }

    private Mappings<IntRangeEntry<Integer>> mappings;

    @Override
    public void onLoad() {
        // Fix non-existent protocol versions
        ProtocolVersion.getProtocols().put("1.11.2", ProtocolConstants.MINECRAFT_1_11_1);
        ProtocolVersion.getProtocols().put("1.16.5", ProtocolConstants.MINECRAFT_1_16_4);
        ProtocolVersion.getProtocols().put("1.19.2", ProtocolConstants.MINECRAFT_1_19_1);

        mappings = new Mappings<>(OneTimePack.get().getProvider().getPluginFolder(), "mappings.json", IntRangeEntry::new);
        mappings.load();
        onLoad("ResourcePackSend", ResourcePackPush.class, ResourcePackPush::new, true);
        onLoad("ResourcePackRemove", ResourcePackPop.class, ResourcePackPop::new, true);
        onLoad("ResourcePackStatus", ResourcePackStatus.class, ResourcePackStatus::new, false);
    }

    protected void onLoad(@NotNull String name, @NotNull Class<? extends DefinedPacket> clazz, @NotNull Supplier<? extends DefinedPacket> constructor, boolean clientbound) {
        if (!mappings.contains(name)) {
            OneTimePack.log(1, "Cannot find mappings for " + clazz.getName() + " this will cause the plugin to not work correctly");
            return;
        }

        try {
            final List<IntRangeEntry<Integer>> play = mappings.getMappings(name, "play");
            if (play == null) {
                OneTimePack.log(1, "Cannot find PLAY mappings for " + clazz.getName() + " this will cause the plugin to not work correctly");
            } else {
                onLoad(play, clientbound ? Protocol.GAME.TO_CLIENT : Protocol.GAME.TO_SERVER, clazz, constructor);
            }

            final List<IntRangeEntry<Integer>> configuration = mappings.getMappings(name, "configuration");
            if (configuration == null) {
                OneTimePack.log(1, "Cannot find CONFIGURATION mappings for " + clazz.getName() + " this will cause the plugin to not work correctly");
            } else {
                onLoad(configuration, clientbound ? Protocol.CONFIGURATION.TO_CLIENT : Protocol.CONFIGURATION.TO_SERVER, clazz, constructor);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    protected void onLoad(@NotNull List<IntRangeEntry<Integer>> entries, @NotNull Protocol.DirectionData direction, @NotNull Class<? extends DefinedPacket> clazz, @NotNull Supplier<? extends DefinedPacket> constructor) throws Throwable {
        final Object[] args = new Object[entries.size() + 3];
        args[0] = direction;
        args[1] = clazz;
        args[2] = constructor;
        int index = 3;
        for (IntRangeEntry<Integer> range : entries) {
            final int protocol = range.getMin();
            args[index] = NEW_PROTOCOL_MAPPING.invoke(protocol, range.getValue());
            index++;
        }

        REGISTER_PACKET.invokeWithArguments(args);
    }

    public void onPackPush(@NotNull ResourcePackPush packet, @NotNull AbstractPacketHandler handler) {
        final ProxiedPlayer player = getPlayer(handler);
        if (player == null) return;

        final Optional<PackResult> optional = onPackPush(player, state(packet.getProtocol()), packet, packet.getUniqueId(), packet.getHash());
        if (optional == null) return;

        final PackResult result = optional.orElse(null);
        if (result == null) {
            throw CancelSendSignal.INSTANCE;
        }

        player.getServer().unsafe().sendPacket(packet.asStatus(result, player.getPendingConnection().getVersion()));
        OneTimePack.log(4, () -> "Sent cached result " + result.name() + " from user " + player.getUniqueId());

        throw CancelSendSignal.INSTANCE;
    }

    public void onPackPop(@NotNull ResourcePackPop packet, @NotNull AbstractPacketHandler handler) throws Exception {
        final ProxiedPlayer player = getPlayer(handler);
        if (player != null && onPackPop(player, state(packet.getProtocol()), packet, packet.getUniqueId())) {
            throw CancelSendSignal.INSTANCE;
        }
    }

    public void onPackStatus(@NotNull ResourcePackStatus packet, @NotNull AbstractPacketHandler handler) throws Exception {
        final ProxiedPlayer player = getPlayer(handler);
        if (player != null) {
            onPackStatus(player, packet.getUniqueId(), packet.getResult());
        }
    }

    @NotNull
    private ProtocolState state(@NotNull Protocol protocol) {
        // For some reason Bungeecord use a different protocol ordinal values
        return switch (protocol) {
            case HANDSHAKE -> ProtocolState.HANDSHAKING;
            case GAME -> ProtocolState.PLAY;
            case STATUS -> ProtocolState.STATUS;
            case LOGIN -> ProtocolState.LOGIN;
            case CONFIGURATION -> ProtocolState.CONFIGURATION;
        };
    }

    @Nullable
    private ProxiedPlayer getPlayer(@NotNull AbstractPacketHandler handler) {
        if (handler instanceof DownstreamBridge) {
            try {
                return (ProxiedPlayer) DOWNSTREAM_CON.invoke(handler);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        } else if (handler instanceof UpstreamBridge) {
            try {
                return (ProxiedPlayer) UPSTREAM_CON.invoke(handler);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        } else {
            OneTimePack.log(2, "Received packed on invalid handler: " + handler.getClass());
            return null;
        }
    }

    @Override
    protected @NotNull UUID getUserId(@NotNull ProxiedPlayer user) {
        return user.getUniqueId();
    }

    @Override
    protected @Nullable ValueComparator<ResourcePackPush> getPackValue(@NotNull String name) {
        return switch (name) {
            case "UUID" -> ResourcePackPush::getUniqueId;
            case "URL" -> ResourcePackPush::getUrl;
            case "HASH" -> ResourcePackPush::getHash;
            case "PROMPT" -> pack -> pack.getPrompt() == null ? null : pack.getJsonPrompt();
            case "ALL" -> pack -> pack;
            case "ANY" -> pack -> true;
            default -> null;
        };
    }

    @Override
    public void clearPackets(@NotNull ProxiedPlayer user, @NotNull ProtocolState state) {
        user.unsafe().sendPacket(new ResourcePackPop(false, null));
    }

    public static final class IntRangeEntry<T> implements Iterable<Integer> {

        private final int min;
        private final int max;
        private final T value;

        public IntRangeEntry(int min, int max, @NotNull T value) {
            this.min = min;
            this.max = max;
            this.value = value;
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        @NotNull
        public T getValue() {
            return value;
        }

        @Override
        public @NotNull Iterator<Integer> iterator() {
            return new Iterator<>() {
                private int current = min;

                @Override
                public boolean hasNext() {
                    return current <= max;
                }

                @Override
                public Integer next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    return current++;
                }
            };
        }
    }
}
