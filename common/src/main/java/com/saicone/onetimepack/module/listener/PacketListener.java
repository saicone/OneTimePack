package com.saicone.onetimepack.module.listener;

import com.saicone.onetimepack.OneTimePack;
import dev.simplix.protocolize.api.Direction;
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.listener.AbstractPacketListener;
import dev.simplix.protocolize.api.listener.PacketReceiveEvent;
import dev.simplix.protocolize.api.listener.PacketSendEvent;
import dev.simplix.protocolize.api.packet.AbstractPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PacketListener {

    private final Map<ListenerKey, Listener<?>> listeners = new HashMap<>();

    public <T extends AbstractPacket> void registerReceive(@NotNull Class<T> packet, @NotNull Direction direction, @NotNull Consumer<PacketReceiveEvent<T>> consumer) {
        registerReceive(packet, direction, 0, consumer);
    }

    public <T extends AbstractPacket> void registerReceive(@NotNull Class<T> packet, @NotNull Direction direction, int priority, @NotNull Consumer<PacketReceiveEvent<T>> consumer) {
        final Listener<T> listener = computeListener(packet, direction, priority);
        listener.setOnReceive(consumer);
        register(listener);
    }

    public <T extends AbstractPacket> void registerSend(@NotNull Class<T> packet, @NotNull Direction direction, @NotNull Consumer<PacketSendEvent<T>> consumer) {
        registerSend(packet, direction, 0, consumer);
    }

    public <T extends AbstractPacket> void registerSend(@NotNull Class<T> packet, @NotNull Direction direction, int priority, @NotNull Consumer<PacketSendEvent<T>> consumer) {
        final Listener<T> listener = computeListener(packet, direction, priority);
        listener.setOnSend(consumer);
        register(listener);
    }

    private void register(@NotNull Listener<?> listener) {
        if (!listener.isRegistered()) {
            listener.setRegistered(true);
            Protocolize.listenerProvider().registerListener(listener);
            OneTimePack.log(4, "The listener of " + listener.type().getSimpleName() + " was registered in direction " + listener.direction().name());
        } else {
            OneTimePack.log(4, "The listener of " + listener.type().getSimpleName() + " is registered in direction " + listener.direction().name());
        }
    }

    public void unregister() {
        OneTimePack.log(4, "The listeners was unregistered");
        for (Map.Entry<ListenerKey, Listener<?>> entry : listeners.entrySet()) {
            if (entry.getValue().isRegistered()) {
                Protocolize.listenerProvider().unregisterListener(entry.getValue());
            }
        }
        clear();
    }

    public void unregister(@NotNull Class<?> packet) {
        listeners.entrySet().removeIf(entry -> {
            if (entry.getKey().getClazz() == packet) {
                Protocolize.listenerProvider().unregisterListener(entry.getValue());
                return true;
            } else {
                return false;
            }
        });
    }

    public void unregister(@NotNull Class<?> packet, @NotNull Direction direction) {
        listeners.entrySet().removeIf(entry -> {
            if (entry.getKey().getClazz() == packet && entry.getKey().getDirection() == direction) {
                Protocolize.listenerProvider().unregisterListener(entry.getValue());
                return true;
            } else {
                return false;
            }
        });
    }

    public void unregister(@NotNull Class<?> packet, int priority) {
        listeners.entrySet().removeIf(entry -> {
            if (entry.getKey().getClazz() == packet && entry.getKey().getPriority() == priority) {
                Protocolize.listenerProvider().unregisterListener(entry.getValue());
                return true;
            } else {
                return false;
            }
        });
    }

    public void unregister(@NotNull Class<?> packet, @NotNull Direction direction, int priority) {
        final ListenerKey key = new ListenerKey(packet, direction, priority);
        final Listener<?> listener = listeners.get(key);
        if (listener != null) {
            Protocolize.listenerProvider().unregisterListener(listener);
            listeners.remove(key);
        }
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private <T> Listener<T> computeListener(@NotNull Class<T> packet, @NotNull Direction direction, int priority) {
        final ListenerKey key = new ListenerKey(packet, direction, priority);
        if (!listeners.containsKey(key)) {
            listeners.put(key, new Listener<>(packet, direction, priority));
        }
        return (Listener<T>) listeners.get(key);
    }

    public void clear() {
        listeners.clear();
    }

    private static class ListenerKey {

        private final Class<?> clazz;
        private final Direction direction;
        private final int priority;

        public ListenerKey(@NotNull Class<?> clazz, @NotNull Direction direction, int priority) {
            this.clazz = clazz;
            this.direction = direction;
            this.priority = priority;
        }

        @NotNull
        public Class<?> getClazz() {
            return clazz;
        }

        @NotNull
        public Direction getDirection() {
            return direction;
        }

        public int getPriority() {
            return priority;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ListenerKey that = (ListenerKey) o;

            if (priority != that.priority) return false;
            if (!clazz.equals(that.clazz)) return false;
            return direction == that.direction;
        }

        @Override
        public int hashCode() {
            int result = clazz.hashCode();
            result = 31 * result + direction.hashCode();
            result = 31 * result + priority;
            return result;
        }
    }

    private static class Listener<T> extends AbstractPacketListener<T> {

        private boolean registered = false;
        private Consumer<PacketReceiveEvent<T>> onReceive;
        private Consumer<PacketSendEvent<T>> onSend;

        public Listener(@NotNull Class<T> packet, @NotNull Direction direction) {
            this(packet, direction, 0);
        }

        public Listener(@NotNull Class<T> packet, @NotNull Direction direction, int priority) {
            super(packet, direction, priority);
        }

        public boolean isRegistered() {
            return registered;
        }

        @Nullable
        public Consumer<PacketReceiveEvent<T>> getOnReceive() {
            return onReceive;
        }

        @Nullable
        public Consumer<PacketSendEvent<T>> getOnSend() {
            return onSend;
        }

        public void setRegistered(boolean registered) {
            this.registered = registered;
        }

        public void setOnReceive(@NotNull Consumer<PacketReceiveEvent<T>> onReceive) {
            this.onReceive = onReceive;
        }

        public void setOnSend(@NotNull Consumer<PacketSendEvent<T>> onSend) {
            this.onSend = onSend;
        }

        @Override
        public void packetReceive(PacketReceiveEvent<T> event) {
            if (onReceive != null) {
                onReceive.accept(event);
            }
        }

        @Override
        public void packetSend(PacketSendEvent<T> event) {
            if (onSend != null) {
                onSend.accept(event);
            }
        }
    }
}
